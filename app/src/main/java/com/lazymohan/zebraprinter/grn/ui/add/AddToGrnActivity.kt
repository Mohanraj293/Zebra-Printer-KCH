package com.lazymohan.zebraprinter.grn.ui.add

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

import com.lazymohan.zebraprinter.utils.DateTimeConverter
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddToGrnActivity : ComponentActivity() {

    @Inject
    lateinit var dateTimeConverter: DateTimeConverter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TUITheme {
                val vm: AddToGrnViewModel = hiltViewModel()
                val ui by vm.ui.collectAsState()
                val snack = remember { SnackbarHostState() }
                val context = LocalContext.current

                AddToGrnScreens(
                    ui = ui,
                    snackbarHostState = snack,
                    dateTimeConverter = dateTimeConverter,
                    onEnterPo = vm::setPoNumber,
                    onFindExisting = vm::findExistingGrns,
                    onSelectExisting = vm::selectExistingGrn,
                    onUpdateLine = vm::updateLine,
                    onRemoveLine = vm::removeLine,
                    onAddLine = vm::addLineFromPo,
                    onAddSection = vm::addSection,
                    onRemoveSection = vm::removeSection,
                    onUpdateSection = vm::updateLineSection,
                    onReview = vm::buildPayloadsAndReview,
                    onBackFromReceive = { finish() },
                    onAddAttachments = { uris -> vm.addManualAttachmentsFromUris(context, uris) },
                    onEditReceive = vm::backToReceive,
                    onSubmit = vm::submitAddToExisting,
                    onStartOver = {
                        vm.startOver()
                        finish()
                    },
                    onInvoiceChanged = vm::updateInvoiceNumber
                )
            }
        }
    }
}
