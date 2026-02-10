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
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
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
    private var extensionsManager: ExtensionsManager? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null

    // Extensions state
    private var activeExtensionMode: Int = ExtensionMode.NONE
    private var _availableExtensions: List<Int> = emptyList()

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

    /** Whether device has both front and back cameras */
    var hasMultipleCameras: Boolean = false
        private set

    /** Human-readable name of active extension mode */
    val activeExtensionLabel: String
        get() = extensionModeLabel(activeExtensionMode)

    /** Whether any extensions are available on this device */
    val hasExtensions: Boolean
        get() = _availableExtensions.any { it != ExtensionMode.NONE }

    // --- Startup ---

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
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                // Detect available cameras
                hasMultipleCameras = hasCamera(provider, CameraSelector.LENS_FACING_BACK) &&
                    hasCamera(provider, CameraSelector.LENS_FACING_FRONT)

                // If the initially requested lens doesn't exist, fall back
                if (!hasCamera(provider, lensFacing)) {
                    lensFacing = if (hasCamera(provider, CameraSelector.LENS_FACING_BACK)) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        CameraSelector.LENS_FACING_FRONT
                    }
                }

                // Initialize extensions
                initExtensions(lifecycleOwner, previewView, onReady, onError)
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun initExtensions(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onReady: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val provider = cameraProvider ?: return
        val extensionsFuture = ExtensionsManager.getInstanceAsync(context, provider)
        extensionsFuture.addListener({
            try {
                extensionsManager = extensionsFuture.get()
                detectAvailableExtensions()

                // Try AUTO first, then HDR, then NONE
                activeExtensionMode = when {
                    isExtensionAvailable(ExtensionMode.AUTO) -> ExtensionMode.AUTO
                    isExtensionAvailable(ExtensionMode.HDR) -> ExtensionMode.HDR
                    else -> ExtensionMode.NONE
                }

                bindCameraUseCases(lifecycleOwner, previewView)
                onReady()
            } catch (e: Exception) {
                // Extensions not available, proceed without them
                Log.w(TAG, "Extensions unavailable, using basic mode", e)
                activeExtensionMode = ExtensionMode.NONE
                _availableExtensions = listOf(ExtensionMode.NONE)
                try {
                    bindCameraUseCases(lifecycleOwner, previewView)
                    onReady()
                } catch (e2: Exception) {
                    Log.e(TAG, "Camera bind failed", e2)
                    onError(e2)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun detectAvailableExtensions() {
        val mgr = extensionsManager ?: return
        val baseCameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val modes = listOf(
            ExtensionMode.AUTO,
            ExtensionMode.HDR,
            ExtensionMode.NIGHT,
            ExtensionMode.BOKEH,
            ExtensionMode.FACE_RETOUCH,
        )

        _availableExtensions = buildList {
            add(ExtensionMode.NONE) // Always available
            for (mode in modes) {
                try {
                    if (mgr.isExtensionAvailable(baseCameraSelector, mode)) {
                        add(mode)
                    }
                } catch (_: Exception) {
                    // Skip unavailable modes
                }
            }
        }

        Log.d(TAG, "Available extensions: ${_availableExtensions.map { extensionModeLabel(it) }}")
    }

    private fun isExtensionAvailable(mode: Int): Boolean =
        _availableExtensions.contains(mode)

    // --- Binding ---

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        val provider = cameraProvider ?: return

        val baseCameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // Apply extensions to camera selector if available (photo mode only)
        val cameraSelector = if (captureMode == CaptureMode.PHOTO && activeExtensionMode != ExtensionMode.NONE) {
            try {
                val mgr = extensionsManager
                if (mgr != null && mgr.isExtensionAvailable(baseCameraSelector, activeExtensionMode)) {
                    mgr.getExtensionEnabledCameraSelector(baseCameraSelector, activeExtensionMode)
                } else {
                    baseCameraSelector
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enable extension mode, falling back", e)
                activeExtensionMode = ExtensionMode.NONE
                baseCameraSelector
            }
        } else {
            baseCameraSelector
        }

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

        camera = try {
            if (captureMode == CaptureMode.PHOTO) {
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
        } catch (e: Exception) {
            // Fallback: try binding without extensions
            Log.w(TAG, "Bind failed, retrying without extensions", e)
            activeExtensionMode = ExtensionMode.NONE
            try {
                if (captureMode == CaptureMode.PHOTO) {
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        baseCameraSelector,
                        preview,
                        imageCapture,
                    )
                } else {
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        baseCameraSelector,
                        preview,
                        videoCapture,
                    )
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Camera bind completely failed", e2)
                null
            }
        }

        // Restore torch state
        if (lightMode == LightMode.ON) {
            camera?.cameraControl?.enableTorch(true)
        }
    }

    private fun rebind() {
        val lo = currentLifecycleOwner ?: return
        val pv = currentPreviewView ?: return
        try {
            bindCameraUseCases(lo, pv)
        } catch (e: Exception) {
            Log.e(TAG, "Rebind failed", e)
        }
    }

    // --- Focus ---

    fun focusOnPoint(x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
        val cam = camera ?: return
        try {
            val factory = SurfaceOrientedMeteringPointFactory(
                viewWidth.toFloat(),
                viewHeight.toFloat(),
            )
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()
            cam.cameraControl.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.w(TAG, "Focus failed", e)
        }
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

    @android.annotation.SuppressLint("MissingPermission")
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

        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            onError(e.message ?: "Failed to start recording")
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
        if (!hasMultipleCameras) return

        val previousLensFacing = lensFacing
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

        // Re-detect extensions for the new lens
        detectAvailableExtensions()
        if (!isExtensionAvailable(activeExtensionMode)) {
            activeExtensionMode = if (isExtensionAvailable(ExtensionMode.AUTO)) {
                ExtensionMode.AUTO
            } else {
                ExtensionMode.NONE
            }
        }

        try {
            bindCameraUseCases(lifecycleOwner, previewView)
        } catch (e: Exception) {
            // Revert to previous lens on failure
            Log.e(TAG, "Switch camera failed, reverting", e)
            lensFacing = previousLensFacing
            detectAvailableExtensions()
            try {
                bindCameraUseCases(lifecycleOwner, previewView)
            } catch (e2: Exception) {
                Log.e(TAG, "Revert also failed", e2)
            }
        }
    }

    val isBackCamera: Boolean
        get() = lensFacing == CameraSelector.LENS_FACING_BACK

    fun release() {
        if (isRecording) stopVideoRecording()
        cameraProvider?.unbindAll()
    }

    // --- Helpers ---

    private fun hasCamera(provider: ProcessCameraProvider, facing: Int): Boolean {
        return try {
            provider.hasCamera(
                CameraSelector.Builder().requireLensFacing(facing).build(),
            )
        } catch (_: Exception) {
            false
        }
    }

    private fun mapFlashMode(): Int = when (flashMode) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }

    companion object {
        private const val TAG = "InsightCamera"

        fun extensionModeLabel(mode: Int): String = when (mode) {
            ExtensionMode.AUTO -> "AUTO"
            ExtensionMode.HDR -> "HDR"
            ExtensionMode.NIGHT -> "NIGHT"
            ExtensionMode.BOKEH -> "BOKEH"
            ExtensionMode.FACE_RETOUCH -> "BEAUTY"
            else -> "STANDARD"
        }
    }
}
