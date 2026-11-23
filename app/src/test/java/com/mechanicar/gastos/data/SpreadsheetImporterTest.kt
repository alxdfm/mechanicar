package com.mechanicar.gastos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class SpreadsheetImporterTest {

    @Test
    fun parsesCsvAndGroupsReadyRows() {
        val csv = """
            data,km,origem,oficina,categoria,item,quantidade,valor_unitario,observacao
            07/07/2023,,Orçamento/recibo,,Revisão/Óleo e filtros,Óleo,1,140,
            07/07/2023,,Orçamento/recibo,,Mão de obra,Mão de obra,1,240,teste
            04/07/2024,135047,OS 31,Giro Car Service,Suspensão,Coxim,1,"163,00",
        """.trimIndent()

        val rows = SpreadsheetImporter.parseCsv(
            ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8))
        )

        assertEquals(3, rows.size)
        assertEquals("Óleo", rows[0].itemName)
        assertEquals(140.0, rows[0].unitPrice, 0.001)
        assertEquals(135047, rows[2].odometerKm)
        assertEquals(163.0, rows[2].unitPrice, 0.001)
        assertTrue(rows[1].notes.contains("teste"))
    }

    @Test
    fun ignoresRowsWithoutItemOrDate() {
        val csv = """
            data,categoria,item,quantidade,valor_unitario
            ,Motor,Junta,1,10
            01/01/2024,Motor,,1,10
            01/01/2024,Motor,Junta,1,10
        """.trimIndent()

        val rows = SpreadsheetImporter.parseCsv(
            ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8))
        )
        assertEquals(1, rows.size)
        assertEquals("Junta", rows[0].itemName)
    }
}
