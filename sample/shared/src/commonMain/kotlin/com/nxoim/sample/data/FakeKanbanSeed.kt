package com.nxoim.sample.data

import com.nxoim.sample.model.KanbanCategory
import com.nxoim.sample.model.KanbanNote
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.model.TaskStatus

internal data class BoardRecord(
    val categories: List<CategoryRecord>,
    val notes: List<NoteRecord>,
    val nextTaskNumber: Int,
)

internal data class CategoryRecord(
    val id: String,
    val title: String,
    val subtitle: String,
    val tasks: List<TaskRecord>,
)

internal data class TaskRecord(
    val id: String,
    val title: String,
    val description: String,
    val status: TaskStatus = TaskStatus.Open,
)

internal data class NoteRecord(
    val id: String,
    val taskId: String,
    val title: String,
    val text: String,
)

internal fun BoardRecord.findTask(taskId: String): TaskRecord? =
    categories.asSequence()
        .flatMap { category -> category.tasks.asSequence() }
        .firstOrNull { task -> task.id == taskId }

internal fun BoardRecord.notesFor(taskId: String): List<NoteRecord> =
    notes.filter { note -> note.taskId == taskId }

internal fun categoryModel(record: CategoryRecord) =
    KanbanCategory(record.id, record.title, record.subtitle, record.tasks.map(::taskModel))

internal fun taskModel(record: TaskRecord) =
    KanbanTask(record.id, record.title, record.description, record.status)

internal fun noteModel(record: NoteRecord) = KanbanNote(record.id, record.title, record.text)

internal fun <T> List<T>.page(startIndex: Int, pageSize: Int): List<T> =
    subList(startIndex.coerceIn(0, size), (startIndex + pageSize).coerceIn(0, size))

