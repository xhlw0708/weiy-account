package com.weiy.account.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.weiy.account.data.local.database.AppDatabase
import com.weiy.account.data.local.entity.CategoryEntity
import com.weiy.account.data.local.entity.CategoryNoteHistoryEntity
import com.weiy.account.data.local.entity.TransactionEntity
import com.weiy.account.model.DataTransferFormat
import com.weiy.account.model.TransactionType
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class DataTransferRepository(
    context: Context,
    private val database: AppDatabase
) {
    private val contentResolver = context.applicationContext.contentResolver
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val categoryNoteHistoryDao = database.categoryNoteHistoryDao()

    suspend fun exportData(
        uri: Uri,
        format: DataTransferFormat
    ): DataExportSummary = withContext(Dispatchers.IO) {
        val snapshot = database.withTransaction {
            DatabaseSnapshot(
                categories = categoryDao.getAllCategories(),
                transactions = transactionDao.getAllTransactions(),
                noteHistories = categoryNoteHistoryDao.getAllHistories()
            )
        }

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            when (format) {
                DataTransferFormat.EXCEL -> ExcelTransferCodec.write(snapshot, outputStream)
                DataTransferFormat.CSV -> CsvArchiveTransferCodec.write(snapshot, outputStream)
            }
        } ?: error("无法打开导出文件")

        DataExportSummary(
            categoryCount = snapshot.categories.size,
            transactionCount = snapshot.transactions.size,
            noteHistoryCount = snapshot.noteHistories.size
        )
    }

    suspend fun importData(
        uri: Uri,
        format: DataTransferFormat
    ): DataImportSummary = withContext(Dispatchers.IO) {
        val importedSnapshot = contentResolver.openInputStream(uri)?.use { inputStream ->
            when (format) {
                DataTransferFormat.EXCEL -> ExcelTransferCodec.read(inputStream)
                DataTransferFormat.CSV -> CsvArchiveTransferCodec.read(inputStream)
            }
        } ?: error("无法读取导入文件")

        database.withTransaction {
            importSnapshot(importedSnapshot)
        }
    }

    private suspend fun importSnapshot(snapshot: DatabaseSnapshot): DataImportSummary {
        val categoryIdMapping = mutableMapOf<Long, Long>()
        var insertedCategories = 0
        var matchedCategories = 0
        var insertedTransactions = 0
        var insertedNoteHistories = 0
        var mergedNoteHistories = 0

        snapshot.categories.forEach { category ->
            val normalizedName = category.name.trim()
            require(normalizedName.isNotEmpty()) { "分类名称不能为空" }

            val existing = categoryDao.getCategoryByNameAndType(
                name = normalizedName,
                type = category.type
            )

            val actualId = if (existing == null) {
                insertedCategories += 1
                categoryDao.insertCategory(
                    category.copy(
                        id = 0L,
                        name = normalizedName
                    )
                )
            } else {
                matchedCategories += 1
                existing.id
            }

            categoryIdMapping[category.id] = actualId
        }

        snapshot.transactions.forEach { transaction ->
            val actualCategoryId = categoryIdMapping[transaction.categoryId]
                ?: error("导入失败：存在找不到分类映射的流水数据")
            transactionDao.insertTransaction(
                transaction.copy(
                    id = 0L,
                    categoryId = actualCategoryId
                )
            )
            insertedTransactions += 1
        }

        snapshot.noteHistories.forEach { history ->
            val actualCategoryId = categoryIdMapping[history.categoryId]
                ?: error("导入失败：存在找不到分类映射的备注历史数据")
            val normalizedNote = history.note.trim()
            if (normalizedNote.isBlank()) return@forEach

            val existing = categoryNoteHistoryDao.getHistoryByCategoryIdAndNote(
                categoryId = actualCategoryId,
                note = normalizedNote
            )
            if (existing == null) {
                categoryNoteHistoryDao.insertHistory(
                    history.copy(
                        id = 0L,
                        categoryId = actualCategoryId,
                        note = normalizedNote
                    )
                )
                insertedNoteHistories += 1
            } else {
                categoryNoteHistoryDao.updateHistory(
                    existing.copy(
                        usageCount = existing.usageCount + history.usageCount,
                        lastUsedAt = maxOf(existing.lastUsedAt, history.lastUsedAt),
                        createdAt = minOf(existing.createdAt, history.createdAt),
                        updatedAt = maxOf(existing.updatedAt, history.updatedAt)
                    )
                )
                mergedNoteHistories += 1
            }
        }

        return DataImportSummary(
            insertedCategoryCount = insertedCategories,
            matchedCategoryCount = matchedCategories,
            insertedTransactionCount = insertedTransactions,
            insertedNoteHistoryCount = insertedNoteHistories,
            mergedNoteHistoryCount = mergedNoteHistories
        )
    }
}

