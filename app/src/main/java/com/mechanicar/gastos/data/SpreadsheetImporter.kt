package com.mechanicar.gastos.data

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.time.LocalDate
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
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return emptyList()
        return parseBytes(bytes, guessIsXlsx(context, uri, bytes))
    }

    fun parseBytes(bytes: ByteArray, forceXlsx: Boolean = false): List<ImportRow> {
        return if (forceXlsx || looksLikeXlsx(bytes)) {
            parseXlsx(ByteArrayInputStream(bytes))
        } else {
            parseCsv(ByteArrayInputStream(bytes))
        }
    }

    fun parseCsv(input: InputStream): List<ImportRow> {
        val reader = BufferedReader(InputStreamReader(input, Charset.forName("UTF-8")))
        val lines = reader.readLines()
            .map { it.removePrefix("\uFEFF") }
            .filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val header = splitCsvLine(lines.first()).map { normalizeHeader(it) }
        val index = headerIndex(header)

        return lines.drop(1).mapNotNull { line ->
            rowFromColumns(splitCsvLine(line), index)
        }
    }

    fun parseXlsx(input: InputStream): List<ImportRow> {
        val matrix = XlsxReader.readFirstOrNamedSheet(input, preferredSheetName = "Importacao")
        if (matrix.isEmpty()) return emptyList()

        val header = matrix.first().map { normalizeHeader(it) }
        val index = headerIndex(header)
        return matrix.drop(1).mapNotNull { cols -> rowFromColumns(cols, index) }
    }

    private fun guessIsXlsx(context: Context, uri: Uri, bytes: ByteArray): Boolean {
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
        val name = uri.lastPathSegment?.lowercase(Locale.ROOT).orEmpty()
        return looksLikeXlsx(bytes) ||
            mime.contains("sheet") ||
            mime.contains("excel") ||
            name.endsWith(".xlsx") ||
            name.endsWith(".xls")
    }

    private fun looksLikeXlsx(bytes: ByteArray): Boolean {
        // ZIP magic used by .xlsx
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte()
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
            odometerKm = parseNumber(col("km"))?.toInt(),
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
        // Excel serial number (days since 1899-12-30)
        parseNumber(value)?.let { serial ->
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
