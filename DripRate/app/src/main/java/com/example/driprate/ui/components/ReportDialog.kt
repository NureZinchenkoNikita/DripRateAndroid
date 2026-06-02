package com.example.driprate.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val reasons = listOf(
        "Spam or Advertising",
        "Inappropriate or Offensive Content",
        "Fraud or Scam",
        "Harassment or Abuse",
        "Other"
    )
    
    var selectedReasonIndex by remember { mutableIntStateOf(0) }
    var additionalDetails by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Submit Report",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Why are you reporting this content?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    reasons.forEachIndexed { index, reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReasonIndex = index }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReasonIndex == index,
                                onClick = { selectedReasonIndex = index }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = additionalDetails,
                    onValueChange = { additionalDetails = it },
                    label = { Text("Report details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReasonIndex == reasons.lastIndex) {
                        if (additionalDetails.isNotBlank()) "Other: $additionalDetails" else "Other"
                    } else {
                        val base = reasons[selectedReasonIndex]
                        if (additionalDetails.isNotBlank()) "$base: $additionalDetails" else base
                    }
                    onSubmit(finalReason)
                }
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
