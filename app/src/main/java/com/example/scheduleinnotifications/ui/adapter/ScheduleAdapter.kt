package com.example.scheduleinnotifications.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.scheduleinnotifications.data.model.Schedule
import com.example.scheduleinnotifications.databinding.ItemScheduleBinding

class ScheduleAdapter(
    private val onToggle: (Schedule, Boolean) -> Unit,
    private val onClick: (Schedule) -> Unit,
    private val onDelete: (Schedule) -> Unit
) : ListAdapter<Schedule, ScheduleAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemScheduleBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: Schedule) {
            b.tvScheduleName.text = item.name
            // Обновляем switch без лишнего вызова listener'а
            b.switchEnabled.setOnCheckedChangeListener(null)
            b.switchEnabled.isChecked = item.isEnabled
            b.switchEnabled.setOnCheckedChangeListener { _, checked ->
                onToggle(item, checked)
            }
            b.root.setOnClickListener { onClick(item) }
            b.btnDeleteSchedule.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScheduleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Schedule>() {
            override fun areItemsTheSame(a: Schedule, b: Schedule) = a.id == b.id
            override fun areContentsTheSame(a: Schedule, b: Schedule) = a == b
        }
    }
}
