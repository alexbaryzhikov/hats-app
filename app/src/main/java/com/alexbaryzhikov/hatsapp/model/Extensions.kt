package com.alexbaryzhikov.hatsapp.model

import android.content.ContentResolver
import android.provider.ContactsContract

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
