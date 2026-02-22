package io.foxbird.edumate.feature.worksheet

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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorksheetScreen(
    onBack: () -> Unit,
    viewModel: WorksheetViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("Practice Worksheet") }
    var numQuestions by remember { mutableFloatStateOf(10f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Worksheet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                .padding(16.dp)
        ) {
            if (uiState.generatedContent == null) {
                // Config
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Worksheet Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Number of questions: ${numQuestions.toInt()}")
                Slider(
                    value = numQuestions,
                    onValueChange = { numQuestions = it },
                    valueRange = 3f..20f,
                    steps = 16
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.generate(title, numQuestions.toInt()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isGenerating
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    } else {
                        Text("Generate")
                    }
                }

                uiState.error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            } else {
                // Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        uiState.generatedContent!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("New Worksheet")
                    }
                    Button(
                        onClick = { viewModel.exportPdf(title) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Text(" Export PDF")
                    }
                }
            }
        }
    }
}
