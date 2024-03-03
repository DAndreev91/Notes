package com.example.test

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.test.data.Note
import com.example.test.data.NoteArchive
import com.example.test.data.NoteDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

const val ACTIVE = "Active"
const val PLANNED = "Planned"
const val DONE = "Done"


class NoteViewModel(private val noteDao: NoteDao, application: Application) : AndroidViewModel(application) {

    val allNotes: LiveData<List<Note>> = noteDao.getNotes().asLiveData()
    val allArchiveNotes: LiveData<List<NoteArchive>> = noteDao.getArchiveNotes().asLiveData()
    val allNotesForView: MutableLiveData<List<Note>> = MutableLiveData()
    private var noteListTmp = mutableListOf<Note>()
    //private val mainListFileName = "QueueFile.txt"
    //private val archiveListFileName = "QueueArchiveFile.txt"

    //var noteArchive: MutableLiveData<MutableList<Note>> = MutableLiveData()

    private var deleteNotePos: Int = -1
    private lateinit var deleteNote: Note
    private var deleteArchiveNotePos: Int = -1
    private lateinit var deleteArchiveNote: NoteArchive
    //private val nullDateStr = "01.01.1900"
    private val hoursBeforeArchiving = 0.01
    private var preFrom = 0
    private var preTo = 0
    private var noteListTmpList: List<Note> = mutableListOf()

    init {
        initLists()
    }
    private fun initLists() {
        moveNotesToArchive()
    }

    fun setNotesFromDB() {
        allNotesForView.value = allNotes.value
    }

    private fun getNewNotesListAfterMoving(tmpList: List<Note>, moveToPosition: Int): List<Note> {
        return tmpList.mapIndexed(){
            index, note ->
            var isChecked = note.isChecked
            var isFuture = note.isFuture
            val isSection = note.isSection
            var doneDate = note.doneDate
            val section = when (index) {
                moveToPosition -> (getSectionFromPosition(tmpList, index)) ?: ACTIVE
                else -> note.section
            }
            when (section) {
                ACTIVE -> {
                    isChecked = false
                    isFuture = false
                }
                PLANNED -> {
                    isChecked = false
                    isFuture = true
                }
                DONE -> {
                    isChecked = true
                    isFuture = false
                    doneDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(Calendar.getInstance().time)
                }
            }
            Log.i("MOVE NOTE. NEW LIST", "$index. isChecked = $isChecked; isFuture = $isFuture; isSection = $isSection; section = $section")
            Note(note.id, note.title, note.desc, isChecked, isFuture, doneDate, isSection, index, section)
        }
    }

    private fun getSectionFromPosition(noteList: List<Note>, to: Int): String? {
        return  try {
            noteList.filterIndexed { index, note -> note.isSection && index <= to }.last().section
        } catch (e: NoSuchElementException) {
            null
        }
    }


    private fun clearPrePositions() {
        preFrom = -1
        preTo = -1
    }

    fun moveNote(from: Int, to: Int) {
        // Защита от идиотизма (
        // 1. когда с UI приходит команда два раза подряд с одним и тем же from и to
        // 2. когда с UI приходит команда перескочить через один элемент (from-to > 1/-1) из-за того что не успела view обновиться из livedata
        if ((preFrom != from || preTo != to) && from != to && abs(from-to) == 1) {
            noteListTmp = allNotesForView.value!!.toMutableList()
            // Swapping note to new place. Old logic had refreshNoteState exec here
            Collections.swap(noteListTmp, from, to)
            // Обновляем список при каждом перетаскивании, но при этом не вызывая diffUtil (не меняем свойства списка, только индексы)
            // Только в конце пересобираем список и обновляем listAdapter
            allNotesForView.value = noteListTmp
            preFrom = from
            preTo = to
            Log.d("MOVE NOTE", "from = $from; to = $to")
        }
    }

    fun moveNotesToDb(changedNoteNewPosition: Int) {
        // Нужно полностью пересобрать изменённые объекты внутри списка, а не менять свойства внутри
        noteListTmpList = getNewNotesListAfterMoving(noteListTmp, changedNoteNewPosition)
        clearPrePositions()
        // Обновляем список в RV вызывая DiffUtil уже после отпускания/сброса элемента
        allNotesForView.value = noteListTmpList
        viewModelScope.launch {
            noteDao.insertAll(allNotesForView.value!!)
        }
    }

    fun deleteNote(pos: Int) {
        // перевести на работу с другим списком
        allNotesForView.value?.get(pos)?.let {
            deleteNotePos = pos
            deleteNote = it
            viewModelScope.launch {
                noteDao.delete(it)
                noteDao.compressPositionsAfterPosition(pos)
            }
        }
    }

    fun undoNote() {
        if (deleteNotePos != -1) {
            viewModelScope.launch {
                noteDao.stretchPositionsAfterPosition(deleteNotePos)
                noteDao.insert(deleteNote)
            }
            deleteNotePos = -1
        }
    }

    fun deleteArchiveNote(pos: Int) {
        // перевести на работу с другим списком
        allArchiveNotes.value?.get(pos)?.let {
            deleteArchiveNotePos = pos
            deleteArchiveNote = it
            viewModelScope.launch {
                noteDao.deleteArchive(it)
            }
        }
    }

    fun undoArchiveNote() {
        if (deleteArchiveNotePos != -1) {
            viewModelScope.launch {
                noteDao.insertArchive(deleteArchiveNote)
            }
            deleteNotePos = -1
        }
    }

