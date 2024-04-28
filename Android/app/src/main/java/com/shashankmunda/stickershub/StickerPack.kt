/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.shashankmunda.stickershub

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
 class StickerPack(
    @JvmField val identifier: String,
    @JvmField val name: String,
    @JvmField val publisher: String,
    @JvmField val trayImageFile: String,
    @JvmField val publisherEmail: String?,
    @JvmField val publisherWebsite: String?,
    @JvmField val privacyPolicyWebsite: String?,
    @JvmField val licenseAgreementWebsite: String?,
    @JvmField val imageDataVersion: String?,
    @JvmField val avoidCache: Boolean,
    @JvmField val animatedStickerPack: Boolean,
    @JvmField var iosAppStoreLink: String? = null,
    var stickers: List<Sticker>,
    var totalSize: Long = 0,
    @JvmField var androidPlayStoreLink: String? = null,
    var isWhitelisted: Boolean = false
) : Parcelable