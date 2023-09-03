/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.samplestickerapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
 class Sticker(
    val imageFileName: String,
    val emojis: List<String>?,
    var size: Long = 0
) : Parcelable