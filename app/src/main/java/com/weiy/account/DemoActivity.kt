package com.weiy.account

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weiy.account.ui.components.WeiyCalendar
import com.weiy.account.ui.components.WeiyCalendarView
import com.weiy.account.ui.theme.WeiyAccountTheme
import java.time.LocalDate
import java.time.YearMonth

class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var selectedDate by remember { mutableStateOf(LocalDate.now()) }
            var visibleMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
            var calendarView by remember { mutableStateOf(WeiyCalendarView.Day) }

            WeiyAccountTheme(
                darkTheme = false,
                dynamicColor = true
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .systemBarsPadding()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                    ) {
                        WeiyCalendar(
                            entries = emptyList(),
                            visibleMonth = visibleMonth,
                            selectedDate = selectedDate,
                            view = calendarView,
                            modifier = Modifier.fillMaxWidth(),
                            onVisibleMonthChange = { visibleMonth = it },
                            onSelectedDateChange = { date ->
                                selectedDate = date
                                visibleMonth = YearMonth.from(date)
                            },
                            onViewChange = { calendarView = it }
                        )
                    }
                }
            }
        }
    }
}
