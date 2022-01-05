package com.arvind.looksea

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.FileUtils
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.arvind.looksea.models.Post
import com.arvind.looksea.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import androidx.core.view.isVisible
import com.arvind.looksea.databinding.ActivitySurveyBinding
import com.arvind.looksea.models.Survey
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.firestore.GeoPoint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.nio.file.Paths

private const val TAG = "SurveyActivity"

class SurveyActivity : AppCompatActivity() {
    private var signedInUser: User? = null
    private var userId: String? = ""
    private var surv: Survey? = null
    private var qn1: String? = "How would you rate this app?"
    private var res1: Int? = 0
    private var qn2: String? = "How handsome are you?"
    private var res2: Int? = 0
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var binding: ActivitySurveyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySurveyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = FirebaseAuth.getInstance().currentUser?.uid as String

        firestoreDb = FirebaseFirestore.getInstance()
        firestoreDb.collection("users")
            .document(userId!!)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                binding.tvQn1.text = qn1.toString()
                binding.tvQn2.text = qn2.toString()

                binding.sbQn1.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (progress <= 3) {
                            binding.tvRes1.text = "${progress}: Bad"
                        } else if (progress in 4..6) {
                            binding.tvRes1.text = "${progress}: Medium"
                        } else {
                            binding.tvRes1.text = "${progress}: Good"
                        }
                        res1 = progress
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        Log.i(TAG, "tracking")
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        Log.i(TAG, "stopped tracking")
                    }
                })

                binding.sbQn2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (progress <= 3) {
                            binding.tvRes2.text = "${progress}: Bad"
                        } else if (progress in 4..6) {
                            binding.tvRes2.text = "${progress}: Medium"
                        } else {
                            binding.tvRes2.text = "${progress}: Good"
                        }
                        res2 = progress
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        Log.i(TAG, "tracking")
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        Log.i(TAG, "stopped tracking")
                    }
                })

                binding.btnSubmit.setOnClickListener {
                    handleSubmitButtonClick()
                }
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failed to fetch signed-in user", exception)
            }
    }

    private fun handleSubmitButtonClick() {
        if (binding.tvRes1.text == "Rating" || binding.tvRes2.text == "Rating") {
            Toast.makeText(this, "Some incomplete questions...", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false

        surv = Survey(
            qn1.toString(),
            res1!!,
            qn2.toString(),
            res2!!
        )
        firestoreDb.collection("surveys").add(surv!!)
            .addOnCompleteListener {
                binding.btnSubmit.isEnabled = true
                Toast.makeText(this, "Survey submitted!", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

}