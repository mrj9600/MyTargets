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

package de.dreier.mytargets.base.db

import androidx.room.RoomDatabase
import de.dreier.mytargets.base.db.dao.RoundDAO
import de.dreier.mytargets.base.db.dao.SignatureDAO
import de.dreier.mytargets.base.db.dao.TrainingDAO
import de.dreier.mytargets.shared.models.ESignatureType
import de.dreier.mytargets.shared.models.augmented.AugmentedTraining
import de.dreier.mytargets.shared.models.db.Signature
import de.dreier.mytargets.shared.models.db.Training

class TrainingRepository(
    private val database: RoomDatabase,
    private val trainingDAO: TrainingDAO,
    private val roundDAO: RoundDAO,
    private val roundRepository: RoundRepository,
    private val signatureDAO: SignatureDAO
) {

    fun loadAugmentedTraining(id: Long): AugmentedTraining =
        AugmentedTraining(trainingDAO.loadTraining(id), roundDAO.loadRounds(id)
            .map { roundRepository.loadAugmentedRound(it) }
            .toMutableList())

    fun insertTraining(training: AugmentedTraining) {
        database.runInTransaction {
            training.training.id = trainingDAO.insertTraining(training.training)
            training.rounds.forEach { round ->
                round.round.trainingId = training.training.id
                roundRepository.insertRound(round)
            }
        }
    }

    fun getOrCreateArcherSignature(training: Training, archerIndex: Int): Signature {
         var signature = signatureDAO.loadSignatureByTrainingId(training.id, ESignatureType.ARCHER, archerIndex)
         if (signature == null) {
            signature = Signature(trainingId = training.id, type = ESignatureType.ARCHER, index = archerIndex)
            signature.id = signatureDAO.insertSignature(signature)
        }
        return signature
    }

    fun getOrCreateWitnessSignature(training: Training): Signature {
        var signature = signatureDAO.loadSignatureByTrainingId(training.id, ESignatureType.WITNESS,0)
        if (signature == null) {
            signature = Signature(trainingId = training.id, type = ESignatureType.WITNESS)
            signature.id = signatureDAO.insertSignature(signature)
        }
        return signature
    }
}
