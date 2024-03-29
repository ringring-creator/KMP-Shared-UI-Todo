package com.ring.ring.kmpsharedtodo.ui.editTodo

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.ring.ring.kmpsharedtodo.data.Todo
import com.ring.ring.kmpsharedtodo.data.TodoRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditTodoScreenModel(
    private val todoRepository: TodoRepository,
    private val id: Long?,
) : ScreenModel, EditTodoStateUpdater {
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()
    private val _done = MutableStateFlow(false)
    val done = _done.asStateFlow()
    private val _deadline =
        MutableStateFlow(EditTodoUiState.Deadline.createCurrentDate())
    val deadline = _deadline.asStateFlow()
    private val _showDatePicker = MutableStateFlow(false)
    val showDatePicker = _showDatePicker.asStateFlow()
    private val _backEvent = Channel<Unit>()
    val backEvent = _backEvent.receiveAsFlow()

    init {
        screenModelScope.launch {
            withContext(dispatcher) {
                id?.let {
                    val todo = todoRepository.get(it)
                    _title.value = todo.title
                    _description.value = todo.description
                    _done.value = todo.done
                    _deadline.value = todo.deadline.toDeadline()
                }
            }
        }
    }

    override fun setTitle(title: String) {
        Napier.d(tag = "EditTodoScreenModel", message = "setTitle")
        _title.value = title
    }

    override fun setDescription(description: String) {
        Napier.d(tag = "EditTodoScreenModel", message = "setDescription")
        _description.value = description
    }

    override fun setDone(done: Boolean) {
        Napier.d(tag = "EditTodoScreenModel", message = "setDone")
        _done.value = done
    }

    override fun setDeadline(deadline: Long) {
        Napier.d(tag = "EditTodoScreenModel", message = "setDeadline")
        _deadline.value = EditTodoUiState.Deadline(milliseconds = deadline)
    }

    override fun showDatePicker() {
        Napier.d(tag = "EditTodoScreenModel", message = "showDatePicker")
        _showDatePicker.value = true
    }

    override fun dismissDatePicker() {
        Napier.d(tag = "EditTodoScreenModel", message = "dismissDatePicker")
        _showDatePicker.value = false
    }

    override fun save() {
        Napier.d(tag = "EditTodoScreenModel", message = "save")
        screenModelScope.launch {
            withContext(dispatcher) {
                todoRepository.save(
                    Todo(
                        id = id,
                        title = _title.value,
                        description = _description.value,
                        done = _done.value,
                        deadline = _deadline.value.toLocalDate(),
                    )
                )
            }
            onBack()
        }
    }

    override fun delete() {
        Napier.d(tag = "EditTodoScreenModel", message = "delete")
        screenModelScope.launch {
            withContext(dispatcher) {
                id?.let { todoRepository.delete(id = it) }
            }
            onBack()
        }
    }

    override fun onBack() {
        Napier.d(tag = "EditTodoScreenModel", message = "onBack")
        _backEvent.trySend(Unit)
    }
}