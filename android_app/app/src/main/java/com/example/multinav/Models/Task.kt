package com.example.multinav.Models

data class Task(
    val title :String,
    val isTurnOn:Boolean = false,

    val triangle: String = "Triangle",
    val circle: String = "circle",
    val square: String = "square",
    val x: String = "x",

    val arrowUP: String = "UP",
    val arrowDown: String = "Down",
    val arrowLeft: String = "Left",
    val arrowRight: String = "Right",

    val buttonA: Char = 'A',
    val buttonB: Char = 'B',
    val buttonC: Char = 'C',
    val buttonD: Char = 'D',
    val mode: String
    )
