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
import androidx.lifecycle.ViewModelProvider
import com.example.test.databinding.NoteIuBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDialog(val viewModel: NoteViewModel): DialogFragment() {
    lateinit var note: com.example.test.data.Note
    private var isEditable = true
    private lateinit var binding: NoteIuBinding
    /*private val noteViewModel: NoteViewModel? by activityViewModels {
        NoteViewModelFactory(
            (activity?.application as NoteApplication).database.noteDao(),
            activity?.application as NoteApplication
        )
    }*/

    fun setDialogNote(id: Int?) {
        if (id != null) {
            Log.i("SET DIALOG NOTE", "id = $id; noteViewModel = $viewModel")
            viewModel.getNote(id).observe(viewLifecycleOwner) {
                note = it
            }
        } else {
            note = com.example.test.data.Note(
                title = "",
                desc = "",
                isChecked = false,
                isFuture = false,
                isSection = false,
                doneDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(Date()),
                pos = 1,
                section = "Active"
            )
        }
    }

    fun setDialogNote(newNote: com.example.test.data.Note) {
        note = newNote
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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
            val newNote = com.example.test.data.Note (
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

            viewModel.addNote(newNote)
        }
        super.onDismiss(dialog)
    }


}