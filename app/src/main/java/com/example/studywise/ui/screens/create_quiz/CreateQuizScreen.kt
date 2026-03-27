package com.example.studywise.ui.screens.create_quiz

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studywise.ui.theme.AppTheme
import com.example.studywise.utils.ObserveAsEvents
import com.example.studywise.viewmodels.CreateQuizScreenViewModel

@Composable
fun CreateQuizScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateQuizScreenViewModel = hiltViewModel(),
    goBack: () -> Unit = {},
    replaceWithAnswerQuizRoute: (String) -> Unit = {}
) {
    // Document picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null)
                viewModel.onAction(CreateQuizScreenAction.OnAddAttachment(AttachmentType.FILE, uri))
        }
    )


    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction

    LaunchedEffect(uiState.pendingEffect) {
        uiState.pendingEffect.let { effect ->
            when (effect) {
                is CreateQuizScreenEffect.OpenFilePicker -> {
                    launcher.launch(
                        arrayOf(
                            "application/pdf",
                            "image/*",
                            "video/*"
                        )
                    )
                }

                is CreateQuizScreenEffect.Dismiss -> {
                    goBack()
                }

                is CreateQuizScreenEffect.QuizGenerated -> {
                    replaceWithAnswerQuizRoute(effect.quizId)
                }

                else -> Unit
            }
        }
    }

    CreateQuizScreenContent(
        state = uiState,
        onAction = onAction,
        modifier = modifier
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreenContent(
    state: CreateQuizUiState,
    onAction: (CreateQuizScreenAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val canAddMore = state.attachments.size < MAX_ATTACHMENTS

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Create New Quiz",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CreateQuizScreenAction.OnDismiss) }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Quiz Size",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                SelectorRow(
                    options = QuizSize.entries,
                    selectedOption = state.quizSize,
                    optionLabel = { it.label },
                    onSelect = { onAction(CreateQuizScreenAction.OnQuizScreenSizeChange(it)) }
                )

                Text(
                    "Difficulty",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                SelectorRow(
                    options = QuizDifficulty.entries,
                    selectedOption = state.quizDifficulty,
                    optionLabel = { it.label },
                    onSelect = { onAction(CreateQuizScreenAction.OnQuizScreenDifficultyChange(it)) }
                )

                Text(
                    "Study Material",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = state.quizSummary,
                    onValueChange = { onAction(CreateQuizScreenAction.OnQuizScreenSummaryChange(it)) },
                    placeholder = { Text("Paste your notes here or type a summary...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 10,
                    shape = RoundedCornerShape(16.dp)
                )

                Text(
                    "Files and Links (${state.attachments.size}/$MAX_ATTACHMENTS)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (state.attachments.isEmpty()) {
                    Text(
                        "No attachments yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                state.attachments.forEach { item ->
                    AttachmentPreviewRow(
                        attachment = item,
                        onRemove = { onAction(CreateQuizScreenAction.OnRemoveAttachment(item)) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onAction(CreateQuizScreenAction.OnFileButtonClick) },
                        enabled = canAddMore,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("File")
                    }

                    Button(
                        onClick = { onAction(CreateQuizScreenAction.OnFileButtonClick) },
                        enabled = canAddMore,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Link")
                    }
                }

                if (!canAddMore) {
                    Text(
                        "Maximum of $MAX_ATTACHMENTS attachments reached.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { onAction(CreateQuizScreenAction.OnGenerateQuizButtonClick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    "Generate Quiz",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun <T> SelectorRow(
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selectedOption == option,
                onClick = { onSelect(option) },
                label = { Text(optionLabel(option)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AttachmentPreviewRow(
    attachment: AttachmentPreview,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (attachment.type == AttachmentType.FILE) {
                    Icons.Rounded.AttachFile
                } else {
                    Icons.Rounded.Link
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )
                Text(
                    text = if (attachment.type == AttachmentType.FILE) "File" else "Link",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove attachment")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateQuizScreenPreview() {
    var state by remember { mutableStateOf(CreateQuizUiState()) }
    AppTheme {
        CreateQuizScreenContent(
            state = state,
            onAction = { action ->
                state = when (action) {
                    is CreateQuizScreenAction.OnQuizScreenDifficultyChange ->
                        state.copy(quizDifficulty = action.quizDifficulty)

                    is CreateQuizScreenAction.OnQuizScreenSizeChange ->
                        state.copy(quizSize = action.quizSize)

                    is CreateQuizScreenAction.OnQuizScreenSummaryChange ->
                        state.copy(quizSummary = action.quizSummary)

                    else -> state
                }
            }
        )
    }
}

