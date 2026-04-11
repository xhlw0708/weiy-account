package com.weiy.account.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class DataTransferFormat(
    val displayName: String,
    val description: String,
    val fileExtension: String,
    val mimeType: String
) {
    EXCEL(
        displayName = "Excel 表格",
        description = "单个 .xlsx 文件，包含全部数据表",
        fileExtension = ".xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ),
    CSV(
        displayName = "CSV",
        description = "单个 .zip 文件，内含多份 .csv 数据表",
        fileExtension = ".zip",
        mimeType = "application/zip"
    );

    fun buildSuggestedFileName(now: LocalDateTime = LocalDateTime.now()): String {
        val timestamp = now.format(FILE_NAME_TIMESTAMP_FORMATTER)
        return "weiy-account-$timestamp$fileExtension"
    }

    companion object {
        private val FILE_NAME_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
