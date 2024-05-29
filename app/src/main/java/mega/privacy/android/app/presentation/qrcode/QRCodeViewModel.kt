package mega.privacy.android.app.presentation.qrcode

import android.content.Context
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.middlelayer.scanner.ScannerHandler
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapper
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.qrcode.mapper.MyQRCodeTextErrorMapper
import mega.privacy.android.app.presentation.qrcode.model.QRCodeUIState
import mega.privacy.android.app.presentation.qrcode.model.ScanResult
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyCodeUIState
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.usecase.UploadUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.CopyToClipBoard
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.qr.GetQRCodeFileUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceUseCase
import mega.privacy.android.domain.usecase.qrcode.CreateContactLinkUseCase
import mega.privacy.android.domain.usecase.qrcode.DeleteQRCodeUseCase
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLinkUseCase
import mega.privacy.android.domain.usecase.qrcode.ResetContactLinkUseCase
import mega.privacy.android.domain.usecase.qrcode.ScanMediaFileUseCase
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for QRCodeActivity
 */
@HiltViewModel
class QRCodeViewModel @Inject constructor(
    private val copyToClipBoard: CopyToClipBoard,
    private val createContactLinkUseCase: CreateContactLinkUseCase,
    private val getQRCodeFileUseCase: GetQRCodeFileUseCase,
    private val deleteQRCodeUseCase: DeleteQRCodeUseCase,
    private val resetContactLinkUseCase: ResetContactLinkUseCase,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val getUserFullNameUseCase: GetUserFullNameUseCase,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val queryScannedContactLinkUseCase: QueryScannedContactLinkUseCase,
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase,
    private val avatarContentMapper: AvatarContentMapper,
    private val myQRCodeTextErrorMapper: MyQRCodeTextErrorMapper,
    private val scannerHandler: ScannerHandler,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val doesPathHaveSufficientSpaceUseCase: DoesPathHaveSufficientSpaceUseCase,
    private val scanMediaFileUseCase: ScanMediaFileUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val uploadUseCase: UploadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QRCodeUIState())

    /**
     * UI state
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Create QR code.
     */
    fun createQRCode(showLoader: Boolean) {
        Timber.d("create QR code")
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(myQRCodeState = MyCodeUIState.CreatingQRCode(showLoader)) }
                createContactLinkUseCase(renew = false)
            }.fold(
                onSuccess = { contactLink ->
                    val fullName = getUserFullNameUseCase(forceRefresh = true)
                    val avatarBgColor = getMyAvatarColorUseCase()
                    val localFile = runCatching { getMyAvatarFileUseCase(true) }.getOrNull()

                    val avatarContent =
                        avatarContentMapper(
                            fullName = fullName,
                            localFile = localFile,
                            showBorder = true,
                            textSize = 38.sp,
                            backgroundColor = avatarBgColor,
                        )
                    _uiState.update {
                        it.copy(
                            myQRCodeState = MyCodeUIState.QRCodeAvailable(
                                contactLink = contactLink,
                                avatarBgColor = avatarBgColor,
                                avatarContent = avatarContent,
                                qrCodeFilePath = getQRCodeFileUseCase()?.absolutePath,
                            )
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(myQRCodeState = MyCodeUIState.Idle) }
                }
            )
        }
    }

    /**
     * Reset QR code
     */
    fun resetQRCode() {
        Timber.d("reset QR code")
        viewModelScope.launch {
            val qrCodeBackup = qrCodeBackup()
            runCatching {
                _uiState.update { it.copy(myQRCodeState = MyCodeUIState.CreatingQRCode()) }
                resetContactLinkUseCase()
            }.fold(
                onSuccess = { contactLink ->
                    _uiState.update { it.copy(myQRCodeState = MyCodeUIState.QRCodeResetDone) }
                    val fullName = getUserFullNameUseCase(forceRefresh = true)
                    val avatarBgColor = getMyAvatarColorUseCase()
                    val avatarContent = avatarContentMapper(
                        fullName = fullName,
                        localFile = runCatching { getMyAvatarFileUseCase(true) }.getOrNull(),
                        showBorder = true,
                        textSize = 38.sp,
                        backgroundColor = getMyAvatarColorUseCase(),
                    )
                    _uiState.update {
                        it.copy(
                            myQRCodeState = MyCodeUIState.QRCodeAvailable(
                                contactLink = contactLink,
                                avatarBgColor = avatarBgColor,
                                avatarContent = avatarContent,
                                qrCodeFilePath = getQRCodeFileUseCase()?.absolutePath,
                            )
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update {
                        it.copy(myQRCodeState = MyCodeUIState.Error(myQRCodeTextErrorMapper(error)))
                    }
                    qrCodeBackup?.let { _uiState.update { it.copy(myQRCodeState = qrCodeBackup) } }
                }
            )
        }
    }

    private fun qrCodeBackup(): MyCodeUIState.QRCodeAvailable? =
        (uiState.value.myQRCodeState as? MyCodeUIState.QRCodeAvailable)?.copy()

    /**
     * Copy contact link to system clip board
     */
    fun copyContactLink() {
        Timber.d("copyLink")
        (uiState.value.myQRCodeState as? MyCodeUIState.QRCodeAvailable)?.let {
            copyToClipBoard(label = "contact link", it.contactLink)
            setResultMessage(R.string.qrcode_link_copied)
        }
    }

    /**
     * Delete QR code
     */
    fun deleteQRCode() {
        Timber.d("deleteQRCode")
        val qrCodeBackup = qrCodeBackup()
        viewModelScope.launch {
            runCatching {
                (uiState.value.myQRCodeState as? MyCodeUIState.QRCodeAvailable)?.let {
                    deleteQRCodeUseCase(contactLink = it.contactLink)
                }
            }.fold(
                onSuccess = {
                    _uiState.update { it.copy(myQRCodeState = MyCodeUIState.QRCodeDeleted) }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update {
                        it.copy(myQRCodeState = MyCodeUIState.Error(myQRCodeTextErrorMapper(error)))
                    }
                    qrCodeBackup?.let { _uiState.update { it.copy(myQRCodeState = qrCodeBackup) } }
                }
            )
        }
    }

    /**
     * Send invitation to new contact
     */
    fun sendInvite(contactHandle: Long, contactEmail: String) {
        viewModelScope.launch {
            runCatching { inviteContactWithHandleUseCase(contactEmail, contactHandle, null) }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            inviteContactResult = triggered(result),
                            scannedContactEmail = contactEmail
                        )
                    }
                }.onFailure { throwable ->
                    Timber.e(throwable)
                    _uiState.update { it.copy(inviteContactResult = triggered(InviteContactRequest.InvalidStatus)) }
                }
        }
    }

    /**
     * Query details of scanned contact and update the ui state
     *
     * @param scannedHandle Base 64 handle of the scanned qr code
     */
    fun queryContactLink(context: Context, scannedHandle: String) {
        viewModelScope.launch {
            runCatching { queryScannedContactLinkUseCase(scannedHandle) }
                .onSuccess { result ->
                    val scannedContactAvatar = avatarContentMapper(
                        fullName = result.contactName,
                        localFile = result.avatarFile,
                        backgroundColor = result.avatarColor
                            ?: context.getColor(R.color.red_600_red_300),
                        showBorder = false,
                        textSize = 36.sp
                    )
                    _uiState.update {
                        it.copy(
                            scannedContactLinkResult = triggered(result),
                            scannedContactAvatarContent = scannedContactAvatar,
                        )
                    }
                }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Start scanning of qr code
     */
    fun scanCode(context: Context) {
        viewModelScope.launch {
            runCatching { scannerHandler.scan() }
                .onSuccess { scanResult ->
                    when (scanResult) {
                        is ScanResult.Success -> {
                            val contactLink = scanResult.rawValue
                            contactLink?.let {
                                val s = contactLink.split("C!").toTypedArray()
                                if (s.size <= 1 || s[0] != Constants.SCANNED_CONTACT_BASE_URL) {
                                    setResultMessage(R.string.invalid_code)
                                } else {
                                    queryContactLink(context, s[1])
                                }
                            }
                        }

                        ScanResult.Cancel -> {
                            _uiState.update { it.copy(scanCancel = triggered) }
                        }
                    }
                }
                .onFailure { error ->
                    Timber.e(error)
                    setResultMessage(R.string.general_text_error)
                }
        }
    }

    /**
     * Save qr code file to cloud drive
     */
    fun saveToCloudDrive() {
        viewModelScope.launch {
            val parentHandle = getRootNodeUseCase()?.id?.longValue ?: return@launch
            val qrFile = getQRCodeFileUseCase()
            if (qrFile == null) {
                setResultMessage(R.string.error_upload_qr)
                return@launch
            }

            val storageState = monitorStorageStateEventUseCase.getState()
            if (storageState === StorageState.PayWall) {
                AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
                return@launch
            }

            runCatching {
                checkNameCollisionUseCase.checkNameCollision(qrFile.name, parentHandle)
            }.onSuccess { handle: Long ->
                val collision =
                    NameCollision.Upload.getUploadCollision(handle, qrFile, parentHandle)
                _uiState.update { it.copy(showCollision = triggered(collision)) }

            }.onFailure { throwable: Throwable? ->
                if (throwable is MegaNodeException.ParentDoesNotExistException) {
                    setResultMessage(R.string.error_upload_qr)
                } else if (throwable is MegaNodeException.ChildDoesNotExistsException) {
                    _uiState.update {
                        it.copy(uploadFile = triggered(Pair(qrFile, parentHandle)))
                    }
                }
            }
        }
    }

    /**
     * Save file to selected path on the device
     */
    fun saveToFileSystem(parentPath: String) {
        viewModelScope.launch {
            val myEmail = getCurrentUserEmail()
            val qrFile = getQRCodeFileUseCase()

            if (qrFile == null) {
                setResultMessage(R.string.error_download_qr)
                return@launch
            }

            if (!doesPathHaveSufficientSpaceUseCase(parentPath, qrFile.length())) {
                setResultMessage(R.string.error_not_enough_free_space)
                return@launch
            }

            val newQrFile = File(parentPath, "$myEmail$QR_IMAGE_FILE_NAME")

            // For Android 11+ device, force to refresh MediaStore. Otherwise it is possible
            // that target file cannot be written.
            if (Util.isAndroid11OrUpper()) {
                scanMediaFileUseCase(arrayOf(newQrFile.absolutePath), arrayOf(MIME_TYPE_IMAGE))
            }

            try {
                withContext(Dispatchers.IO) {
                    newQrFile.createNewFile()
                    val src = FileInputStream(qrFile).channel
                    val dst = FileOutputStream(newQrFile, false).channel
                    dst.transferFrom(src, 0, src.size())
                    src.close()
                    dst.close()
                    setResultMessage(R.string.success_download_qr, arrayOf(parentPath))
                }
            } catch (e: IOException) {
                Timber.e(e)
                setResultMessage(R.string.general_error)
            }
        }
    }

    /**
     * Set finish activity on scan complete
     */
    fun setFinishActivityOnScanComplete(finish: Boolean) {
        _uiState.update { it.copy(finishActivityOnScanComplete = finish) }
    }

    /**
     * Show result message of a operation
     *
     * @param messageId String ID of the message
     */
    private fun setResultMessage(messageId: Int, formatArgs: Array<Any> = emptyArray()) =
        _uiState.update { it.copy(resultMessage = triggered(Pair(messageId, formatArgs))) }

    /**
     * Reset and notify resultMessage is consumed
     */
    fun resetResultMessage() = _uiState.update { it.copy(resultMessage = consumed()) }

    /**
     * Reset and notify scannedContactLinkResult is consumed
     */
    fun resetScannedContactLinkResult() =
        _uiState.update { it.copy(scannedContactLinkResult = consumed()) }

    /**
     * Reset and notify inviteContactResult is consumed
     */
    fun resetInviteContactResult() = _uiState.update { it.copy(inviteContactResult = consumed()) }

    /**
     * Reset scannedContactEmail
     */
    fun resetScannedContactEmail() = _uiState.update { it.copy(scannedContactEmail = null) }

    /**
     * Reset scannedContactAvatarContent
     */
    fun resetScannedContactAvatar() =
        _uiState.update { it.copy(scannedContactAvatarContent = null) }

    /**
     * Reset and notify showCollision is consumed
     */
    fun resetShowCollision() = _uiState.update { it.copy(showCollision = consumed()) }

    /**
     * Reset and notify uploadFile is consumed
     */
    fun resetUploadFile() = _uiState.update { it.copy(uploadFile = consumed()) }

    /**
     *Reset and notify scan cancel is consumed
     */
    fun resetScanCancel() = _uiState.update { it.copy(scanCancel = consumed) }

    /**
     * Upload file to cloud drive.
     */
    fun uploadFile(context: Context, qrFile: File, parentHandle: Long) {
        viewModelScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.UploadWorker)) {
                _uiState.update { state ->
                    state.copy(
                        uploadEvent = triggered(
                            TransferTriggerEvent.StartUpload.Files(
                                listOf(qrFile.toUri()),
                                NodeId(parentHandle)
                            )
                        )
                    )
                }
            } else {
                PermissionUtils.checkNotificationsPermission(context as QRCodeComposeActivity)
                uploadUseCase.upload(context, qrFile, parentHandle)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        setResultMessage(
                            R.string.save_qr_cloud_drive,
                            arrayOf(qrFile.name)
                        )
                    }) { t: Throwable? -> Timber.e(t) }
            }
        }
    }

    /**
     * Notify upload event is consumed
     */
    fun onUploadEventConsumed() {
        _uiState.update { it.copy(uploadEvent = consumed()) }
    }

    companion object {
        private const val QR_IMAGE_FILE_NAME = "QR_code_image.jpg"
        private const val MIME_TYPE_IMAGE = "image/jpeg"
    }
}