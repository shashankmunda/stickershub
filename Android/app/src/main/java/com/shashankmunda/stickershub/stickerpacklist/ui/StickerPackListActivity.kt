/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.stickershub.stickerpacklist.ui

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shashankmunda.stickershub.R
import com.shashankmunda.stickershub.addstickerpack.ui.AddStickerPackActivity
import com.shashankmunda.stickershub.StickerPack
import com.shashankmunda.stickershub.stickerpacklist.ui.StickerPackListAdapter.OnAddButtonClickedListener
import com.shashankmunda.stickershub.Utils.getParcelableArrayListExtraCompat
import com.shashankmunda.stickershub.WhitelistCheck.isWhitelisted
import com.shashankmunda.stickershub.databinding.ActivityStickerPackListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class StickerPackListActivity : AddStickerPackActivity() {
    private lateinit var packLayoutManager: LinearLayoutManager
    private lateinit var packRecyclerView: RecyclerView
    private lateinit var allStickerPacksListAdapter: StickerPackListAdapter
    private lateinit var stickerPackList: ArrayList<StickerPack>

    private lateinit var binding: ActivityStickerPackListBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStickerPackListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        stickerPackList = intent.getParcelableArrayListExtraCompat(
            EXTRA_STICKER_PACK_LIST_DATA,
            StickerPack::class.java)!!
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

    private fun showStickerPackList(stickerPackList: List<StickerPack>?) {
        allStickerPacksListAdapter =
            StickerPackListAdapter(stickerPackList!!, onAddButtonClickedListener)
        binding.stickerPackList.adapter = allStickerPacksListAdapter
        packLayoutManager = LinearLayoutManager(this)
        packLayoutManager.orientation = RecyclerView.VERTICAL
        val dividerItemDecoration = DividerItemDecoration(
            binding.stickerPackList.context,
            packLayoutManager.orientation
        )
        binding.stickerPackList.addItemDecoration(dividerItemDecoration)
        binding.stickerPackList.layoutManager = packLayoutManager
        binding.stickerPackList.viewTreeObserver.addOnGlobalLayoutListener { recalculateColumnCount() }
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
            binding.stickerPackList.findViewHolderForAdapterPosition(firstVisibleItemPosition) as StickerPackListItemViewHolder?
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


    companion object {
        const val EXTRA_STICKER_PACK_LIST_DATA = "sticker_pack_list"
        private const val STICKER_PREVIEW_DISPLAY_LIMIT = 5
    }
}