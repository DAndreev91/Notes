package com.example.test

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.test.data.Note
import com.example.test.databinding.NoteIuBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val NOTE_ID = "note_id"

class NoteDialog(): DialogFragment() {
    lateinit var note: Note
    private var noteId: Int = -1
    private var isEditable = true
    private lateinit var binding: NoteIuBinding
    private val newNoteTemplate = Note(
        title = "",
        desc = "",
        isChecked = false,
        isFuture = false,
        isSection = false,
        doneDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(Date()),
        pos = 1,
        section = "Active"
    )
    private val noteViewModel: NoteViewModel? by activityViewModels {
        NoteViewModelFactory(
            (activity?.application as NoteApplication).database.noteDao(),
            activity?.application as NoteApplication
        )
    }

    fun setDialogNote(id: Int) {
        noteId = id
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(NOTE_ID, noteId)
        Log.i("OPEN DIALOG", "onSaveInstanceState $noteId")
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
        Log.i("OPEN DIALOG", "noteId = $noteId; savedInstanceState?.getInt(NOTE_ID) = ${savedInstanceState?.getInt(NOTE_ID)?: -1}")
        if (noteId == -1) {
            noteId = savedInstanceState?.getInt(NOTE_ID)?: -1
        }
        binding = NoteIuBinding.inflate(layoutInflater)
        noteViewModel?.getNote(noteId)?.observe(requireParentFragment().viewLifecycleOwner) {
            Log.i("OPEN DIALOG", "id = $noteId")
            // Если noteId = -1 тогда подставляем шаблон для пустой заметки
            note = it ?: newNoteTemplate
            bind(note)
        }
        return AlertDialog.Builder(requireActivity()).setView(binding.root).create()
    }
    override fun onCancel(dialog: DialogInterface) {
        if (isEditable) {
            note.desc = binding.noteDesc.text.toString()
            // В зависимости от noteId сохраняем либо уже существующую заметку, либо новую с обновлением position у всех последующих
            if (noteId == -1) {
                noteViewModel?.addNewNote(note)
            } else {
                noteViewModel?.addNote(note)
            }
        }
        super.onDismiss(dialog)
    }


}