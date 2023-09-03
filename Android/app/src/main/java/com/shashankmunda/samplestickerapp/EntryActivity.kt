/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.samplestickerapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.shashankmunda.samplestickerapp.StickerPackLoader.fetchStickerPacks
import com.shashankmunda.samplestickerapp.StickerPackValidator.verifyStickerPackValidity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntryActivity : BaseActivity() {
    private lateinit var progressBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        overridePendingTransition(0, 0)
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
        progressBar = findViewById(R.id.entry_activity_progress)
        lifecycleScope.launch(Dispatchers.IO){
            val stickerPackList: ArrayList<StickerPack>
            val stringListPair = try {
                    stickerPackList = fetchStickerPacks(this@EntryActivity)
                    if (stickerPackList.size == 0) {
                        Pair("could not find any packs", null)
                    }
                    for (stickerPack in stickerPackList) {
                        verifyStickerPackValidity(this@EntryActivity, stickerPack)
                    }
                    Pair(null, stickerPackList)
                     }
                catch (e: Exception) {
                Log.e("EntryActivity", "error fetching sticker packs", e)
                Pair(e.message, null)
            }
            withContext(Dispatchers.Main){
                if (stringListPair.first != null) {
                    showErrorMessage(stringListPair.first)
                } else {
                    showStickerPack(stringListPair.second)
                }
            }
        }
       /* loadListAsyncTask = LoadListAsyncTask(this)
        loadListAsyncTask!!.execute()*/
    }

    private fun showStickerPack(stickerPackList: ArrayList<StickerPack>?) {
        progressBar!!.visibility = View.GONE
        if (stickerPackList!!.size > 1) {
            val intent = Intent(this, StickerPackListActivity::class.java)
            intent.putParcelableArrayListExtra(
                StickerPackListActivity.EXTRA_STICKER_PACK_LIST_DATA,
                stickerPackList
            )
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        } else {
            val intent = Intent(this, StickerPackDetailsActivity::class.java)
            intent.putExtra(StickerPackDetailsActivity.EXTRA_SHOW_UP_BUTTON, false)
            intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_DATA, stickerPackList[0])
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        }
    }

    private fun showErrorMessage(errorMessage: String?) {
        progressBar!!.visibility = View.GONE
        Log.e("EntryActivity", "error fetching sticker packs, $errorMessage")
        val errorMessageTV = findViewById<TextView>(R.id.error_message)
        errorMessageTV.text = getString(R.string.error_message, errorMessage)
    }

   /* override fun onDestroy() {
        super.onDestroy()
        if (loadListAsyncTask != null && !loadListAsyncTask!!.isCancelled) {
            loadListAsyncTask!!.cancel(true)
        }
    }*/

    /*internal class LoadListAsyncTask(activity: EntryActivity) :
        AsyncTask<Void?, Void?, Pair<String?, ArrayList<StickerPack>?>>() {
        private val contextWeakReference: WeakReference<EntryActivity>

        init {
            contextWeakReference = WeakReference(activity)
        }

        protected override fun doInBackground(vararg voids: Void): Pair<String?, ArrayList<StickerPack>?> {
            val stickerPackList: ArrayList<StickerPack>
            return try {
                val context: Context? = contextWeakReference.get()
                if (context != null) {
                    stickerPackList = fetchStickerPacks(context)
                    if (stickerPackList.size == 0) {
                        return Pair("could not find any packs", null)
                    }
                    for (stickerPack in stickerPackList) {
                        verifyStickerPackValidity(context, stickerPack)
                    }
                    Pair(null, stickerPackList)
                } else {
                    Pair("could not fetch sticker packs", null)
                }
            } catch (e: Exception) {
                Log.e("EntryActivity", "error fetching sticker packs", e)
                Pair(e.message, null)
            }
        }

        override fun onPostExecute(stringListPair: Pair<String?, ArrayList<StickerPack>?>) {
            val entryActivity = contextWeakReference.get()
            if (entryActivity != null) {
                if (stringListPair.first != null) {
                    entryActivity.showErrorMessage(stringListPair.first)
                } else {
                    entryActivity.showStickerPack(stringListPair.second)
                }
            }
        }
    }*/
}