package io.foxbird.edumate.feature.knowledge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.edumate.data.local.entity.ConceptEntity
import org.koin.androidx.compose.koinViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KnowledgeGraphScreen(
    viewModel: KnowledgeGraphViewModel = koinViewModel()
) {
    val concepts by viewModel.concepts.collectAsStateWithLifecycle()
    var selectedType by remember { mutableStateOf<String?>(null) }
    var selectedConcept by remember { mutableStateOf<ConceptEntity?>(null) }

    val types = concepts.map { it.type }.distinct()
    val filteredConcepts = if (selectedType != null) {
        concepts.filter { it.type == selectedType }
    } else concepts

    Scaffold(
        topBar = { TopAppBar(title = { Text("Knowledge Graph") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Type filter chips
            if (types.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("All") }
                    )
                    types.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = if (selectedType == type) null else type },
                            label = { Text(type.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            if (filteredConcepts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No concepts extracted yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Force-directed graph canvas
                ForceDirectedGraph(
                    concepts = filteredConcepts,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    onNodeTap = { selectedConcept = it }
                )
            }

            // Selected concept detail
            selectedConcept?.let { concept ->
                ConceptDetailCard(concept = concept, onDismiss = { selectedConcept = null })
            }
        }
    }
}

@Composable
private fun ForceDirectedGraph(
    concepts: List<ConceptEntity>,
    modifier: Modifier = Modifier,
    onNodeTap: (ConceptEntity) -> Unit = {}
) {
    val nodePositions = remember(concepts) {
        concepts.mapIndexed { index, _ ->
            val angle = 2.0 * Math.PI * index / concepts.size.coerceAtLeast(1)
            val radius = 200f
            Offset(
                (300f + radius * cos(angle)).toFloat(),
                (400f + radius * sin(angle)).toFloat()
            )
        }
    }

    val colorMap = mapOf(
        "concept" to Color(0xFF1565C0),
        "term" to Color(0xFF00897B),
        "person" to Color(0xFFE65100),
        "formula" to Color(0xFF7C4DFF),
        "theorem" to Color(0xFFC62828),
        "event" to Color(0xFF2E7D32),
        "place" to Color(0xFF00838F),
        "definition" to Color(0xFF4527A0),
    )

    Canvas(
        modifier = modifier.pointerInput(concepts) {
            detectTapGestures { tapOffset ->
                val tapped = nodePositions.indexOfFirst { pos ->
                    (tapOffset - pos).getDistance() < 30f
                }
                if (tapped >= 0 && tapped < concepts.size) {
                    onNodeTap(concepts[tapped])
                }
            }
        }
    ) {
        val scaleX = size.width / 600f
        val scaleY = size.height / 800f

        // Draw nodes
        concepts.forEachIndexed { index, concept ->
            val pos = nodePositions.getOrNull(index) ?: return@forEachIndexed
            val scaledPos = Offset(pos.x * scaleX, pos.y * scaleY)
            val color = colorMap[concept.type] ?: Color(0xFF757575)
            val nodeSize = (10f + concept.frequency * 3f).coerceAtMost(30f)

            drawCircle(
                color = color,
                radius = nodeSize,
                center = scaledPos
            )

            drawContext.canvas.nativeCanvas.drawText(
                concept.name.take(15),
                scaledPos.x,
                scaledPos.y + nodeSize + 14f,
                android.graphics.Paint().apply {
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                    this.color = android.graphics.Color.DKGRAY
                }
            )
        }
    }
}

@Composable
private fun ConceptDetailCard(
    concept: ConceptEntity,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(concept.name, style = MaterialTheme.typography.titleMedium)
            Text(
                concept.type.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            concept.definition?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
            Text(
                "Frequency: ${concept.frequency}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
