package com.example.test

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.databinding.FragmentHistoryBinding
import com.google.android.material.snackbar.Snackbar


class HistoryFragment : Fragment() {

    private lateinit var binding: FragmentHistoryBinding
    private val noteViewModel: NoteViewModel by activityViewModels {
        NoteViewModelFactory(
            (activity?.application as NoteApplication).database.noteDao(),
            activity?.application as NoteApplication
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(layoutInflater)

        noteViewModel.allNotes.observe(requireActivity()) {
            noteViewModel.initLists()
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = NoteListAdapter({ pos -> openDialog(noteViewModel.noteArchiveList[pos], pos)}, {null})
        val recyclerView = binding.recyclerViewIdHistory

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator()

        // Observe
        noteViewModel.noteArchive.observe(requireActivity()) {
            // notify adapter about dml operations
            // Delete item
            it.let { adapter.submitList(it) }
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.RIGHT, ItemTouchHelper.RIGHT){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (noteViewModel.noteArchiveList[viewHolder.adapterPosition].isSection) {
                    return 0
                }
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if(!noteViewModel.noteArchiveList[viewHolder.adapterPosition].isSection) {
                    noteViewModel.deleteNote(viewHolder.adapterPosition, noteViewModel.noteArchiveList)

                    Snackbar.make(recyclerView, "Deleted archive note", Snackbar.LENGTH_LONG)
                        .setAction("Undo") {
                            noteViewModel.undoNote(noteViewModel.noteArchiveList)
                        }.show()
                }
            }
        }).attachToRecyclerView(recyclerView)

        binding.fabH.setOnClickListener {
            noteViewModel.addArchiveSectionsAndSortList()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onPause() {
        noteViewModel.writeHistToAssets()
        super.onPause()
    }

    private fun openDialog (note: com.example.test.Note, pos: Int) {
        val dialog = NoteDialog()
        dialog.setDialogNote(note, pos)
        dialog.setTextNonEditable()
        dialog.show(parentFragmentManager, "1")
    }

    companion object {
        private var instance: HistoryFragment? = null
        fun getInstance(): HistoryFragment {
            if (instance == null) {
                instance = HistoryFragment()
            }
            return instance!!
        }
    }
}