/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.stickershub

import android.R.string
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class MessageDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            @StringRes val title = requireArguments().getInt(ARG_TITLE_ID)
            val message = requireArguments().getString(ARG_MESSAGE)
            val dialogBuilder = Builder(
                requireActivity()
            )
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(string.ok) { dialog: DialogInterface?, which: Int -> dismiss() }
            if (title != 0) {
                dialogBuilder.setTitle(title)
            }
            return dialogBuilder.create()
        }

        companion object {
            private const val ARG_TITLE_ID = "title_id"
            private const val ARG_MESSAGE = "message"
            @JvmStatic fun newInstance(@StringRes titleId: Int, message: String?): DialogFragment {
                val fragment: DialogFragment = MessageDialogFragment()
                val arguments = Bundle()
                arguments.putInt(ARG_TITLE_ID, titleId)
                arguments.putString(ARG_MESSAGE, message)
                fragment.arguments = arguments
                return fragment
            }
        }
    }
}