package com.mechanicar.gastos.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class ExpenseRepository(
    private val vehicleDao: VehicleDao,
    private val visitDao: VisitDao,
    private val serviceDao: ServiceDao
) {
    fun observeVehicle(): Flow<VehicleEntity?> = vehicleDao.observe()

    suspend fun ensureDefaultVehicle() {
        if (vehicleDao.get() == null) {
            vehicleDao.upsert(VehicleEntity.defaultCorsa())
        }
    }

    suspend fun saveVehicle(vehicle: VehicleEntity) {
        vehicleDao.upsert(vehicle.copy(id = VehicleEntity.SINGLE_VEHICLE_ID))
    }

    fun observeVisits(): Flow<List<VisitWithServices>> = visitDao.observeVisits()

    fun observeVisit(id: Long): Flow<VisitWithServices?> = visitDao.observeVisit(id)

    fun observeDashboard(): Flow<DashboardStats> {
        return combine(
            visitDao.observeVisits(),
            serviceDao.observeCategoryTotals(),
            serviceDao.observeTotalSpent(),
            serviceDao.observeServiceCount()
        ) { visits, categories, total, serviceCount ->
            val byYear = visits
                .groupBy { LocalDate.ofEpochDay(it.visit.dateEpochDay).year }
                .map { (year, list) -> YearTotal(year, list.sumOf { it.total }) }
                .sortedByDescending { it.year }

            val kms = visits.mapNotNull { it.visit.odometerKm }
            DashboardStats(
                totalSpent = total,
                visitCount = visits.size,
                serviceCount = serviceCount,
                byCategory = categories,
                byYear = byYear,
                minKm = kms.minOrNull(),
                maxKm = kms.maxOrNull()
            )
        }
    }

    suspend fun saveVisit(
        visitId: Long?,
        date: LocalDate,
        odometerKm: Int?,
        origin: String,
        workshop: String,
        notes: String,
        services: List<ServiceDraft>
    ): Long {
        val entity = VisitEntity(
            id = visitId ?: 0,
            dateEpochDay = date.toEpochDay(),
            odometerKm = odometerKm,
            origin = origin.trim(),
            workshop = workshop.trim(),
            notes = notes.trim()
        )
        val id = if (visitId == null || visitId == 0L) {
            visitDao.insert(entity)
        } else {
            visitDao.update(entity)
            serviceDao.deleteForVisit(visitId)
            visitId
        }
        if (services.isNotEmpty()) {
            serviceDao.insertAll(
                services.map {
                    ServiceEntity(
                        visitId = id,
                        category = it.category,
                        itemName = it.itemName.trim(),
                        quantity = it.quantity,
                        unitPrice = it.unitPrice,
                        notes = it.notes.trim()
                    )
                }
            )
        }
        return id
    }

    suspend fun deleteVisit(visit: VisitEntity) {
        visitDao.delete(visit)
    }

    suspend fun clearAll() {
        serviceDao.deleteAll()
        visitDao.deleteAll()
    }

    suspend fun importRows(rows: List<ImportRow>, replaceExisting: Boolean): ImportResult {
        if (replaceExisting) {
            clearAll()
        }

        val grouped = rows.groupBy {
            Triple(it.date.toEpochDay(), it.origin.trim(), it.workshop.trim())
        }

        var visitsCreated = 0
        var servicesCreated = 0

        grouped.forEach { (key, items) ->
            val (epochDay, origin, workshop) = key
            val km = items.mapNotNull { it.odometerKm }.maxOrNull()
            val notes = items.map { it.notes }.filter { it.isNotBlank() }.distinct().joinToString(" | ")
            val visitId = visitDao.insert(
                VisitEntity(
                    dateEpochDay = epochDay,
                    odometerKm = km,
                    origin = origin,
                    workshop = workshop,
                    notes = notes
                )
            )
            visitsCreated++
            val services = items.map {
                ServiceEntity(
                    visitId = visitId,
                    category = ServiceCategory.fromLabel(it.category).label,
                    itemName = it.itemName,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    notes = it.notes
                )
            }
            serviceDao.insertAll(services)
            servicesCreated += services.size
        }

        return ImportResult(visitsCreated, servicesCreated)
    }

    suspend fun hasData(): Boolean = visitDao.count() > 0
}

data class ServiceDraft(
    val category: String,
    val itemName: String,
    val quantity: Double,
    val unitPrice: Double,
    val notes: String = ""
)

data class ImportRow(
    val date: LocalDate,
    val odometerKm: Int?,
    val origin: String,
    val workshop: String,
    val category: String,
    val itemName: String,
    val quantity: Double,
    val unitPrice: Double,
    val notes: String
)

data class ImportResult(
    val visitsCreated: Int,
    val servicesCreated: Int
)
