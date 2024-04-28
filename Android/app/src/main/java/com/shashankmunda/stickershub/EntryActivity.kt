/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.stickershub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.shashankmunda.stickershub.StickerPackLoader.fetchStickerPacks
import com.shashankmunda.stickershub.StickerPackValidator.verifyStickerPackValidity
import com.shashankmunda.stickershub.databinding.ActivityEntryBinding
import com.shashankmunda.stickershub.stickerpackdetails.ui.StickerPackDetailsActivity
import com.shashankmunda.stickershub.stickerpacklist.ui.StickerPackListActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class EntryActivity : BaseActivity() {
    private lateinit var binding: ActivityEntryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        overridePendingTransition(0, 0)
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
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
    }

    private fun showStickerPack(stickerPackList: ArrayList<StickerPack>?) {
        binding.entryActivityProgress.visibility = View.GONE
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
        binding.entryActivityProgress!!.visibility = View.GONE
        Log.e("EntryActivity", "error fetching sticker packs, $errorMessage")
        val errorMessageTV = binding.errorMessage
        errorMessageTV.text = getString(R.string.error_message, errorMessage)
    }
}