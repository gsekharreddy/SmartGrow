package com.example.smartgrow

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/** Make all buttons, cards, and images clicky */
fun View.makeEverythingInteractive(context: Context) {
    when (this) {
        is MaterialButton -> setOnClickListener {
            val btnText = text.toString().takeIf { it.isNotEmpty() } ?: "Icon Button"
            Toast.makeText(context, "$btnText clicked!", Toast.LENGTH_SHORT).show()
        }
        is MaterialCardView -> setOnClickListener {
            Toast.makeText(context, "Card expanded!", Toast.LENGTH_SHORT).show()
        }
        is ImageView -> setOnClickListener {
            Toast.makeText(context, "Image action triggered!", Toast.LENGTH_SHORT).show()
        }
        is ViewGroup -> {
            for (i in 0 until childCount) {
                getChildAt(i).makeEverythingInteractive(context)
            }
        }
    }
}

/** * Finds a specific TextView in the layout by its starting text so we can cache it
 * and update it live without needing an android:id tag.
 */
fun View.findTextViewByExactText(targetText: String): TextView? {
    if (this is TextView && this.text.toString() == targetText) return this
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val found = getChildAt(i).findTextViewByExactText(targetText)
            if (found != null) return found
        }
    }
    return null
}

/** * Recursively search and replace placeholder text with live telemetry data.
 * (Restored so your older fragments don't crash! 💀)
 */
fun View.replaceText(oldText: String, newText: String) {
    if (this is TextView && this.text.toString().contains(oldText)) {
        this.text = this.text.toString().replace(oldText, newText)
    } else if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).replaceText(oldText, newText)
        }
    }
}