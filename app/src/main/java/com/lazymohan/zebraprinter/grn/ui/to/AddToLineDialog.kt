package com.lazymohan.zebraprinter.grn.ui.to

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazymohan.zebraprinter.grn.data.TransferOrderLine

@Composable
fun AddToLineDialog(
    candidates: List<TransferOrderLine>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Line From TO") },
        text = {
            if (candidates.isEmpty()) {
                Text("No more TO lines available to add.")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidates) { line ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(line.transferOrderLineId) },
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 1.dp
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    text = line.itemNumber,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "UOM: ${line.unitOfMeasure ?: "-"} • Subinv: ${line.subinventory ?: "-"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
