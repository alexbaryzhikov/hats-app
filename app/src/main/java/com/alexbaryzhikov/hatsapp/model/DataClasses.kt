package com.alexbaryzhikov.hatsapp.model

data class User(val uid: String, val name: String, val phone: String)

data class Chat(val id: String)

data class Message(val messageId: String, val authorId: String, val text: String, val imageUrls: List<String>)