package com.arvind.looksea.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class Post(
    @get:PropertyName("creation_time_ms") @set:PropertyName("creation_time_ms") var creationTimeMs: Long = 0,
    var description: String = "",
    var type: String = "",
    var likes: Int = 0,
    @get:PropertyName("file_url") @set:PropertyName("file_url") var fileUrl: String = "",
    var location: GeoPoint = GeoPoint(0.0, 0.0),
    @get:PropertyName("user") @set:PropertyName("user") var userId: String? = "",
    var username: String? = ""
)