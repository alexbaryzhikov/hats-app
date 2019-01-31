package com.alexbaryzhikov.hatsapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val phoneAuth = PhoneAuthProvider.getInstance()
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted: credential = [$credential]")

            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w(TAG, "onVerificationFailed", e)

            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                // ...
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
                // ...
            }

            // Show a message and update the UI
            // ...
        }

        override fun onCodeSent(
                verificationId: String?,
                token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent: verificationId = [$verificationId]")

            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId!!
            resendToken = token

            vSend.text = getString(R.string.verify_code)

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        auth.setLanguageCode("en") // language of auth operations
        userIsLoggedIn() // if user is logged in go straight to NextActivity

        vSend.setOnClickListener {
            if (storedVerificationId == null) {
                startVerification()
            } else {
                verifyCode()
            }
        }
    }

    private fun startVerification() {
        phoneAuth.verifyPhoneNumber(
                vPhoneNumber.text.toString(), // phone number to verify
                60, // timeout duration
                TimeUnit.SECONDS, // unit of timeout
                this, // activity (for callback binding)
                callbacks) // OnVerificationStateChangedCallbacks
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        Log.d(TAG, "signInWithPhoneAuthCredential called with: credential = [$credential]")
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "signInWithCredential: success")
                userIsLoggedIn()
            } else {
                // Sign in failed, display a message and update the UI
                Log.w(TAG, "signInWithCredential: failure", task.exception)
                if (task.exception is FirebaseAuthInvalidCredentialsException) {
                    // The verification code entered was invalid
                }
            }
        }
    }

    private fun userIsLoggedIn() {
        val user = auth.currentUser
        if (user != null) {
            startActivity(Intent(applicationContext, NextActivity::class.java))
            finish()
            return
        }
    }

    private fun verifyCode() {
        val verificationId: String = storedVerificationId ?: return
        val code = vCode.text.toString()
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

}
