package com.alexbaryzhikov.hatsapp.activities

import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.WRITE_CONTACTS
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexbaryzhikov.hatsapp.R
import com.alexbaryzhikov.hatsapp.model.Chat
import com.alexbaryzhikov.hatsapp.model.auth
import com.alexbaryzhikov.hatsapp.model.db
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.onesignal.OneSignal
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.item_chat.view.*

private const val TAG = "HomeActivity"

class HomeActivity : AppCompatActivity() {

    private val chatAdapter = ChatAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initOneSignal()
        getPermissions()
        initChats()

        vFindUser.setOnClickListener {
            startActivity(Intent(applicationContext, FindUserActivity::class.java))
        }

        vLogout.setOnClickListener {
            Log.d(TAG, "Signing out...")
            OneSignal.setSubscription(false) // unsubscribe from notifications
            auth.signOut()
            startActivity(Intent(applicationContext, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
            finish()
        }
    }

    /** OneSignal Initialization. */
    private fun initOneSignal() {
        OneSignal.startInit(applicationContext)
            .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
            .unsubscribeWhenNotificationsAreDisabled(true)
            .init()
        OneSignal.setSubscription(true) // subscribe to notifications
        OneSignal.idsAvailable { userId, registrationId ->
            val uid = auth.uid ?: return@idsAvailable
            db.reference.child("user").child(uid).child("notificationKey").setValue(userId)
            Log.d(TAG, "initOneSignal: userId=$userId, registrationId=$registrationId")
        }
    }

    /** Initializes Chats View and [ChatAdapter]. */
    private fun initChats() {
        with(vChats) {
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            adapter = chatAdapter
        }
        fillChats()
    }

    /** Fills [ChatAdapter] with chats. */
    private fun fillChats() {
        val uid = auth.uid ?: return
        val userChatDb = db.reference.child("user").child(uid).child("chat")

        userChatDb.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                chatAdapter.chats.clear()
                for (chat in snapshot.children) {
                    val key = chat.key ?: continue
                    fillChatUsers(key)
                }
            }

            override fun onCancelled(e: DatabaseError) {}
        })
    }

    /** Mutates chatAdapter.chats */
    private fun fillChatUsers(chatId: String) {
        val chatDb = db.reference.child("chat").child(chatId).child("users")
        chatDb.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val userIds = snapshot.children.map { it.key.toString() }
                chatAdapter.chats += Chat(chatId, userIds)
                chatAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(e: DatabaseError) {}
        })
    }

    private fun getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(READ_CONTACTS, WRITE_CONTACTS), 1)
        }
    }
}

private class ChatAdapter(val chats: MutableList<Chat> = mutableListOf()) : RecyclerView.Adapter<ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false).apply {
            // Set layout params explicitly to ensure the item view has correct size.
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.vTitle.text = chats[position].id
        holder.vChat.setOnClickListener {
            val intent = Intent(it.context, ChatActivity::class.java).apply { putExtra("chat", chats[position]) }
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = chats.size
}

private class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val vChat: ViewGroup = itemView.vChat
    val vTitle: TextView = itemView.vTitle
}

