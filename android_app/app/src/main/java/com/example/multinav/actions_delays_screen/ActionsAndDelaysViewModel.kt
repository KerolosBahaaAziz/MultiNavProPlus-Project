

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
    val totalDelayMilliseconds = mutableStateOf<Long?>(null)

    val selectedAction = mutableStateOf<String?>(null)
    val actionHistory = mutableStateListOf<String>()

    // Function to add an action to the history
    fun addActionToHistory(action: String) {
        // Allow multiple instances of directional and shape actions
        val repeatableActions = listOf("Up", "Down", "Right", "Left", "Triangle", "Cross", "Circle", "Square")
        if (repeatableActions.contains(action)) {
            actionHistory.add(action)
            selectedAction.value = action
            Log.d("ActionsAndDelaysViewModel", "Action added: $action, History: $actionHistory")
        } else {
            // For non-repeatable actions, prevent duplicates
            if (!actionHistory.contains(action)) {
                actionHistory.add(action)
                selectedAction.value = action
                Log.d("ActionsAndDelaysViewModel", "Action added: $action, History: $actionHistory")
            }
        }
    }

    // Function to add toggle button actions
    fun addActionToHistoryToggleButtons(action: String) {
        when (action) {
            "A" -> {
                Log.d("ToggleButtonA", "Before press: isToggleButtonA = ${isToggleButtonA.value}")
                val lastActionA = actionHistory.lastOrNull { it == "A-on" || it == "A-off" }
                if (lastActionA == "A-on") {
                    // Last was on, so add off
                    actionHistory.add("A-off")
                    isToggleButtonA.value = false
                    selectedAction.value = "A-off"
                } else {
                    // Last was off or none, so add on
                    actionHistory.add("A-on")
                    isToggleButtonA.value = true
                    selectedAction.value = "A-on"
                }
                Log.d("ToggleButtonA", "After press: isToggleButtonA = ${isToggleButtonA.value}, History: $actionHistory")
            }
            "B" -> {
                Log.d("ToggleButtonB", "Before press: isToggleButtonB = ${isToggleButtonB.value}")
                val lastActionB = actionHistory.lastOrNull { it == "B-on" || it == "B-off" }
                if (lastActionB == "B-on") {
                    actionHistory.add("B-off")
                    isToggleButtonB.value = false
                    selectedAction.value = "B-off"
                } else {
                    actionHistory.add("B-on")
                    isToggleButtonB.value = true
                    selectedAction.value = "B-on"
                }
                Log.d("ToggleButtonB", "After press: isToggleButtonB = ${isToggleButtonB.value}, History: $actionHistory")
            }
            "C" -> {
                Log.d("ToggleButtonC", "Before press: isToggleButtonC = ${isToggleButtonC.value}")
                val lastActionC = actionHistory.lastOrNull { it == "C-on" || it == "C-off" }
                if (lastActionC == "C-on") {
                    actionHistory.add("C-off")
                    isToggleButtonC.value = false
                    selectedAction.value = "C-off"
                } else {
                    actionHistory.add("C-on")
                    isToggleButtonC.value = true
                    selectedAction.value = "C-on"
                }
                Log.d("ToggleButtonC", "After press: isToggleButtonC = ${isToggleButtonC.value}, History: $actionHistory")
            }
            "D" -> {
                Log.d("ToggleButtonD", "Before press: isToggleButtonD = ${isToggleButtonD.value}")
                val lastActionD = actionHistory.lastOrNull { it == "D-on" || it == "D-off" }
                if (lastActionD == "D-on") {
                    actionHistory.add("D-off")
                    isToggleButtonD.value = false
                    selectedAction.value = "D-off"
                } else {
                    actionHistory.add("D-on")
                    isToggleButtonD.value = true
                    selectedAction.value = "D-on"
                }
                Log.d("ToggleButtonD", "After press: isToggleButtonD = ${isToggleButtonD.value}, History: $actionHistory")
            }
        }
    }

    // Function to add a delay to the history
    fun addDelayToHistory(delay: Long) {
        if (delay <= 0) {
            Log.w("ActionsAndDelaysViewModel", "Attempted to add invalid delay: $delay")
            return
        }
        val delayEntry = delay.toString() // Store delay as plain string, e.g., "1"
        actionHistory.add(delayEntry)
        selectedAction.value = delayEntry
        totalDelayMilliseconds.value = delay
        Log.d("ActionsAndDelaysViewModel", "Delay added: $delayEntry, Total ms: $delay, History: $actionHistory")
    }

    // Function to reset delay picker values after navigation
    fun resetDelayPickerValues() {
        selectedHours.value = 0
        selectedMinutes.value = 0
        selectedSeconds.value = 0
        selectedMilliseconds.value = 0
        Log.d("ActionsAndDelaysViewModel", "Delay picker values reset")
    }


    // Function to update the mode in history
    fun updateModeInHistory(mode: String) {
        val modeEntry = "${mode}m"
        actionHistory.add(modeEntry)
        selectedMode.value = mode
        Log.d("ActionsAndDelaysViewModel", "Mode updated: $modeEntry, History: $actionHistory")
    }
    fun clearActionHistory() {
        actionHistory.clear()
        selectedAction.value = null
        isToggleButtonA.value = false
        isToggleButtonB.value = false
        isToggleButtonC.value = false
        isToggleButtonD.value = false
        totalDelayMilliseconds.value = null
        selectedMode.value = ""
        textField.value = ""
        Log.d("ActionsAndDelaysViewModel", "Action history cleared")
    }

    // Function to save actions to the database
    fun saveActionToDatabase(userUid: String?, taskTitle: String?) {
        if (userUid == null) {
            Log.e("TaskDatabase", "Cannot save: userUid is null")
            return
        }
        if (taskTitle.isNullOrBlank()) {
            Log.e("TaskDatabase", "Cannot save: taskTitle is null or empty")
            return
        }


        if (actionHistory.isEmpty()) {
            Log.e("TaskDatabase", "No actions to save")
            return
        }
        viewModelScope.launch {
            try {
                val task = createTaskFromAction(actionHistory, userUid, taskTitle)
                MyDatabase.getInstance().getTaskDao().addTask(task)
                actionHistory.clear()
                selectedAction.value = null
                totalDelayMilliseconds.value = null
                isToggleButtonA.value = false
                isToggleButtonB.value = false
                isToggleButtonC.value = false
                isToggleButtonD.value = false
                textField.value = ""
                selectedMode.value = ""
                Log.d("TaskDatabase", "Task saved: $taskTitle, Actions: $actionHistory")
            } catch (e: Exception) {
                Log.e("TaskDatabase", "Error saving task: ${e.message}")
            }
        }
    }

    // Function to create a Task object from actions
    private fun createTaskFromAction(actions: List<String>, userUid: String, taskTitle: String): Task {
        return Task(
            actions = actions,
            userUid = userUid,
            taskTitle = taskTitle,
            taskOn = false,
            taskId = 0
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
