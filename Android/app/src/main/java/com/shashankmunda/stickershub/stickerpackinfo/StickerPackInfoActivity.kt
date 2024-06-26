/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.stickershub.stickerpackinfo

import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.appcompat.content.res.AppCompatResources
import com.shashankmunda.stickershub.BaseActivity
import com.shashankmunda.stickershub.R
import com.shashankmunda.stickershub.databinding.ActivityStickerPackInfoBinding
import com.shashankmunda.stickershub.stickerpackdetails.ui.StickerPackDetailsActivity
import java.io.FileNotFoundException

class StickerPackInfoActivity : BaseActivity() {

    private lateinit var binding: ActivityStickerPackInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStickerPackInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val trayIconUriString =
            intent.getStringExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_TRAY_ICON)
        val website = intent.getStringExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_WEBSITE)
        val email = intent.getStringExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_EMAIL)
        val privacyPolicy =
            intent.getStringExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_PRIVACY_POLICY)
        val licenseAgreement =
            intent.getStringExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_LICENSE_AGREEMENT)
        try {
            val inputStream = contentResolver.openInputStream(Uri.parse(trayIconUriString))
            val trayDrawable = BitmapDrawable(resources, inputStream)
            val emailDrawable = getDrawableForAllAPIs(R.drawable.sticker_3rdparty_email)
            trayDrawable.bounds =
                Rect(0, 0, emailDrawable!!.intrinsicWidth, emailDrawable.intrinsicHeight)
            binding.trayIcon.setCompoundDrawablesRelative(trayDrawable, null, null, null)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "could not find the uri for the tray image:$trayIconUriString")
        }
        setupTextView(website, R.id.view_webpage)
        if (TextUtils.isEmpty(email)) {
            binding.sendEmail.visibility = View.GONE
        } else {
            binding.sendEmail.setOnClickListener { v: View? -> launchEmailClient(email) }
        }
        setupTextView(privacyPolicy, R.id.privacy_policy)
        setupTextView(licenseAgreement, R.id.license_agreement)
    }

    private fun setupTextView(website: String?, @IdRes textViewResId: Int) {
        val viewWebpage = findViewById<TextView>(textViewResId)
        if (TextUtils.isEmpty(website)) {
            viewWebpage.visibility = View.GONE
        } else {
            viewWebpage.setOnClickListener { v: View? -> launchWebpage(website) }
        }
    }

    private fun launchEmailClient(email: String?) {
        val emailIntent = Intent(
            Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", email, null
            )
        )
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        startActivity(
            Intent.createChooser(
                emailIntent,
                resources.getString(R.string.info_send_email_to_prompt)
            )
        )
    }

    private fun launchWebpage(website: String?) {
        val uri = Uri.parse(website)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun getDrawableForAllAPIs(@DrawableRes id: Int): Drawable? {
        return AppCompatResources.getDrawable(applicationContext,id)
    }

    companion object {
        private const val TAG = "StickerPackInfoActivity"
    }
}