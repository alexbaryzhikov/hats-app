package com.alexbaryzhikov.hatsapp.activities

import android.app.Activity
import android.content.Intent
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
import kotlinx.android.synthetic.main.item_image.view.*
import kotlinx.android.synthetic.main.item_message.view.*

private const val TAG = "ChatActivity"
private const val GET_IMAGES_CODE = 1

class ChatActivity : AppCompatActivity() {

    private lateinit var chatId: String
    private lateinit var chatDb: DatabaseReference
    private val messageAdapter = MessageAdapter()
    private val imageUriAdapter = ImageUriAdapter()

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
        vAddImage.setOnClickListener { openGallery() }

        initChat()
        getMessages()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        if (requestCode == GET_IMAGES_CODE) {
            val clipData = data?.clipData
            val singleData = data?.data
            imageUriAdapter.imageUris.clear()
            if (clipData != null) {
                repeat(clipData.itemCount) { imageUriAdapter.imageUris += clipData.getItemAt(it).uri.toString() }
            } else if (singleData != null) {
                imageUriAdapter.imageUris += singleData.toString()
            }
        }
    }

    /** Initializes Chat View and [MessageAdapter]. */
    private fun initChat() {
        with(vMessages) {
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            adapter = messageAdapter
        }
        with(vImages) {
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            adapter = imageUriAdapter
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

    /** Sends implicit intent to browse and select images and get the result in [onActivityResult]. */
    private fun openGallery() {
        val intent = Intent().apply {
            action = Intent.ACTION_GET_CONTENT
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(Intent.createChooser(intent, "Select image(s)"), GET_IMAGES_CODE)
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

private class ImageUriAdapter(val imageUris: MutableList<String> = mutableListOf()) :
    RecyclerView.Adapter<UriViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UriViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return UriViewHolder(view)
    }

    override fun onBindViewHolder(holder: UriViewHolder, position: Int) {
    }

    override fun getItemCount(): Int = imageUris.size
}

private class UriViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val vImage = itemView.vImage
}