    fun addNewNote(note: Note) {
        // adding new item
        viewModelScope.launch {
            noteDao.stretchPositionsAfterPosition(note.pos)
            noteDao.insert(note)
        }
    }
    fun addNote(note: Note) {
        // adding new item
        viewModelScope.launch {
            noteDao.insert(note)
        }
    }

    fun toggleCheckNote(id: Int) {
        noteListTmp = allNotesForView.value!!.toMutableList()
        // Ищем заметку в списке по id
        val note = noteListTmp.last { it.id == id }
        val newNotePosition: Int

        // В зависимости от флага isChecked переставляем заметку либо в начало, либо в конец
        Log.i("TOGGLE CHECK NOTE", "isChecked = ${note.isChecked}, pos = ${noteListTmp.lastIndexOf(note)}, note = ${note.desc}")
        if (!note.isChecked) {
            noteListTmp.remove(note)
            noteListTmp.add(note)
            newNotePosition = noteListTmp.lastIndex
            Log.i("TOGGLE CHECK NOTE", "TRUE. add to last place")
        } else {
            noteListTmp.remove(note)
            noteListTmp.add(1, note)
            newNotePosition = 1
            Log.i("TOGGLE CHECK NOTE", "FALSE. add to 1 place")
        }
        val c = Calendar.getInstance()
        // Проставляем дату завершения и флаг переворачиваем
        note.doneDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(c.time)
        note.isChecked = !note.isChecked
        // Update all list
        moveNotesToDb(newNotePosition)
    }

    fun getNote(id: Int): LiveData<Note> {
        return noteDao.getNote(id).asLiveData()
    }

    fun isNoteSection(position: Int): Boolean {
        return allNotesForView.value?.get(position)?.isSection ?: false
    }

    fun isArchivedNoteSection(position: Int): Boolean {
        return allArchiveNotes.value?.get(position)?.isSection ?: false
    }

    fun writeToAssets() {
        // don't change notes list here
        viewModelScope.launch {
            allNotes.value?.let { noteDao.insertAll(it) }
        }
        //writeToAsset(noteList, mainListFileName)
        //writeToAsset(noteArchiveList, archiveListFileName)
    }

    /*fun writeHistToAssets() {
        //writeToAsset(noteArchiveList, archiveListFileName)
    }*/
    /*
    private fun writeToAsset(list: MutableList<Note>, fileName: String) {
        try {
            val gson = GsonBuilder().create()
            val str = gson.toJson(list)
            writeToFile(str, fileName)
        } catch (e:ConcurrentModificationException) {
            // if someone change MutableList when we trying to save then just save later
            Log.e("ConcurrentModificationException", e.stackTraceToString())
        }
    }

     */
    /*
    private fun writeToFile(s: String, fileName: String) {
        val file = File(getApplication<Application>().filesDir, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText(s)

        Log.d("File dir",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toString()
        )

        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName).writeText(s)
    }

     */
    /*
    private fun readFromFile(fileName: String): String {
        val file = File(getApplication<Application>().filesDir, fileName)
        val v: String = if (file.exists()) {
            file.readText()
        } else {
            "[]"
        }
        return v
    }

     */

    private fun moveNotesToArchive() {
        viewModelScope.launch {
            var doneDate = ""
            val archiveNotes: MutableList<NoteArchive> = mutableListOf()
            var newPos = allArchiveNotes.value?.size ?: 0

            // Get notes for archive
            val notesToArchive = noteDao.getNotesNeededToArchive(hoursBeforeArchiving)

            // Adding each notes to noteArchive with sections and set new position to the end of allArchiveNotes list
            notesToArchive.forEach { noteArchive ->

                // Так как формат doneDate yyyy-MM-dd то просто проверяем каждую уникальную doneDate
                // из списка notesToArchive на наличие в notes_archive и добавляем при необходимости в archiveNotes
                if (doneDate != noteArchive.doneDate) {
                    if (noteDao.checkIfArchiveSectionExist(noteArchive.doneDate) == 0) {
                        archiveNotes.add(
                            NoteArchive(
                                title = noteArchive.doneDate,
                                desc = noteArchive.desc,
                                doneDate = noteArchive.doneDate,
                                isSection = true,
                                pos = newPos++,
                                section = noteArchive.section
                            )
                        )
                    }
                    doneDate = noteArchive.doneDate
                }

                archiveNotes.add(
                    NoteArchive(
                        title = noteArchive.title,
                        desc = noteArchive.desc,
                        doneDate = noteArchive.doneDate,
                        isSection = noteArchive.isSection,
                        pos = newPos++,
                        section = noteArchive.section
                    )
                )
            }
            // Insert new noteArchive to table
            noteDao.insertArchiveAll(archiveNotes)
            // Delete inserted notes from table "notes"
            noteDao.deleteNotes(notesToArchive)
        }
    }

    fun notesArchiveToNotes(list: List<NoteArchive>): List<Note> {
        return list.map {
            Note(
                id = it.id,
                title = it.title,
                desc = it.desc,
                doneDate = it.doneDate,
                isFuture = false,
                isChecked = true,
                isSection = it.isSection,
                pos = it.pos,
                section = it.section
            )
        }
    }
}

class NoteViewModelFactory(private val noteDao: NoteDao, private val application: Application): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(noteDao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}