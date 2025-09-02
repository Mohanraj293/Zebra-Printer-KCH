package com.lazymohan.zebraprinter.login

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lazymohan.zebraprinter.utils.EAMLoader
import com.lazymohan.zebraprinter.utils.EAMLoaderStyle
import com.tarkalabs.tarkaui.components.TUISnackBarHost
import com.tarkalabs.tarkaui.components.TUISnackBarState
import com.tarkalabs.tarkaui.components.TUISnackBarType
import com.tarkalabs.tarkaui.components.base.TUIButton
import com.tarkalabs.tarkaui.icons.TarkaIcons.Filled
import com.tarkalabs.tarkaui.icons.Info24

@Composable
fun LoginScreenContent(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel,
    moveToLandingPage: () -> Unit,
    updateError: (String?) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0E63FF), Color(0xFF5AA7FF)))

    LaunchedEffect(Unit) {
        viewModel.loginEvents.collect { event ->
            when (event) {
                is LoginEvent.Success -> moveToLandingPage()
                is LoginEvent.Failure -> updateError(event.message)
                else -> Unit
            }
        }
    }

    val snackState by remember {
        mutableStateOf(
            TUISnackBarState(
                SnackbarHostState(),
                TUISnackBarType.Error,
                Filled.Info24
            )
        )
    }

    Scaffold(
        containerColor = Color(0xFFF6F8FF),
        snackbarHost = { TUISnackBarHost(snackState) },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            val contentModifier =
                if (state.loading) Modifier.blur(12.dp) else Modifier

            LazyColumn(
                modifier = modifier
                    .then(contentModifier)
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(gradient)
                            .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 52.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                color = Color.White.copy(alpha = 0.9f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "KCH",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = Color(0xFF0E63FF),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "King's College Hospital",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Dubai - Warehouse & Pharmacy Management",
                                color = Color.White.copy(alpha = 0.95f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))

                        TUIButton(
                            onClick = { viewModel.startOAuth() },
                            modifier = Modifier.fillMaxWidth(),
                            label = if (state.loading)
                                (state.loadingMessage ?: "Redirecting…")
                            else
                                "Login with SSO",
                            enabled = !state.loading
                        )
                    }
                }
            }
            if (state.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    EAMLoader(loaderStyle = EAMLoaderStyle.S)
                }
            }
        }
    }
}
