package com.lazymohan.zebraprinter.inventory

data class InventoryUiState(
    val orgName: String = "KDH KCH Dubai Hospital",
    val subinventoryName: String = "DHHMainWHS Main General Warehouse",
    val onHandLoaded: Boolean = false,
    val counts: List<Pair<Long, Int>> = emptyList(),
    val matchDialog: MatchDialogData? = null,
    val stage: InventoryStage = InventoryStage.Choose
)

data class MatchDialogData(
    val transactionId: Long,
    val description: String?,
    val gtin: String?,
    val lot: String,
    val expiryIso: String
)

enum class InventoryStage { Choose, Actions }
