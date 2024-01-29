package com.example.test

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.adapters.TextViewBindingAdapter.setTextSize
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.test.data.Note
import com.example.test.databinding.NoteItemBinding

class NoteListAdapter(private val cardClick: (Int) -> Unit, private val checkClick: (Int) -> Unit): ListAdapter<Note, NoteListAdapter.NoteListViewHolder>(DiffCallback) {

    companion object DiffCallback: DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }

    inner class NoteListViewHolder(private val binding: NoteItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(cardClick: (Int) -> Unit, checkClick: (Int) -> Unit, pos: Int, note: Note) {
            binding.apply {
                // Если не секция
                if (!note.isSection) {
                    noteDesc.text = note.desc
                    noteCheck.isChecked = note.isChecked

                    // Затемнение выполненных задач
                    if (noteCheck.isChecked) {
                        card.alpha = 0.5f
                    } else {
                        card.alpha = 1f
                    }

                    // Затемнение будущих задач
                    if (note.isFuture) {
                        noteDesc.setTextColor(Color.LTGRAY)
                        noteDesc.setTypeface(null, Typeface.ITALIC)
                    } else {
                        noteDesc.setTextColor(Color.BLACK)
                        noteDesc.setTypeface(null, Typeface.NORMAL)
                    }

                    card.setOnClickListener { cardClick(note.id) }
                    noteCheck.setOnClickListener { checkClick(note.id) }
                }
                // Если секция
                else {
                    noteCheck.visibility = View.GONE
                    noteDesc.text = note.title
                    noteDesc.setTextColor(Color.parseColor("#676767"))
                    noteDesc.setTypeface(noteDesc.typeface, Typeface.BOLD)
                    noteDesc.textSize = 16.0F
                    card.elevation = 0F
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteListAdapter.NoteListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return NoteListViewHolder(NoteItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: NoteListAdapter.NoteListViewHolder, position: Int) {
        currentList.forEachIndexed { index, note -> Log.i("SUBMIT SUCCESSFULL", "index = $index id = ${note.id} title = ${note.title} desc = ${note.desc} section = ${note.section} pos = ${note.pos}") }
        holder.bind(cardClick, checkClick, position, getItem(position))
    }

    override fun onCurrentListChanged(
        previousList: MutableList<Note>,
        currentList: MutableList<Note>
    ) {
        previousList.forEachIndexed { index, note -> Log.i("SUBMIT PREVIOUS", "index = $index id = ${note.id} title = ${note.title} desc = ${note.desc} section = ${note.section} pos = ${note.pos}") }
        currentList.forEachIndexed { index, note -> Log.i("SUBMIT CURRENT", "index = $index id = ${note.id} title = ${note.title} desc = ${note.desc} section = ${note.section} pos = ${note.pos}") }
        super.onCurrentListChanged(previousList, currentList)
    }
}