package com.alexbaryzhikov.hatsapp.activities

import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.WRITE_CONTACTS
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.alexbaryzhikov.hatsapp.R
import com.alexbaryzhikov.hatsapp.model.auth
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
            auth.signOut()
            startActivity(Intent(applicationContext, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
            finish()
        }

        getPermissions()
    }

    private fun getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(READ_CONTACTS, WRITE_CONTACTS), 1)
        }
    }
}
