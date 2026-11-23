package com.mechanicar.gastos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mechanicar.gastos.data.VehicleEntity
import com.mechanicar.gastos.ui.AppViewModel
import com.mechanicar.gastos.util.Formatters

@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onOpenVisits: () -> Unit,
    onEditVehicle: () -> Unit
) {
    val stats by viewModel.dashboard.collectAsState()
    val vehicle by viewModel.vehicle.collectAsState()
    val maxCategory = stats.byCategory.maxOfOrNull { it.total } ?: 1.0
    val vehicleLabel = vehicle?.displayName ?: VehicleEntity.defaultCorsa().displayName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Column {
                Text(
                    text = "Gastos Corsa",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onEditVehicle)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                    Text(
                        text = vehicleLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar veículo",
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
                val vehicleColor = vehicle?.color
                if (!vehicleColor.isNullOrBlank()) {
                    Text(
                        text = vehicleColor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                    )
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Total investido",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Text(
                    text = Formatters.money(stats.totalSpent),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    MetricChip("${stats.visitCount}", "visitas")
                    MetricChip("${stats.serviceCount}", "serviços")
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            TextButton(onClick = onEditVehicle) {
                Text("Editar dados do carro")
            }

            if (stats.visitCount == 0) {
                Text(
                    text = "Nenhum gasto cadastrado ainda.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Importe a planilha inicial ou registre a primeira visita ao mecânico.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onOpenVisits) { Text("Ver visitas") }
            } else {
                SectionTitle("Por categoria")
                Spacer(Modifier.height(12.dp))
                stats.byCategory.forEach { item ->
                    CategoryBar(
                        label = item.category,
                        value = item.total,
                        fraction = (item.total / maxCategory).toFloat().coerceIn(0f, 1f)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(12.dp))
                SectionTitle("Por ano")
                Spacer(Modifier.height(12.dp))
                stats.byYear.forEach { year ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(year.year.toString(), style = MaterialTheme.typography.titleMedium)
                        Text(Formatters.money(year.total), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                val minKm = stats.minKm
                val maxKm = stats.maxKm
                if (minKm != null && maxKm != null) {
                    Spacer(Modifier.height(12.dp))
                    SectionTitle("Quilometragem")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${Formatters.km(minKm)} → ${Formatters.km(maxKm)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    val delta = maxKm - minKm
                    if (delta > 0) {
                        Text(
                            text = "Período com KM informado: %,d km".format(delta),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onOpenVisits) { Text("Abrir histórico de visitas") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricChip(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineMedium)
}

@Composable
private fun CategoryBar(label: String, value: Double, fraction: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(Formatters.money(value), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
