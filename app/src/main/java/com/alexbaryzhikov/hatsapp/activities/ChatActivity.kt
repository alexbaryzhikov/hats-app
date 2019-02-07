package com.alexbaryzhikov.hatsapp.activities

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
import com.alexbaryzhikov.hatsapp.model.Message
import com.alexbaryzhikov.hatsapp.model.auth
import com.alexbaryzhikov.hatsapp.model.db
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.item_message.view.*

private const val TAG = "ChatActivity"

class ChatActivity : AppCompatActivity() {

    private lateinit var chatId: String
    private lateinit var chatDb: DatabaseReference
    private val messageAdapter = MessageAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatId = intent?.extras?.getString("chatId") ?: ""
        if (chatId.isEmpty()) {
            Log.e(TAG, "onCreate: Error -- chatId is empty")
            finish()
        }
        chatDb = db.reference.child("chat").child(chatId)

        vSend.setOnClickListener { sendMessage() }

        initChat()
        getMessages()
    }

    /** Initializes Chat View and [MessageAdapter]. */
    private fun initChat() {
        with(vMessages) {
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            adapter = messageAdapter
        }
    }

    private fun getMessages() {
        chatDb.addChildEventListener(object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, prev: String?) {
                if (!snapshot.exists()) return
                val key = snapshot.key ?: return
                val text = snapshot.child("text").value?.toString() ?: ""
                val authorId = snapshot.child("authorId").value?.toString() ?: ""

                messageAdapter.messages += Message(key, authorId, text)
                vMessages.layoutManager?.scrollToPosition(messageAdapter.messages.size - 1)
                messageAdapter.notifyDataSetChanged()
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {}

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

            override fun onChildRemoved(p0: DataSnapshot) {}

            override fun onCancelled(p0: DatabaseError) {}
        })
    }

    private fun sendMessage() {
        val text = vInput.text.toString()
        if (text.isEmpty()) return
        val uid = auth.uid ?: return

        // Create a message record and fill the content fields
        chatDb.push().updateChildren(mapOf<String, Any>("authorId" to uid, "text" to text))

        vInput.text.clear()
    }
}

private class MessageAdapter(val messages: MutableList<Message> = mutableListOf()) :
    RecyclerView.Adapter<MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false).apply {
            // Set layout params explicitly to ensure the item view has correct size.
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.vText.text = messages[position].text
        holder.vSenderId.text = messages[position].authorId
    }

    override fun getItemCount(): Int = messages.size
}

private class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val vText: TextView = itemView.vText
    val vSenderId: TextView = itemView.vSenderId
}

