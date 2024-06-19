package mega.privacy.android.shared.original.core.ui.controls.camera

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class CameraBottomAppBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that bottom app bar shows correctly when isRecording as false`() {
        composeTestRule.setContent {
            CameraBottomAppBar(
                isCaptureVideo = false,
                isRecording = false,
                rotationDegree = 0f
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_GALLERY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_CAMERA).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_ROTATE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_PHOTO).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_VIDEO).assertIsDisplayed()
    }

    @Test
    fun `test that bottom app bar shows correctly when isRecording as true `() {
        composeTestRule.setContent {
            CameraBottomAppBar(
                isCaptureVideo = true,
                isRecording = true,
                rotationDegree = 0f
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_CAMERA).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_ROTATE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_PHOTO).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_VIDEO).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_GALLERY).assertDoesNotExist()
    }

    @Test
    fun `test that onCameraAction triggers when camera button is clicked`() {
        val onCameraAction = mock<() -> Unit>()
        composeTestRule.setContent {
            CameraBottomAppBar(
                isCaptureVideo = false,
                isRecording = false,
                rotationDegree = 0f,
                onCameraAction = onCameraAction
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_CAMERA).performClick()
        verify(onCameraAction).invoke()
    }

    @Test
    fun `test that onToggleCaptureMode triggers when photo button is clicked`() {
        val onToggleCaptureMode = mock<() -> Unit>()
        composeTestRule.setContent {
            CameraBottomAppBar(
                isCaptureVideo = true,
                isRecording = false,
                rotationDegree = 0f,
                onToggleCaptureMode = onToggleCaptureMode
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_PHOTO).performClick()
        verify(onToggleCaptureMode).invoke()
    }

    @Test
    fun `test that onToggleCaptureMode doesn't trigger when photo button is clicked and the mode is capturing photo`() {
        val onToggleCaptureMode = mock<() -> Unit>()
        composeTestRule.setContent {
            CameraBottomAppBar(
                isCaptureVideo = false,
                isRecording = false,
                rotationDegree = 0f,
                onToggleCaptureMode = onToggleCaptureMode
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_PHOTO).performClick()
        verifyNoInteractions(onToggleCaptureMode)
    }

    @Test
    fun `test that onToggleCaptureMode triggers when video button is clicked`() {
        val onToggleCaptureMode = mock<() -> Unit>()
        composeTestRule.setContent {
            CameraBottomAppBar(
                isCaptureVideo = false,
                isRecording = false,
                rotationDegree = 0f,
                onToggleCaptureMode = onToggleCaptureMode
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_VIDEO).performClick()
        verify(onToggleCaptureMode).invoke()
    }

    @Test
    fun `test that onToggleCaptureMode doesn't trigger when video button is clicked and the mode is capturing video`() {
        val onToggleCaptureMode = mock<() -> Unit>()
        composeTestRule.setContent {
            CameraBottomAppBar(
                isCaptureVideo = true,
                isRecording = false,
                rotationDegree = 0f,
                onToggleCaptureMode = onToggleCaptureMode
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_VIDEO).performClick()
        verifyNoInteractions(onToggleCaptureMode)
    }

    @Test
    fun `test that onOpenGallery triggers when gallery button is clicked`() {
        val onOpenGallery = mock<() -> Unit>()
        composeTestRule.setContent {
            CameraBottomAppBar(
                isCaptureVideo = false,
                isRecording = false,
                rotationDegree = 0f,
                onOpenGallery = onOpenGallery
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_GALLERY).performClick()
        verify(onOpenGallery).invoke()
    }

    @Test
    fun `test that onRotateCamera triggers when rotate button is clicked`() {
        val onToggleCamera = mock<() -> Unit>()
        composeTestRule.setContent {
            CameraBottomAppBar(
                isCaptureVideo = false,
                isRecording = false,
                rotationDegree = 0f,
                onToggleCamera = onToggleCamera
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_ROTATE).performClick()
        verify(onToggleCamera).invoke()
    }
}