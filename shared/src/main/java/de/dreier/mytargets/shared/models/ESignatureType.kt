/*
 * Copyright (C) 2018 Florian Dreier
 *
 * This file is part of MyTargets.
 *
 * MyTargets is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * MyTargets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package de.dreier.mytargets.shared.models

import androidx.annotation.StringRes
import de.dreier.mytargets.shared.R

import de.dreier.mytargets.shared.SharedApplicationInstance

enum class ESignatureType constructor(@StringRes private val nameRes: Int) {
    ARCHER(R.string.signature_type_archer),
    WITNESS(R.string.signature_type_witness);

    override fun toString(): String {
        return SharedApplicationInstance.getStr(nameRes)
    }

    companion object {
        fun fromId(id: Int) = ESignatureType.values()[id]
    }
}
