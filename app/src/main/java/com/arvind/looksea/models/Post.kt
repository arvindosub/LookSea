package com.arvind.looksea.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class Post(
    var description: String = "",
    @get:PropertyName("file_url") @set:PropertyName("file_url") var fileUrl: String = "",
    @get:PropertyName("creation_time_ms") @set:PropertyName("creation_time_ms") var creationTimeMs: Long = 0,
    var location: GeoPoint = GeoPoint(0.0, 0.0),
    var user: User? = null
)