internal fun seedBoard() = BoardRecord(
    categories = listOf(
        CategoryRecord(
            "inbox", "Inbox", "Start here", listOf(
                TaskRecord(
                    "task-1",
                    "Everything is powered by swipeable",
                    "The core gesture engine driving card dismissal, stacks, and custom animations."
                ),
                TaskRecord(
                    "task-2",
                    "RTL-aware SwipeConstraint",
                    "8-directional gesture classification with automatic bidirectional mirroring."
                ),
                TaskRecord(
                    "task-3",
                    "Custom spring builders",
                    "Physics-first spring specs for seamless, interruptible motion."
                ),
                TaskRecord(
                    "task-4",
                    "Predictive back via ESC key",
                    "Desktop keyboard integration mapped to predictive back dispatchers."
                ),
            )
        ),
        CategoryRecord(
            "work", "Work", "Build the next step", listOf(
                TaskRecord(
                    "task-5",
                    "Reactive Decompose stack animator",
                    "Stack screens remain rendered until their transition animations fully complete."
                ),
                TaskRecord(
                    "task-6",
                    "Dynamic visibility querying",
                    "Stack animator queries animations to dynamically adapt gesture touch targets."
                ),
                TaskRecord(
                    "task-7",
                    "Deep stack occlusion pruning",
                    "AffectedItemsPolicy skips traversing occluded screens in deep navigation histories."
                ),
                TaskRecord(
                    "task-8",
                    "Decompose extensions library",
                    "Turnkey navigation primitives with pre-packaged transition animations."
                ),
                TaskRecord(
                    "task-9",
                    "Card stabilization physics",
                    "Stabilizing visible cards under continuous drag to produce realistic tactile weight."
                ),
            )
        ),
        CategoryRecord(
            "ideas", "Ideas", "Try an experiment", listOf(
                TaskRecord(
                    "task-10",
                    "Mock AnimatedVisibilityScope",
                    "Enabling Compose shared element bounds across custom Decompose stack boundaries."
                ),
                TaskRecord(
                    "task-11",
                    "Contextual shared elements",
                    "Isolating transitions to avoid visual artifacts between board cards and lists."
                ),
                TaskRecord(
                    "task-12",
                    "Card stack review interactions",
                    "Layered card gestures combining swipe classification with dynamic tilt and scale.",
                    TaskStatus.Done
                ),
            )
        ),
        CategoryRecord(
            "personal", "Personal", "Keep momentum", listOf(
                TaskRecord(
                    "task-13",
                    "Magnetic list animations",
                    "Magnetism shifts collection items smoothly during swipe and dismiss gestures."
                ),
                TaskRecord(
                    "task-14",
                    "Velocity preservation in gestures",
                    "Fling speeds feed directly into springs for natural kinetic settling.",
                    TaskStatus.Done
                ),
            )
        ),
        CategoryRecord("someday", "Someday", "No pressure", emptyList()),
    ),
    notes = listOf(
        NoteRecord(
            "note-1",
            "task-1",
            "Target & angle cancellation",
            "Swipeable ignores and cancels touch reception when exceeding the cancellation cone threshold, preventing accidental swipes while scrolling vertically."
        ),
        NoteRecord(
            "note-2",
            "task-1",
            "Kinetic velocity reaction",
            "Animations directly consume gesture release velocity to preserve momentum, with the exception of shared transitions."
        ),
        NoteRecord(
            "note-3",
            "task-2",
            "Automatic mirroring",
            "Start and End angles automatically invert based on LocalLayoutDirection, ensuring natural gesture ergonomics in RTL locales."
        ),
        NoteRecord(
            "note-4",
            "task-2",
            "Tolerance cones",
            "Supports 120° single-direction and 90° multi-direction tolerance cones to reject off-axis finger drift."
        ),
        NoteRecord(
            "note-5",
            "task-3",
            "Interruptibility",
            "Gestures can retarget mid-flight without snapping or velocity spikes because spring states retain physical momentum."
        ),
        NoteRecord(
            "note-6",
            "task-4",
            "Keyboard gestures",
            "Bound the Escape key directly to Decompose's predictive back handler to emulate back swipe gestures on desktop."
        ),
        NoteRecord(
            "note-7",
            "task-5",
            "Lifecycle decoupling",
            "Decompose stack items remain rendered until the animation finishes, even after Decompose has already popped the item from state."
        ),
        NoteRecord(
            "note-8",
            "task-5",
            "Immediate cleanup",
            "Items that are not rendered and have been removed skip running animations and get cleared out immediately."
        ),
        NoteRecord(
            "note-9",
            "task-6",
            "Target inspection",
            "The stack animator asks animations whether they will make items visible at target states to adjust the number of interactive items."
        ),
        NoteRecord(
            "note-10",
            "task-7",
            "Occlusion stopping condition",
            "Traversal stops as soon as it encounters occluded items that are neither visible nor transitioning, avoiding wasted draw passes."
        ),
        NoteRecord(
            "note-11",
            "task-7",
            "Render order strategy",
            "RenderOrderStrategy.byStackIndex() ensures entering and exiting screens maintain correct Z-index layering without z-fighting."
        ),
        NoteRecord(
            "note-12",
            "task-8",
            "Default presets",
            "Provides default slide, fade, and predictive back animations without repetitive custom animators."
        ),
        NoteRecord(
            "note-13",
            "task-9",
            "Restoring forces",
            "Offsets and rotations on visible cards stabilize dynamically under continuous drag input."
        ),
        NoteRecord(
            "note-14",
            "task-10",
            "Synthetic scope workaround",
            "A mock AnimatedVisibilityScope makes shared element transitions possible outside standard Compose Navigation."
        ),
        NoteRecord(
            "note-15",
            "task-10",
            "Pre-render side-effect",
            "Because of the mock scope, target items appear rendered until the transition actually starts."
        ),
        NoteRecord(
            "note-16",
            "task-11",
            "Mitigating scope quirks",
            "Disabling list item transitions from board category cards prevents layout jumps caused by scope mocking."
        ),
        NoteRecord(
            "note-17",
            "task-11",
            "skipToLookaheadSize",
            "Wrapping shared content in skipToLookaheadSize() prevents text re-wrapping jitter during bounds resizing."
        ),
        NoteRecord(
            "note-18",
            "task-12",
            "3D evaluation gestures",
            "Tracks translation and tilt falloff during rapid swipe evaluation in review mode."
        ),
        NoteRecord(
            "note-19",
            "task-13",
            "Magnetism vs dismissal",
            "Magnetism is a custom animation engine while swipe-to-dismiss integrates seamlessly with Material 3."
        ),
        NoteRecord(
            "note-20",
            "task-13",
            "Pagination integration",
            "Coordinates with evolpagink to maintain stable scroll offsets during dynamic list item insertions and removals."
        ),
        NoteRecord(
            "note-21",
            "task-14",
            "Kinetic handoff",
            "Velocity is passed into spring specs so quick flicks snap instantly while slow drags settle gently."
        ),
    ),
    nextTaskNumber = 15,
)
