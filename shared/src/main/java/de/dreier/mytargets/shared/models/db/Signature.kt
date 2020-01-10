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

package de.dreier.mytargets.shared.models.db

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import de.dreier.mytargets.shared.models.ESignatureType
import de.dreier.mytargets.shared.utils.readBitmap
import de.dreier.mytargets.shared.utils.writeBitmap

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Training::class,
            parentColumns = ["id"],
            childColumns = ["trainingId"],
            onDelete = CASCADE
        )
    ],
    indices = [
        Index(value = ["trainingId"])
    ]
) data class Signature(
        @PrimaryKey(autoGenerate = true)
        var id: Long = 0,

        var trainingId: Long,

        var type: ESignatureType = ESignatureType.ARCHER,

        var index: Int = 0,

        var name: String = "",

        /** A bitmap of the signature or null if no signature has been set. */
        @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
        var bitmap: Bitmap? = null
) : Parcelable {

    val isSigned: Boolean
        get() = bitmap != null

    fun getName(defaultName: String): String {
        return if (name.isEmpty()) defaultName else name
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeLong(trainingId)
        dest.writeInt(type.ordinal)
        dest.writeInt(index)
        dest.writeString(name)
        dest.writeBitmap(bitmap)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<Signature> {
            override fun createFromParcel(source: Parcel): Signature {
                val id = source.readLong()
                val trainingId = source.readLong()
                val typeId = source.readInt()
                val index = source.readInt()
                val name = source.readString()!!
                val bitmap = source.readBitmap()
                return Signature(id, trainingId, ESignatureType.fromId(typeId), index, name, bitmap)
            }

            override fun newArray(size: Int) = arrayOfNulls<Signature>(size)
        }
    }
}
