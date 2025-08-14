package com.lazymohan.zebraprinter.product

import androidx.compose.runtime.Composable
import com.lazymohan.zebraprinter.utils.EAMCustomSelectionCard


@Composable
fun OrgIdSelectionCard(
    selectedOrgId: String?,
    onOrgIdClick: () -> Unit
) {
    EAMCustomSelectionCard(
        description = selectedOrgId,
        label = "Select OrgId",
        onCardClicked = onOrgIdClick,
        showTrailingIcon = true
    )
}