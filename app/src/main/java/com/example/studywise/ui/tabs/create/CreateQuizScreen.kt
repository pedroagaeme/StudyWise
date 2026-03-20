package com.example.studywise.ui.tabs.create

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studywise.ui.theme.AppTheme

private const val MAX_ATTACHMENTS = 3

private enum class QuizSize(val label: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large")
}

private enum class QuizDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

private enum class AttachmentType {
    FILE,
    LINK
}

private data class AttachmentPreview(
    val type: AttachmentType,
    val value: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var quizSize by remember { mutableStateOf(QuizSize.MEDIUM) }
    var quizDifficulty by remember { mutableStateOf(QuizDifficulty.MEDIUM) }
    var quizContent by remember { mutableStateOf("") }
    val attachments = remember { mutableStateListOf<AttachmentPreview>() }

    fun addAttachment(type: AttachmentType) {
        if (attachments.size >= MAX_ATTACHMENTS) return
        val sameTypeCount = attachments.count { it.type == type } + 1
        val value = when (type) {
            AttachmentType.FILE -> "file_$sameTypeCount.pdf"
            AttachmentType.LINK -> "https://example.com/resource-$sameTypeCount"
        }
        attachments.add(AttachmentPreview(type = type, value = value))
    }

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
                    IconButton(onClick = onDismiss) {
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
                    selectedOption = quizSize,
                    optionLabel = { it.label },
                    onSelect = { quizSize = it }
                )

                Text(
                    "Difficulty",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                SelectorRow(
                    options = QuizDifficulty.entries,
                    selectedOption = quizDifficulty,
                    optionLabel = { it.label },
                    onSelect = { quizDifficulty = it }
                )

                Text(
                    "Study Material",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = quizContent,
                    onValueChange = { quizContent = it },
                    placeholder = { Text("Paste your notes here or type a summary...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 10,
                    shape = RoundedCornerShape(16.dp)
                )

                Text(
                    "Files and Links (${attachments.size}/$MAX_ATTACHMENTS)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (attachments.isEmpty()) {
                    Text(
                        "No attachments yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                attachments.forEachIndexed { index, item ->
                    AttachmentPreviewRow(
                        attachment = item,
                        onRemove = { attachments.removeAt(index) }
                    )
                }

                val canAddMore = attachments.size < MAX_ATTACHMENTS

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { addAttachment(AttachmentType.FILE) },
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
                        onClick = { addAttachment(AttachmentType.LINK) },
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
                onClick = { /* Handle quiz generation */ },
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    text = attachment.value,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
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
    AppTheme {
        CreateQuizScreen(onDismiss = {})
    }
}

