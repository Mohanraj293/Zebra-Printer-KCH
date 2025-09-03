package com.lazymohan.zebraprinter.product.lots

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.lazymohan.zebraprinter.ZPLPrinterActivity
import com.lazymohan.zebraprinter.product.data.Item
import com.lazymohan.zebraprinter.product.lots.data.LotsListViewModel
import com.lazymohan.zebraprinter.product.lots.ui.LotsListScreenContent
import com.lazymohan.zebraprinter.product.lots.ui.LotsListsEvent
import com.lazymohan.zebraprinter.utils.DateTimeConverter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LotsListActivity : ComponentActivity() {

    companion object {
        fun getCallingIntent(context: Context, item: Item): Intent {
            return Intent(context, LotsListActivity::class.java).apply {
                putExtra("item", item)
            }
        }
    }

    @Inject
    lateinit var dateTimeConverter: DateTimeConverter

    @Inject
    lateinit var lotsListViewModelFactory: LotsListViewModel.LotsListViewModelFactory

    private val viewModel: LotsListViewModel by viewModels {
        LotsListViewModel.providesFactory(
            assistedFactory = lotsListViewModelFactory,
            item = intent.getSerializableExtra("item") as Item
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LotsListScreenContent(
                viewModel = viewModel,
                handleEvent = ::handleEvents,
                dateTimeConverter = dateTimeConverter
            )
        }
    }

    private fun handleEvents(event: LotsListsEvent) {
        when (event) {
            is LotsListsEvent.OnLotSelected -> {
                startActivity(
                    ZPLPrinterActivity.getCallingIntent(
                        context = this,
                        product = event.lot,
                        gtinNumber = event.gtinNumber,
                    )
                )
            }
            is LotsListsEvent.OnBackPressed -> {
                onBackPressedDispatcher.onBackPressed()
            }
            is LotsListsEvent.RemoveError -> {
                // Handle error removal
            }
        }
    }
}