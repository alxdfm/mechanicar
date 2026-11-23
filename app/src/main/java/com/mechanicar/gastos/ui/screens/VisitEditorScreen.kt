package com.mechanicar.gastos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mechanicar.gastos.data.ServiceCategory
import com.mechanicar.gastos.data.ServiceDraft
import com.mechanicar.gastos.data.VisitWithServices
import com.mechanicar.gastos.ui.AppViewModel
import com.mechanicar.gastos.util.Formatters
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.flowOf

private data class ServiceForm(
    var category: String = ServiceCategory.REVISAO_OLEO.label,
    var itemName: String = "",
    var quantity: String = "1",
    var unitPrice: String = "",
    var notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitEditorScreen(
    visitId: Long?,
    viewModel: AppViewModel,
    onDone: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val existingFlow = remember(visitId) {
        if (visitId != null) viewModel.visitFlow(visitId) else flowOf<VisitWithServices?>(null)
    }
    val existing by existingFlow.collectAsState(initial = null)

    var dateText by remember { mutableStateOf(Formatters.date(LocalDate.now())) }
    var kmText by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("") }
    var workshop by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val services = remember { mutableStateListOf(ServiceForm()) }
    var hydrated by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        val visit = existing
        if (visit != null && !hydrated) {
            dateText = Formatters.date(visit.visit.dateEpochDay)
            kmText = visit.visit.odometerKm?.toString().orEmpty()
            origin = visit.visit.origin
            workshop = visit.visit.workshop
            notes = visit.visit.notes
            services.clear()
            if (visit.services.isEmpty()) {
                services.add(ServiceForm())
            } else {
                visit.services.forEach {
                    services.add(
                        ServiceForm(
                            category = it.category,
                            itemName = it.itemName,
                            quantity = formatEditableNumber(it.quantity),
                            unitPrice = formatEditableNumber(it.unitPrice),
                            notes = it.notes
                        )
                    )
                }
            }
            hydrated = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (visitId == null) "Nova consulta" else "Editar consulta")
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Data (DD/MM/AAAA)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = kmText,
                onValueChange = { kmText = it.filter { ch -> ch.isDigit() } },
                label = { Text("KM (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = origin,
                onValueChange = { origin = it },
                label = { Text("Origem (ex.: OS 31, Orçamento/recibo)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = workshop,
                onValueChange = { workshop = it },
                label = { Text("Oficina") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Observações da consulta") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("Serviços do dia", style = MaterialTheme.typography.titleLarge)

            services.forEachIndexed { index, form ->
                ServiceEditorCard(
                    index = index,
                    form = form,
                    canDelete = services.size > 1,
                    onChange = { services[index] = it },
                    onDelete = { services.removeAt(index) }
                )
            }

            OutlinedButton(
                onClick = { services.add(ServiceForm()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Adicionar serviço")
            }

            Button(
                onClick = {
                    val date = parseDate(dateText) ?: return@Button
                    val drafts = services.mapNotNull { form ->
                        val name = form.itemName.trim()
                        val qty = parseDecimal(form.quantity) ?: return@mapNotNull null
                        val price = parseDecimal(form.unitPrice) ?: return@mapNotNull null
                        if (name.isBlank()) return@mapNotNull null
                        ServiceDraft(
                            category = form.category,
                            itemName = name,
                            quantity = qty,
                            unitPrice = price,
                            notes = form.notes
                        )
                    }
                    viewModel.saveVisit(
                        visitId = visitId,
                        date = date,
                        odometerKm = kmText.toIntOrNull(),
                        origin = origin,
                        workshop = workshop,
                        notes = notes,
                        services = drafts,
                        onSaved = onDone
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar consulta")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceEditorCard(
    index: Int,
    form: ServiceForm,
    canDelete: Boolean,
    onChange: (ServiceForm) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Serviço ${index + 1}", style = MaterialTheme.typography.titleMedium)
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover serviço")
                }
            }
        }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = form.category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoria") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ServiceCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.label) },
                        onClick = {
                            onChange(form.copy(category = category.label))
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = form.itemName,
            onValueChange = { onChange(form.copy(itemName = it)) },
            label = { Text("Item / serviço") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.quantity,
                onValueChange = { onChange(form.copy(quantity = it)) },
                label = { Text("Qtd") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = form.unitPrice,
                onValueChange = { onChange(form.copy(unitPrice = it)) },
                label = { Text("Valor unitário") },
                modifier = Modifier.weight(1.4f),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = form.notes,
            onValueChange = { onChange(form.copy(notes = it)) },
            label = { Text("Obs. do serviço") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val dateParser = DateTimeFormatter.ofPattern("d/M/yyyy")

private fun parseDate(raw: String): LocalDate? = try {
    LocalDate.parse(raw.trim(), dateParser)
} catch (_: Exception) {
    null
}

private fun parseDecimal(raw: String): Double? {
    val value = raw.trim()
        .replace("R$", "", ignoreCase = true)
        .replace(" ", "")
    if (value.isEmpty()) return null
    val normalized = when {
        value.contains(',') && value.contains('.') -> value.replace(".", "").replace(',', '.')
        value.contains(',') -> value.replace(',', '.')
        else -> value
    }
    return normalized.toDoubleOrNull()
}

private fun formatEditableNumber(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).replace('.', ',')
}
