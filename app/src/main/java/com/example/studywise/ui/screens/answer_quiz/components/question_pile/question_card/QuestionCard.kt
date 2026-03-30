package com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studywise.ui.theme.AppTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.answer.AnswerUiState
import com.example.studywise.ui.theme.LogoPink
import androidx.compose.runtime.setValue
import com.example.studywise.ui.screens.answer_quiz.AnswerQuizScreenAction
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.answer.AnswerOptionCard
import com.example.studywise.ui.theme.CardSurfaceColor
import com.example.studywise.ui.theme.CardTextColor


@Composable
fun QuestionCard(
    state: QuestionCardUiState,
    onAction: (AnswerQuizScreenAction) -> Unit,
    modifier: Modifier = Modifier,
    questionColor: Color,
    questionNumber: Int,
    ) {
    val paddedQuestionNumber = questionNumber.toString().padStart(2, '0')

    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 10.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = questionColor
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.55f)

    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        CardSurfaceColor,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Text(
                    text = paddedQuestionNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = questionColor
                    )
                )
            }
            Box (
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = CardSurfaceColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .defaultMinSize(0.dp, 140.dp)
                    .padding(20.dp)
            ) {
                Text(
                    text = state.description,
                    textAlign = TextAlign.Center,
                    color = CardTextColor,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            state.answers.forEachIndexed { index, answer ->
                AnswerOptionCard(
                    onAction = onAction,
                    questionNumber = questionNumber,
                    state = answer,
                    selectedAnswer = state.selectedAnswer,
                    questionColor = questionColor,
                    label = ('A' + index).toString()
                )
            }
        }
    }
}

@Preview()
@Composable
fun QuestionCardPreview(
) {

    var selectedAnswer by remember { mutableStateOf<AnswerUiState?>(null) }

    fun onAction(action: AnswerQuizScreenAction) {
        when(action) {
            is AnswerQuizScreenAction.AnswerSelected -> {
                selectedAnswer = action.answer
            }
            else -> {}
        }
    }

    AppTheme() {
        QuestionCard(
            state = QuestionCardUiState(
                id = "1",
                selectedAnswer = selectedAnswer, // Mocked selection
                description = "What is the square root of 4?",
                answers = listOf(
                    AnswerUiState(id = "1", text = "2", isCorrect = true),
                    AnswerUiState(id = "2", text = "4", isCorrect = false),
                    AnswerUiState(id = "3", text = "6", isCorrect = false),
                    AnswerUiState(id = "4", text = "8", isCorrect = false)
                )
            ),
            modifier = Modifier
                .padding(32.dp),
            questionColor = LogoPink,
            questionNumber = 1,
            onAction = { action -> onAction(action) }
        )
    }
}
