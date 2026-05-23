package dev.fordewe.fohudmon.internal

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import dev.fordewe.fohudmon.DevHud

internal class HudInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context?.applicationContext?.let { DevHud.install(it as Application) }
        return true
    }

    override fun query(uri: Uri, p: Array<String>?, s: String?, sA: Array<String>?, so: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, sA: Array<String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, sA: Array<String>?): Int = 0
}
