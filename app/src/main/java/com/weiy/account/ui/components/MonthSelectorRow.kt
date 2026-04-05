package com.weiy.account.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MonthSelectorRow(
    monthLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledTonalIconButton(onClick = onPrevious) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一月")
        }
        Text(text = monthLabel, style = MaterialTheme.typography.titleMedium)
        FilledTonalIconButton(onClick = onNext) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一月")
        }
    }
}

