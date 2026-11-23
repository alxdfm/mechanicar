package com.mechanicar.gastos.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mechanicar.gastos.ui.AppViewModel

@Composable
fun ImportScreen(viewModel: AppViewModel) {
    var replaceExisting by remember { mutableStateOf(true) }
    val importing by viewModel.importing.collectAsState()
    var selectedName by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedName = uri.lastPathSegment
            viewModel.importFromUri(uri, replaceExisting)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Importar planilha", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Use a planilha gerada em import/gastos_corsa_importacao.xlsx (ou .csv). " +
                "O app agrupa os itens pela mesma data, origem e oficina em uma visita.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))
        Text("Colunas esperadas", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "data · km · origem · oficina · categoria · item · quantidade · valor_unitario · observacao",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = replaceExisting,
                onCheckedChange = { replaceExisting = it }
            )
            Text("Substituir dados atuais ao importar")
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                picker.launch(
                    arrayOf(
                        "text/*",
                        "text/csv",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "application/octet-stream"
                    )
                )
            },
            enabled = !importing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Escolher arquivo (.xlsx ou .csv)")
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { viewModel.importSeedData(replaceExisting) },
            enabled = !importing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Carregar dados iniciais do Corsa")
        }

        if (selectedName != null) {
            Spacer(Modifier.height(12.dp))
            Text("Arquivo: $selectedName", style = MaterialTheme.typography.bodyMedium)
        }

        if (importing) {
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("Dica", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Se o Excel estiver aberto em outro app, exporte/salve como CSV UTF-8 ou use o .xlsx da pasta import/.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
