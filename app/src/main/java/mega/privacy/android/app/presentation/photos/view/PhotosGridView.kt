@file:OptIn(ExperimentalFoundationApi::class)

package mega.privacy.android.app.presentation.photos.view

import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.icon.pack.R as IconPackR
import android.content.res.Configuration
import android.text.format.DateFormat.getBestDateTimePattern
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.util.DATE_FORMAT_DAY
import mega.privacy.android.app.presentation.photos.util.DATE_FORMAT_MONTH
import mega.privacy.android.app.presentation.photos.util.DATE_FORMAT_MONTH_WITH_DAY
import mega.privacy.android.app.presentation.photos.util.DATE_FORMAT_YEAR_WITH_MONTH
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_032
import mega.privacy.android.shared.original.core.ui.theme.white
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@Composable
internal fun PhotosGridView(
    photoDownland: PhotoDownload,
    selectedPhotoIds: Set<Long>,
    uiPhotoList: List<UIPhoto>,
    modifier: Modifier = Modifier,
    accountType: AccountType? = null,
    currentZoomLevel: ZoomLevel = ZoomLevel.Grid_3,
    endSpacing: Dp = 56.dp,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
    isBlurUnselectItem: Boolean = false,
    separatorRightPlaceHolderView: @Composable () -> Unit = {},
    showSeparatorRightView: (index: Int) -> Boolean = { false },
) {

    val configuration = LocalConfiguration.current
    val spanCount = remember(configuration.orientation, currentZoomLevel) {
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            currentZoomLevel.portrait
        } else {
            currentZoomLevel.landscape
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(spanCount),
        modifier = modifier
            .fillMaxSize(),
        state = lazyGridState,
    ) {
        this.itemsIndexed(
            items = uiPhotoList,
            key = { _, item ->
                item.key
            },
            span = { _, item ->
                if (item is UIPhoto.Separator)
                    GridItemSpan(maxLineSpan)
                else GridItemSpan(1)
            },
        ) { index, item ->

            when (item) {
                is UIPhoto.Separator -> {
                    Separator(
                        currentZoomLevel = currentZoomLevel,
                        modificationTime = item.modificationTime,
                        separatorRightPlaceHolderView = separatorRightPlaceHolderView,
                        showSeparatorRightView = showSeparatorRightView(index)
                    )
                }

                is UIPhoto.PhotoItem -> {
                    val isSelected = item.photo.id in selectedPhotoIds
                    PhotoViewContainer(
                        photo = item.photo,
                        isSelected = isSelected,
                        currentZoomLevel = currentZoomLevel,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { onClick(item.photo) },
                                onLongClick = { onLongPress(item.photo) }
                            ),
                        photoView = {
                            PhotoView(
                                photo = item.photo,
                                isPreview = isDownloadPreview(configuration, currentZoomLevel),
                                downloadPhoto = photoDownland,
                                alpha = if (isBlurUnselectItem && !isSelected) 0.4f else 1.0f,
                                accountType = accountType,
                            )
                        }
                    )
                }
            }
        }

        item(span = { GridItemSpan(currentLineSpan = maxLineSpan) }) {
            Spacer(
                modifier = Modifier.height(endSpacing)
            )
        }
    }
}

@Composable
internal fun PhotoViewContainer(
    photo: Photo,
    isSelected: Boolean,
    currentZoomLevel: ZoomLevel,
    modifier: Modifier = Modifier,
    photoView: @Composable () -> Unit,
) {
    Box(
        modifier = when (currentZoomLevel) {
            ZoomLevel.Grid_1 -> modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)

            ZoomLevel.Grid_3 -> modifier
                .fillMaxSize()
                .padding(all = 1.5.dp)

            ZoomLevel.Grid_5 -> modifier
                .fillMaxSize()
                .padding(all = 1.dp)
        }.isSelected(isSelected)
    ) {

        photoView()

        if (photo.isFavourite) {
            if (photo is Photo.Image) {
                Image(
                    painter = painterResource(id = R.drawable.ic_overlay),
                    contentScale = ContentScale.FillBounds,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_favourite_white),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                tint = Color.Unspecified
            )
        }
        if (photo is Photo.Video) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = grey_alpha_032)
            )

            Text(
                text = TimeUtils.getVideoDuration(photo.fileTypeInfo.duration.inWholeSeconds.toInt()),
                color = white,
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (isSelected) {
            Icon(
                painter = painterResource(id = CoreUiR.drawable.ic_select_folder),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
internal fun PhotoView(
    photo: Photo,
    isPreview: Boolean,
    accountType: AccountType?,
    downloadPhoto: PhotoDownload,
    alpha: Float = DefaultAlpha,
) {
    val imageState = produceState<String?>(initialValue = null) {
        downloadPhoto(isPreview, photo) { downloadSuccess ->
            if (downloadSuccess) {
                value = if (isPreview) {
                    photo.previewFilePath
                } else {
                    photo.thumbnailFilePath
                }
            }
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageState.value)
            .crossfade(true)
            .build(),
        alpha = alpha,
        contentDescription = null,
        placeholder = painterResource(id = IconPackR.drawable.ic_image_medium_solid),
        error = painterResource(id = IconPackR.drawable.ic_image_medium_solid),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .alpha(0.5f.takeIf {
                accountType?.isPaid == true && (photo.isSensitive || photo.isSensitiveInherited)
            } ?: 1f)
            .blur(16.dp.takeIf {
                accountType?.isPaid == true && (photo.isSensitive || photo.isSensitiveInherited)
            } ?: 0.dp)
    )
}


@Composable
internal fun Separator(
    currentZoomLevel: ZoomLevel,
    modificationTime: LocalDateTime,
    separatorRightPlaceHolderView: @Composable () -> Unit = {},
    showSeparatorRightView: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dateText(
                currentZoomLevel = currentZoomLevel,
                modificationTime = modificationTime,
                locale = LocalContext.current.resources.configuration.locales[0],
            ),
            textAlign = TextAlign.Start,
            color = colorResource(id = R.color.grey_087_white_087),
            modifier = Modifier
                .wrapContentSize()
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp)
        )
        if (showSeparatorRightView) {
            separatorRightPlaceHolderView()
        }
    }
}

private fun dateText(
    currentZoomLevel: ZoomLevel,
    modificationTime: LocalDateTime,
    locale: Locale,
): String {
    val datePattern = if (currentZoomLevel == ZoomLevel.Grid_1) {
        if (modificationTime.year == LocalDateTime.now().year) {
            getBestDateTimePattern(locale, "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH_WITH_DAY")
        } else {
            getBestDateTimePattern(
                locale,
                "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH_WITH_DAY $DATE_FORMAT_YEAR_WITH_MONTH"
            )
        }
    } else {
        if (modificationTime.year == LocalDateTime.now().year) {
            getBestDateTimePattern(locale, DATE_FORMAT_MONTH)
        } else {
            getBestDateTimePattern(locale, "$DATE_FORMAT_MONTH $DATE_FORMAT_YEAR_WITH_MONTH")
        }
    }
    return SimpleDateFormat(
        datePattern,
        locale
    ).format(
        Date.from(
            modificationTime
                .toLocalDate()
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )
    )
}

fun Modifier.photosZoomGestureDetector(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
) = then(pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent(
                pass = PointerEventPass.Initial
            )
            val canceled = event.changes.any { it.isConsumed }
            val zoomChange = event.calculateZoom()
            if (zoomChange != 1.0f) {
                if (zoomChange > 1.0f) {
                    onZoomIn()
                } else {
                    onZoomOut()
                }
                // Consume event in case to trigger scroll
                event.changes.map { it.consume() }
                break
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
})
