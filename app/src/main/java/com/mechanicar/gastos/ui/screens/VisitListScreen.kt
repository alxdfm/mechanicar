package com.mechanicar.gastos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mechanicar.gastos.data.VisitWithServices
import com.mechanicar.gastos.ui.AppViewModel
import com.mechanicar.gastos.util.Formatters

@Composable
fun VisitListScreen(
    viewModel: AppViewModel,
    onOpenVisit: (Long) -> Unit
) {
    val visits by viewModel.visits.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Manutenções", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "Cada manutenção no mecânico com serviços, valores e classificação.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        if (visits.isEmpty()) {
            Text(
                text = "Nenhuma manutenção cadastrada. Toque em + para criar ou importe a planilha.",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visits, key = { it.visit.id }) { visit ->
                    VisitRow(visit = visit, onClick = { onOpenVisit(visit.visit.id) })
                }
            }
        }
    }
}

@Composable
private fun VisitRow(visit: VisitWithServices, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = Formatters.date(visit.visit.dateEpochDay),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = Formatters.money(visit.total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(6.dp))
        val subtitle = buildList {
            if (visit.visit.origin.isNotBlank()) add(visit.visit.origin)
            if (visit.visit.workshop.isNotBlank()) add(visit.visit.workshop)
            add("${visit.services.size} serviço(s)")
        }.joinToString(" · ")
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (visit.visit.odometerKm != null) {
            Text(
                text = Formatters.km(visit.visit.odometerKm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
