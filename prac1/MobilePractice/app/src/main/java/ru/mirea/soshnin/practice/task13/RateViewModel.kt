package ru.mirea.soshnin.practice.task13

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class RateViewModel : ViewModel() {

    private val _rate = MutableStateFlow(90.5)
    val rate: StateFlow<Double> = _rate.asStateFlow()

    // 1 - вырос, -1 - упал, 0 - без изменений
    private val _trend = MutableStateFlow(0)
    val trend: StateFlow<Int> = _trend.asStateFlow()

    init {
        // автообновление каждые 5 секунд
        viewModelScope.launch {
            while (true) {
                delay(5000)
                refresh()
            }
        }
    }

    fun refresh() {
        val old = _rate.value
        val new = 90.5 + Random.nextDouble(-2.0, 2.0)
        _trend.value = new.compareTo(old)
        _rate.value = new
    }
}
