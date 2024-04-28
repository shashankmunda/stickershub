/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.stickershub.stickerpackdetails.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.facebook.drawee.view.SimpleDraweeView
import com.shashankmunda.stickershub.R

class StickerPreviewViewHolder(itemView: View) : ViewHolder(itemView) {
    @JvmField val stickerPreviewView: SimpleDraweeView

    init {
        stickerPreviewView = itemView.findViewById(R.id.sticker_preview)
    }
}