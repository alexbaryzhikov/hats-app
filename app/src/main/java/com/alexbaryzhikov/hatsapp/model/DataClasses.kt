package com.alexbaryzhikov.hatsapp.model

data class User(val uid: String, val name: String, val phone: String)

data class Chat(val id: String)

data class Message(val messageId: String, val senderId: String, val text: String)