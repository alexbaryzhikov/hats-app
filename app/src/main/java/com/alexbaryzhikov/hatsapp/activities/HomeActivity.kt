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
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.item_chat.view.*

private const val TAG = "HomeActivity"

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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
        initChats()
        fillChats()
    }

    /** Initializes Chats View and [ChatAdapter]. */
    private fun initChats() {
        with(vChats) {
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            adapter = ChatAdapter()
        }
    }

    /** Fills [ChatAdapter] with chats. */
    private fun fillChats() {
        val uid = auth.uid ?: return
        val adapter = vChats.adapter as? ChatAdapter ?: return
        val chatDb = db.reference.child("user").child(uid).child("chat")

        chatDb.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(data: DataSnapshot) {
                if (!data.exists()) return
                adapter.chats.clear()
                for (chat in data.children) {
                    val key = chat.key ?: continue
                    adapter.chats += Chat(key)
                }
                adapter.notifyDataSetChanged()
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
            val bundle = Bundle().apply { putString("chatId", chats[position].id) }
            val intent = Intent(it.context, ChatActivity::class.java).apply { putExtras(bundle) }
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = chats.size
}

private class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val vChat: ViewGroup = itemView.vChat
    val vTitle: TextView = itemView.vTitle
}

