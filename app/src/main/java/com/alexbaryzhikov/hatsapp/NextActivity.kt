package com.alexbaryzhikov.hatsapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_next.*

private const val TAG = "NextActivity"

class NextActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)

        vLogout.setOnClickListener {
            Log.d(TAG, "Signing out...")
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
            finish()
        }
    }
}
