package com.example.test

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.test.data.NoteDao
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*



class NoteViewModel(private val noteDao: NoteDao, application: Application) : AndroidViewModel(application) {

    val allNotes: LiveData<List<com.example.test.data.Note>> = noteDao.getNotes().asLiveData()

    var noteList: MutableList<Note>
    var noteArchiveList: MutableList<Note>
    private lateinit var noteTempList: MutableList<Note>
    private val mainListFileName = "QueueFile.txt"
    private val archiveListFileName = "QueueArchiveFile.txt"

    var notePosChange: MutableLiveData<NoteState> = MutableLiveData()
    var noteArchive: MutableLiveData<MutableList<Note>> = MutableLiveData()

    private var deleteNotePos: Int = -1
    private lateinit var deleteNote: Note
    private val noteState = NoteState(-1,-1, false, 0, 0)
    private var preSection: Note? = null
    private var postSection: Note? = null
    private var doneSectionPos: Int = 0
    private val nullDateStr = "01.01.1900"
    private val activeSection = Note("Active", "", false,
        isFuture = false,
        doneDate = nullDateStr,
        isSection = true
    )
    private val plannedSection = Note("Planned", "", false,
        isFuture = true,
        doneDate = nullDateStr,
        isSection = true
    )
    private val doneSection = Note("Done", "", true,
        isFuture = false,
        doneDate = nullDateStr,
        isSection = true
    )
    private val hoursBeforeArchiving = 0.01


    inner class NoteState(var prePos: Int, var postPos: Int, var isSectionChanged: Boolean, var sectionPrePos: Int, var sectionPostPos: Int)

    init {
        initData(mainListFileName)
        initData(archiveListFileName)
        noteList = getListData(mainListFileName)
        noteArchiveList = getListData(archiveListFileName)
        noteList.refreshDoneDate()
        addMainSections()
        viewModelScope.launch  {
                moveNotesToArchive()
        }
        setNoteArchive()
    }

