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
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.test.data.NoteDao
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs


class NoteViewModel(private val noteDao: NoteDao, application: Application) : AndroidViewModel(application) {

    val allNotes: LiveData<List<com.example.test.data.Note>> = noteDao.getNotes().asLiveData()
    val allNotesForView: MutableLiveData<List<com.example.test.data.Note>> = MutableLiveData()
    private var noteListTmp = mutableListOf<com.example.test.data.Note>()

    lateinit var noteArchiveList: MutableList<Note>
    private val mainListFileName = "QueueFile.txt"
    private val archiveListFileName = "QueueArchiveFile.txt"

    var notePosChange: MutableLiveData<NoteState> = MutableLiveData()
    var noteArchive: MutableLiveData<MutableList<Note>> = MutableLiveData()

    private var deleteNotePos: Int = -1
    private lateinit var deleteNote: com.example.test.data.Note
    private val noteState = NoteState(-1,-1, false, 0, 0)
    private var preSection: Note? = null
    private var postSection: Note? = null
    private var doneSectionPos: Int = 0
    private val nullDateStr = "01.01.1900"
    private val hoursBeforeArchiving = 0.01
    private var preFrom = 0
    private var preTo = 0

    inner class NoteState(var prePos: Int, var postPos: Int, var isSectionChanged: Boolean, var sectionPrePos: Int, var sectionPostPos: Int)

    init {
        initLists()
    }
    fun initLists() {
        noteArchiveList = mutableListOf()
        setNoteArchive()
    }

    fun setNotesFromDB() {
        allNotesForView.value = allNotes.value
    }

    private fun updateNotesPositions(tmpList: MutableList<com.example.test.data.Note>): MutableList<com.example.test.data.Note> {
        tmpList.forEachIndexed{
            index, note -> note.pos = index
        }
        return tmpList
    }

    /*
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
    */

    /*
    private suspend fun moveNotesToArchive() {
        withContext(Dispatchers.IO) {
            noteTempList = getMovingNoteList(noteList) { getOldNotes(it) }
            addOldNotesToArchive(noteArchiveList, noteTempList)
        }
    }
    */
    /*
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

     */
    /*
    private fun getMovingNoteList(list: MutableList<Note>, getSubList: (MutableList<Note>) -> MutableList<Note>): MutableList<Note> {
        val tmpList = getSubList(list)
        //Log.e("getMovingNoteList", tmpList.size.toString())
        if (tmpList.size > 0) {
            list.removeAll(tmpList)
            tmpList.sortWith(compareByDescending<Note> { SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).parse(it.doneDate) }.thenByDescending { it.isSection })
        }
        return tmpList
    }

     */
    /*
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

     */
    /*
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

     */

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

    private fun getSectionFromPosition(noteList: List<com.example.test.data.Note>, to: Int): String? {
        return  try {
            noteList.filterIndexed { index, note -> note.isSection && index <= to }.last().section
        } catch (e: NoSuchElementException) {
            null
        }
    }

    private fun setNoteSectionOnNewPos(noteList: MutableList<com.example.test.data.Note>, to: Int) {
        // Определяем на какой позиции будут записи секций и откуда соответственно искать первую секцию
        noteList[to].section = (getSectionFromPosition(noteList, to)) ?: "Active"
    }

    private fun setNoteArchive() {
        noteArchive.value = noteArchiveList
    }


