package com.alexbaryzhikov.hatsapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_hats.*

private const val TAG = "HatsActivity"

class HatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hats)

        vFindUser.setOnClickListener {
            startActivity(Intent(applicationContext, FindUserActivity::class.java))
        }

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