data class DataExportSummary(
    val categoryCount: Int,
    val transactionCount: Int,
    val noteHistoryCount: Int
)

data class DataImportSummary(
    val insertedCategoryCount: Int,
    val matchedCategoryCount: Int,
    val insertedTransactionCount: Int,
    val insertedNoteHistoryCount: Int,
    val mergedNoteHistoryCount: Int
)

private data class DatabaseSnapshot(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val noteHistories: List<CategoryNoteHistoryEntity>
)

private object ExcelTransferCodec {
    private const val CATEGORY_SHEET = "categories"
    private const val TRANSACTION_SHEET = "transactions"
    private const val NOTE_HISTORY_SHEET = "category_note_history"

    fun write(
        snapshot: DatabaseSnapshot,
        outputStream: OutputStream
    ) {
        XSSFWorkbook().use { workbook ->
            workbook.createSheet(CATEGORY_SHEET).apply {
                writeHeader(CATEGORY_HEADERS)
                snapshot.categories.forEachIndexed { index, category ->
                    createRow(index + 1).writeCells(
                        listOf(
                            category.id.toString(),
                            category.name,
                            category.type.name,
                            category.iconKey.orEmpty(),
                            category.isDefault.toString(),
                            category.sortOrder.toString()
                        )
                    )
                }
            }

            workbook.createSheet(TRANSACTION_SHEET).apply {
                writeHeader(TRANSACTION_HEADERS)
                snapshot.transactions.forEachIndexed { index, transaction ->
                    createRow(index + 1).writeCells(
                        listOf(
                            transaction.id.toString(),
                            transaction.type.name,
                            transaction.amount.toString(),
                            transaction.categoryId.toString(),
                            transaction.note,
                            transaction.dateTime.toString(),
                            transaction.createdAt.toString(),
                            transaction.updatedAt.toString()
                        )
                    )
                }
            }

            workbook.createSheet(NOTE_HISTORY_SHEET).apply {
                writeHeader(NOTE_HISTORY_HEADERS)
                snapshot.noteHistories.forEachIndexed { index, history ->
                    createRow(index + 1).writeCells(
                        listOf(
                            history.id.toString(),
                            history.categoryId.toString(),
                            history.note,
                            history.usageCount.toString(),
                            history.lastUsedAt.toString(),
                            history.createdAt.toString(),
                            history.updatedAt.toString()
                        )
                    )
                }
            }

            CATEGORY_HEADERS.indices.forEach { columnIndex ->
                workbook.getSheet(CATEGORY_SHEET).autoSizeColumn(columnIndex)
            }
            TRANSACTION_HEADERS.indices.forEach { columnIndex ->
                workbook.getSheet(TRANSACTION_SHEET).autoSizeColumn(columnIndex)
            }
            NOTE_HISTORY_HEADERS.indices.forEach { columnIndex ->
                workbook.getSheet(NOTE_HISTORY_SHEET).autoSizeColumn(columnIndex)
            }

            workbook.write(outputStream)
        }
    }

