package com.example.test

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.test.data.Note
import com.example.test.databinding.NoteIuBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDialog(): DialogFragment() {
    lateinit var note: Note
    private var noteId: Int = 0
    private var isEditable = true
    private lateinit var binding: NoteIuBinding
    private val noteViewModel: NoteViewModel? by activityViewModels {
        NoteViewModelFactory(
            (activity?.application as NoteApplication).database.noteDao(),
            activity?.application as NoteApplication
        )
    }

    fun setDialogNote(id: Int?) {
        if (id != null) {
            noteId = id
        } else {
            note = Note(
                title = "",
                desc = "",
                isChecked = false,
                isFuture = false,
                isSection = false,
                doneDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(Date()),
                pos = 1,
                section = "Active"
            )
            bind(note)
        }
    }

    fun setDialogNote(newNote: Note) {
        note = newNote
    }

    private fun bind(newNote: Note) {
        binding.apply {
            noteDesc.setText(newNote.desc)
            if (newNote.isChecked) {
                noteDesc.setTextColor(Color.LTGRAY)
            } else {
                noteDesc.setTextColor(Color.BLACK)
            }
            if (!isEditable) {
                noteDesc.inputType = InputType.TYPE_NULL
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = NoteIuBinding.inflate(layoutInflater)
        return AlertDialog.Builder(requireActivity()).setView(binding.root).create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteViewModel?.getNote(noteId)?.observe(viewLifecycleOwner) {
            note = it
            bind(note)
        }
    }

    fun setTextNonEditable() {
        isEditable = false
    }

    override fun onCancel(dialog: DialogInterface) {
        if (isEditable) {
            val newNote = Note (
                note.id,
                note.title,
                binding.noteDesc.text.toString(),
                note.isChecked,
                note.isFuture,
                note.doneDate,
                note.isSection,
                note.pos,
                note.section
            )

            noteViewModel?.addNote(newNote)
        }
        super.onDismiss(dialog)
    }


}