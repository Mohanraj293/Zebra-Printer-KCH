package com.lazymohan.zebraprinter.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.eam360.uicomponents.EAMLoader
import com.eam360.uicomponents.EAMLoaderStyle.S

const val PROGRESS_TAG = "PROGRESS_TAG"

@Composable
fun EAMLoader(progressTag: String = PROGRESS_TAG) {
    Box(
        modifier = Modifier
            .testTag(progressTag)
            .background(color = Color.Black.copy(alpha = 0.4f))
            .fillMaxSize()
            .clickable(enabled = false, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        EAMLoader(loaderStyle = S)
    }
}