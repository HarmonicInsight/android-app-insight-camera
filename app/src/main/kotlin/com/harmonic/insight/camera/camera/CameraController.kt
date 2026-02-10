package com.harmonic.insight.camera.camera

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class FlashMode {
    OFF, ON, AUTO
}

enum class LightMode {
    OFF, ON
}

enum class CaptureMode {
    PHOTO, VIDEO
}

enum class InsightAspectRatio(val value: Int, val label: String) {
    RATIO_4_3(AspectRatio.RATIO_4_3, "4:3"),
    RATIO_16_9(AspectRatio.RATIO_16_9, "16:9"),
}

class InsightCameraController(private val context: Context) {

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null

    var flashMode: FlashMode = FlashMode.OFF
        private set
    var lightMode: LightMode = LightMode.OFF
        private set
    var captureMode: CaptureMode = CaptureMode.PHOTO
        private set
    var aspectRatio: InsightAspectRatio = InsightAspectRatio.RATIO_4_3
        private set
    var isRecording: Boolean = false
        private set

    val hasFlashUnit: Boolean
        get() = camera?.cameraInfo?.hasFlashUnit() == true

    val maxZoomRatio: Float
        get() = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f

    val minZoomRatio: Float
        get() = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f

    val currentZoomRatio: Float
        get() = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onReady: () -> Unit = {},
        onError: (Exception) -> Unit = {},
    ) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                onReady()
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        val provider = cameraProvider ?: return

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        preview = Preview.Builder()
            .setTargetAspectRatio(aspectRatio.value)
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetAspectRatio(aspectRatio.value)
            .setFlashMode(mapFlashMode())
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        provider.unbindAll()

        camera = if (captureMode == CaptureMode.PHOTO) {
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
            )
        } else {
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture,
            )
        }

        // Restore light (torch) state after rebind
        if (lightMode == LightMode.ON) {
            camera?.cameraControl?.enableTorch(true)
        }
    }

    private fun rebind() {
        val lo = currentLifecycleOwner ?: return
        val pv = currentPreviewView ?: return
        bindCameraUseCases(lo, pv)
    }

    // --- Focus ---

    fun focusOnPoint(x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
        val cam = camera ?: return
        val factory = SurfaceOrientedMeteringPointFactory(
            viewWidth.toFloat(),
            viewHeight.toFloat(),
        )
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    // --- Photo ---

    fun takePhoto(
        onSuccess: (String) -> Unit,
        onError: (ImageCaptureException) -> Unit,
    ) {
        val capture = imageCapture ?: return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val fileName = "IMG_${timestamp}"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/InsightCamera")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues,
        ).build()

        capture.flashMode = mapFlashMode()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    val uri = result.savedUri?.toString() ?: ""
                    Log.d(TAG, "Photo saved: $uri")
                    onSuccess(uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    onError(exception)
                }
            },
        )
    }

    // --- Video ---

    @androidx.annotation.OptIn(androidx.camera.video.ExperimentalPersistentRecording::class)
    fun startVideoRecording(
        withAudio: Boolean,
        onStarted: () -> Unit = {},
        onFinished: (String) -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        val vc = videoCapture ?: return
        if (isRecording) return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val fileName = "VID_${timestamp}"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/InsightCamera")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).setContentValues(contentValues).build()

        var pendingRecording = vc.output.prepareRecording(context, outputOptions)
        if (withAudio) {
            pendingRecording = pendingRecording.withAudioEnabled()
        }

        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    isRecording = true
                    onStarted()
                }
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    if (event.hasError()) {
                        Log.e(TAG, "Video recording error: ${event.cause?.message}")
                        onError(event.cause?.message ?: "Unknown error")
                    } else {
                        val uri = event.outputResults.outputUri.toString()
                        Log.d(TAG, "Video saved: $uri")
                        onFinished(uri)
                    }
                }
            }
        }
    }

    fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    // --- Mode switching ---

    fun setCaptureMode(mode: CaptureMode) {
        if (captureMode == mode) return
        if (isRecording) stopVideoRecording()
        captureMode = mode
        rebind()
    }

    fun setAspectRatio(ratio: InsightAspectRatio) {
        if (aspectRatio == ratio) return
        aspectRatio = ratio
        rebind()
    }

    // --- Zoom ---

    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(minZoomRatio, maxZoomRatio))
    }

    fun setLinearZoom(fraction: Float) {
        camera?.cameraControl?.setLinearZoom(fraction.coerceIn(0f, 1f))
    }

    // --- Flash / Light ---

    fun cycleFlashMode(): FlashMode {
        flashMode = when (flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        imageCapture?.flashMode = mapFlashMode()
        return flashMode
    }

    fun toggleLight(): LightMode {
        lightMode = when (lightMode) {
            LightMode.OFF -> LightMode.ON
            LightMode.ON -> LightMode.OFF
        }
        camera?.cameraControl?.enableTorch(lightMode == LightMode.ON)
        return lightMode
    }

    // --- Camera switch ---

    fun switchCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        if (lensFacing == CameraSelector.LENS_FACING_FRONT && lightMode == LightMode.ON) {
            lightMode = LightMode.OFF
        }
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView
        bindCameraUseCases(lifecycleOwner, previewView)
    }

    val isBackCamera: Boolean
        get() = lensFacing == CameraSelector.LENS_FACING_BACK

    fun release() {
        if (isRecording) stopVideoRecording()
        cameraProvider?.unbindAll()
    }

    private fun mapFlashMode(): Int = when (flashMode) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }

    companion object {
        private const val TAG = "InsightCamera"
    }
}
