package com.mechanicar.gastos

import android.app.Application
import com.mechanicar.gastos.data.AppDatabase
import com.mechanicar.gastos.data.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GastosCorsaApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: ExpenseRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = ExpenseRepository(db.vehicleDao(), db.visitDao(), db.serviceDao())
        appScope.launch {
            repository.ensureDefaultVehicle()
        }
    }
}