    private fun setIsChecked(note: com.example.test.data.Note, isChecked: Boolean) {
        note.isChecked = isChecked
        val c = Calendar.getInstance()
        note.doneDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).format(c.time)
        Log.e("doneDate", note.doneDate)
    }

    /*
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

     */

    /*
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

     */


    fun moveNote(from: Int, to: Int) {
        // Защита от идиотизма (
        // 1. когда с UI приходит команда два раза подряд с одним и тем же from и to
        // 2. когда с UI приходит команда перескочить через один элемент (from-to > 1/-1) из-за того что не успела view обновиться из livedata
        if ((preFrom != from || preTo != to) && from != to && abs(from-to) == 1) {
            noteListTmp = allNotesForView.value!!.toMutableList()
            // Swapping note to new place. Old logic had refreshNoteState exec here
            Collections.swap(noteListTmp, from, to)
            allNotesForView.value = noteListTmp
            setNoteSectionOnNewPos(noteListTmp, to)
            preFrom = from
            preTo = to
            Log.d("MOVE NOTE", "from = $from; to = $to")
        }
    }

    fun moveNoteToDb() {
        viewModelScope.launch {
            noteListTmp = updateNotesPositions(noteListTmp)
            noteDao.insertAll(noteListTmp)
        }
    }

    fun deleteNote(pos: Int) {
        allNotes.value?.get(pos)?.let {
            deleteNotePos = pos
            deleteNote = it
            viewModelScope.launch {
                noteDao.delete(it)
                noteDao.compressPositionsAfterDelete(pos)
            }
        }
    }

    fun undoNote() {
        if (deleteNotePos != -1) {
            viewModelScope.launch {
                noteDao.stretchPositionsAfterUndoDelete(deleteNotePos)
                noteDao.insert(deleteNote)
            }
            deleteNotePos = -1
        }
    }

    fun addNote(note: com.example.test.data.Note) {
        // adding new item
        viewModelScope.launch {
            noteDao.insert(note)
        }
    }

    fun toggleCheckNote(pos: Int) {
        val note = allNotes.value?.get(pos)!!
        val c = Calendar.getInstance()
        val newNote = com.example.test.data.Note (
            note.id,
            note.title,
            note.desc,
            !note.isChecked,
            note.isFuture,
            SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).format(c.time),
            note.isSection,
            note.pos,
            note.section
        )
        viewModelScope.launch {
            noteDao.update(newNote)
        }
    }

    fun getNote(id: Int): LiveData<com.example.test.data.Note> {
        return noteDao.getNote(id).asLiveData()
    }

    fun isNoteSection(position: Int): Boolean {
        return allNotes.value?.get(position)?.isSection ?: false
    }

    // Methods for noteList persistence
    /*
    private fun getListData(fileName: String): MutableList<Note> {
        var notesFromDb = allNotes.value?.map {
            Note(it.title, it.desc, it.isChecked, it.isFuture, it.doneDate, it.isSection)
        }?.toMutableList()

        // Just workaround for error in db (too many sections)
        /*if (notesFromDb?.size ?: 0 > 12) {
            notesFromDb = null
        }*/

        return notesFromDb ?: run {
            val text = readFromFile(fileName)

            val gson = GsonBuilder().create()
            gson.fromJson(text, Array<Note>::class.java).toMutableList()
        }
    }

     */
    /*
    private fun readDataFromExtStorage(fileName: String): String {
        return  File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName).readText()
    }

     */
    /*
    private fun initData(fileName: String) {
        val text = readDataFromExtStorage(fileName)
        val file = File(getApplication<Application>().filesDir, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText(text)
    }
     */

    fun writeToAssets() {
        // don't change notes list here
        viewModelScope.launch {
            allNotes.value?.let { noteDao.insertAll(it) }
        }
        //writeToAsset(noteList, mainListFileName)
        //writeToAsset(noteArchiveList, archiveListFileName)
    }

    fun writeHistToAssets() {
        //writeToAsset(noteArchiveList, archiveListFileName)
    }
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

    /*
    private fun readArchive(): MutableList<Note> {
        doneSectionPos = noteList.indexOf(noteList.first { note -> note.title == "Done" })
        Log.e("Last", noteList.subList(doneSectionPos, noteList.size).last().desc)
        return noteList.subList(doneSectionPos, noteList.size).toMutableList()
    }
     */
    /*
    private fun noteToNote(note: com.example.test.Note, pos: Int, section: String) : com.example.test.data.Note {
        //noteList.map {  }
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

     */
    /*
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

     */
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