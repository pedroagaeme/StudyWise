package com.example.studywise.ui.components.quiz_card
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studywise.data.QuizDto
import com.example.studywise.ui.components.ProgressIndicatorBox
import com.example.studywise.ui.theme.AppTheme
import com.example.studywise.utils.formatDateHumanReadable

@Composable
fun AdditionalInfoRow(quiz: QuizDto) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${quiz.questionCount} Questions",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatDateHumanReadable(quiz.lastInteracted),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Visible

        )
    }
}


@Composable
fun NewQuizBadge() {
    Box(
        modifier = Modifier
            .size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = "NEW",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            )
            }
        }
    }

@Composable
fun QuizCard(
    quiz: QuizDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val progress = if (quiz.averageScore != null && quiz.questionCount > 0) {
        (quiz.averageScore / quiz.questionCount.toFloat()).coerceIn(0f, 1f)
    } else {
        null
    }

    Box (modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f)
                ) {
                    if (quiz.collectionName != null) {
                        Text(
                            text = quiz.collectionName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = quiz.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AdditionalInfoRow(quiz)
                }
                if (progress != null) {
                    ProgressIndicatorBox(progress = progress, indicatorPadding = 20.dp)
                } else {
                    NewQuizBadge()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuizCardPreview() {
    AppTheme {
        QuizCard(
            quiz = QuizDto(
                id = "preview-1",
                title = "Organic Chemistry",
                lastInteracted = "2m ago",
                collectionName = "University",
                questionCount = 18,
                averageScore = 14f
            )
        )
    }
}
