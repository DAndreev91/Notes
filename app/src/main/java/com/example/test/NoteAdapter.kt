package com.example.test

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(val cardClick: (Int) -> Unit, val checkClick: (Int) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val SECTION_VIEW= 0
    private val CONTENT_VIEW = 1

    private var noteList: MutableList<Note>? = null

    fun submitList(list: MutableList<Note>) {
        noteList = list
    }

    inner class NoteHolder(view: View): RecyclerView.ViewHolder(view) {
        //val noteTitle = view.findViewById<TextView>(R.id.noteTitle)!!
        val noteDesc = view.findViewById<TextView>(R.id.noteDesc)!!
        val noteIsChecked = view.findViewById<CheckBox>(R.id.noteCheck)!!
        val cardView = view.findViewById<CardView>(R.id.card)!!
        init {
            // listen clicks on all view (card view)
            cardView.setOnClickListener {
                cardClick(adapterPosition)
            }
            // listen clicks on checkbox
            noteIsChecked.setOnClickListener {
                checkClick(adapterPosition)
            }
        }
    }

    inner class SectionHolder(view: View): RecyclerView.ViewHolder(view) {
        val sectionTitle = view.findViewById<TextView>(R.id.sectionTitle)!!
        val textHolder = view.findViewById<TextView>(R.id.textHolder)!!
    }

    override fun getItemViewType(position: Int): Int {
        return if (noteList?.get(position)?.isSection == true) {
            SECTION_VIEW
        } else {
            CONTENT_VIEW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == CONTENT_VIEW) {
            NoteHolder(LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false))
        } else {
            SectionHolder(LayoutInflater.from(parent.context).inflate(R.layout.section_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val note = noteList?.get(position)
        if (getItemViewType(position) == SECTION_VIEW ) {
            (holder as SectionHolder).sectionTitle.text = note?.title
            if ((noteList?.size ?: 0) <= position + 1) {
                holder.textHolder.visibility = View.VISIBLE
            } else if (noteList?.get(position+1)?.isSection == true) {
                holder.textHolder.visibility = View.VISIBLE
            } else {
                holder.textHolder.visibility = View.GONE
            }
            return
        }
        val noteHolder = holder as NoteHolder
        //noteHolder.noteTitle.text = noteList[position].title
        // Title is gone when not filled
        /*
        if (noteHolder.noteTitle.text == "") {
            noteHolder.noteTitle.visibility = View.GONE
        } else {
            noteHolder.noteTitle.visibility = View.VISIBLE
        }
         */
        noteHolder.noteDesc.text = note?.desc// + ";" + noteList[position].doneDate
        noteHolder.noteIsChecked.isChecked = note?.isChecked == true

        // set text color to gray when note is checked
        if (noteHolder.noteIsChecked.isChecked) {
            noteHolder.cardView.alpha = 0.5f
        } else {
            noteHolder.cardView.alpha = 1f
        }
        if (note?.isFuture == true) {
            noteHolder.noteDesc.setTextColor(Color.LTGRAY)
            noteHolder.noteDesc.setTypeface(null, Typeface.ITALIC)
        } else {
            noteHolder.noteDesc.setTextColor(Color.BLACK)
            noteHolder.noteDesc.setTypeface(null, Typeface.NORMAL)
        }
    }

    override fun getItemCount(): Int {
        return noteList?.size ?: 0
    }

}