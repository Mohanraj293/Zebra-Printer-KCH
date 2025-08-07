package com.lazymohan.zebraprinter.utils

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode.Restart
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tarkalabs.tarkaui.theme.TUITheme

@Composable
fun EAMLoader(
    modifier: Modifier = Modifier,
    spinnerImage: EAMLoaderSpinnerImage? = null,
    loaderStyle: EAMLoaderStyle = EAMLoaderStyle.L,
    tags: EAMLoaderTags = EAMLoaderTags()
) {
    Box(
        modifier = modifier.testTag(tags.parentTag),
        contentAlignment = Alignment.Center
    ) {
        EAMLoaderProgressIndicator(
            modifier = Modifier
                .size(loaderStyle.spinnerSize)
                .testTag(tags.progressBarTag)
        )
        spinnerImage?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(
                    start = 31.dp,
                    top = 6.dp,
                    end = 31.dp,
                    bottom = 6.dp
                )
            ) {
                Image(
                    modifier = Modifier
                        .testTag(tags.loaderImageTag)
                        .size(loaderStyle.iconSize),
                    painter = painterResource(id = spinnerImage.resourceId),
                    contentDescription = spinnerImage.contentDescription
                )
            }
        }
    }
}

@Composable
private fun EAMLoaderProgressIndicator(modifier: Modifier) {
    val angle by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1300,
                easing = LinearEasing
            ),
            repeatMode = Restart
        )
    )

    val outerCircleColor = TUITheme.colors.surfaceVariantHover
    val rotatingAngleColor = TUITheme.colors.primary

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .rotate(angle)
    ) {
        val canvasSize = size.minDimension
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = canvasSize / 2
        val strokeWidth = 4.dp.toPx()

        val circleRadius = radius - strokeWidth / 2
        drawCircle(
            color = outerCircleColor,
            center = Offset(centerX, centerY),
            radius = circleRadius,
            style = Stroke(width = strokeWidth)
        )

        val arcStartX = centerX - circleRadius
        val arcStartY = centerY - circleRadius
        drawArc(
            color = rotatingAngleColor,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(arcStartX, arcStartY),
            size = Size(circleRadius * 2, circleRadius * 2),
            style = Stroke(width = strokeWidth)
        )
    }
}

data class EAMLoaderTags(
    val parentTag: String = "EAMLoaderTag",
    val progressBarTag: String = "EAMLoaderTag_spinnerTag",
    val loaderImageTag: String = "EAMLoaderTag_spinnerImageTag"
)

data class EAMLoaderSpinnerImage(@DrawableRes val resourceId: Int, val contentDescription: String)

@Preview(showBackground = true)
@Composable
private fun EAMLoaderPreview() {
    TUITheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TUITheme.colors.surface),
            contentAlignment = Alignment.Center
        ) {
            _root_ide_package_.com.lazymohan.zebraprinter.utils.EAMLoader()
        }
    }
}

enum class EAMLoaderStyle(val spinnerSize: Dp, val iconSize: Dp) {
    L(spinnerSize = 240.dp, iconSize = 90.dp),
    M(spinnerSize = 180.dp, iconSize = 65.dp),
    S(spinnerSize = 90.dp, iconSize = 30.dp)
}