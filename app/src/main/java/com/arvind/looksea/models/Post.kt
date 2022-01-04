package com.arvind.looksea.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class Post(
    var filename: String = "",
    var description: String = "",
    var type: String = "",
    @get:PropertyName("file_url") @set:PropertyName("file_url") var fileUrl: String = "",
    @get:PropertyName("creation_time_ms") @set:PropertyName("creation_time_ms") var creationTimeMs: Long = 0,
    var location: GeoPoint = GeoPoint(0.0, 0.0),
    @get:PropertyName("user") @set:PropertyName("user") var userId: String? = "",
    var username: String? = ""
)