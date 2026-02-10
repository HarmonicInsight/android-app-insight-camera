package com.harmonic.insight.camera.ui

import android.content.Intent
import android.net.Uri
import android.view.ScaleGestureDetector
import android.widget.Toast
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import coil.compose.rememberAsyncImagePainter
import com.harmonic.insight.camera.camera.FlashMode
import com.harmonic.insight.camera.camera.InsightCameraController
import com.harmonic.insight.camera.camera.LightMode
import com.harmonic.insight.camera.ui.components.CameraTopBar
import com.harmonic.insight.camera.ui.components.LightToggleButton
import com.harmonic.insight.camera.ui.components.ShutterButton
import com.harmonic.insight.camera.ui.components.ZoomIndicator
import com.harmonic.insight.camera.ui.components.ZoomPresetBar

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraController = remember { InsightCameraController(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var lightMode by remember { mutableStateOf(LightMode.OFF) }
    var isBackCamera by remember { mutableStateOf(true) }
    var currentZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var minZoom by remember { mutableFloatStateOf(1f) }
    var isCameraReady by remember { mutableStateOf(false) }
    var lastPhotoUri by remember { mutableStateOf<String?>(null) }
    var showZoomIndicator by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER

                    // Pinch-to-zoom
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

                    setOnTouchListener { _, event ->
                        scaleDetector.onTouchEvent(event)
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
                        },
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Hide zoom indicator after delay
        LaunchedEffect(showZoomIndicator, currentZoom) {
            if (showZoomIndicator) {
                delay(2000)
                showZoomIndicator = false
            }
        }

        // Top bar with flash + camera switch
        CameraTopBar(
            flashMode = flashMode,
            onCycleFlash = {
                flashMode = cameraController.cycleFlashMode()
            },
            onSwitchCamera = {
                val pv = previewView ?: return@CameraTopBar
                cameraController.switchCamera(lifecycleOwner, pv)
                isBackCamera = cameraController.isBackCamera
                lightMode = cameraController.lightMode
                maxZoom = cameraController.maxZoomRatio
                minZoom = cameraController.minZoomRatio
                currentZoom = cameraController.currentZoomRatio
            },
            isBackCamera = isBackCamera,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // Zoom indicator (shows during pinch)
        AnimatedVisibility(
            visible = showZoomIndicator,
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
            // Light toggle (prominent feature)
            if (isBackCamera) {
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
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Shutter row: gallery thumbnail | shutter | spacer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Last photo thumbnail
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .then(
                            if (lastPhotoUri != null) {
                                Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(lastPhotoUri), "image/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                }
                            } else {
                                Modifier
                            }
                        ),
                ) {
                    if (lastPhotoUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(lastPhotoUri),
                            contentDescription = "Last photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }

                // Shutter button
                ShutterButton(
                    onClick = {
                        if (isCapturing) return@ShutterButton
                        isCapturing = true
                        cameraController.takePhoto(
                            onSuccess = { uri ->
                                lastPhotoUri = uri
                                isCapturing = false
                            },
                            onError = { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to capture",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                isCapturing = false
                            },
                        )
                    },
                    enabled = isCameraReady && !isCapturing,
                )

                // Balance spacer (same size as thumbnail)
                Spacer(modifier = Modifier.size(52.dp))
            }
        }
    }
}
