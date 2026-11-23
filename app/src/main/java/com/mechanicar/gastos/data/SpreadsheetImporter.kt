package com.mechanicar.gastos.data

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object SpreadsheetImporter {

    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    )

    fun importFromAssets(context: Context, assetName: String = "gastos_corsa_importacao.csv"): List<ImportRow> {
        return context.assets.open(assetName).use { parseCsv(it) }
    }

    fun importFromUri(context: Context, uri: Uri): List<ImportRow> {
        val name = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
        val display = uri.lastPathSegment?.lowercase(Locale.ROOT).orEmpty()
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            when {
                name.contains("sheet") || display.endsWith(".xlsx") || display.endsWith(".xls") ->
                    parseXlsx(stream)
                else -> parseCsv(stream)
            }
        } ?: emptyList()
    }

    fun parseCsv(input: InputStream): List<ImportRow> {
        val reader = BufferedReader(InputStreamReader(input, Charset.forName("UTF-8")))
        val lines = reader.readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val header = splitCsvLine(lines.first()).map { normalizeHeader(it) }
        val index = headerIndex(header)

        return lines.drop(1).mapNotNull { line ->
            val cols = splitCsvLine(line)
            rowFromColumns(cols, index)
        }
    }

    fun parseXlsx(input: InputStream): List<ImportRow> {
        val workbook = XSSFWorkbook(input)
        val sheet = workbook.sheetIterator().asSequence()
            .firstOrNull { it.sheetName.equals("Importacao", ignoreCase = true) }
            ?: workbook.getSheetAt(0)

        val formatter = DataFormatter(Locale("pt", "BR"))
        val rows = sheet.iterator().asSequence().toList()
        if (rows.isEmpty()) {
            workbook.close()
            return emptyList()
        }

        val headerRow = rows.first()
        val header = headerRow.map { normalizeHeader(formatter.formatCellValue(it)) }
        val index = headerIndex(header)

        val result = rows.drop(1).mapNotNull { row ->
            val cols = MutableList(header.size) { "" }
            row.forEach { cell ->
                val idx = cell.columnIndex
                if (idx < cols.size) {
                    cols[idx] = when {
                        DateUtil.isCellDateFormatted(cell) -> {
                            cell.localDateTimeCellValue.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        }
                        else -> formatter.formatCellValue(cell).trim()
                    }
                }
            }
            // Prefer real date cells when present
            index["data"]?.let { dataIdx ->
                val cell = row.getCell(dataIdx)
                if (cell != null && DateUtil.isCellDateFormatted(cell)) {
                    val date = cell.dateCellValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    cols[dataIdx] = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                }
            }
            rowFromColumns(cols, index)
        }
        workbook.close()
        return result
    }

    private fun headerIndex(header: List<String>): Map<String, Int> {
        fun find(vararg aliases: String): Int? {
            return header.indexOfFirst { h -> aliases.any { alias -> h == alias } }
                .takeIf { it >= 0 }
        }
        return buildMap {
            find("data", "date")?.let { put("data", it) }
            find("km", "odometro", "odometer", "quilometragem")?.let { put("km", it) }
            find("origem", "origin", "os")?.let { put("origem", it) }
            find("oficina", "workshop", "mecanico")?.let { put("oficina", it) }
            find("categoria", "category", "classificacao")?.let { put("categoria", it) }
            find("item", "servico", "descricao", "serviço")?.let { put("item", it) }
            find("quantidade", "qtd", "qty", "quantity")?.let { put("quantidade", it) }
            find("valor_unitario", "valor_unit", "preco", "preco_unitario", "unit_price")
                ?.let { put("valor", it) }
            find("observacao", "obs", "notes", "observação")?.let { put("obs", it) }
        }
    }

    private fun rowFromColumns(cols: List<String>, index: Map<String, Int>): ImportRow? {
        fun col(key: String): String = index[key]?.let { cols.getOrNull(it)?.trim().orEmpty() }.orEmpty()

        val date = parseDate(col("data")) ?: return null
        val item = col("item")
        if (item.isBlank()) return null

        return ImportRow(
            date = date,
            odometerKm = col("km").toIntOrNull(),
            origin = col("origem"),
            workshop = col("oficina"),
            category = col("categoria").ifBlank { ServiceCategory.OUTROS.label },
            itemName = item,
            quantity = parseNumber(col("quantidade")) ?: 1.0,
            unitPrice = parseNumber(col("valor")) ?: 0.0,
            notes = col("obs")
        )
    }

    private fun normalizeHeader(raw: String): String {
        val normalized = raw.trim()
            .lowercase(Locale.ROOT)
            .replace("á", "a")
            .replace("à", "a")
            .replace("ã", "a")
            .replace("â", "a")
            .replace("é", "e")
            .replace("ê", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ô", "o")
            .replace("õ", "o")
            .replace("ú", "u")
            .replace("ç", "c")
            .replace('.', ' ')
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .replace(" ", "_")

        return when (normalized) {
            "valor_unit", "valor_unitario", "preco_unitario", "unit_price" -> "valor_unitario"
            else -> normalized
        }
    }

    private fun parseDate(raw: String): LocalDate? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        dateFormats.forEach { fmt ->
            try {
                return LocalDate.parse(value, fmt)
            } catch (_: DateTimeParseException) {
            }
        }
        // Excel serial as plain number in CSV edge-case
        value.toDoubleOrNull()?.let { serial ->
            if (serial > 20000) {
                return LocalDate.of(1899, 12, 30).plusDays(serial.toLong())
            }
        }
        return null
    }

    private fun parseNumber(raw: String): Double? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        val normalized = value
            .replace("R$", "", ignoreCase = true)
            .replace(" ", "")
            .let {
                if (it.contains(',') && it.contains('.')) {
                    // 1.234,56
                    it.replace(".", "").replace(',', '.')
                } else if (it.contains(',')) {
                    it.replace(',', '.')
                } else {
                    it
                }
            }
        return normalized.toDoubleOrNull()
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                (c == ',' || c == ';') && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }
}
