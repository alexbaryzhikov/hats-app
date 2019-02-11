package com.alexbaryzhikov.hatsapp.model

import android.content.ContentResolver
import android.provider.ContactsContract
import android.util.Log
import com.onesignal.OneSignal
import org.json.JSONException
import org.json.JSONObject

/** Creates a list of [User] from phone contacts. */
fun ContentResolver.getContacts(): List<User> = query(
    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null
)?.use {
    with(it) {
        List(count) {
            moveToNext()
            User(
                "",
                getString(getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)),
                getString(getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            )
        }
    }
} ?: listOf()

/** Converts country code to phone prefix. */
fun String.toPhonePrefix(): String = countryCodes[this] ?: ""

/**
 * Normalizes phone number of a [User].
 * [defaultPrefix] is required for phone numbers that have no country prefix.
 */
fun User.normalizePhone(defaultPrefix: String): User {
    val normalized = phone.filterNot { it in charArrayOf(' ', '-', '(', ')') }
        .let { if (it[0] != '+') defaultPrefix + it else it }
    return User(uid, name, normalized)
}

/** Send notification via OneSignal. */
fun String.sendNotification(heading: String, notificationKey: String) {
    try {
        val content =
            JSONObject("{'contents':{'en':'$this'}, 'include_player_ids':['$notificationKey'],'headings':{'en':'$heading'}}")
        OneSignal.postNotification(content, null)
    } catch (e: JSONException) {
        Log.e("sendNotification", "Error", e)
    }
}
