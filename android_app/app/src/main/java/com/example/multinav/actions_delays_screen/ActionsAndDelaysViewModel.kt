package com.example.desgin.actions_delays_screen

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.desgin.utils.TimeUtils
import com.example.multinav.database.entities.Task
import com.example.multinav.model.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ActionHistoryItem(val type: String, val value: String, val delayMs: Long? = null)

class ActionsAndDelaysViewModel(private val repository: TaskRepository) : ViewModel() {

    val textField = mutableStateOf("")
    val selectedMode = mutableStateOf("")
    val isToggleButtonA = mutableStateOf(false)
    val isToggleButtonB = mutableStateOf(false)
    val isToggleButtonC = mutableStateOf(false)
    val isToggleButtonD = mutableStateOf(false)
    val selectedHours = mutableStateOf(0)
    val selectedMinutes = mutableStateOf(0)
    val selectedSeconds = mutableStateOf(0)
    val selectedMilliseconds = mutableStateOf(0)
    val totalDelayMilliseconds = mutableStateOf<Long?>(null)
    val errorMessage = mutableStateOf<String?>(null)

    val selectedAction = mutableStateOf<String?>(null)
    val actionHistory = mutableStateListOf<ActionHistoryItem>()

    fun addActionToHistory(action: String) {
        if (actionHistory.none { it.value == action }) {
            actionHistory.add(ActionHistoryItem(type = "Action", value = action))
            selectedAction.value = action

        }
    }

    fun addActionToHistoryToggleButtons(action: String) {
        when (action) {
            "A" -> {
                if (actionHistory.any { it.value == action }) {
                    actionHistory.removeAll { it.value == action }
                    isToggleButtonA.value = false
                } else {
                    actionHistory.add(ActionHistoryItem(type = "Toggle", value = action))
                    isToggleButtonA.value = true
                }
            }
            "B" -> {
                if (actionHistory.any { it.value == action }) {
                    actionHistory.removeAll { it.value == action }
                    isToggleButtonB.value = false
                } else {
                    actionHistory.add(ActionHistoryItem(type = "Toggle", value = action))
                    isToggleButtonB.value = true
                }
            }
            "C" -> {
                if (actionHistory.any { it.value == action }) {
                    actionHistory.removeAll { it.value == action }
                    isToggleButtonC.value = false
                } else {
                    actionHistory.add(ActionHistoryItem(type = "Toggle", value = action))
                    isToggleButtonC.value = true
                }
            }
            "D" -> {
                if (actionHistory.any { it.value == action }) {
                    actionHistory.removeAll { it.value == action }
                    isToggleButtonD.value = false
                } else {
                    actionHistory.add(ActionHistoryItem(type = "Toggle", value = action))
                    isToggleButtonD.value = true
                }
            }
        }
        selectedAction.value = if (actionHistory.isNotEmpty()) actionHistory.lastOrNull()?.value else null

    }

    fun addDelayToHistory(delay: Long) {
        if (delay <= 0) {

            return
        }
        val formattedDelay = TimeUtils.formatDelay(delay)
        actionHistory.add(ActionHistoryItem(type = "Delay", value = formattedDelay, delayMs = delay))
        selectedAction.value = formattedDelay
        totalDelayMilliseconds.value = delay

    }

    fun resetDelayPickerValues() {
        selectedHours.value = 0
        selectedMinutes.value = 0
        selectedSeconds.value = 0
        selectedMilliseconds.value = 0

    }

    fun updateModeInHistory(mode: String) {
        val modes = listOf("1", "2", "3")
        actionHistory.removeAll { it.value in modes }
        if (actionHistory.none { it.value == mode }) {
            actionHistory.add(ActionHistoryItem(type = "Mode", value = mode))
        }
        selectedMode.value = mode

    }

    fun saveActionToDatabase(userUid: String?, taskTitle: String) {
        if (userUid == null) {
            errorMessage.value = "User not logged in"
            return
        }
        val modes = listOf("1", "2", "3")
        val actions = actionHistory.filter { it.value !in modes }.map { it.value }
        if (actions.isEmpty()) {
            errorMessage.value = "No actions to save"

            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val task = createTaskFromAction(actions, userUid, taskTitle)
                repository.addTask(task)
                withContext(Dispatchers.Main) {
                    actionHistory.clear()
                    selectedAction.value = null
                    totalDelayMilliseconds.value = null
                    isToggleButtonA.value = false
                    isToggleButtonB.value = false
                    isToggleButtonC.value = false
                    isToggleButtonD.value = false
                    textField.value = ""
                    selectedMode.value = ""
                    errorMessage.value = null

                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage.value = "Failed to save task: ${e.message}"

                }
            }
        }
    }

    private fun createTaskFromAction(actions: List<String>, userUid: String, taskTitle: String): Task {
        val delay = totalDelayMilliseconds.value ?: actionHistory.find { it.type == "Delay" }?.delayMs

        return Task(
            actions = actions,
            mode = selectedMode.value,
            delay = delay,
            userUid = userUid,
            taskTitle = taskTitle,
            taskOn = false,
            taskId = 0,
            buttonA = if (actions.contains("A")) 'A' else null,
            buttonB = if (actions.contains("B")) 'B' else null,
            buttonC = if (actions.contains("C")) 'C' else null,
            buttonD = if (actions.contains("D")) 'D' else null,
            buttonUP = if (actions.contains("Up")) "Up" else null,
            buttonDown = if (actions.contains("Down")) "Down" else null,
            buttonRight = if (actions.contains("Right")) "Right" else null,
            buttonLeft = if (actions.contains("Left")) "Left" else null,
            buttonTriangle = if (actions.contains("Triangle")) "Triangle" else null,
            buttonX = if (actions.contains("Cross")) "X" else null,
            buttonCircle = if (actions.contains("Circle")) "Circle" else null,
            buttonSquare = if (actions.contains("Square")) "Square" else null
        )
    }

    object TimeUtils {
        fun formatDelay(milliseconds: Long): String {
            val hours = milliseconds / 3600000
            val minutes = (milliseconds % 3600000) / 60000
            val seconds = (milliseconds % 60000) / 1000
            val millis = milliseconds % 1000
            return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
        }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ActionsAndDelaysViewModel::class.java)) {
                return ActionsAndDelaysViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}