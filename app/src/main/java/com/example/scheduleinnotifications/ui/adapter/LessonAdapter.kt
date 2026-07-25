package com.example.scheduleinnotifications.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.scheduleinnotifications.data.model.Lesson
import com.example.scheduleinnotifications.databinding.ItemLessonBinding

class LessonAdapter(
    private val onDelete: (Lesson) -> Unit
) : ListAdapter<Lesson, LessonAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemLessonBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: Lesson) {
            b.tvLessonName.text = item.name
            b.tvLessonTime.text = "%s – %s".format(
                formatMinutes(item.startMinute),
                formatMinutes(item.endMinute)
            )
            b.btnDeleteLesson.setOnClickListener { onDelete(item) }
        }

        private fun formatMinutes(min: Int) =
            "%02d:%02d".format(min / 60, min % 60)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLessonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Lesson>() {
            override fun areItemsTheSame(a: Lesson, b: Lesson) = a.id == b.id
            override fun areContentsTheSame(a: Lesson, b: Lesson) = a == b
        }
    }
}
