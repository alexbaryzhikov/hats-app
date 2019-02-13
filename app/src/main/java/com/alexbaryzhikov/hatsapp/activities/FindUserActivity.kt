package com.alexbaryzhikov.hatsapp.activities

import android.content.Context
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexbaryzhikov.hatsapp.R
import com.alexbaryzhikov.hatsapp.model.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_find_user.*
import kotlinx.android.synthetic.main.item_user.view.*
import android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI as PHONE_CONTENT_URI
import android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME as PHONE_DISPLAY_NAME
import android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER as PHONE_NUMBER

class FindUserActivity : AppCompatActivity() {

    private val userAdapter = UserAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_user)

        initUsers()
    }

    /** Initializes Users View and [UserAdapter]. */
    private fun initUsers() {
        with(vUsers) {
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            adapter = userAdapter
        }
        fillUsers()
    }

    /**
     * Fills [UserAdapter] with users.
     *
     * First it gets contacts from the device, then checks each contact if it's present id DB, and if positive --
     * adds it to the adapter list.
     */
    private fun fillUsers() {
        // Country phone prefix for numbers without one.
        // They are assumed to have prefix equivalent to MCC of the current provider.
        val phonePrefix: String = with(getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager) {
            networkCountryIso?.toPhonePrefix() ?: ""
        }
        contentResolver.getContacts().map { it.normalizePhone(phonePrefix) }.forEach(::getUserDetails)
    }

    /** Queries DB for users equivalent to [contact] and adds them to the [UserAdapter]. */
    private fun getUserDetails(contact: User) {
        val userDb = db.reference.child("user")
        val query = userDb.orderByChild("phone").equalTo(contact.phone)

        query.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                for (user in snapshot.children) {
                    val key = user.key ?: continue
                    var name = user.child("name").value?.toString() ?: ""
                    val notificationKey = user.child("notificationKey").value?.toString() ?: ""
                    val phone = user.child("phone").value?.toString() ?: continue
                    // Change name to contact name if user hasn't customized it
                    if (name.isEmpty() || name == phone) name = contact.name
                    userAdapter.users += User(key, name, notificationKey, phone)
                }
                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(e: DatabaseError) {}
        })
    }
}

private class UserAdapter(val users: MutableList<User> = mutableListOf()) : RecyclerView.Adapter<UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false).apply {
            // Set layout params explicitly to ensure the item view has correct size.
            layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.vName.text = users[position].name
        holder.vPhone.text = users[position].phone
        holder.vUser.setOnClickListener { createChat(position) }
    }

    override fun getItemCount(): Int = users.size

    /** Create chat with unique id and add reference to it to both users. */
    private fun createChat(position: Int) {
        val uid = auth.uid ?: return
        val key = db.reference.child("chat").push().key ?: return

        val chatInfo = mapOf("users/$uid" to true, "users/${users[position].id}" to true)
        val chatInfoDb = db.reference.child("chat").child(key).child("info")
        chatInfoDb.updateChildren(chatInfo)

        val userDb = db.reference.child("user")
        userDb.child(uid).child("chat").child(key).setValue(true)
        userDb.child(users[position].id).child("chat").child(key).setValue(true)
    }
}

private class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val vUser: ViewGroup = itemView.vUser
    val vName: TextView = itemView.vName
    val vPhone: TextView = itemView.vPhone
}
