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



        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = NoteListAdapter({ id -> openDialog(id)}, {null})
        val recyclerView = binding.recyclerViewIdHistory

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator()

        // Observe archived notes that transforms to notes
        noteViewModel.allArchiveNotes.observe(viewLifecycleOwner) {
            it.let { adapter.submitList(noteViewModel.notesArchiveToNotes(it)) }
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
                if (noteViewModel.isArchivedNoteSection(viewHolder.adapterPosition)) {
                    return 0
                }
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if(!noteViewModel.isArchivedNoteSection(viewHolder.adapterPosition)) {
                    noteViewModel.deleteArchiveNote(viewHolder.adapterPosition)

                    Snackbar.make(recyclerView, "Deleted archive note", Snackbar.LENGTH_LONG)
                        .setAction("Undo") {
                            noteViewModel.undoArchiveNote()
                        }.show()
                }
            }
        }).attachToRecyclerView(recyclerView)

        /*binding.fabH.setOnClickListener {
            noteViewModel.addArchiveSectionsAndSortList()
            adapter.notifyDataSetChanged()
        }*/
    }

    override fun onPause() {
        //noteViewModel.writeHistToAssets()
        super.onPause()
    }

    private fun openDialog (pos: Int) {
        val dialog = NoteDialog()
        dialog.show(childFragmentManager, "1")
        dialog.setDialogNote(id)
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