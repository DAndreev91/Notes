package com.example.test

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.databinding.FragmentRecyclerBinding
import com.google.android.material.snackbar.Snackbar

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

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT){
            var from = -1
            var to = -1
            var drag = false
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                Log.d("START MOVE NOTE", "START MOVE NOTE! from = ${viewHolder.adapterPosition}; to = ${target.adapterPosition}")
                if (target.adapterPosition == 0) {
                    return false
                }
                if (from == -1) {
                    from = viewHolder.adapterPosition
                }
                to = target.adapterPosition
                noteViewModel.moveNote(viewHolder.adapterPosition, target.adapterPosition)
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
                        noteViewModel.moveNotesToDb()
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
                    // In case of changing list inside coroutine bc i don't know how to send signal within coroutine
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
                    // In case of changing list inside coroutine bc i don't know how send signal within coroutine
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
            //try {
                openDialog(null)
            /*} catch(e:Exception) {
                binding.logView.text = e.stackTraceToString()
                binding.logView.visibility = View.VISIBLE
                binding.logView.setOnClickListener {
                    binding.logView.visibility = View.GONE
                }
            }*/
        }
    }

    override fun onPause() {
        noteViewModel.writeToAssets()
        super.onPause()
    }

    private fun openDialog (id: Int?) {
        Log.i("OPEN DIALOG", "id = $id")
        //try {
            val dialog = NoteDialog(noteViewModel)
            dialog.setDialogNote(id)
            dialog.show(parentFragmentManager, "1")
        /*} catch(e:Exception) {
            binding.logView.text = e.stackTraceToString()
            binding.logView.visibility = View.VISIBLE
            binding.logView.setOnClickListener {
                binding.logView.visibility = View.GONE
            }
        }*/
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