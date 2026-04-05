package com.weiy.account.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weiy.account.model.TransactionRecord
import com.weiy.account.model.TransactionType
import com.weiy.account.utils.formatAmount
import com.weiy.account.utils.formatTime

@Composable
fun TransactionListItem(
    transaction: TransactionRecord,
    modifier: Modifier = Modifier,
    incomeAmountColor: Color = MaterialTheme.colorScheme.primary,
    expenseAmountColor: Color = MaterialTheme.colorScheme.error,
    onClick: (Long) -> Unit
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) {
        incomeAmountColor
    } else {
        expenseAmountColor
    }
    val tagBgColor = if (isIncome) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val tagTextColor = if (isIncome) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick(transaction.id) },
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(tagBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isIncome) "入" else "出",
                    style = MaterialTheme.typography.labelMedium,
                    color = tagTextColor
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = transaction.categoryName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (transaction.note.isBlank()) "无备注" else transaction.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = (if (isIncome) "+" else "-") + formatAmount(transaction.amount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = amountColor
                )
                Text(
                    text = formatTime(transaction.dateTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
