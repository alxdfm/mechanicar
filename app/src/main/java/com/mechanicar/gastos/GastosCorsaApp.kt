package com.mechanicar.gastos

import android.app.Application
import com.mechanicar.gastos.data.AppDatabase
import com.mechanicar.gastos.data.ExpenseRepository

class GastosCorsaApp : Application() {
    lateinit var repository: ExpenseRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = ExpenseRepository(db.visitDao(), db.serviceDao())
    }
}
