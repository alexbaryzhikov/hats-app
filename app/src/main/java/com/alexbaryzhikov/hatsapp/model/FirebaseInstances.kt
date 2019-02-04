package com.alexbaryzhikov.hatsapp.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase

val auth: FirebaseAuth = FirebaseAuth.getInstance()
val phoneAuth: PhoneAuthProvider = PhoneAuthProvider.getInstance()
val db: FirebaseDatabase = FirebaseDatabase.getInstance()
