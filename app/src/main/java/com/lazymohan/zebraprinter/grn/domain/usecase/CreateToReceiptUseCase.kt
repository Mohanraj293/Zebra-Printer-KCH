package com.lazymohan.zebraprinter.grn.domain.usecase

import com.lazymohan.zebraprinter.grn.data.GrnRepository
import com.lazymohan.zebraprinter.grn.data.ReceiptResponse
import javax.inject.Inject

class CreateToReceiptUseCase @Inject constructor(
    private val repo: GrnRepository
) {
    // It can be PO ReceiptRequest or TO payload
    suspend operator fun invoke(body: Any): Result<ReceiptResponse> =
        repo.createReceipt(body)
}
