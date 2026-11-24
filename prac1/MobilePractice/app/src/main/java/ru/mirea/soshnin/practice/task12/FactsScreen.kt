package ru.mirea.soshnin.practice.task12

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.mirea.soshnin.practice.ui.TaskScaffold

@Composable
fun FactsScreen(
    onBack: () -> Unit,
    vm: FactsViewModel = viewModel()
) {
    TaskScaffold("Задание 12. Факты о животных", onBack) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.fillMaxWidth().height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            if (vm.loading) {
                CircularProgressIndicator()
            } else {
                // карточка появляется с анимацией
                AnimatedContent(
                    targetState = vm.fact,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.8f))
                            .togetherWith(fadeOut())
                    },
                    label = "fact"
                ) { fact -> FactCard(fact) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = vm::newFact, enabled = !vm.loading) {
            Text("Новый факт!", fontSize = 18.sp)
        }
    }
}

@Composable
fun FactCard(fact: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Text(
            fact ?: "Нажмите кнопку, чтобы узнать факт",
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}
