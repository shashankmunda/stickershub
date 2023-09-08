/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.samplestickerapp

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shashankmunda.samplestickerapp.StickerPackListAdapter.OnAddButtonClickedListener
import com.shashankmunda.samplestickerapp.Utils.getParcelableArrayListExtraCompat
import com.shashankmunda.samplestickerapp.WhitelistCheck.isWhitelisted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StickerPackListActivity : AddStickerPackActivity() {
    private lateinit var packLayoutManager: LinearLayoutManager
    private lateinit var packRecyclerView: RecyclerView
    private lateinit var allStickerPacksListAdapter: StickerPackListAdapter
    private lateinit var stickerPackList: ArrayList<StickerPack>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sticker_pack_list)
        packRecyclerView = findViewById(R.id.sticker_pack_list)
        stickerPackList = intent.getParcelableArrayListExtraCompat(EXTRA_STICKER_PACK_LIST_DATA,StickerPack::class.java)!!
        showStickerPackList(stickerPackList)
        if (supportActionBar != null) {
            supportActionBar!!.title = resources.getQuantityString(
                R.plurals.title_activity_sticker_packs_list,
                stickerPackList.size
            )
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED){
                val stickersList = withContext(Dispatchers.IO){
                    for (stickerPack in stickerPackList) {
                        stickerPack.isWhitelisted =
                            isWhitelisted(this@StickerPackListActivity, stickerPack.identifier)
                    }
                    return@withContext stickerPackList
                }
                this@StickerPackListActivity.allStickerPacksListAdapter.setStickerPackList(
                    stickersList
                )
                this@StickerPackListActivity.allStickerPacksListAdapter!!.notifyDataSetChanged()
            }
        }
    }

    /*override fun onResume() {
        super.onResume()
        whiteListCheckAsyncTask = WhiteListCheckAsyncTask(this)
        whiteListCheckAsyncTask!!.execute(*stickerPackList!!.toTypedArray())
    }

    override fun onPause() {
        super.onPause()
        if (whiteListCheckAsyncTask != null && !whiteListCheckAsyncTask!!.isCancelled) {
            whiteListCheckAsyncTask!!.cancel(true)
        }
    }*/

    private fun showStickerPackList(stickerPackList: List<StickerPack>?) {
        allStickerPacksListAdapter =
            StickerPackListAdapter(stickerPackList!!, onAddButtonClickedListener)
        packRecyclerView.adapter = allStickerPacksListAdapter
        packLayoutManager = LinearLayoutManager(this)
        packLayoutManager.orientation = RecyclerView.VERTICAL
        val dividerItemDecoration = DividerItemDecoration(
            packRecyclerView.context,
            packLayoutManager.orientation
        )
        packRecyclerView.addItemDecoration(dividerItemDecoration)
        packRecyclerView.layoutManager = packLayoutManager
        packRecyclerView.viewTreeObserver.addOnGlobalLayoutListener { recalculateColumnCount() }
    }

    private val onAddButtonClickedListener: OnAddButtonClickedListener =
        object : OnAddButtonClickedListener {
            override fun onAddButtonClicked(stickerPack: StickerPack) {
                addStickerPackToWhatsApp(
                    stickerPack.identifier,
                    stickerPack.name
                )
            }
        }

    private fun recalculateColumnCount() {
        val previewSize =
            resources.getDimensionPixelSize(R.dimen.sticker_pack_list_item_preview_image_size)
        val firstVisibleItemPosition = packLayoutManager.findFirstVisibleItemPosition()
        val viewHolder =
            packRecyclerView.findViewHolderForAdapterPosition(firstVisibleItemPosition) as StickerPackListItemViewHolder?
        if (viewHolder != null) {
            val widthOfImageRow = viewHolder.imageRowView.measuredWidth
            val max = Math.max(widthOfImageRow / previewSize, 1)
            val maxNumberOfImagesInARow = Math.min(STICKER_PREVIEW_DISPLAY_LIMIT, max)
            val minMarginBetweenImages =
                (widthOfImageRow - maxNumberOfImagesInARow * previewSize) / (maxNumberOfImagesInARow - 1)
            allStickerPacksListAdapter.setImageRowSpec(
                maxNumberOfImagesInARow,
                minMarginBetweenImages
            )
        }
    }

  /*  internal class WhiteListCheckAsyncTask(stickerPackListActivity: StickerPackListActivity) :
        AsyncTask<StickerPack?, Void?, List<StickerPack>>() {
        private val stickerPackListActivityWeakReference: WeakReference<StickerPackListActivity>

        init {
            stickerPackListActivityWeakReference = WeakReference(stickerPackListActivity)
        }

        protected override fun doInBackground(vararg stickerPackArray: StickerPack): List<StickerPack> {
            val stickerPackListActivity = stickerPackListActivityWeakReference.get()
                ?: return Arrays.asList(*stickerPackArray)
            for (stickerPack in stickerPackArray) {
                stickerPack.isWhitelisted =
                    isWhitelisted(stickerPackListActivity, stickerPack.identifier)
            }
            return Arrays.asList(*stickerPackArray)
        }

        override fun onPostExecute(stickerPackList: List<StickerPack>) {
            val stickerPackListActivity = stickerPackListActivityWeakReference.get()
            if (stickerPackListActivity != null) {
                stickerPackListActivity.allStickerPacksListAdapter!!.setStickerPackList(
                    stickerPackList
                )
                stickerPackListActivity.allStickerPacksListAdapter!!.notifyDataSetChanged()
            }
        }
    }*/

    companion object {
        const val EXTRA_STICKER_PACK_LIST_DATA = "sticker_pack_list"
        private const val STICKER_PREVIEW_DISPLAY_LIMIT = 5
    }
}