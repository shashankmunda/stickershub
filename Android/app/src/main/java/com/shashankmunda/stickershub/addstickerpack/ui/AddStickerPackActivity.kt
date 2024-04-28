/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.stickershub.addstickerpack.ui

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog.Builder
import androidx.fragment.app.DialogFragment
import com.shashankmunda.stickershub.BaseActivity
import com.shashankmunda.stickershub.BaseActivity.MessageDialogFragment.Companion.newInstance
import com.shashankmunda.stickershub.BuildConfig
import com.shashankmunda.stickershub.R
import com.shashankmunda.stickershub.WhitelistCheck
import com.shashankmunda.stickershub.WhitelistCheck.isPackageInstalled
import com.shashankmunda.stickershub.WhitelistCheck.isStickerPackWhitelistedInWhatsAppConsumer
import com.shashankmunda.stickershub.WhitelistCheck.isStickerPackWhitelistedInWhatsAppSmb
import com.shashankmunda.stickershub.WhitelistCheck.isWhatsAppConsumerAppInstalled
import com.shashankmunda.stickershub.WhitelistCheck.isWhatsAppSmbAppInstalled
import com.shashankmunda.stickershub.stickerpackdetails.ui.StickerPackDetailsActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class AddStickerPackActivity : BaseActivity() {

    private val activityLauncher =registerForActivityResult(StartActivityForResult()){
        if (it.resultCode == RESULT_CANCELED) {
            if (it.data != null) {
                val validationError = it.data!!.getStringExtra("validation_error")
                if (validationError != null) {
                    if (BuildConfig.DEBUG) {
                        //validation error should be shown to developer only, not users.
                        newInstance(R.string.title_validation_error, validationError).show(
                            supportFragmentManager, "validation error"
                        )
                    }
                    Log.e(TAG, "Validation failed:$validationError")
                }
            } else {
                StickerPackNotAddedMessageFragment().show(
                    supportFragmentManager,
                    "sticker_pack_not_added"
                )
            }
        }
    }
    protected fun addStickerPackToWhatsApp(identifier: String, stickerPackName: String) {
        try {
            //if neither WhatsApp Consumer or WhatsApp Business is installed, then tell user to install the apps.
            if (!isWhatsAppConsumerAppInstalled(packageManager) && !isWhatsAppSmbAppInstalled(
                    packageManager
                )
            ) {
                Toast.makeText(this, R.string.add_pack_fail_prompt_update_whatsapp, Toast.LENGTH_LONG)
                    .show()
                return
            }
            val stickerPackWhitelistedInWhatsAppConsumer =
                isStickerPackWhitelistedInWhatsAppConsumer(this, identifier)
            val stickerPackWhitelistedInWhatsAppSmb =
                isStickerPackWhitelistedInWhatsAppSmb(this, identifier)
            if (!stickerPackWhitelistedInWhatsAppConsumer && !stickerPackWhitelistedInWhatsAppSmb) {
                //ask users which app to add the pack to.
                launchIntentToAddPackToChooser(identifier, stickerPackName)
            } else if (!stickerPackWhitelistedInWhatsAppConsumer) {
                launchIntentToAddPackToSpecificPackage(
                    identifier,
                    stickerPackName,
                    WhitelistCheck.CONSUMER_WHATSAPP_PACKAGE_NAME
                )
            } else if (!stickerPackWhitelistedInWhatsAppSmb) {
                launchIntentToAddPackToSpecificPackage(
                    identifier,
                    stickerPackName,
                    WhitelistCheck.SMB_WHATSAPP_PACKAGE_NAME
                )
            } else {
                Toast.makeText(this, R.string.add_pack_fail_prompt_update_whatsapp, Toast.LENGTH_LONG)
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "error adding sticker pack to WhatsApp", e)
            Toast.makeText(this, R.string.add_pack_fail_prompt_update_whatsapp, Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun launchIntentToAddPackToSpecificPackage(
        identifier: String,
        stickerPackName: String,
        whatsappPackageName: String
    ) {
        val intent = createIntentToAddStickerPack(identifier, stickerPackName)
        intent.setPackage(whatsappPackageName)
        try {
            activityLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.add_pack_fail_prompt_update_whatsapp, Toast.LENGTH_LONG)
                .show()
        }
    }

    //Handle cases either of WhatsApp are set as default app to handle this intent. We still want users to see both options.
    private fun launchIntentToAddPackToChooser(identifier: String, stickerPackName: String) {
        val intent = createIntentToAddStickerPack(identifier, stickerPackName)

        try {
            activityLauncher.launch(
                Intent.createChooser(intent, getString(R.string.add_to_whatsapp))
            )
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.add_pack_fail_prompt_update_whatsapp, Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun createIntentToAddStickerPack(identifier: String, stickerPackName: String): Intent {
        val intent = Intent()
        intent.action = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_ID, identifier)
        intent.putExtra(
            StickerPackDetailsActivity.EXTRA_STICKER_PACK_AUTHORITY,
            BuildConfig.CONTENT_PROVIDER_AUTHORITY
        )
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_NAME, stickerPackName)
        return intent
    }

    class StickerPackNotAddedMessageFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialogBuilder = Builder(
                requireActivity()
            )
                .setMessage(R.string.add_pack_fail_prompt_update_whatsapp)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int -> dismiss() }
                .setNeutralButton(R.string.add_pack_fail_prompt_update_play_link) { dialog: DialogInterface?, which: Int -> launchWhatsAppPlayStorePage() }
            return dialogBuilder.create()
        }

        private fun launchWhatsAppPlayStorePage() {
            if (activity != null) {
                val packageManager = requireActivity().packageManager
                val whatsAppInstalled = isPackageInstalled(
                    WhitelistCheck.CONSUMER_WHATSAPP_PACKAGE_NAME,
                    packageManager
                )
                val smbAppInstalled =
                    isPackageInstalled(WhitelistCheck.SMB_WHATSAPP_PACKAGE_NAME, packageManager)
                val playPackageLinkPrefix = "http://play.google.com/store/apps/details?id="
                if (whatsAppInstalled && smbAppInstalled) {
                    launchPlayStoreWithUri("https://play.google.com/store/apps/developer?id=WhatsApp+LLC")
                } else if (whatsAppInstalled) {
                    launchPlayStoreWithUri(playPackageLinkPrefix + WhitelistCheck.CONSUMER_WHATSAPP_PACKAGE_NAME)
                } else if (smbAppInstalled) {
                    launchPlayStoreWithUri(playPackageLinkPrefix + WhitelistCheck.SMB_WHATSAPP_PACKAGE_NAME)
                }
            }
        }

        private fun launchPlayStoreWithUri(uriString: String) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(uriString)
            intent.setPackage("com.android.vending")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(activity, R.string.cannot_find_play_store, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val ADD_PACK = 200
        private const val TAG = "AddStickerPackActivity"
    }
}