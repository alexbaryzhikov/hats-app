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
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.item_message.view.*

private const val TAG = "ChatActivity"

class ChatActivity : AppCompatActivity() {

    private lateinit var chatId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatId = intent?.extras?.getString("chatId") ?: ""
        if (chatId.isEmpty()) {
            Log.e(TAG, "onCreate: Error -- chatId is empty")
            finish()
        }

        vSend.setOnClickListener { sendMessage() }

        initChat()
    }

    private fun sendMessage() {
        val text = vInput.text.toString()
        if (text.isEmpty()) return
        val uid = auth.uid ?: return
        val messageDb = db.reference.child("chat").child(chatId).push()
        val messageMap = mutableMapOf<String, Any>("author" to uid, "text" to text)
        messageDb.updateChildren(messageMap)

        vInput.text.clear()
    }

    /** Initializes Chat View and [MessageAdapter]. */
    private fun initChat() {
        with(vMessages) {
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            adapter = MessageAdapter()
        }
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
        holder.vSenderId.text = messages[position].senderId
    }

    override fun getItemCount(): Int = messages.size
}

private class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val vText: TextView = itemView.vText
    val vSenderId: TextView = itemView.vSenderId
}

