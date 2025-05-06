package com.example.desgin.actions_delays_screen

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multinav.database.MyDatabase
import com.example.multinav.database.entities.Task
import kotlinx.coroutines.launch

class ActionsAndDelaysViewModel : ViewModel() {

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

    val selectedAction = mutableStateOf<String?>(null)
    val actionHistory = mutableStateListOf<String>()

    val totalDelayMilliseconds = mutableStateOf<Long?>(null) // Store total milliseconds


    // Function to add an action to the history
    fun addActionToHistory(action: String) {
        if (!actionHistory.contains(action)) {
            actionHistory.add(action)
            selectedAction.value = action // Update the selected action
            Log.d("ActionsAndDelaysViewModel", "Action added: $action, History: $actionHistory")
        }
    }



    // Function to add or remove toggle button actions
    fun addActionToHistoryToggleButtons(action: String) {
        when (action) {
            "A" -> {
                if (actionHistory.contains(action)) {
                    actionHistory.remove(action)
                    isToggleButtonA.value = false
                } else {
                    actionHistory.add(action)
                    isToggleButtonA.value = true
                }
            }
            "B" -> {
                if (actionHistory.contains(action)) {
                    actionHistory.remove(action)
                    isToggleButtonB.value = false
                } else {
                    actionHistory.add(action)
                    isToggleButtonB.value = true
                }
            }
            "C" -> {
                if (actionHistory.contains(action)) {
                    actionHistory.remove(action)
                    isToggleButtonC.value = false
                } else {
                    actionHistory.add(action)
                    isToggleButtonC.value = true
                }
            }
            "D" -> {
                if (actionHistory.contains(action)) {
                    actionHistory.remove(action)
                    isToggleButtonD.value = false
                } else {
                    actionHistory.add(action)
                    isToggleButtonD.value = true
                }
            }
            else -> {
                if (!actionHistory.contains(action)) {
                    actionHistory.add(action)
                }
            }
        }
        selectedAction.value = if (actionHistory.isNotEmpty()) actionHistory.lastOrNull() else null
        Log.d("ActionsAndDelaysViewModel", "Toggle action: $action, History: $actionHistory")
    }

//    // Function to add a delay to the history
//    fun addDelayToHistory() {
//        val delay = (selectedHours.value * 3600000L) +
//                (selectedMinutes.value * 60000L) +
//                (selectedSeconds.value * 1000L) +
//                selectedMilliseconds.value
//
//        val formattedDelay = formatDelay(delay)
//
//        // Add to actionHistory as "Delay: HH:MM:SS.MMM"
//        val delayEntry = "Delay: $formattedDelay"
//        actionHistory.add(delayEntry)
//        selectedAction.value = delayEntry
//        selectedHours.value = 0
//        selectedMinutes.value = 0
//        selectedSeconds.value = 0
//        selectedMilliseconds.value = 0
//
//        Log.d("ActionsAndDelaysViewModel", "Delay added: $delayEntry, History: $actionHistory")
//    }

    // Function to add a delay to the history
    fun addDelayToHistory(delay: Long) {
        val formattedDelay = formatDelay(delay)
        val delayEntry = "Delay: $formattedDelay ($delay ms)"
        actionHistory.add(delayEntry)
        selectedAction.value = delayEntry
        selectedHours.value = 0
        selectedMinutes.value = 0
        selectedSeconds.value = 0
        selectedMilliseconds.value = 0
        Log.d("ActionsAndDelaysViewModel", "Delay added: $delayEntry, History: $actionHistory")
    }


    // Function to update the mode in history
    fun updateModeInHistory(mode: String) {
        val modes = listOf("1", "2", "3") // Adjust based on Modes.MODE_ONE, MODE_TWO, MODE_THREE
        actionHistory.removeAll { it in modes }
        if (!actionHistory.contains(mode)) {
            actionHistory.add(mode)
        }
        selectedMode.value = mode
        Log.d("ActionsAndDelaysViewModel", "Mode updated: $mode, History: $actionHistory")
    }

    // Function to save actions to the database
    // Function to save actions to the database
    fun saveActionToDatabase(userUid: String?, taskTitle: String) {
        if (userUid == null) {
            Log.e("TaskDatabase", "Cannot save: userUid is null")
            return
        }
        val modes = listOf("1", "2", "3")
        val actions = actionHistory.filter { it !in modes }.map { action ->
            if (action.startsWith("Delay: ")) {
                // Extract the HH:MM:SS.MMM part by removing the "Delay: " prefix and the "(X ms)" suffix
                val formattedDelay = action.removePrefix("Delay: ").replace(Regex("\\(\\d+ ms\\)"), "").trim()
                Log.d("TaskDatabase", "Processed delay action: $action -> $formattedDelay")
                formattedDelay
            } else {
                action
            }
        }
        if (actions.isEmpty()) {
            Log.e("TaskDatabase", "No actions to save")
            return
        }

        viewModelScope.launch {
            try {
                val task = createTaskFromAction(actions, userUid, taskTitle)
                MyDatabase.getInstance().getTaskDao().addTask(task)
                actionHistory.clear()
                selectedAction.value = null
                totalDelayMilliseconds.value = null // Reset total delay
                isToggleButtonA.value = false
                isToggleButtonB.value = false
                isToggleButtonC.value = false
                isToggleButtonD.value = false
                textField.value = ""
                selectedMode.value = ""
                Log.d("TaskDatabase", "Task saved: $taskTitle, Actions: $actions")
            } catch (e: Exception) {
                Log.e("TaskDatabase", "Error saving task: ${e.message}")
            }
        }
    }
    // Function to create a Task object from actions
    private fun createTaskFromAction(actions: List<String>, userUid: String, taskTitle: String): Task {
        // Use the stored totalDelayMilliseconds if available
        val delay = totalDelayMilliseconds.value ?: actions.find { it.matches(Regex("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")) }?.let {
            val parts = it.split(":", ".").map { part -> part.toLong() }
            (parts[0] * 3600000) + (parts[1] * 60000) + (parts[2] * 1000) + parts[3]
        }

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

    // Function to format delay in HH:MM:SS.MMM format
    fun formatDelay(milliseconds: Long): String {
        val hours = milliseconds / 3600000
        val minutes = (milliseconds % 3600000) / 60000
        val seconds = (milliseconds % 60000) / 1000
        val millis = milliseconds % 1000
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
}