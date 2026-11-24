package ru.mirea.soshnin.practice.task13

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.mirea.soshnin.practice.ui.TaskScaffold

@Composable
fun RateScreen(
    onBack: () -> Unit,
    vm: RateViewModel = viewModel()
) {
    val rate by vm.rate.collectAsStateWithLifecycle()
    val trend by vm.trend.collectAsStateWithLifecycle()

    TaskScaffold("Задание 13. Курс валют", onBack) {
        Spacer(Modifier.height(48.dp))
        Text("USD / RUB", fontSize = 24.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("%.2f ₽".format(rate), fontSize = 56.sp)
            Spacer(Modifier.width(12.dp))
            TrendArrow(trend)
        }
        Text("Обновляется каждые 5 секунд", color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        Button(onClick = vm::refresh) {
            Text("Обновить сейчас", fontSize = 18.sp)
        }
    }
}

@Composable
fun TrendArrow(trend: Int) {
    when {
        trend > 0 -> Text("▲", color = Color(0xFF2E7D32), fontSize = 40.sp)
        trend < 0 -> Text("▼", color = Color(0xFFC62828), fontSize = 40.sp)
    }
}