    private fun MutableList<Note>.refreshDoneDate() {
        this.forEach {
            if (!it.isSection) {
                try {
                    SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).parse(it.doneDate)
                } catch (e: Exception) {
                    it.doneDate = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).format(Date())
                }
            } else {
                it.doneDate = nullDateStr
            }
        }
    }

    private suspend fun moveNotesToArchive() {
        withContext(Dispatchers.IO) {
            noteTempList = getMovingNoteList(noteList) { getOldNotes(it) }
            addOldNotesToArchive(noteArchiveList, noteTempList)
        }
    }

    private fun addOldNotesToArchive(archiveList: MutableList<Note>, tmpNoteList: MutableList<Note>) {
        var tmpDate = nullDateStr
        var tmpSectionPos = 0
        tmpNoteList.forEach {
            if (tmpDate != it.doneDate) {
                tmpDate = it.doneDate
                val tmpSection = Note(tmpDate, "", true,
                    isFuture = false,
                    doneDate = tmpDate,
                    isSection = true
                )
                tmpSectionPos = archiveList.indexOf(tmpSection)
                if (tmpSectionPos == -1) {
                    // Add search right tmpSectionPos value inside collection in future
                    tmpSectionPos = 0
                    archiveList.add(tmpSectionPos, tmpSection)
                }
                archiveList.add(tmpSectionPos+1, it)
            } else {
                archiveList.add(tmpSectionPos+1, it)
            }
        }
    }

    private fun getMovingNoteList(list: MutableList<Note>, getSubList: (MutableList<Note>) -> MutableList<Note>): MutableList<Note> {
        val tmpList = getSubList(list)
        //Log.e("getMovingNoteList", tmpList.size.toString())
        if (tmpList.size > 0) {
            list.removeAll(tmpList)
            tmpList.sortWith(compareByDescending<Note> { SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).parse(it.doneDate) }.thenByDescending { it.isSection })
        }
        return tmpList
    }

    private fun getOldNotes(list: MutableList<Note>): MutableList<Note> {
        //Log.e("getOldNotesBorders", "list size = ${list.size}")
        // get Done section position
        val donePos = try {
            list.indexOf(doneSection)+1
        } catch(e: NoSuchElementException) {
            list.size-1
        }
        //Log.e("getOldNotes", "from $donePos to ${list.size-1}")
        // get all notes in that section and filter for notes older 1d
        return try{
            val tmpList = list.subList(donePos, list.size).filter {
                val c = Calendar.getInstance()
                val diff = c.time.time - SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).parse(it.doneDate)!!.time
                val seconds = diff / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                //Log.e("getOldNotes", "for ${it.doneDate} filter diff = $diff min = $minutes hours = $hours days = $days")
                hours >= hoursBeforeArchiving
            }.toMutableList()
            //Log.e("getOldNotes", "tmpList size = ${tmpList.size}")
            tmpList
        } catch (e: NoSuchElementException) {
            mutableListOf()
        } catch (e: IllegalArgumentException) {
            mutableListOf()
        }
    }

    // Adding sections no main list when they lost after collapses =)
    private fun addMainSections() {
        if (noteList.getOrNull(0)?.title != "Active" && (noteList.getOrNull(0)?.isSection != true)) {
            noteList.add(0, activeSection)
        }
        if (noteList.indexOf(plannedSection) == -1) {
            // limp leg
            val oldSectionPos = noteList.indexOf(Note("Planned", "", false,
                isFuture = false,
                doneDate = nullDateStr,
                isSection = true
            ))
            if (oldSectionPos != -1) {
                noteList[oldSectionPos].isFuture = true
            } else {
                // Add plannedSection on position of first note with isFuture == true
                // or on next position after last item with isChecked == false
                val futurePos = try {
                    noteList.indexOf(noteList.first { it.isFuture })
                } catch(e: NoSuchElementException) {
                    -1
                }
                if (futurePos != -1) {
                    noteList.add(futurePos, plannedSection)
                } else {
                    try {
                        noteList.add(noteList.indexOf(noteList.last { !it.isChecked })+1, plannedSection)
                    } catch(e: NoSuchElementException) {
                        noteList.add(plannedSection)
                    }

                }
            }
        }
        if (noteList.indexOf(doneSection) == -1) {
            // Add doneSection on position of first note with isChecked == true
            // or on last position
            val donePos = try {
                noteList.indexOf(noteList.first { it.isChecked })
            } catch(e: NoSuchElementException) {
                -1
            }
            if (donePos != -1) {
                noteList.add(donePos, doneSection)
            } else {
                noteList.add(doneSection)
            }
        }
    }

    fun addArchiveSectionsAndSortList() {
        noteArchiveList.sortWith(compareByDescending<Note> { SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).parse(it.doneDate)}.thenByDescending {it.isSection})
        var sectionDate: String = nullDateStr
        noteArchiveList.forEachIndexed { index, note ->
            // First item in section after sort must be the section
            if (sectionDate != note.doneDate) {
                sectionDate = note.doneDate
                if (!note.isSection) {
                    noteArchiveList.add(index, Note(sectionDate, "", true,
                        isFuture = false,
                        doneDate = sectionDate,
                        isSection = true
                    ))
                }
            }
        }
    }

    private fun refreshSection(to: Int): Note? {
        //Log.e("sectionPostPos", "${noteState.sectionPostPos}")
        //Log.e("to", "$to")
        return  try {
            noteList.filterIndexed { index, note -> note.isSection && index < to }.last()
        } catch (e: NoSuchElementException) {
            null
        }
    }

    private fun setNoteArchive() {
        noteArchive.value = noteArchiveList
    }

    private fun setIsChecked(note: Note, isChecked: Boolean) {
        note.isChecked = isChecked
        val c = Calendar.getInstance()
        //c.add(Calendar.DATE, -1)
        note.doneDate = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).format(c.time)
        Log.e("doneDate", note.doneDate)
    }

    private fun refreshSections() {
        if (preSection != null) {
            noteState.sectionPrePos = noteList.indexOf(preSection)
        } else {
            noteState.sectionPrePos = -1
        }
        if (postSection != null) {
            noteState.sectionPostPos = noteList.indexOf(postSection)
        } else {
            noteState.sectionPostPos = -1
        }

        // for moving notes we change noteList[pos].isChecked if note changing sections from/to "Done"
        if (preSection != null && postSection != null &&
                preSection!!.title != postSection!!.title) {
            setIsChecked(noteList[noteState.postPos], postSection!!.title == "Done")
            noteList[noteState.postPos].isFuture = postSection!!.title == "Planned"
            noteState.isSectionChanged = true
        }

        doneSectionPos = noteList.indexOf(noteList.first { note -> note.title == "Done" })

        //noteArchive.value = readArchive()
        setNoteArchive()

    }

    private fun refreshNoteState(from: Int, to: Int, isSectionChanged: Boolean, func:() -> Unit) {
        preSection = refreshSection(from)
        func()
        postSection = refreshSection(to)
        noteState.prePos = from
        noteState.postPos = to
        noteState.isSectionChanged = isSectionChanged
        refreshSections()
        setNoteArchive()
        notePosChange.value = noteState
    }

    fun moveNote(from: Int, to: Int) {
        refreshNoteState(from, to, false) {
            Collections.swap(noteList, from, to)
        }
    }

    fun deleteNote(pos: Int, list: MutableList<Note>) {
        refreshNoteState(pos, -1, false) {
            deleteNotePos = pos
            deleteNote = list.removeAt(deleteNotePos)
        }
    }

    fun undoNote(list: MutableList<Note>) {
        if (deleteNotePos != -1) {
            refreshNoteState(-1, deleteNotePos, false) {
                list.add(deleteNotePos, deleteNote)
            }
            deleteNotePos = -1
        }
    }

    fun addNote(pos: Int, note: Note) {
        // adding new item
        if (pos == -1) {
            // first section "active" with index = 0 thus for adding new items - index = 1
            refreshNoteState(-1, 1, false) {
                noteList.add(1, note)
            }
        } else {
            refreshNoteState(pos, pos, false) {
                noteList[pos] = note
            }
        }
    }

    fun toggleCheckNote(pos: Int) {
        if (!noteList[pos].isChecked) {
            refreshNoteState(pos, noteList.size - 1, true) {
                val note = noteList.removeAt(pos)
                setIsChecked(note, !note.isChecked)
                noteList.add(note)
            }
        } else {
            refreshNoteState(pos, 1, true) {
                val note = noteList.removeAt(pos)
                setIsChecked(note, !note.isChecked)
                noteList.add(1, note)
            }
        }
    }

    // Methods for noteList persistence
    private fun getListData(fileName: String): MutableList<Note> {
        val text = readFromFile(fileName)
        //val text = getApplication<Application>().assets.open("Notes.json").bufferedReader().readText()

        val gson = GsonBuilder().create()
        return gson.fromJson(text, Array<Note>::class.java).toMutableList()
    }

    private fun readDataFromExtStorage(fileName: String): String {
        return  File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName).readText()
    }

    private fun initData(fileName: String) {
        val text = readDataFromExtStorage(fileName)
        val file = File(getApplication<Application>().filesDir, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText(text)
    }

    fun writeToAssets() {
        // don't change notes list here
        /*
        viewModelScope.launch {
            moveNotesToArchive()
        }
        */
        viewModelScope.launch {
            noteDao.deleteAll()
            noteDao.insertAll(noteListToNoteList(noteList))
        }

        writeToAsset(noteList, mainListFileName)
        writeToAsset(noteArchiveList, archiveListFileName)
    }

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

    private fun readFromFile(fileName: String): String {
        val file = File(getApplication<Application>().filesDir, fileName)
        val v: String = if (file.exists()) {
            file.readText()
        } else {
            "[]"
        }
        return v
    }

    /*
    private fun readArchive(): MutableList<Note> {
        doneSectionPos = noteList.indexOf(noteList.first { note -> note.title == "Done" })
        Log.e("Last", noteList.subList(doneSectionPos, noteList.size).last().desc)
        return noteList.subList(doneSectionPos, noteList.size).toMutableList()
    }
     */

    private fun noteToNote(note: com.example.test.Note, pos: Int, section: String) : com.example.test.data.Note {
        noteList.map {  }
        return com.example.test.data.Note (
            title = note.title,
            desc = note.desc,
            doneDate = note.doneDate,
            isChecked = note.isChecked,
            isFuture = note.isFuture,
            isSection = note.isSection,
            pos = pos,
            section = section
        )
    }

    private fun noteListToNoteList(list: MutableList<Note>) : MutableList<com.example.test.data.Note> {
        val plannedSectionPos = list.indexOf(plannedSection)
        val doneSectionPos = list.indexOf(doneSection)
        return list.mapIndexed { index, note ->
            val section = when(index) {
                in 0 until plannedSectionPos -> "ACTIVE"
                in plannedSectionPos until doneSectionPos -> "PLANNED"
                else -> "DONE"
            }
            noteToNote(note, index, section)
        }.toMutableList()
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