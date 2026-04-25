package com.weiy.account

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                WeiyCalendar(
                    entries = emptyList(),
                    visibleMonth = visibleMonth,
                    selectedDate = selectedDate,
                    view = calendarView,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
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
