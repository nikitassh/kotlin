package ru.mirea.soshnin.practice.task12

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.random.Random

class FactsViewModel : ViewModel() {

    private val facts = listOf(
        "Сердце кита весит около 180 кг.",
        "Улитка может спать до трех лет.",
        "У осьминога три сердца и голубая кровь.",
        "Жирафы спят всего около двух часов в сутки.",
        "Коровы узнают друг друга и заводят друзей.",
        "Кошки проводят во сне до 70% жизни.",
        "Колибри - единственная птица, летающая задом наперед.",
        "Слоны не умеют прыгать.",
        "Отпечатки носа у собак уникальны, как у людей пальцы.",
        "Пингвины дарят друг другу камешки.",
        "Язык хамелеона длиннее его тела.",
        "Морские выдры держатся за лапы во сне.",
        "Муравьи никогда не спят в привычном смысле.",
        "Бабочки чувствуют вкус ногами.",
        "Совы не могут вращать глазами, зато крутят головой на 270°."
    )

    // Холодный поток: каждый collect генерирует новый факт
    fun getRandomFact(): Flow<String> = flow {
        delay(Random.nextLong(1500, 3000))
        emit(facts.random())
    }

    // состояние живет во ViewModel и переживает поворот экрана
    var fact by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    private var job: Job? = null

    fun newFact() {
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            getRandomFact().collect { fact = it }
            loading = false
        }
    }
}
