package com.example.studywise.ui.screens.create_quiz

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studywise.ui.components.stack_screen.StackScreen
import com.example.studywise.ui.theme.AppTheme
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
            if (uri != null) {
                viewModel.onAction(CreateQuizScreenAction.OnAddAttachment(AttachmentType.FILE, uri))
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction

    LaunchedEffect(uiState.pendingEffect) {
        uiState.pendingEffect?.let { effect ->
            when (effect) {
                is CreateQuizScreenEffect.OpenFilePicker -> {
                    launcher.launch(
                        arrayOf(
                            "application/pdf",
                            "image/*",
                            "video/*"
                        )
                    )
                    viewModel.effectConsumed()
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

@Composable
fun CreateQuizScreenContent(
    state: CreateQuizUiState,
    onAction: (CreateQuizScreenAction) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = state.currentStep,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "CreateQuizStepTransition"
    ) { step ->
        when (step) {
            CreateQuizStep.CONFIGURATION -> {
                CreateQuizStep1Screen(
                    state = state,
                    onAction = onAction,
                    modifier = modifier
                )
            }

            CreateQuizStep.GENERATING -> {
                CreateQuizStep2Screen(modifier = modifier)
            }

            CreateQuizStep.CONFIRMATION -> {
                CreateQuizStep3Screen(
                    state = state,
                    onAction = onAction,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
fun CreateQuizStep1Screen(
    state: CreateQuizUiState,
    onAction: (CreateQuizScreenAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val canAddMore = state.attachments.size < MAX_ATTACHMENTS

    // enter animation progress for this screen
    val enterProgressState = remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        enterProgressState.value = 1f
    }

    StackScreen(
        title = "Create New Quiz",
        modifier = modifier,
        onBackClick = { onAction(CreateQuizScreenAction.OnDismiss) },
        transitionProgress = enterProgressState.value,
        navigationIcon = {
            IconButton(onClick = { onAction(CreateQuizScreenAction.OnDismiss) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .padding(WindowInsets.navigationBars.asPaddingValues()),
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
                    options = QuizSize.entries.toList(),
                    selectedOption = state.quizSize,
                    optionLabel = { it.label },
                    onSelect = { onAction(CreateQuizScreenAction.OnQuizScreenSizeChange(it)) }
                )

                Text(
                    "Difficulty",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                SelectorRow(
                    options = QuizDifficulty.entries.toList(),
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
                        onClick = { /* Implement link addition if needed */ },
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
fun CreateQuizStep2Screen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                "This may take a few seconds",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizStep3Screen(
    state: CreateQuizUiState,
    onAction: (CreateQuizScreenAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val enterProgressState = remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        enterProgressState.value = 1f
    }

    StackScreen(
        title = "Finalize Quiz",
        modifier = modifier,
        onBackClick = { onAction(CreateQuizScreenAction.OnDismiss) },
        transitionProgress = enterProgressState.value,
        navigationIcon = {
            IconButton(onClick = { onAction(CreateQuizScreenAction.OnDismiss) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .padding(WindowInsets.navigationBars.asPaddingValues()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Quiz Name",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                OutlinedTextField(
                    value = state.quizName,
                    onValueChange = { onAction(CreateQuizScreenAction.OnQuizNameChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Enter quiz name") }
                )

                Text(
                    "Collection",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                SelectorRow(
                    options = CollectionMode.entries.toList(),
                    selectedOption = state.collectionMode,
                    optionLabel = { if (it == CollectionMode.NEW) "New" else "Existing" },
                    onSelect = { onAction(CreateQuizScreenAction.OnCollectionModeChange(it)) }
                )

                if (state.collectionMode == CollectionMode.NEW) {
                    OutlinedTextField(
                        value = state.collectionName,
                        onValueChange = { onAction(CreateQuizScreenAction.OnCollectionNameChange(it)) },
                        placeholder = { Text("Collection name (e.g. Biology)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = state.selectedCollection ?: "Select a collection",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            state.existingCollections.forEach { collection ->
                                DropdownMenuItem(
                                    text = { Text(collection) },
                                    onClick = {
                                        onAction(CreateQuizScreenAction.OnCollectionSelected(collection))
                                        expanded = false
                                    }
                                )
                            }
                            if (state.existingCollections.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No collections found", style = MaterialTheme.typography.bodySmall) },
                                    onClick = { expanded = false },
                                    enabled = false
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onAction(CreateQuizScreenAction.OnConfirmQuizCreation) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    "Confirm and Start",
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
                label = {
                    Text(
                        text = optionLabel(option),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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

                    is CreateQuizScreenAction.OnCollectionModeChange ->
                        state.copy(collectionMode = action.mode)

                    else -> state
                }
            }
        )
    }
}
