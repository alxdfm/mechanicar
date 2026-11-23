package com.mechanicar.gastos.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val odometerKm: Int? = null,
    val origin: String = "",
    val workshop: String = "",
    val notes: String = ""
)

@Entity(
    tableName = "services",
    foreignKeys = [
        ForeignKey(
            entity = VisitEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("visitId"), Index("category")]
)
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visitId: Long,
    val category: String,
    val itemName: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val notes: String = ""
) {
    val subtotal: Double get() = quantity * unitPrice
}

data class VisitWithServices(
    @Embedded val visit: VisitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "visitId"
    )
    val services: List<ServiceEntity>
) {
    val total: Double get() = services.sumOf { it.subtotal }
}

data class CategoryTotal(
    val category: String,
    val total: Double
)

data class YearTotal(
    val year: Int,
    val total: Double
)

data class DashboardStats(
    val totalSpent: Double,
    val visitCount: Int,
    val serviceCount: Int,
    val byCategory: List<CategoryTotal>,
    val byYear: List<YearTotal>,
    val minKm: Int?,
    val maxKm: Int?
)
