/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.samplestickerapp

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.net.Uri.Builder
import android.text.TextUtils
import android.util.Log
import java.io.IOException
import java.util.Objects

class StickerContentProvider : ContentProvider() {
    private var stickerPackList: List<StickerPack>? = null
    override fun onCreate(): Boolean {
        val authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY
        check(authority.startsWith(requireNotNull(context).packageName)) { "your authority (" + authority + ") for the content provider should start with your package name: " + context!!.packageName }

        //the call to get the metadata for the sticker packs.
        MATCHER.addURI(authority, METADATA, METADATA_CODE)

        //the call to get the metadata for single sticker pack. * represent the identifier
        MATCHER.addURI(authority, METADATA + "/*", METADATA_CODE_FOR_SINGLE_PACK)

        //gets the list of stickers for a sticker pack, * respresent the identifier.
        MATCHER.addURI(authority, STICKERS + "/*", STICKERS_CODE)
        for (stickerPack in getStickerPackList()!!) {
            MATCHER.addURI(
                authority,
                STICKERS_ASSET + "/" + stickerPack.identifier + "/" + stickerPack.trayImageFile,
                STICKER_PACK_TRAY_ICON_CODE
            )
            for (sticker in stickerPack.stickers) {
                MATCHER.addURI(
                    authority,
                    STICKERS_ASSET + "/" + stickerPack.identifier + "/" + sticker.imageFileName,
                    STICKERS_ASSET_CODE
                )
            }
        }
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor {
        return when (MATCHER.match(uri)) {
            METADATA_CODE -> {
                getPackForAllStickerPacks(uri)
            }
            METADATA_CODE_FOR_SINGLE_PACK -> {
                getCursorForSingleStickerPack(uri)
            }
            STICKERS_CODE -> {
                getStickersForAStickerPack(uri)
            }
            else -> {
                throw IllegalArgumentException("Unknown URI: $uri")
            }
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val matchCode = MATCHER.match(uri)
        return if (matchCode == STICKERS_ASSET_CODE || matchCode == STICKER_PACK_TRAY_ICON_CODE) {
            getImageAsset(uri)
        } else null
    }

    override fun getType(uri: Uri): String {
        return when (MATCHER.match(uri)) {
            METADATA_CODE -> "vnd.android.cursor.dir/vnd." + BuildConfig.CONTENT_PROVIDER_AUTHORITY + "." + METADATA
            METADATA_CODE_FOR_SINGLE_PACK -> "vnd.android.cursor.item/vnd." + BuildConfig.CONTENT_PROVIDER_AUTHORITY + "." + METADATA
            STICKERS_CODE -> "vnd.android.cursor.dir/vnd." + BuildConfig.CONTENT_PROVIDER_AUTHORITY + "." + STICKERS
            STICKERS_ASSET_CODE -> "image/webp"
            STICKER_PACK_TRAY_ICON_CODE -> "image/png"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    @Synchronized private fun readContentFile(context: Context) {
        try {
            context.assets.open(CONTENT_FILE_NAME).use { contentsInputStream ->
                stickerPackList = ContentFileParser.parseStickerPacks(contentsInputStream)
            }
        } catch (e: IOException) {
            throw RuntimeException(CONTENT_FILE_NAME + " file has some issues: " + e.message, e)
        } catch (e: IllegalStateException) {
            throw RuntimeException(CONTENT_FILE_NAME + " file has some issues: " + e.message, e)
        }
    }

    private fun getStickerPackList(): List<StickerPack>? {
        if (stickerPackList == null) {
            readContentFile(requireNotNull(context))
        }
        return stickerPackList
    }

    private fun getPackForAllStickerPacks(uri: Uri): Cursor {
        return getStickerPackInfo(uri, getStickerPackList()!!)
    }

    private fun getCursorForSingleStickerPack(uri: Uri): Cursor {
        val identifier = uri.lastPathSegment
        for (stickerPack in getStickerPackList()!!) {
            if (identifier == stickerPack.identifier) {
                return getStickerPackInfo(uri, listOf(stickerPack))
            }
        }
        return getStickerPackInfo(uri, ArrayList())
    }

    private fun getStickerPackInfo(uri: Uri, stickerPackList: List<StickerPack>): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                STICKER_PACK_IDENTIFIER_IN_QUERY,
                STICKER_PACK_NAME_IN_QUERY,
                STICKER_PACK_PUBLISHER_IN_QUERY,
                STICKER_PACK_ICON_IN_QUERY,
                ANDROID_APP_DOWNLOAD_LINK_IN_QUERY,
                IOS_APP_DOWNLOAD_LINK_IN_QUERY,
                PUBLISHER_EMAIL,
                PUBLISHER_WEBSITE,
                PRIVACY_POLICY_WEBSITE,
                LICENSE_AGREENMENT_WEBSITE,
                IMAGE_DATA_VERSION,
                AVOID_CACHE,
                ANIMATED_STICKER_PACK
            )
        )
        for (stickerPack in stickerPackList) {
            val builder = cursor.newRow()
            builder.add(stickerPack.identifier)
            builder.add(stickerPack.name)
            builder.add(stickerPack.publisher)
            builder.add(stickerPack.trayImageFile)
            builder.add(stickerPack.androidPlayStoreLink)
            builder.add(stickerPack.iosAppStoreLink)
            builder.add(stickerPack.publisherEmail)
            builder.add(stickerPack.publisherWebsite)
            builder.add(stickerPack.privacyPolicyWebsite)
            builder.add(stickerPack.licenseAgreementWebsite)
            builder.add(stickerPack.imageDataVersion)
            builder.add(if (stickerPack.avoidCache) 1 else 0)
            builder.add(if (stickerPack.animatedStickerPack) 1 else 0)
        }
        cursor.setNotificationUri(requireNotNull(context).contentResolver, uri)
        return cursor
    }

    private fun getStickersForAStickerPack(uri: Uri): Cursor {
        val identifier = uri.lastPathSegment
        val cursor = MatrixCursor(arrayOf(STICKER_FILE_NAME_IN_QUERY, STICKER_FILE_EMOJI_IN_QUERY))
        for (stickerPack in getStickerPackList()!!) {
            if (identifier == stickerPack.identifier) {
                for (sticker in stickerPack.stickers) {
                    cursor.addRow(
                        arrayOf(
                            sticker.imageFileName,
                            TextUtils.join(",", sticker.emojis as MutableIterable<Any?>)
                        )
                    )
                }
            }
        }
        cursor.setNotificationUri(requireNotNull(context).contentResolver, uri)
        return cursor
    }

    @Throws(IllegalArgumentException::class)
    private fun getImageAsset(uri: Uri): AssetFileDescriptor? {
        val am = Objects.requireNonNull(context)!!.assets
        val pathSegments = uri.pathSegments
        require(pathSegments.size == 3) { "path segments should be 3, uri is: $uri" }
        val fileName = pathSegments[pathSegments.size - 1]
        val identifier = pathSegments[pathSegments.size - 2]
        require(!TextUtils.isEmpty(identifier)) { "identifier is empty, uri: $uri" }
        require(!TextUtils.isEmpty(fileName)) { "file name is empty, uri: $uri" }
        //making sure the file that is trying to be fetched is in the list of stickers.
        for (stickerPack in getStickerPackList()!!) {
            if (identifier == stickerPack.identifier) {
                if (fileName == stickerPack.trayImageFile) {
                    return fetchFile(uri, requireNotNull(am), fileName, identifier)
                } else {
                    for (sticker in stickerPack.stickers) {
                        if (fileName == sticker.imageFileName) {
                            return fetchFile(uri, requireNotNull(am), fileName, identifier)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun fetchFile(
        uri: Uri,
        am: AssetManager,
        fileName: String,
        identifier: String
    ): AssetFileDescriptor? {
        return try {
            am.openFd("$identifier/$fileName")
        } catch (e: IOException) {
            Log.e(
                requireNotNull(context).packageName,
                "IOException when getting asset file, uri:$uri",
                e
            )
            null
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("Not supported")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Not supported")
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        throw UnsupportedOperationException("Not supported")
    }

    companion object {
        /**
         * Do not change the strings listed below, as these are used by WhatsApp. And changing these will break the interface between sticker app and WhatsApp.
         */
        const val STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier"
        const val STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name"
        const val STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher"
        const val STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon"
        const val ANDROID_APP_DOWNLOAD_LINK_IN_QUERY = "android_play_store_link"
        const val IOS_APP_DOWNLOAD_LINK_IN_QUERY = "ios_app_download_link"
        const val PUBLISHER_EMAIL = "sticker_pack_publisher_email"
        const val PUBLISHER_WEBSITE = "sticker_pack_publisher_website"
        const val PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website"
        const val LICENSE_AGREENMENT_WEBSITE = "sticker_pack_license_agreement_website"
        const val IMAGE_DATA_VERSION = "image_data_version"
        private const val METADATA = "metadata"
        const val AVOID_CACHE = "whatsapp_will_not_cache_stickers"
        const val ANIMATED_STICKER_PACK = "animated_sticker_pack"
        const val STICKER_FILE_NAME_IN_QUERY = "sticker_file_name"
        const val STICKER_FILE_EMOJI_IN_QUERY = "sticker_emoji"
        private const val CONTENT_FILE_NAME = "contents.json"
        val AUTHORITY_URI = Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(
            BuildConfig.CONTENT_PROVIDER_AUTHORITY
        ).appendPath(METADATA).build()

        /**
         * Do not change the values in the UriMatcher because otherwise, WhatsApp will not be able to fetch the stickers from the ContentProvider.
         */
        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH)
        private const val METADATA_CODE = 1
        private const val METADATA_CODE_FOR_SINGLE_PACK = 2
        const val STICKERS = "stickers"
        private const val STICKERS_CODE = 3
        const val STICKERS_ASSET = "stickers_asset"
        private const val STICKERS_ASSET_CODE = 4
        private const val STICKER_PACK_TRAY_ICON_CODE = 5
    }
}