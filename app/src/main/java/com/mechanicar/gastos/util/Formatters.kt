package com.mechanicar.gastos.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val ptBr = Locale("pt", "BR")
    private val currency = NumberFormat.getCurrencyInstance(ptBr)
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBr)
    private val monthYearFmt = DateTimeFormatter.ofPattern("MMM yyyy", ptBr)

    fun money(value: Double): String = currency.format(value)

    fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFmt)

    fun date(date: LocalDate): String = date.format(dateFmt)

    fun monthYear(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(monthYearFmt)

    fun km(value: Int?): String = value?.let { "%,d km".format(ptBr, it) } ?: "KM não informado"
}
