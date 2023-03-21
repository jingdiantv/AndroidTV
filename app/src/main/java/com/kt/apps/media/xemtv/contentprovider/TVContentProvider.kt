package com.kt.apps.media.xemtv.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.kt.apps.core.base.CoreApp
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase

class TVContentProvider : ContentProvider() {

    private val db by lazy {
        RoomDataBase.getInstance(CoreApp.getInstance())
            .tvChannelEntityDao()
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = if (uri.pathSegments.firstOrNull() == "search") {
        Logger.d(this, message = "Handling query for $uri ${selectionArgs?.firstOrNull()}")
        selectionArgs?.firstOrNull()?.let { selector ->
            // Perform light processing of the query selector as we send the request to the database
            db.contentProviderQuery(selector.replace(Regex("[^A-Za-z0-9 ]"), ""))
        }
    } else {
        throw IllegalArgumentException("Invalid URI: $uri")
    }

    override fun getType(uri: Uri): String? {
        TODO("Not yet implemented")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        TODO("Not yet implemented")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        TODO("Not yet implemented")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        TODO("Not yet implemented")
    }
}