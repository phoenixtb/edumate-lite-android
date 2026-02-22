package io.foxbird.edumate.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.foxbird.edgeai.model.ModelState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state.step) {
            OnboardingStep.WELCOME -> WelcomeStep(
                isPreparing = state.isPreparing,
                onContinue = { viewModel.advanceToModelSetup() },
                onSkip = { viewModel.skipOnboarding() }
            )
            OnboardingStep.MODEL_SETUP -> ModelSetupStep(
                progress = state.downloadProgress,
                error = state.error,
                needsDownload = false,
                onRetry = { viewModel.downloadAndSetupModels() },
                onSkip = { viewModel.skipOnboarding() }
            )
            OnboardingStep.COMPLETE -> CompleteStep(
                onFinish = { viewModel.completeOnboarding() }
            )
        }
    }
}

@Composable
private fun WelcomeStep(isPreparing: Boolean, onContinue: () -> Unit, onSkip: () -> Unit) {
    Icon(
        imageVector = Icons.Filled.School,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text("Welcome to EduMate Lite", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "Your AI-powered study companion. Let's set up your models to get started.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), enabled = !isPreparing) {
        Text(if (isPreparing) "Preparing..." else "Set Up Models")
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
        Text("Skip for Now")
    }
}

@Composable
private fun ModelSetupStep(
    progress: Float,
    error: String?,
    needsDownload: Boolean,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    Text("Setting Up Models", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(16.dp))

    if (progress > 0f && error == null) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (needsDownload) "${(progress * 100).toInt()}%" else "Loading models...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (error != null) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Retry")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for Now")
        }
    } else if (progress == 0f) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (needsDownload) {
                "Download Gemma 3n E2B (~3.5GB) and EmbeddingGemma (~200MB) to enable chat and search."
            } else {
                "Models are included with the app. No download needed."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(if (needsDownload) "Download Models" else "Load Models")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for Now")
        }
    }
}

@Composable
private fun CompleteStep(onFinish: () -> Unit) {
    Text("All Set!", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "Your models are ready. Start adding study materials!",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
        Text("Get Started")
    }
}
