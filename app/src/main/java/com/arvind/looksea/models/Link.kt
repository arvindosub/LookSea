package com.arvind.looksea.models

data class Link(
    var start: String = "",
    var category: String = "",
    var end: String = "",
    var name: String = "",
    var value: String = "",
    var startOwner: String = "",
    var endOwner: String = "",
    var linkOwner: String = "",
    var keywords: ArrayList<String> = arrayListOf<String>()
)