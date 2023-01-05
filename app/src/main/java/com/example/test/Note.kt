package com.example.test

import java.util.*

data class Note(var title: String, var desc: String, var isChecked: Boolean, var isFuture: Boolean, var doneDate: String, var isSection: Boolean) {
    override fun toString(): String {
        return "title = ${title}; desc = ${desc}; isChecked = ${isChecked}; isFuture = ${isFuture}; doneDate = ${doneDate}; isSection = ${isSection};"
    }
}