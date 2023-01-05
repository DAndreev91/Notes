package com.example.test

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.databinding.FragmentHistoryBinding
import com.google.android.material.snackbar.Snackbar


class HistoryFragment : Fragment() {

    private lateinit var binding: FragmentHistoryBinding
    private var adapter: NoteAdapter? = null
    lateinit var noteViewModel: NoteViewModel

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        noteViewModel = ViewModelProvider(requireActivity())[NoteViewModel::class.java]
        binding = FragmentHistoryBinding.inflate(layoutInflater)

        adapter = NoteAdapter(noteViewModel.noteArchive.value!!, { pos -> openDialog(noteViewModel.noteArchiveList[pos], pos)}, {adapter?.notifyItemChanged(it)})
        val recyclerView = binding.recyclerViewIdHistory

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator()

        // Observe
        noteViewModel.noteArchive.observe(requireActivity()) {
            // notify adapter about dml operations
            // Delete item
            adapter?.notifyDataSetChanged()
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
            adapter?.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onPause() {
        noteViewModel.writeToAssets()
        super.onPause()
    }

    private fun openDialog (note: Note, pos: Int) {
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