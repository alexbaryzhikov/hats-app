package com.alexbaryzhikov.hatsapp.model

import java.io.Serializable

data class User(val id: String, val name: String, val notificationKey: String, val phone: String)

data class Chat(val id: String, val userIds: List<String>) : Serializable

data class Message(val id: String, val authorId: String, val text: String, val imageUrls: List<String>)
