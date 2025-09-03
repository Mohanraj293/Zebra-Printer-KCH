package com.lazymohan.zebraprinter.product

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.lazymohan.zebraprinter.product.lots.LotsListActivity
import com.tarkalabs.tarkaui.theme.TUITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductsActivity : ComponentActivity() {

    private val viewModel: ProductsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TUITheme {
                ProductScreenContent(
                    handleEvent = ::handleEvents,
                    viewModel = viewModel
                )
            }
        }
    }

    private fun handleEvents(event: ProductsEvent) {
        when (event) {
            is ProductsEvent.OnSearchClicked -> {
                viewModel.searchProducts()
            }

            is ProductsEvent.OnSearchQueryChanged -> {
                viewModel.updateSearchQuery(event.query)
            }

            ProductsEvent.RemoveError -> {
                viewModel.removeError()
            }

            is ProductsEvent.OnProductSelected -> {
                startActivity(
                    LotsListActivity.getCallingIntent(this, event.product)
                )
            }

            ProductsEvent.OnBackPressed -> {
                viewModel.updateShowProductScreen(false)
            }

            ProductsEvent.Finish -> {
                finish()
            }

            is ProductsEvent.OnOrgIdClicked -> {
                viewModel.showOrgIdBottomSheet(event.show)
            }

            is ProductsEvent.SelectedOrgId -> {
                viewModel.updateSelectedOrgId(event.orgId)
            }
        }
    }
}