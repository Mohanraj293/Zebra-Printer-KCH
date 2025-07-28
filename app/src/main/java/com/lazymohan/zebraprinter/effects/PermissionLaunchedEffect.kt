package com.lazymohan.zebraprinter.effects

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

private var allPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )
} else {
    arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
}

@Composable
fun PermissionLaunchedEffect(
    onAllPermissionGranted: () -> Unit,
    onSomePermissionDenied: () -> Unit
) {
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { map ->
            val isAllPermissionsGranted = map.values.all { it }
            if (isAllPermissionsGranted) {
                onAllPermissionGranted()
            } else {
                onSomePermissionDenied()
            }
        }

    LaunchedEffect(true) {
        permissionLauncher.launch(allPermissions)
    }
}