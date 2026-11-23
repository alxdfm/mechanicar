package com.mechanicar.gastos.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Leitor mínimo de .xlsx (OOXML) para planilhas tabulares simples.
 * Evita Apache POI, que costuma crashar no Android.
 */
internal object XlsxReader {

    fun readFirstOrNamedSheet(input: InputStream, preferredSheetName: String = "Importacao"): List<List<String>> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val sheetPath = resolveSheetPath(entries, preferredSheetName)
            ?: entries.keys.firstOrNull { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            ?: throw IllegalArgumentException("Planilha .xlsx sem abas legíveis.")

        val sheetBytes = entries[sheetPath]
            ?: throw IllegalArgumentException("Não foi possível ler a aba da planilha.")
        return parseSheet(sheetBytes.inputStream(), sharedStrings)
    }

    private fun newParser(namespaces: Boolean): XmlPullParser {
        return XmlPullParserFactory.newInstance().newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, namespaces)
        }
    }

    private fun resolveSheetPath(entries: Map<String, ByteArray>, preferredSheetName: String): String? {
        val workbook = entries["xl/workbook.xml"] ?: return null
        val rels = entries["xl/_rels/workbook.xml.rels"] ?: return null

        val sheets = parseWorkbookSheets(workbook.inputStream())
        val targets = parseWorkbookRels(rels.inputStream())
        if (sheets.isEmpty()) return null

        val preferred = sheets.firstOrNull { it.name.equals(preferredSheetName, ignoreCase = true) }
            ?: sheets.first()
        val target = targets[preferred.relId] ?: return null
        return when {
            target.startsWith("/xl/") -> target.removePrefix("/")
            target.startsWith("xl/") -> target
            target.startsWith("/") -> target.removePrefix("/")
            else -> "xl/$target"
        }
    }

    private data class SheetRef(val name: String, val relId: String)

    private fun parseWorkbookSheets(input: InputStream): List<SheetRef> {
        val parser = newParser(namespaces = true).apply { setInput(input, null) }
        val result = mutableListOf<SheetRef>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && localName(parser) == "sheet") {
                val name = parser.getAttributeValue(null, "name").orEmpty()
                var relId = ""
                for (i in 0 until parser.attributeCount) {
                    if (parser.getAttributeName(i) == "id") {
                        relId = parser.getAttributeValue(i).orEmpty()
                        break
                    }
                }
                if (name.isNotBlank() && relId.isNotBlank()) {
                    result += SheetRef(name, relId)
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseWorkbookRels(input: InputStream): Map<String, String> {
        val parser = newParser(namespaces = false).apply { setInput(input, null) }
        val result = mutableMapOf<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && localName(parser) == "Relationship") {
                val id = parser.getAttributeValue(null, "Id").orEmpty()
                val target = parser.getAttributeValue(null, "Target").orEmpty()
                if (id.isNotBlank() && target.isNotBlank()) {
                    result[id] = target
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val parser = newParser(namespaces = false).apply { setInput(bytes.inputStream(), null) }
        val strings = mutableListOf<String>()
        val current = StringBuilder()
        var insideSi = false
        var insideT = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (localName(parser)) {
                    "si" -> {
                        insideSi = true
                        current.clear()
                    }
                    "t" -> if (insideSi) insideT = true
                }
                XmlPullParser.TEXT -> if (insideT) current.append(parser.text)
                XmlPullParser.END_TAG -> when (localName(parser)) {
                    "t" -> insideT = false
                    "si" -> {
                        strings += current.toString()
                        insideSi = false
                    }
                }
            }
            event = parser.next()
        }
        return strings
    }

    private fun parseSheet(input: InputStream, sharedStrings: List<String>): List<List<String>> {
        val parser = newParser(namespaces = false).apply { setInput(input, null) }

        val rows = linkedMapOf<Int, MutableMap<Int, String>>()
        var currentRow = -1
        var currentCol = -1
        var cellType = ""
        var readingText = false
        val valueBuffer = StringBuilder()

        fun commitCell() {
            if (currentRow < 0 || currentCol < 0) return
            val raw = valueBuffer.toString()
            val text = when (cellType) {
                "s" -> raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) }.orEmpty()
                else -> raw
            }
            rows.getOrPut(currentRow) { mutableMapOf() }[currentCol] = text
        }

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (localName(parser)) {
                    "row" -> {
                        currentRow = parser.getAttributeValue(null, "r")?.toIntOrNull()?.minus(1)
                            ?: (currentRow + 1)
                    }
                    "c" -> {
                        currentCol = columnIndexFromRef(parser.getAttributeValue(null, "r").orEmpty())
                        cellType = parser.getAttributeValue(null, "t").orEmpty()
                        valueBuffer.clear()
                        readingText = false
                    }
                    "v" -> readingText = true
                    "t" -> {
                        // shared string parts, inlineStr <is><t>..., rich text, etc.
                        if (cellType == "inlineStr" || cellType == "s" || cellType.isEmpty()) {
                            readingText = true
                        }
                    }
                }
                XmlPullParser.TEXT -> if (readingText) valueBuffer.append(parser.text)
                XmlPullParser.END_TAG -> when (localName(parser)) {
                    "v", "t" -> readingText = false
                    "c" -> {
                        commitCell()
                        currentCol = -1
                        cellType = ""
                        readingText = false
                    }
                }
            }
            event = parser.next()
        }

        if (rows.isEmpty()) return emptyList()
        val maxCol = rows.values.maxOf { row -> row.keys.maxOrNull() ?: 0 }
        return rows.toSortedMap().values.map { rowMap ->
            List(maxCol + 1) { idx -> rowMap[idx].orEmpty() }
        }
    }

    private fun localName(parser: XmlPullParser): String {
        val name = parser.name
        val idx = name.indexOf(':')
        return if (idx >= 0) name.substring(idx + 1) else name
    }

    private fun columnIndexFromRef(ref: String): Int {
        var col = 0
        for (ch in ref) {
            if (!ch.isLetter()) break
            col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return (col - 1).coerceAtLeast(0)
    }
}
