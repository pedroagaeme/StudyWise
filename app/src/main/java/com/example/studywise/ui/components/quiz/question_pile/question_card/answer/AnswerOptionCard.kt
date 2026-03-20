package com.example.studywise.ui.components.quiz.question_pile.question_card.answer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.studywise.ui.components.quiz.question_pile.question_card.QuestionPileAction
import com.example.studywise.ui.theme.CardSurfaceColor
import com.example.studywise.ui.theme.CardTextColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnswerOptionCard(
    onAction: (QuestionPileAction) -> Unit,
    questionNumber: Int,
    state: AnswerUiState,
    selectedAnswer: AnswerUiState?,
    questionColor: Color,
    label: String = "A",
) {

    // Logic for answer feedback
    val isAnswered = selectedAnswer != null
    val isSelected = state == selectedAnswer
    val isCorrect = state.isCorrect

    // Logic for animated colors
    // 1. Define the target colors and duration of the animation
    val animationDurationMS = 300

    val targetContainerColor = when {
        !isAnswered -> CardSurfaceColor
        isCorrect -> Color(0xFFA8D5B5)
        isSelected ->  Color(0xFFE8A8A8)
        else -> CardSurfaceColor
    }

    val targetLabelColor = when {
        isSelected -> CardTextColor
        isCorrect and isAnswered -> CardTextColor
        else -> questionColor
    }


    // 2. Wrap them in animateColorAsState
    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = animationDurationMS),
        label = "ContainerColorAnimation"
    )

    val animatedLabelColor by animateColorAsState(
        targetValue = targetLabelColor,
        animationSpec = tween(durationMillis = animationDurationMS)
    )



    // Create the annotated string with the question label and answer text
    val annotatedText = buildAnnotatedString {
        // Style for the label (Bold)
        withStyle(
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = animatedLabelColor
            ).toSpanStyle()
        ) {
            append("${label})" + " ".repeat(2))
        }
        // Style for the actual Answer Text
        withStyle(
            style = MaterialTheme.typography.titleMedium.copy(
                color = CardTextColor
            ).toSpanStyle()
        ) {
            append(state.text)
        }
    }

    val questionIndex = questionNumber - 1

    val scope = rememberCoroutineScope()

    fun onClick() {
        scope.launch {
            onAction(QuestionPileAction.AnswerSelected(state, questionIndex))

            // Now delay will work!
            delay(animationDurationMS.toLong())

            onAction(QuestionPileAction.NextQuestionCardRequested(questionIndex))
        }
    }

    Button(
        onClick = { onClick() },
        enabled = !isAnswered, // Automatically disables interaction once answered
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = animatedContainerColor,
            disabledContainerColor = animatedContainerColor, // Keeps our feedback color visible
            contentColor = CardTextColor,
            disabledContentColor = CardTextColor,
        ),
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = annotatedText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Left
        )
    }
}