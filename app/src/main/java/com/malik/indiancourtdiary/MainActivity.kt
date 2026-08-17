package com.malik.indiancourtdiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent { MaterialTheme { Diary() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Diary(vm: CourtDiaryViewModel = viewModel()) {
    val cases by vm.cases.collectAsStateWithLifecycle()
    val isAdding by vm.isAdding.collectAsStateWithLifecycle()
    var show by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Court Diary")
                    Text("Your hearings, organised", style = MaterialTheme.typography.labelSmall)
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { show = true }) {
                Icon(Icons.Outlined.Add, "Add case")
            }
        }
    ) { padding ->
        if (cases.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No cases added\nTap + and enter the CNR number.")
            }
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("My Cases", style = MaterialTheme.typography.headlineSmall) }
                items(cases, key = { it.cnr }) { item ->
                    ElevatedCard {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(item.caseTitle, style = MaterialTheme.typography.titleMedium)
                            Text(item.cnr, color = MaterialTheme.colorScheme.primary)
                            Text(item.courtName)
                            Text("Stage: " + item.stage)
                            Text(item.nextHearingDate?.let { "Next hearing: $it" } ?: "Hearing date unavailable")
                            IconButton(onClick = { vm.delete(item.cnr) }) {
                                Icon(Icons.Outlined.Delete, "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (show) {
        AddDialog(
            loading = isAdding,
            close = { if (!isAdding) show = false },
            save = { cnr, reply ->
                vm.add(cnr) { error ->
                    reply(error)
                    if (error == null) show = false
                }
            }
        )
    }
}

@Composable
fun AddDialog(loading: Boolean, close: () -> Unit, save: (String, (String?) -> Unit) -> Unit) {
    var cnr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = close,
        title = { Text("Add case by CNR") },
        text = {
            Column {
                OutlinedTextField(
                    value = cnr,
                    onValueChange = { cnr = it; error = null },
                    enabled = !loading,
                    label = { Text("16-character CNR") },
                    isError = error != null,
                    singleLine = true
                )
                if (loading) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Fetching case details…", style = MaterialTheme.typography.bodySmall)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = { save(cnr) { error = it } }, enabled = !loading) {
                Text(if (loading) "Adding…" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = close, enabled = !loading) { Text("Cancel") }
        }
    )
}
