package com.harmonic.insight.camera.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.harmonic.insight.camera.R
import com.harmonic.insight.camera.camera.CaptureMode
import com.harmonic.insight.camera.camera.FlashMode
import com.harmonic.insight.camera.camera.InsightAspectRatio
import com.harmonic.insight.camera.camera.InsightCameraController
import com.harmonic.insight.camera.camera.LightMode
import com.harmonic.insight.camera.ui.components.CameraTopBar
import com.harmonic.insight.camera.ui.components.FocusPoint
import com.harmonic.insight.camera.ui.components.FocusRingOverlay
import com.harmonic.insight.camera.ui.components.LightToggleButton
import com.harmonic.insight.camera.ui.components.ModeSelector
import com.harmonic.insight.camera.ui.components.RecordButton
import com.harmonic.insight.camera.ui.components.ShutterButton
import com.harmonic.insight.camera.ui.components.TimerCountdownOverlay
import com.harmonic.insight.camera.ui.components.TimerDuration
import com.harmonic.insight.camera.ui.components.ZoomIndicator
import com.harmonic.insight.camera.ui.components.ZoomPresetBar
import com.harmonic.insight.camera.ui.theme.InsightError
import com.harmonic.insight.camera.ui.theme.InsightWhite
import kotlinx.coroutines.delay

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraController = remember { InsightCameraController(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Camera state
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var lightMode by remember { mutableStateOf(LightMode.OFF) }
    var isBackCamera by remember { mutableStateOf(true) }
    var currentZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var minZoom by remember { mutableFloatStateOf(1f) }
    var isCameraReady by remember { mutableStateOf(false) }
    var lastMediaUri by remember { mutableStateOf<String?>(null) }
    var showZoomIndicator by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var hasMultipleCameras by remember { mutableStateOf(false) }
    var extensionLabel by remember { mutableStateOf("") }
    var hasExtensions by remember { mutableStateOf(false) }

    // New feature state
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var timerDuration by remember { mutableStateOf(TimerDuration.OFF) }
    var timerCountdown by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var aspectRatio by remember { mutableStateOf(InsightAspectRatio.RATIO_4_3) }
    var focusPoint by remember { mutableStateOf<FocusPoint?>(null) }
    var focusId by remember { mutableLongStateOf(0L) }
    var isVideoRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    // Track MIME type for the last captured media
    var lastMediaIsVideo by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.release()
        }
    }

    // Timer countdown logic
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            for (i in timerCountdown downTo 1) {
                timerCountdown = i
                delay(1000)
            }
            isTimerRunning = false
            timerCountdown = 0
            // Take photo after countdown
            isCapturing = true
            cameraController.takePhoto(
                onSuccess = { uri ->
                    lastMediaUri = uri
                    lastMediaIsVideo = false
                    isCapturing = false
                },
                onError = {
                    Toast.makeText(context, context.getString(R.string.capture_failed), Toast.LENGTH_SHORT).show()
                    isCapturing = false
                },
            )
        }
    }

    // Recording duration counter
    LaunchedEffect(isVideoRecording) {
        if (isVideoRecording) {
            recordingDuration = 0
            while (isVideoRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Camera preview with touch handling
        AndroidView(
            factory = { ctx ->
                @SuppressLint("ClickableViewAccessibility")
                fun createPreviewView(): PreviewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER

                    val scaleDetector = ScaleGestureDetector(
                        ctx,
                        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(detector: ScaleGestureDetector): Boolean {
                                val newZoom = currentZoom * detector.scaleFactor
                                val clamped = newZoom.coerceIn(minZoom, maxZoom)
                                cameraController.setZoomRatio(clamped)
                                currentZoom = clamped
                                showZoomIndicator = true
                                return true
                            }
                        },
                    )

                    var isScaling = false

                    setOnTouchListener { view, event ->
                        scaleDetector.onTouchEvent(event)

                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                isScaling = false
                            }
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                isScaling = true
                            }
                            MotionEvent.ACTION_UP -> {
                                if (!isScaling && event.pointerCount == 1) {
                                    // Single tap -> focus
                                    val x = event.x
                                    val y = event.y
                                    cameraController.focusOnPoint(
                                        x, y, view.width, view.height,
                                    )
                                    focusId++
                                    focusPoint = FocusPoint(x, y, focusId)
                                    view.performClick()
                                }
                            }
                        }
                        true
                    }

                    previewView = this

                    cameraController.startCamera(
                        lifecycleOwner = lifecycleOwner,
                        previewView = this,
                        onReady = {
                            isCameraReady = true
                            maxZoom = cameraController.maxZoomRatio
                            minZoom = cameraController.minZoomRatio
                            currentZoom = cameraController.currentZoomRatio
                            hasMultipleCameras = cameraController.hasMultipleCameras
                            extensionLabel = cameraController.activeExtensionLabel
                            hasExtensions = cameraController.hasExtensions
                        },
                    )
                }
                createPreviewView()
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Focus ring overlay
        FocusRingOverlay(focusPoint = focusPoint)

        // Hide zoom indicator after delay
        LaunchedEffect(showZoomIndicator, currentZoom) {
            if (showZoomIndicator) {
                delay(2000)
                showZoomIndicator = false
            }
        }

        // Top bar
        CameraTopBar(
            flashMode = flashMode,
            onCycleFlash = {
                flashMode = cameraController.cycleFlashMode()
            },
            timerDuration = timerDuration,
            onCycleTimer = {
                timerDuration = when (timerDuration) {
                    TimerDuration.OFF -> TimerDuration.THREE
                    TimerDuration.THREE -> TimerDuration.TEN
                    TimerDuration.TEN -> TimerDuration.OFF
                }
            },
            aspectRatio = aspectRatio,
            onToggleAspectRatio = {
                aspectRatio = when (aspectRatio) {
                    InsightAspectRatio.RATIO_4_3 -> InsightAspectRatio.RATIO_16_9
                    InsightAspectRatio.RATIO_16_9 -> InsightAspectRatio.RATIO_4_3
                }
                cameraController.setAspectRatio(aspectRatio)
            },
            onSwitchCamera = {
                val pv = previewView ?: return@CameraTopBar
                cameraController.switchCamera(lifecycleOwner, pv)
                isBackCamera = cameraController.isBackCamera
                lightMode = cameraController.lightMode
                maxZoom = cameraController.maxZoomRatio
                minZoom = cameraController.minZoomRatio
                currentZoom = cameraController.currentZoomRatio
                extensionLabel = cameraController.activeExtensionLabel
                hasExtensions = cameraController.hasExtensions
            },
            captureMode = captureMode,
            hasMultipleCameras = hasMultipleCameras,
            extensionLabel = extensionLabel,
            hasExtensions = hasExtensions,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // Recording indicator
        if (isVideoRecording) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(InsightError),
                )
                Text(
                    text = formatDuration(recordingDuration),
                    color = InsightWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Extensions badge (shows active OEM processing mode)
        if (hasExtensions && captureMode == CaptureMode.PHOTO && !isVideoRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 100.dp)
                    .background(
                        Color.Black.copy(alpha = 0.45f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = extensionLabel,
                    color = InsightWhite.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Zoom indicator (shows during pinch)
        AnimatedVisibility(
            visible = showZoomIndicator && !isVideoRecording,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp),
        ) {
            ZoomIndicator(currentZoom = currentZoom)
        }

        // Bottom controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            // Light toggle (back camera, photo mode)
            if (isBackCamera && captureMode == CaptureMode.PHOTO) {
                LightToggleButton(
                    lightMode = lightMode,
                    onToggle = {
                        lightMode = cameraController.toggleLight()
                    },
                    enabled = cameraController.hasFlashUnit,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Zoom presets
            if (isCameraReady && maxZoom > 1f) {
                ZoomPresetBar(
                    currentZoom = currentZoom,
                    minZoom = minZoom,
                    maxZoom = maxZoom,
                    onZoomSelected = { ratio ->
                        val clamped = ratio.coerceIn(minZoom, maxZoom)
                        cameraController.setZoomRatio(clamped)
                        currentZoom = clamped
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Mode selector (PHOTO / VIDEO)
            ModeSelector(
                currentMode = captureMode,
                onModeChanged = { mode ->
                    if (isVideoRecording) return@ModeSelector
                    captureMode = mode
                    cameraController.setCaptureMode(mode)
                    maxZoom = cameraController.maxZoomRatio
                    minZoom = cameraController.minZoomRatio
                    currentZoom = cameraController.currentZoomRatio
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Shutter row: gallery thumbnail | shutter/record | spacer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Last media thumbnail
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .then(
                            if (lastMediaUri != null) {
                                Modifier.clickable {
                                    try {
                                        val mimeType = if (lastMediaIsVideo) "video/*" else "image/*"
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(lastMediaUri), mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, context.getString(R.string.no_app_to_open_media), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    if (lastMediaUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(lastMediaUri),
                            contentDescription = stringResource(R.string.last_media),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }

                // Shutter or Record button
                if (captureMode == CaptureMode.PHOTO) {
                    ShutterButton(
                        onClick = {
                            if (isCapturing || isTimerRunning) return@ShutterButton
                            if (timerDuration != TimerDuration.OFF) {
                                // Start timer countdown
                                timerCountdown = timerDuration.seconds
                                isTimerRunning = true
                            } else {
                                isCapturing = true
                                cameraController.takePhoto(
                                    onSuccess = { uri ->
                                        lastMediaUri = uri
                                        lastMediaIsVideo = false
                                        isCapturing = false
                                    },
                                    onError = {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.capture_failed),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        isCapturing = false
                                    },
                                )
                            }
                        },
                        enabled = isCameraReady && !isCapturing && !isTimerRunning,
                    )
                } else {
                    RecordButton(
                        isRecording = isVideoRecording,
                        onClick = {
                            if (isVideoRecording) {
                                cameraController.stopVideoRecording()
                                isVideoRecording = false
                            } else {
                                val hasAudioPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED
                                cameraController.startVideoRecording(
                                    withAudio = hasAudioPermission,
                                    onStarted = {
                                        isVideoRecording = true
                                    },
                                    onFinished = { uri ->
                                        lastMediaUri = uri
                                        lastMediaIsVideo = true
                                        isVideoRecording = false
                                    },
                                    onError = { msg ->
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.recording_failed, msg),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        isVideoRecording = false
                                    },
                                )
                            }
                        },
                        enabled = isCameraReady,
                    )
                }

                // Balance spacer
                Spacer(modifier = Modifier.size(52.dp))
            }
        }

        // Timer countdown overlay
        if (isTimerRunning && timerCountdown > 0) {
            TimerCountdownOverlay(secondsRemaining = timerCountdown)
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
