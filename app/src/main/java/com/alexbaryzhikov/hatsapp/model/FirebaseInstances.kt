package com.alexbaryzhikov.hatsapp.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

val auth: FirebaseAuth = FirebaseAuth.getInstance()
val phoneAuth: PhoneAuthProvider = PhoneAuthProvider.getInstance()
val db: FirebaseDatabase = FirebaseDatabase.getInstance()
val storage = FirebaseStorage.getInstance()
