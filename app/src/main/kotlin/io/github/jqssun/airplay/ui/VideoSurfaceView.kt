package io.github.jqssun.airplay.ui

import android.content.Context
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private fun normalizeRotation(degrees: Int): Int {
    val normalized = ((degrees % 360) + 360) % 360
    return when (normalized) {
        0, 90, 180, 270 -> normalized
        else -> 0
    }
}

private class RotatingSurfaceView(context: Context) : SurfaceView(context) {
    var outputRotationDegrees: Int = 0
        set(value) {
            field = normalizeRotation(value)
            applyOutputTransform()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyOutputTransform()
    }

    private fun applyOutputTransform() {
        rotation = outputRotationDegrees.toFloat()

        val quarterTurn = outputRotationDegrees == 90 || outputRotationDegrees == 270
        val fitScale = if (quarterTurn && width > 0 && height > 0) {
            minOf(
                width.toFloat() / height.toFloat(),
                height.toFloat() / width.toFloat()
            )
        } else {
            1f
        }

        scaleX = fitScale
        scaleY = fitScale
    }
}

// shared surface for mirroring (VideoRenderer) and airplay video (AirPlayVideoPlayer)
@Composable
fun VideoSurfaceView(
    onSurfaceAvailable: (Surface) -> Unit,
    onSurfaceDestroyed: (Surface) -> Unit,
    aspectRatio: Float = 16f / 9f,
    // false = caller sizes the surface (content-scale modes)
    applyAspectRatio: Boolean = true,
    rotationDegrees: Int = 0,
    modifier: Modifier = Modifier
) {
    val callbacks = remember {
        object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                onSurfaceAvailable(holder.surface)
            }

            override fun surfaceChanged(holder: SurfaceHolder, fmt: Int, w: Int, h: Int) {
                onSurfaceAvailable(holder.surface)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                onSurfaceDestroyed(holder.surface)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            RotatingSurfaceView(ctx).also {
                it.outputRotationDegrees = rotationDegrees
                it.holder.addCallback(callbacks)
            }
        },
        update = {
            it.outputRotationDegrees = rotationDegrees
        },
        modifier = if (applyAspectRatio) {
            modifier
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = aspectRatio < 1f)
                .fillMaxSize()
        } else {
            modifier
        }
    )
}
