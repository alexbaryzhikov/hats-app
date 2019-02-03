package com.alexbaryzhikov.hatsapp

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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_find_user.*
import kotlinx.android.synthetic.main.item_user.view.*
import android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI as PHONE_CONTENT_URI
import android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME as PHONE_DISPLAY_NAME
import android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER as PHONE_NUMBER

class FindUserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_user)

        initUserList()
        fillUserList()
    }

    /** Initializes UserList View and [UserListAdapter]. */
    private fun initUserList() {
        with(vUserList) {
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            adapter = UserListAdapter()
        }
    }

    /**
     * Fills [UserListAdapter] with users.
     *
     * First it gets contacts from the device, then checks each contact if it's present id DB, and if positive --
     * adds it to the adapter list.
     */
    private fun fillUserList() {
        // Country phone prefix for numbers without one.
        // They are assumed to have prefix equivalent to MCC of the current provider.
        val phonePrefix: String = with(getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager) {
            networkCountryIso?.toPhonePrefix() ?: ""
        }
        contentResolver.getContacts().map { it.normalizePhone(phonePrefix) }.forEach(::getUserDetails)
    }

    /** Queries DB for users equivalent to [contact] and adds them to the [UserListAdapter]. */
    private fun getUserDetails(contact: User) {
        val adapter = vUserList.adapter as? UserListAdapter ?: return
        val userDb = FirebaseDatabase.getInstance().reference.child("user")
        val query = userDb.orderByChild("phone").equalTo(contact.phone)
        query.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(data: DataSnapshot) {
                if (!data.exists()) return
                var phone = ""
                var name = ""
                for (child in data.children) {
                    phone = child.child("phone").value?.toString() ?: phone
                    name = child.child("name").value?.toString() ?: name

                    adapter.users += User(name, phone)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(e: DatabaseError) {}
        })
    }
}

private class UserListAdapter(val users: MutableList<User> = mutableListOf()) : RecyclerView.Adapter<UserViewHolder>() {

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
    }

    override fun getItemCount(): Int = users.size
}

private class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val vName: TextView = itemView.vName
    val vPhone: TextView = itemView.vPhone
}
