package org.wordpress.android.ui.quickstart.viewholders

import android.graphics.Paint
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.databinding.QuickStartListItemBinding
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment.QuickStartTaskCard
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.util.extensions.redirectContextClickToLongPressListener
import org.wordpress.android.util.extensions.viewBinding

class TaskViewHolder(
    parent: ViewGroup,
    private val binding: QuickStartListItemBinding = parent.viewBinding(QuickStartListItemBinding::inflate)
) : ViewHolder(binding.root) {
    fun bind(taskCard: QuickStartTaskCard, shouldHideDivider: Boolean) {
        val clickListener = View.OnClickListener {
            taskCard.onTaskTapped(taskCard.task)
        }
        val longClickListener = View.OnLongClickListener {
            val popup = PopupMenu(itemView.context, binding.popupAnchor)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                if (item.itemId == R.id.quick_start_task_menu_skip) {
                    taskCard.let { taskCard.onSkipTaskTapped(it.task) }
                    return@setOnMenuItemClickListener true
                }
                false
            }
            popup.inflate(R.menu.quick_start_task_menu)
            popup.show()
            true
        }
        itemView.setOnClickListener(clickListener)
        itemView.setOnLongClickListener(longClickListener)
        itemView.redirectContextClickToLongPressListener()

        val isEnabled = !taskCard.isCompleted
        with(binding) {
            icon.isEnabled = isEnabled
            title.isEnabled = isEnabled
            itemView.isLongClickable = isEnabled
            if (!isEnabled) title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            // Hide divider for tasks before header and end of list.
            divider.visibility = if (shouldHideDivider) View.INVISIBLE else View.VISIBLE

            val quickStartTaskDetails = QuickStartTaskDetails.getDetailsForTask(taskCard.task)
                    ?: throw IllegalStateException("$taskCard task is not recognized in adapter.")
            icon.setImageResource(quickStartTaskDetails.iconResId)
            title.setText(quickStartTaskDetails.titleResId)
            subtitle.setText(quickStartTaskDetails.subtitleResId)
        }
    }
}
