package com.nxoim.sample.model

enum class TaskStatus { Open, Done, Archived }

data class KanbanNote(
    val id: String,
    val title: String,
    val text: String,
)

data class KanbanTask(
    val id: String,
    val title: String,
    val description: String,
    val status: TaskStatus = TaskStatus.Open,
)

data class KanbanCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val tasks: List<KanbanTask>,
)