    fun read(inputStream: InputStream): DatabaseSnapshot {
        WorkbookFactory.create(inputStream).use { workbook ->
            val formatter = DataFormatter()
            val categories = workbook.getSheet(CATEGORY_SHEET)?.readRows(formatter, CATEGORY_HEADERS)
                ?.map(::parseCategoryRow)
                ?: error("导入文件缺少 $CATEGORY_SHEET 工作表")
            val transactions = workbook.getSheet(TRANSACTION_SHEET)?.readRows(formatter, TRANSACTION_HEADERS)
                ?.map(::parseTransactionRow)
                ?: error("导入文件缺少 $TRANSACTION_SHEET 工作表")
            val noteHistories = workbook.getSheet(NOTE_HISTORY_SHEET)?.readRows(formatter, NOTE_HISTORY_HEADERS)
                ?.map(::parseNoteHistoryRow)
                ?: error("导入文件缺少 $NOTE_HISTORY_SHEET 工作表")

            return DatabaseSnapshot(
                categories = categories,
                transactions = transactions,
                noteHistories = noteHistories
            )
        }
    }
}

private object CsvArchiveTransferCodec {
    private const val CATEGORY_ENTRY = "categories.csv"
    private const val TRANSACTION_ENTRY = "transactions.csv"
    private const val NOTE_HISTORY_ENTRY = "category_note_history.csv"

    fun write(
        snapshot: DatabaseSnapshot,
        outputStream: OutputStream
    ) {
        ZipOutputStream(outputStream.buffered()).use { zipOutputStream ->
            zipOutputStream.writeCsvEntry(CATEGORY_ENTRY, CATEGORY_HEADERS) { writer ->
                snapshot.categories.forEach { category ->
                    writer.writeCsvRow(
                        listOf(
                            category.id.toString(),
                            category.name,
                            category.type.name,
                            category.iconKey.orEmpty(),
                            category.isDefault.toString(),
                            category.sortOrder.toString()
                        )
                    )
                }
            }

            zipOutputStream.writeCsvEntry(TRANSACTION_ENTRY, TRANSACTION_HEADERS) { writer ->
                snapshot.transactions.forEach { transaction ->
                    writer.writeCsvRow(
                        listOf(
                            transaction.id.toString(),
                            transaction.type.name,
                            transaction.amount.toString(),
                            transaction.categoryId.toString(),
                            transaction.note,
                            transaction.dateTime.toString(),
                            transaction.createdAt.toString(),
                            transaction.updatedAt.toString()
                        )
                    )
                }
            }

            zipOutputStream.writeCsvEntry(NOTE_HISTORY_ENTRY, NOTE_HISTORY_HEADERS) { writer ->
                snapshot.noteHistories.forEach { history ->
                    writer.writeCsvRow(
                        listOf(
                            history.id.toString(),
                            history.categoryId.toString(),
                            history.note,
                            history.usageCount.toString(),
                            history.lastUsedAt.toString(),
                            history.createdAt.toString(),
                            history.updatedAt.toString()
                        )
                    )
                }
            }
        }
    }

    fun read(inputStream: InputStream): DatabaseSnapshot {
        val csvEntries = mutableMapOf<String, String>()
        ZipInputStream(inputStream.buffered()).use { zipInputStream ->
            while (true) {
                val entry = zipInputStream.nextEntry ?: break
                if (!entry.isDirectory) {
                    csvEntries[entry.name] = zipInputStream.readEntryAsString()
                }
                zipInputStream.closeEntry()
            }
        }

        val categoriesText = csvEntries[CATEGORY_ENTRY] ?: error("导入压缩包缺少 $CATEGORY_ENTRY")
        val transactionsText = csvEntries[TRANSACTION_ENTRY] ?: error("导入压缩包缺少 $TRANSACTION_ENTRY")
        val noteHistoriesText = csvEntries[NOTE_HISTORY_ENTRY] ?: error("导入压缩包缺少 $NOTE_HISTORY_ENTRY")

        return DatabaseSnapshot(
            categories = categoriesText.readCsvRows(CATEGORY_HEADERS).map(::parseCategoryRow),
            transactions = transactionsText.readCsvRows(TRANSACTION_HEADERS).map(::parseTransactionRow),
            noteHistories = noteHistoriesText.readCsvRows(NOTE_HISTORY_HEADERS).map(::parseNoteHistoryRow)
        )
    }
}

