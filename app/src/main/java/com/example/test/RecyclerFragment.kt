package com.example.test

import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.databinding.FragmentRecyclerBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class RecyclerFragment: Fragment() {
    private lateinit var binding: FragmentRecyclerBinding
    //private var adapter: NoteAdapter? = null
    private val noteViewModel: NoteViewModel by activityViewModels {
        NoteViewModelFactory(
            (activity?.application as NoteApplication).database.noteDao(),
            activity?.application as NoteApplication
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //super.onViewCreated(view, savedInstanceState)
        binding = FragmentRecyclerBinding.inflate(layoutInflater)

        val adapter = NoteAdapter({ pos -> openDialog(noteViewModel.noteList[pos], pos)}, { noteViewModel.toggleCheckNote(it) })
        val recyclerView = binding.recyclerViewIdFr

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator()

        noteViewModel.allNotes.observe(activity as MainActivity) {
            it.let { adapter.submitList(noteViewModel.noteList) }
            adapter?.notifyDataSetChanged()
        }

        // Observe
        noteViewModel.notePosChange.observe(activity as MainActivity) {
            // notify adapter about dml operations
            // Delete item
            if (it.postPos == -1 && it.prePos >= 0) {
                adapter?.notifyItemRemoved(it.prePos)
                // upd sections
                adapter?.notifyItemChanged(it.sectionPrePos)
            }
            // Add item
            else if (it.prePos == -1 && it.postPos >= 0) {
                adapter?.notifyItemInserted(it.postPos)
                // upd sections
                adapter?.notifyItemChanged(it.sectionPostPos)
            }
            // Change item
            else if (it.prePos >= 0 && it.postPos >= 0 && it.postPos == it.prePos) {
                adapter?.notifyItemChanged(it.postPos)
            }
            // Move item within Active and Planned sections
            /*else if (it.prePos >= 0 && it.postPos >= 0 && !it.isSectionChanged){
                adapter?.notifyItemMoved(it.prePos, it.postPos)
                // upd sections
                adapter?.notifyItemChanged(it.sectionPrePos)
                adapter?.notifyItemChanged(it.sectionPostPos)
            }

             */
            // Move from/to Done section or check item as done
            else if (it.prePos >= 0 && it.postPos >= 0 /*&& it.isSectionChanged*/
            ){
                //adapter?.notifyItemRemoved(it.prePos)
                //adapter?.notifyItemInserted(it.postPos)
                adapter?.notifyItemMoved(it.prePos, it.postPos)
                adapter?.notifyItemChanged(it.postPos)
                // upd sections
                adapter?.notifyItemChanged(it.sectionPrePos)
                adapter?.notifyItemChanged(it.sectionPostPos)
            }
            // scroll to top when we adding item to not done section
            if (it.postPos == 0) {
                binding.recyclerViewIdFr.scrollToPosition(0)
            }
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (target.adapterPosition == 0) {
                    return false
                }
                noteViewModel.moveNote(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun getDragDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                try {
                    if (noteViewModel.noteList[viewHolder.adapterPosition].isSection) {
                        return 0
                    }
                    return super.getDragDirs(recyclerView, viewHolder)
                } catch (e:IndexOutOfBoundsException) {
                    // In case of changing list inside coroutine bc i don't know how send signal from within coroutine
                    return 0
                }

            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                try {
                    if (noteViewModel.noteList[viewHolder.adapterPosition].isSection) {
                        return 0
                    }
                    return super.getSwipeDirs(recyclerView, viewHolder)
                } catch (e:IndexOutOfBoundsException) {
                    // In case of changing list inside coroutine bc i don't know how send signal from within coroutine
                    return 0
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if(!noteViewModel.noteList[viewHolder.adapterPosition].isSection) {
                    noteViewModel.deleteNote(viewHolder.adapterPosition, noteViewModel.noteList)

                    Snackbar.make(recyclerView, "Deleted note", Snackbar.LENGTH_LONG)
                        .setAction("Undo") {
                            noteViewModel.undoNote(noteViewModel.noteList)
                        }.show()
                }
            }
        }).attachToRecyclerView(recyclerView)

        binding.fabFr.setOnClickListener {
            try {
                val newNote = Note(
                    "", "", false,
                    isFuture = false,
                    doneDate = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).format(Date()),
                    isSection = false
                )
                openDialog(newNote, -1)
            } catch(e:Exception) {
                binding.logView.text = e.stackTraceToString()
                binding.logView.visibility = View.VISIBLE
                binding.logView.setOnClickListener {
                    binding.logView.visibility = View.GONE
                }
            }
        }

        return binding.root
    }

    override fun onPause() {
        noteViewModel.writeToAssets()
        super.onPause()
    }

    private fun openDialog (note: Note, pos: Int) {
        try {
            val dialog = NoteDialog()
            dialog.setDialogNote(note, pos)
            dialog.show(parentFragmentManager, "1")
        } catch(e:Exception) {
            binding.logView.text = e.stackTraceToString()
            binding.logView.visibility = View.VISIBLE
            binding.logView.setOnClickListener {
                binding.logView.visibility = View.GONE
            }
        }
    }

    companion object {
        private var instance: RecyclerFragment? = null
        fun getInstance(): RecyclerFragment {
            if (instance == null) {
                instance = RecyclerFragment()
            }
            return instance!!
        }
    }
}