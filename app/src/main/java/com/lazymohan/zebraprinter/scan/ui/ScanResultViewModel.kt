// app/src/main/java/com/lazymohan/zebraprinter/scan/ui/ScanResultViewModel.kt
package com.lazymohan.zebraprinter.scan.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazymohan.zebraprinter.scan.data.OcrContentResponse
import com.lazymohan.zebraprinter.scan.data.OcrRepository
import com.lazymohan.zebraprinter.scan.data.OcrResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Uploading : ScanUiState() // covers upload + poll
    data class Completed(val data: OcrContentResponse) : ScanUiState()
    data class Error(val message: String, val raw: String? = null, val code: Int? = null) : ScanUiState()
}

@HiltViewModel
class ScanResultViewModel @Inject constructor(
    private val repo: OcrRepository,
    @ApplicationContext private val app: Context
) : ViewModel() {

    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = _state

    fun start(uris: List<Uri>) {
        if (uris.isEmpty()) {
            _state.value = ScanUiState.Error("No image to upload")
            return
        }
        val target = uris.first()
        viewModelScope.launch {
            _state.value = ScanUiState.Uploading
            when (val res = repo.uploadAndPoll(app, target)) {
                is OcrResult.Success -> _state.value = ScanUiState.Completed(res.content)
                is OcrResult.HttpError -> _state.value = ScanUiState.Error(res.message ?: "HTTP Error", res.raw, res.code)
                is OcrResult.ExceptionError -> _state.value = ScanUiState.Error(res.message ?: "Unexpected Error")
            }
        }
    }
}