private val CATEGORY_HEADERS = listOf("id", "name", "type", "iconKey", "isDefault", "sortOrder")
private val TRANSACTION_HEADERS = listOf(
    "id",
    "type",
    "amount",
    "categoryId",
    "note",
    "dateTime",
    "createdAt",
    "updatedAt"
)
private val NOTE_HISTORY_HEADERS = listOf(
    "id",
    "categoryId",
    "note",
    "usageCount",
    "lastUsedAt",
    "createdAt",
    "updatedAt"
)

private fun parseCategoryRow(row: Map<String, String>): CategoryEntity {
    return CategoryEntity(
        id = row.requiredLong("id"),
        name = row.required("name"),
        type = row.requiredTransactionType("type"),
        iconKey = row.optional("iconKey"),
        isDefault = row.requiredBoolean("isDefault"),
        sortOrder = row.requiredInt("sortOrder")
    )
}

private fun parseTransactionRow(row: Map<String, String>): TransactionEntity {
    return TransactionEntity(
        id = row.requiredLong("id"),
        type = row.requiredTransactionType("type"),
        amount = row.requiredDouble("amount"),
        categoryId = row.requiredLong("categoryId"),
        note = row.required("note"),
        dateTime = row.requiredLong("dateTime"),
        createdAt = row.requiredLong("createdAt"),
        updatedAt = row.requiredLong("updatedAt")
    )
}

private fun parseNoteHistoryRow(row: Map<String, String>): CategoryNoteHistoryEntity {
    return CategoryNoteHistoryEntity(
        id = row.requiredLong("id"),
        categoryId = row.requiredLong("categoryId"),
        note = row.required("note"),
        usageCount = row.requiredInt("usageCount"),
        lastUsedAt = row.requiredLong("lastUsedAt"),
        createdAt = row.requiredLong("createdAt"),
        updatedAt = row.requiredLong("updatedAt")
    )
}

private fun org.apache.poi.ss.usermodel.Sheet.writeHeader(headers: List<String>) {
    createRow(0).writeCells(headers)
}

private fun Row.writeCells(values: List<String>) {
    values.forEachIndexed { index, value ->
        createCell(index).setCellValue(value)
    }
}

private fun org.apache.poi.ss.usermodel.Sheet.readRows(
    formatter: DataFormatter,
    expectedHeaders: List<String>
): List<Map<String, String>> {
    val headerRow = getRow(0) ?: error("工作表 $sheetName 缺少表头")
    val actualHeaders = headerRow.toValues(formatter)
    require(actualHeaders == expectedHeaders) {
        "工作表 $sheetName 表头不匹配，应为 ${expectedHeaders.joinToString()}"
    }

    val rows = mutableListOf<Map<String, String>>()
    for (rowIndex in 1..lastRowNum) {
        val row = getRow(rowIndex) ?: continue
        val values = row.toValues(formatter, expectedHeaders.size)
        if (values.all { it.isBlank() }) continue
        rows += expectedHeaders.zip(values).toMap()
    }
    return rows
}

private fun Row.toValues(
    formatter: DataFormatter,
    expectedSize: Int = lastCellNum.toInt().coerceAtLeast(0)
): List<String> {
    return (0 until expectedSize).map { index ->
        formatter.formatCellValue(getCell(index)).trim()
    }
}

private fun ZipOutputStream.writeCsvEntry(
    entryName: String,
    headers: List<String>,
    block: (BufferedWriter) -> Unit
) {
    putNextEntry(ZipEntry(entryName))
    OutputStreamWriter(this, StandardCharsets.UTF_8).buffered().useWithoutClosing { writer ->
        writer.writeCsvRow(headers)
        block(writer)
        writer.flush()
    }
    closeEntry()
}

