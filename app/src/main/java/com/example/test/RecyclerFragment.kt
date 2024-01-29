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
import com.example.test.data.Note
import com.example.test.databinding.FragmentRecyclerBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class RecyclerFragment: Fragment() {
    private lateinit var binding: FragmentRecyclerBinding
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = NoteListAdapter({ id -> openDialog(id)}, { noteViewModel.toggleCheckNote(it) })
        val recyclerView = binding.recyclerViewIdFr

        recyclerView.layoutManager = LinearLayoutManager(this.context)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator()

        noteViewModel.allNotes.observe(viewLifecycleOwner) {
            noteViewModel.setNotesFromDB()
        }
        noteViewModel.allNotesForView.observe(viewLifecycleOwner) {
            it.let {
                it.forEachIndexed { index, note -> Log.i("SUBMIT LIST", "index = $index id = ${note.id} title = ${note.title} desc = ${note.desc} section = ${note.section} pos = ${note.pos}") }
                adapter.submitList(it)
            }
        }

        /*
        // Observe
        noteViewModel.notePosChange.observe(activity as MainActivity) {
            // notify adapter about dml operations
            // Delete item
            if (it.postPos == -1 && it.prePos >= 0) {
                adapter.notifyItemRemoved(it.prePos)
                // upd sections
                adapter.notifyItemChanged(it.sectionPrePos)
            }
            // Add item
            else if (it.prePos == -1 && it.postPos >= 0) {
                adapter.notifyItemInserted(it.postPos)
                // upd sections
                adapter.notifyItemChanged(it.sectionPostPos)
            }
            // Change item
            else if (it.prePos >= 0 && it.postPos >= 0 && it.postPos == it.prePos) {
                adapter.notifyItemChanged(it.postPos)
            }
            // Move from/to Done section or check item as done
            else if (it.prePos >= 0 && it.postPos >= 0 /*&& it.isSectionChanged*/
            ){
                adapter.notifyItemMoved(it.prePos, it.postPos)
                adapter.notifyItemChanged(it.postPos)
                // upd sections
                adapter.notifyItemChanged(it.sectionPrePos)
                adapter.notifyItemChanged(it.sectionPostPos)
            }
            // scroll to top when we adding item to not done section
            if (it.postPos == 0) {
                binding.recyclerViewIdFr.scrollToPosition(0)
            }
        }

         */

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT){
            var from = -1
            var to = -1
            var drag = false
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (target.adapterPosition == 0) {
                    return false
                }
                if (from == -1) {
                    from = viewHolder.adapterPosition
                }
                to = target.adapterPosition
                noteViewModel.moveNote(viewHolder.adapterPosition, target.adapterPosition)
                //adapter.notifyItemChanged(target.adapterPosition)
                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if(actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    drag = true;
                }
                if(actionState == ItemTouchHelper.ACTION_STATE_IDLE && drag) {
                    if (from != to) {
                        Log.d("FINISH MOVE NOTE", "FINISH MOVE NOTE! from = $from; to = $to")
                        //noteViewModel.moveNotesToDb()
                        drag = false
                        from = -1
                        to = -1
                    }
                }
            }

            override fun getDragDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                try {
                    if (noteViewModel.isNoteSection(viewHolder.adapterPosition)) {
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
                    if (noteViewModel.isNoteSection(viewHolder.adapterPosition)) {
                        return 0
                    }
                    return super.getSwipeDirs(recyclerView, viewHolder)
                } catch (e:IndexOutOfBoundsException) {
                    // In case of changing list inside coroutine bc i don't know how send signal from within coroutine
                    return 0
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if(!noteViewModel.isNoteSection(viewHolder.adapterPosition)) {
                    noteViewModel.deleteNote(viewHolder.adapterPosition)

                    Snackbar.make(recyclerView, "Deleted note", Snackbar.LENGTH_LONG)
                        .setAction("Undo") {
                            noteViewModel.undoNote()
                        }.show()
                }
            }
        }).attachToRecyclerView(recyclerView)

        binding.fabFr.setOnClickListener {
            try {
                openDialog(null)
            } catch(e:Exception) {
                binding.logView.text = e.stackTraceToString()
                binding.logView.visibility = View.VISIBLE
                binding.logView.setOnClickListener {
                    binding.logView.visibility = View.GONE
                }
            }
        }
    }

    override fun onPause() {
        noteViewModel.writeToAssets()
        super.onPause()
    }

    private fun openDialog (id: Int?) {
        try {
            val dialog = NoteDialog()
            dialog.setDialogNote(id)
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