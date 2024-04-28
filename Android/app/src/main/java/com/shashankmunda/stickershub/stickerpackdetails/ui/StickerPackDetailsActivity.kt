/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.stickershub.stickerpackdetails.ui

import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.shashankmunda.stickershub.R
import com.shashankmunda.stickershub.addstickerpack.ui.AddStickerPackActivity
import com.shashankmunda.stickershub.StickerPack
import com.shashankmunda.stickershub.StickerPackLoader.getStickerAssetUri
import com.shashankmunda.stickershub.Utils.getParcelableExtraCompat
import com.shashankmunda.stickershub.WhitelistCheck.isWhitelisted
import com.shashankmunda.stickershub.databinding.ActivityStickerPackDetailsBinding
import com.shashankmunda.stickershub.stickerpackinfo.StickerPackInfoActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class StickerPackDetailsActivity : AddStickerPackActivity() {
    private lateinit var layoutManager: GridLayoutManager
    private var stickerPreviewAdapter: StickerPreviewAdapter? = null
    private var numColumns = 0
    private var stickerPack: StickerPack? = null


    private lateinit var binding: ActivityStickerPackDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStickerPackDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val showUpButton = intent.getBooleanExtra(EXTRA_SHOW_UP_BUTTON, false)
        layoutManager = GridLayoutManager(this, 1)
        stickerPack = intent.getParcelableExtraCompat(EXTRA_STICKER_PACK_DATA, StickerPack::class.java)!!
        binding.stickerList.apply {
            layoutManager = this@StickerPackDetailsActivity.layoutManager
            viewTreeObserver.addOnGlobalLayoutListener(pageLayoutListener)
            addOnScrollListener(dividerScrollListener)
        }
        if (stickerPreviewAdapter == null) {
            stickerPreviewAdapter = StickerPreviewAdapter(
                layoutInflater,
                R.drawable.sticker_error,
                resources.getDimensionPixelSize(R.dimen.sticker_pack_details_image_size),
                resources.getDimensionPixelSize(R.dimen.sticker_pack_details_image_padding),
                stickerPack!!,
                binding.stickerDetailsExpandedSticker
            )
            binding.stickerList.adapter = stickerPreviewAdapter
        }
        binding.packName.text = stickerPack!!.name
        binding.author.text = stickerPack!!.publisher
        binding.trayImage.setImageURI(
            getStickerAssetUri(
                stickerPack!!.identifier,
                stickerPack!!.trayImageFile
            )
        )
        binding.packSize.text = Formatter.formatShortFileSize(this, stickerPack!!.totalSize)
        binding.addToWhatsappButton.setOnClickListener { v: View? ->
            addStickerPackToWhatsApp(
                stickerPack!!.identifier, stickerPack!!.name
            )
        }
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(showUpButton)
            supportActionBar!!.title = if (showUpButton) resources.getString(R.string.title_activity_sticker_pack_details_multiple_pack) else resources.getQuantityString(
                R.plurals.title_activity_sticker_packs_list,
                1
            )
        }
        binding.stickerPackAnimationIndicator.visibility =
            if (stickerPack!!.animatedStickerPack) View.VISIBLE else View.GONE
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED){
                val isWhitelisted = withContext(Dispatchers.IO){
                    isWhitelisted(this@StickerPackDetailsActivity, requireNotNull( stickerPack).identifier)
                }
                this@StickerPackDetailsActivity.updateAddUI(isWhitelisted)
            }
        }
    }

    private fun launchInfoActivity(
        publisherWebsite: String,
        publisherEmail: String,
        privacyPolicyWebsite: String,
        licenseAgreementWebsite: String,
        trayIconUriString: String
    ) {
        val intent = Intent(this@StickerPackDetailsActivity, StickerPackInfoActivity::class.java)
        intent.putExtra(EXTRA_STICKER_PACK_ID, stickerPack!!.identifier)
        intent.putExtra(EXTRA_STICKER_PACK_WEBSITE, publisherWebsite)
        intent.putExtra(EXTRA_STICKER_PACK_EMAIL, publisherEmail)
        intent.putExtra(EXTRA_STICKER_PACK_PRIVACY_POLICY, privacyPolicyWebsite)
        intent.putExtra(EXTRA_STICKER_PACK_LICENSE_AGREEMENT, licenseAgreementWebsite)
        intent.putExtra(EXTRA_STICKER_PACK_TRAY_ICON, trayIconUriString)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_info && stickerPack != null) {
            val trayIconUri =
                getStickerAssetUri(stickerPack!!.identifier, stickerPack!!.trayImageFile)
            launchInfoActivity(
                requireNotNull( stickerPack!!.publisherWebsite),
                requireNotNull(stickerPack!!.publisherEmail),
                requireNotNull(stickerPack!!.privacyPolicyWebsite),
                requireNotNull(stickerPack!!.licenseAgreementWebsite),
                trayIconUri.toString()
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private val pageLayoutListener = OnGlobalLayoutListener {
        setNumColumns(
            binding.stickerList.width / binding.stickerList!!.context.resources.getDimensionPixelSize(
                R.dimen.sticker_pack_details_image_size
            )
        )
    }

    private fun setNumColumns(numColumns: Int) {
        if (this.numColumns != numColumns) {
            layoutManager.spanCount = numColumns
            this.numColumns = numColumns
            if (stickerPreviewAdapter != null) {
                stickerPreviewAdapter!!.notifyDataSetChanged()
            }
        }
    }

    private val dividerScrollListener: OnScrollListener = object : OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            updateDivider(recyclerView)
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            updateDivider(recyclerView)
        }

        private fun updateDivider(recyclerView: RecyclerView) {
            val showDivider = recyclerView.computeVerticalScrollOffset() > 0
            if (binding.divider != null) {
                binding.divider.visibility =
                    if (showDivider) View.VISIBLE else View.INVISIBLE
            }
        }
    }

    /*override fun onResume() {
        super.onResume()
        whiteListCheckAsyncTask = WhiteListCheckAsyncTask(this)
        whiteListCheckAsyncTask!!.execute(stickerPack)
    }

    override fun onPause() {
        super.onPause()
        if (whiteListCheckAsyncTask != null && !whiteListCheckAsyncTask!!.isCancelled) {
            whiteListCheckAsyncTask!!.cancel(true)
        }
    }*/

    private fun updateAddUI(isWhitelisted: Boolean) {
        if (isWhitelisted) {
            binding.addToWhatsappButton.visibility = View.GONE
            binding.alreadyAddedText.visibility = View.VISIBLE
            binding.stickerPackDetailsTapToPreview.visibility =
                View.GONE
        } else {
            binding.addToWhatsappButton.visibility = View.VISIBLE
            binding.alreadyAddedText.visibility = View.GONE
            binding.stickerPackDetailsTapToPreview.visibility =
                View.VISIBLE
        }
    }

    companion object {
        /**
         * Do not change below values of below 3 lines as this is also used by WhatsApp
         */
        const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
        const val EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority"
        const val EXTRA_STICKER_PACK_NAME = "sticker_pack_name"
        const val EXTRA_STICKER_PACK_WEBSITE = "sticker_pack_website"
        const val EXTRA_STICKER_PACK_EMAIL = "sticker_pack_email"
        const val EXTRA_STICKER_PACK_PRIVACY_POLICY = "sticker_pack_privacy_policy"
        const val EXTRA_STICKER_PACK_LICENSE_AGREEMENT = "sticker_pack_license_agreement"
        const val EXTRA_STICKER_PACK_TRAY_ICON = "sticker_pack_tray_icon"
        const val EXTRA_SHOW_UP_BUTTON = "show_up_button"
        const val EXTRA_STICKER_PACK_DATA = "sticker_pack"
    }
}