private fun BufferedWriter.writeCsvRow(values: List<String>) {
    write(
        values.joinToString(",") { value ->
            value.toCsvField()
        }
    )
    newLine()
}

private fun String.readCsvRows(expectedHeaders: List<String>): List<Map<String, String>> {
    val rows = CsvParser.parse(this).filterNot { row -> row.all { it.isBlank() } }
    require(rows.isNotEmpty()) { "CSV 文件为空" }
    require(rows.first() == expectedHeaders) {
        "CSV 表头不匹配，应为 ${expectedHeaders.joinToString()}"
    }

    return rows.drop(1).map { row ->
        require(row.size == expectedHeaders.size) {
            "CSV 列数不正确，期望 ${expectedHeaders.size} 列，实际 ${row.size} 列"
        }
        expectedHeaders.zip(row).toMap()
    }
}

private fun String.toCsvField(): String {
    val escaped = replace("\"", "\"\"")
    return if (contains(',') || contains('"') || contains('\n') || contains('\r')) {
        "\"$escaped\""
    } else {
        escaped
    }
}

private fun ZipInputStream.readEntryAsString(): String {
    val bytes = readBytes()
    return bytes.toString(StandardCharsets.UTF_8)
}

private object CsvParser {
    fun parse(content: String): List<List<String>> {
        if (content.isEmpty()) return emptyList()

        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < content.length) {
            val char = content[index]
            if (inQuotes) {
                if (char == '"') {
                    if (index + 1 < content.length && content[index + 1] == '"') {
                        currentCell.append('"')
                        index += 1
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentCell.append(char)
                }
            } else {
                when (char) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow += currentCell.toString()
                        currentCell.clear()
                    }

                    '\r' -> {
                        if (index + 1 < content.length && content[index + 1] == '\n') {
                            index += 1
                        }
                        currentRow += currentCell.toString()
                        currentCell.clear()
                        rows += currentRow.toList()
                        currentRow.clear()
                    }

                    '\n' -> {
                        currentRow += currentCell.toString()
                        currentCell.clear()
                        rows += currentRow.toList()
                        currentRow.clear()
                    }

                    else -> currentCell.append(char)
                }
            }
            index += 1
        }

        if (inQuotes) {
            error("CSV 格式错误：存在未闭合的引号")
        }

        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow += currentCell.toString()
            rows += currentRow.toList()
        }

        return rows
    }
}

private fun <T> Map<String, T>.required(key: String): String {
    val value = this[key]?.toString()?.trim()
    require(!value.isNullOrEmpty()) { "字段 $key 不能为空" }
    return value
}

private fun Map<String, String>.optional(key: String): String? {
    return this[key]?.trim()?.takeIf { it.isNotEmpty() }
}

private fun Map<String, String>.requiredLong(key: String): Long {
    return required(key).toLongOrNull() ?: error("字段 $key 不是有效的 Long")
}

private fun Map<String, String>.requiredInt(key: String): Int {
    return required(key).toIntOrNull() ?: error("字段 $key 不是有效的 Int")
}

private fun Map<String, String>.requiredDouble(key: String): Double {
    return required(key).toDoubleOrNull() ?: error("字段 $key 不是有效的 Double")
}

private fun Map<String, String>.requiredBoolean(key: String): Boolean {
    return when (required(key).lowercase()) {
        "true" -> true
        "false" -> false
        else -> error("字段 $key 不是有效的 Boolean")
    }
}

private fun Map<String, String>.requiredTransactionType(key: String): TransactionType {
    return runCatching { TransactionType.valueOf(required(key)) }
        .getOrElse { error("字段 $key 不是有效的交易类型") }
}

private inline fun <T : AutoCloseable?, R> T.useWithoutClosing(block: (T) -> R): R {
    return block(this)
}
