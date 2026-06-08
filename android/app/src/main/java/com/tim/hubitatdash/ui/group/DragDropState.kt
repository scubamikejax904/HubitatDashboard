package com.tim.hubitatdash.ui.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Manages drag-and-drop state for the tile grid.
 *
 * Each tile registers its screen bounds via [updateBounds]. On long-press,
 * [onDragStart] sets the dragging index. [onDrag] accumulates the delta and
 * swaps items when the finger crosses into a different slot. [onDragEnd] fires
 * [onReorder] with the final ordered list of tile keys.
 */
class DragDropState(
    initialKeys: List<String>,
    private val onReorder: (List<String>) -> Unit
) {
    // Mutable snapshot list – swapped in real time during drag
    var keys by mutableStateOf(initialKeys)
        private set

    var draggingIndex by mutableIntStateOf(-1)
        private set

    // Accumulated pixel offset of the dragged tile relative to its original position
    var dragOffset by mutableStateOf(Offset.Zero)
        private set

    // Screen-space bounds of each tile, keyed by list index
    val bounds = mutableStateMapOf<Int, Rect>()

    fun updateKeys(newKeys: List<String>) {
        if (!dragging) keys = newKeys
    }

    val dragging get() = draggingIndex >= 0

    fun onDragStart(index: Int) {
        draggingIndex = index
        dragOffset = Offset.Zero
    }

    fun onDrag(delta: Offset) {
        if (!dragging) return
        dragOffset += delta

        // Compute absolute centre of the dragged tile in screen space
        val origin = bounds[draggingIndex] ?: return
        val absCenter = Offset(
            origin.center.x + dragOffset.x,
            origin.center.y + dragOffset.y
        )

        // Find the target slot whose bounds contain the finger centre
        val targetIndex = bounds.entries
            .filter { it.key != draggingIndex }
            .firstOrNull { (_, rect) -> rect.contains(absCenter) }
            ?.key ?: return

        // Swap in the list
        val mutable = keys.toMutableList()
        val tmp = mutable[draggingIndex]
        mutable[draggingIndex] = mutable[targetIndex]
        mutable[targetIndex] = tmp
        keys = mutable

        // Update bounds to match the swap
        val tmpBounds = bounds[draggingIndex]
        bounds[draggingIndex] = bounds[targetIndex] ?: return
        if (tmpBounds != null) bounds[targetIndex] = tmpBounds

        draggingIndex = targetIndex
    }

    fun onDragEnd() {
        draggingIndex = -1
        dragOffset = Offset.Zero
        onReorder(keys)
    }

    fun onDragCancel() {
        draggingIndex = -1
        dragOffset = Offset.Zero
    }
}

