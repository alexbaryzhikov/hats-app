package com.alexbaryzhikov.hatsapp.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexbaryzhikov.hatsapp.R
import com.alexbaryzhikov.hatsapp.model.*
import com.bumptech.glide.Glide
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.item_image.view.*
import kotlinx.android.synthetic.main.item_message.view.*
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "ChatActivity"
private const val GET_IMAGES_CODE = 1

class ChatActivity : AppCompatActivity() {

    private lateinit var chat: Chat
    private val messageAdapter = MessageAdapter()
    private val imageUriAdapter = ImageUriAdapter(this)
    private val users = mutableSetOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chat = intent.getSerializableExtra("chat") as Chat

        initChat()

        vSend.setOnClickListener { sendMessage() }
        vAddImage.setOnClickListener { openGallery() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        if (requestCode == GET_IMAGES_CODE) {
            val clipData = data?.clipData
            val singleData = data?.data
            Log.d(TAG, "onActivityResult: singleData=$singleData")
            Log.d(TAG, "onActivityResult: clipData=$clipData")
            imageUriAdapter.imageUris.clear()
            if (clipData != null) {
                repeat(clipData.itemCount) { imageUriAdapter.imageUris += clipData.getItemAt(it).uri.toString() }
            } else if (singleData != null) {
                imageUriAdapter.imageUris += singleData.toString()
            }
            imageUriAdapter.notifyDataSetChanged()
            vImages.visibility = View.VISIBLE
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
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.HORIZONTAL, false)
            adapter = imageUriAdapter
        }
        getUsers(chat.userIds)
        getMessages()
    }

    /** Replenishes [users] set with all users from [userIds] list. */
    private fun getUsers(userIds: List<String>) {
        val userDb = db.reference.child("user")
        for (userId in userIds) {
            userDb.child(userId).addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return
                    val name = snapshot.child("name").value?.toString() ?: return
                    val notificationKey = snapshot.child("notificationKey").value?.toString() ?: return
                    val phone = snapshot.child("phone").value?.toString() ?: return
                    users += User(userId, name, notificationKey, phone)
                }

                override fun onCancelled(e: DatabaseError) {}
            })
        }
    }

    private fun getMessages() {
        val messagesDb = db.reference.child("chat").child(chat.id).child("messages")
        messagesDb.addChildEventListener(object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, prev: String?) {
                if (!snapshot.exists()) return
                val key = snapshot.key ?: return
                val authorId = snapshot.child("authorId").value?.toString() ?: ""
                val text = snapshot.child("text").value?.toString() ?: ""
                val imageUrls: List<String> = snapshot.child("images").run {
                    if (exists()) children.map { it.value.toString() } else listOf()
                }

                messageAdapter.messages += Message(key, authorId, text, imageUrls)
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
        val uid = auth.uid ?: return
        val text = vInput.text.toString()
        val imageUris = imageUriAdapter.imageUris
        if (text.isEmpty() && imageUris.isEmpty()) return

        // Create a message record and fill the content fields
        val messagesDb = db.reference.child("chat").child(chat.id).child("messages")
        val messageId = messagesDb.push().key ?: return
        val messageDb = messagesDb.child(messageId)
        val messageMap = mutableMapOf<String, Any>("authorId" to uid)
        if (!text.isEmpty()) messageMap += "text" to text

        // Upload images if any and update DB with message record
        val uploaded = AtomicInteger(0)
        when {
            !imageUris.isEmpty() -> imageUris.forEach {
                val imageId = messageDb.child("images").push().key ?: return@forEach
                val filePath = storage.reference.child("chat").child(chat.id).child(messageId).child(imageId)
                val uploadTask = filePath.putFile(Uri.parse(it))
                uploadTask.addOnSuccessListener {
                    filePath.downloadUrl.addOnSuccessListener { uri ->
                        messageMap += "/images/$imageId/" to uri.toString()
                        Log.d(TAG, "sendMessage: Image uploaded (${uploaded.get() + 1}/${imageUris.size}): $imageId")
                        if (uploaded.incrementAndGet() == imageUris.size) messageDb.updateChildren(messageMap)
                    }
                }
            }
            !text.isEmpty() -> messageDb.updateChildren(messageMap)
        }

        // Send notifications
        val msg = if (!text.isEmpty()) text else "[Pictures...]"
        users.filter { it.id != uid }.forEach { msg.sendNotification("New message", it.notificationKey) }

        // Clear views
        vInput.text.clear()
        vImages.visibility = View.GONE
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
        val message = messages[position]
        holder.vText.text = message.text
        holder.vSenderId.text = message.authorId
        if (message.imageUrls.isNotEmpty()) with(holder.vViewImages) {
            setOnClickListener {
                StfalconImageViewer.Builder<String>(it.context, message.imageUrls) { view, imageUrl ->
                    Glide.with(it.context).load(Uri.parse(imageUrl)).into(view)
                }.show()
            }
            visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = messages.size
}

private class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val vText: TextView = itemView.vText
    val vSenderId: TextView = itemView.vSenderId
    val vViewImages: Button = itemView.vViewImages
}

private class ImageUriAdapter(val context: Context, val imageUris: MutableList<String> = mutableListOf()) :
    RecyclerView.Adapter<UriViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UriViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false)
        return UriViewHolder(view)
    }

    override fun onBindViewHolder(holder: UriViewHolder, position: Int) {
        Glide.with(context).load(Uri.parse(imageUris[position])).into(holder.vImage)
    }

    override fun getItemCount(): Int = imageUris.size
}

private class UriViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val vImage: ImageView = itemView.vImage
}

