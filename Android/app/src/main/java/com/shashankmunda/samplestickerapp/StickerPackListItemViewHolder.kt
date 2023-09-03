/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.samplestickerapp

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class StickerPackListItemViewHolder(@JvmField val container: View) : ViewHolder(
    container
) {
    @JvmField val titleView: TextView
    @JvmField val publisherView: TextView
    @JvmField val filesizeView: TextView
    @JvmField val addButton: ImageView
    @JvmField val animatedStickerPackIndicator: ImageView
    @JvmField val imageRowView: LinearLayout

    init {
        titleView = itemView.findViewById(R.id.sticker_pack_title)
        publisherView = itemView.findViewById(R.id.sticker_pack_publisher)
        filesizeView = itemView.findViewById(R.id.sticker_pack_filesize)
        addButton = itemView.findViewById(R.id.add_button_on_list)
        imageRowView = itemView.findViewById(R.id.sticker_packs_list_item_image_list)
        animatedStickerPackIndicator = itemView.findViewById(R.id.sticker_pack_animation_indicator)
    }
}