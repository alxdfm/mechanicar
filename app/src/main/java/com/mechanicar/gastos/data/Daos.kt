package com.mechanicar.gastos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicle WHERE id = :id LIMIT 1")
    fun observe(id: Long = VehicleEntity.SINGLE_VEHICLE_ID): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicle WHERE id = :id LIMIT 1")
    suspend fun get(id: Long = VehicleEntity.SINGLE_VEHICLE_ID): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vehicle: VehicleEntity)
}

@Dao
interface VisitDao {
    @Transaction
    @Query("SELECT * FROM visits ORDER BY dateEpochDay DESC, id DESC")
    fun observeVisits(): Flow<List<VisitWithServices>>

    @Transaction
    @Query("SELECT * FROM visits WHERE id = :id")
    fun observeVisit(id: Long): Flow<VisitWithServices?>

    @Transaction
    @Query("SELECT * FROM visits WHERE id = :id")
    suspend fun getVisit(id: Long): VisitWithServices?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: VisitEntity): Long

    @Update
    suspend fun update(visit: VisitEntity)

    @Delete
    suspend fun delete(visit: VisitEntity)

    @Query("DELETE FROM visits")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM visits")
    suspend fun count(): Int
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services WHERE visitId = :visitId ORDER BY id ASC")
    fun observeForVisit(visitId: Long): Flow<List<ServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(service: ServiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(services: List<ServiceEntity>): List<Long>

    @Update
    suspend fun update(service: ServiceEntity)

    @Delete
    suspend fun delete(service: ServiceEntity)

    @Query("DELETE FROM services WHERE visitId = :visitId")
    suspend fun deleteForVisit(visitId: Long)

    @Query("DELETE FROM services")
    suspend fun deleteAll()

    @Query(
        """
        SELECT category AS category, SUM(quantity * unitPrice) AS total
        FROM services
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeCategoryTotals(): Flow<List<CategoryTotal>>

    @Query("SELECT COUNT(*) FROM services")
    fun observeServiceCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity * unitPrice), 0) FROM services")
    fun observeTotalSpent(): Flow<Double>
}
