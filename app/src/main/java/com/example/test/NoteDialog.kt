package com.example.test

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.test.databinding.NoteIuBinding

class NoteDialog: DialogFragment() {
    lateinit var note: Note
    private var notePosition: Int = -1
    private var isEditable = true
    private lateinit var binding: NoteIuBinding
    lateinit var noteViewModel: NoteViewModel

    fun setDialogNote(noteSend: Note, notePos: Int) {
        note = noteSend
        notePosition = notePos
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        noteViewModel = ViewModelProvider(requireActivity())[NoteViewModel::class.java]
        binding = NoteIuBinding.inflate(layoutInflater)
        //binding.noteTitle.setText(note.title)
        binding.noteDesc.setText(note.desc)
        if (note.isChecked) {
            binding.noteDesc.setTextColor(Color.LTGRAY)
        } else {
            binding.noteDesc.setTextColor(Color.BLACK)
        }
        if (!isEditable) {
            binding.noteDesc.inputType = InputType.TYPE_NULL
        }
        return AlertDialog.Builder(requireActivity()).setView(binding.root).create()
    }

    fun setTextNonEditable() {
        isEditable = false
    }

    override fun onCancel(dialog: DialogInterface) {
        if (isEditable) {
            note.desc = binding.noteDesc.text.toString()

            noteViewModel.addNote(notePosition, note)
        }
        super.onDismiss(dialog)
    }


}