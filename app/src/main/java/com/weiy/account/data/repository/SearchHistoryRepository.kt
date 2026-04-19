package com.weiy.account.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class SearchHistoryRepository(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _histories = MutableStateFlow(readHistories())
    val histories: StateFlow<List<String>> = _histories.asStateFlow()

    fun record(keyword: String) {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) return

        val updated = buildList {
            add(normalizedKeyword)
            addAll(_histories.value.filterNot { it.equals(normalizedKeyword, ignoreCase = true) })
        }.take(MAX_HISTORY_SIZE)

        writeHistories(updated)
    }

    fun remove(keyword: String) {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) return

        val updated = _histories.value.filterNot { it.equals(normalizedKeyword, ignoreCase = true) }
        writeHistories(updated)
    }

    fun clear() {
        writeHistories(emptyList())
    }

    private fun readHistories(): List<String> {
        val raw = preferences.getString(KEY_HISTORIES, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return runCatching {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val keyword = jsonArray.optString(index).trim()
                    if (keyword.isNotBlank()) {
                        add(keyword)
                    }
                }
            }.distinctBy { it.lowercase() }.take(MAX_HISTORY_SIZE)
        }.getOrElse { emptyList() }
    }

    private fun writeHistories(histories: List<String>) {
        val jsonArray = JSONArray().apply {
            histories.forEach(::put)
        }

        preferences.edit {
            putString(KEY_HISTORIES, jsonArray.toString())
        }
        _histories.value = histories
    }

    private companion object {
        private const val PREFS_NAME = "search_history"
        private const val KEY_HISTORIES = "keyword_histories"
        private const val MAX_HISTORY_SIZE = 20
    }
}

