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

package de.dreier.mytargets.base.db.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import android.database.Cursor
import de.dreier.mytargets.shared.models.Dimension
import de.dreier.mytargets.shared.models.Target
import de.dreier.mytargets.shared.models.db.Shot
import timber.log.Timber

object Migration27 : Migration(26, 27) {

    override fun migrate(database: SupportSQLiteDatabase) {
        Timber.i("Migrating DB from version 26 to 27")

        database.execSQL("ALTER TABLE `Signature` ADD COLUMN `trainingId` INTEGER NOT NULL DEFAULT 0;")
        database.execSQL("ALTER TABLE `Signature` ADD COLUMN `type` INTEGER NOT NULL DEFAULT 0;")
        database.execSQL("ALTER TABLE `Signature` ADD COLUMN `index` INTEGER NOT NULL DEFAULT 0;")

        database.execSQL("UPDATE `Signature` " +
                "SET trainingId = (SELECT `Training`.id " +
                "FROM `Training` " +
                "WHERE `Training`.archerSignatureId = `Signature`.id ) " +
                "WHERE EXISTS ( " +
                " SELECT * FROM `Training` " +
                "        WHERE `Training`.archerSignatureId = `Signature`.id " +
                ")")

        database.execSQL("UPDATE `Signature` " +
                "SET type = 1, trainingId = (SELECT `Training`.id " +
                "FROM `Training` " +
                "WHERE `Training`.witnessSignatureId = `Signature`.id ) " +
                "WHERE EXISTS ( " +
                "SELECT * FROM `Training` " +
                "        WHERE `Training`.witnessSignatureId = `Signature`.id " +
                ")")

        database.execSQL("DELETE FROM `Signature` WHERE trainingId = 0 OR bitmap IS NULL")

        recreateTablesWithNonNull(database)

        createScoreTriggers(database)

        createIndices(database)
    }

    private fun createScoreTriggers(database: SupportSQLiteDatabase) {
        // Insert Triggers
        database.execSQL(getInsertTrigger("Round", "End", "roundId"))
        database.execSQL(getInsertTrigger("Training", "Round", "trainingId"))

        // Update Triggers
        database.execSQL(getUpdateTrigger("Round", "End", "roundId"))
        database.execSQL(getUpdateTrigger("Training", "Round", "trainingId"))

        // Delete Triggers
        database.execSQL(getDeleteTrigger("Round", "End", "roundId"))
        database.execSQL(getDeleteTrigger("Training", "Round", "trainingId"))
    }

    private fun getInsertTrigger(
        parentTable: String,
        childTable: String,
        joinColumn: String
    ): String {
        return "CREATE TRIGGER insert_${parentTable.toLowerCase()}_sum_score " +
                "AFTER INSERT ON `$childTable` " +
                "BEGIN " +
                getUpdateQuery(parentTable, childTable, joinColumn, "NEW") +
                "END;"
    }

    private fun getUpdateTrigger(
        parentTable: String,
        childTable: String,
        joinColumn: String
    ): String {
        return "CREATE TRIGGER update_${parentTable.toLowerCase()}_sum_score " +
                "AFTER UPDATE OF reachedPoints, totalPoints, shotCount ON `$childTable` " +
                "BEGIN " +
                getUpdateQuery(parentTable, childTable, joinColumn, "NEW") +
                "END;"
    }

    private fun getDeleteTrigger(
        parentTable: String,
        childTable: String,
        joinColumn: String
    ): String {
        return "CREATE TRIGGER delete_${parentTable.toLowerCase()}_sum_score " +
                "AFTER DELETE ON `$childTable` " +
                "BEGIN " +
                getUpdateQuery(parentTable, childTable, joinColumn, "OLD") +
                "END;"
    }

    private fun getUpdateQuery(
        parentTable: String,
        childTable: String,
        joinColumn: String,
        reference: String
    ): String {
        return "UPDATE `$parentTable` SET " +
                "reachedPoints = (SELECT IFNULL(SUM(reachedPoints), 0) FROM `$childTable` WHERE $joinColumn = $reference.$joinColumn), " +
                "totalPoints = (SELECT IFNULL(SUM(totalPoints), 0) FROM `$childTable` WHERE $joinColumn = $reference.$joinColumn), " +
                "shotCount = (SELECT IFNULL(SUM(shotCount), 0) FROM `$childTable` WHERE $joinColumn = $reference.$joinColumn) " +
                "WHERE id = $reference.$joinColumn;"
    }

    private fun createIndices(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE  INDEX `index_ArrowImage_arrowId` ON `ArrowImage` (`arrowId`)")
        database.execSQL("CREATE  INDEX `index_BowImage_bowId` ON `BowImage` (`bowId`)")
        database.execSQL("CREATE  INDEX `index_End_roundId` ON `End` (`roundId`)")
        database.execSQL("CREATE  INDEX `index_EndImage_endId` ON `EndImage` (`endId`)")
        database.execSQL("CREATE  INDEX `index_Round_trainingId` ON `Round` (`trainingId`)")
        database.execSQL("CREATE  INDEX `index_RoundTemplate_standardRoundId` ON `RoundTemplate` (`standardRoundId`)")
        database.execSQL("CREATE  INDEX `index_Shot_endId` ON `Shot` (`endId`)")
        database.execSQL("CREATE  INDEX `index_SightMark_bowId` ON `SightMark` (`bowId`)")
        database.execSQL("CREATE  INDEX `index_Training_arrowId` ON `Training` (`arrowId`)")
        database.execSQL("CREATE  INDEX `index_Training_bowId` ON `Training` (`bowId`)")
        database.execSQL("CREATE  INDEX `index_Training_standardRoundId` ON `Training` (`standardRoundId`)")
        database.execSQL("CREATE  INDEX `index_Signature_trainingId` ON `Signature` (`trainingId`)")
    }

    private fun recreateTablesWithNonNull(database: SupportSQLiteDatabase) {
        val migrations = listOf(
            TableMigrationBuilder("Arrow")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(name = "name", notNull = true)
                .field(name = "maxArrowNumber", affinity = "INTEGER", notNull = true)
                .field(name = "length", affinity = "TEXT")
                .field(name = "material", affinity = "TEXT")
                .field(name = "spine", affinity = "TEXT")
                .field(name = "weight", affinity = "TEXT")
                .field(name = "tipWeight", affinity = "TEXT")
                .field(name = "vanes", affinity = "TEXT")
                .field(name = "nock", affinity = "TEXT")
                .field(name = "comment", affinity = "TEXT")
                .field(name = "diameter", notNull = true)
                .field(name = "thumbnail", affinity = "BLOB"),

            TableMigrationBuilder("ArrowImage")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(name = "fileName", notNull = true)
                .field(
                    name = "arrowId",
                    columnName = "arrowId",
                    affinity = "INTEGER",
                    refTable = "Arrow",
                    refColumn = "id",
                    onDelete = "CASCADE"
                ),

            TableMigrationBuilder("Bow")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(name = "name", notNull = true)
                .field(name = "type", affinity = "INTEGER")
                .field(name = "brand", affinity = "TEXT")
                .field(name = "size", affinity = "TEXT")
                .field(name = "braceHeight", affinity = "TEXT")
                .field(name = "tiller", affinity = "TEXT")
                .field(name = "limbs", affinity = "TEXT")
                .field(name = "sight", affinity = "TEXT")
                .field(name = "drawWeight", affinity = "TEXT")
                .field(name = "stabilizer", affinity = "TEXT")
                .field(name = "clicker", affinity = "TEXT")
                .field(name = "button", affinity = "TEXT")
                .field(name = "string", affinity = "TEXT")
                .field(name = "nockingPoint", affinity = "TEXT")
                .field(name = "letoffWeight", affinity = "TEXT")
                .field(name = "arrowRest", affinity = "TEXT")
                .field(name = "restHorizontalPosition", affinity = "TEXT")
                .field(name = "restVerticalPosition", affinity = "TEXT")
                .field(name = "restStiffness", affinity = "TEXT")
                .field(name = "camSetting", affinity = "TEXT")
                .field(name = "scopeMagnification", affinity = "TEXT")
                .field(name = "description", affinity = "TEXT")
                .field(name = "thumbnail", affinity = "BLOB"),

            TableMigrationBuilder("BowImage")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(name = "fileName", notNull = true)
                .field(
                    name = "bowId",
                    columnName = "bowId",
                    affinity = "INTEGER",
                    refTable = "Bow",
                    refColumn = "id",
                    onDelete = "CASCADE"
                ),

            TableMigrationBuilder("StandardRound")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(name = "club", affinity = "INTEGER", notNull = true)
                .field(name = "name", notNull = true),

            TableMigrationBuilder("RoundTemplate")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(
                    name = "standardRoundId",
                    columnName = "standardRoundId",
                    affinity = "INTEGER",
                    refTable = "StandardRound",
                    refColumn = "id",
                    onDelete = "CASCADE"
                )
                .field(name = "index", affinity = "INTEGER", notNull = true)
                .field(name = "shotsPerEnd", affinity = "INTEGER", notNull = true)
                .field(name = "endCount", affinity = "INTEGER", notNull = true)
                .field(name = "distance", notNull = true)
                .field(
                    name = "targetId",
                    columnName = "targetId",
                    affinity = "INTEGER",
                    notNull = true
                )
                .field(
                    name = "targetScoringStyleIndex",
                    columnName = "targetScoringStyleIndex",
                    affinity = "INTEGER",
                    notNull = true
                )
                .field(
                    name = "targetDiameter",
                    columnName = "targetDiameter",
                    notNull = true
                ),

            TableMigrationBuilder("SightMark")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(
                    name = "bowId",
                    columnName = "bowId",
                    affinity = "INTEGER",
                    refTable = "Bow",
                    refColumn = "id",
                    onDelete = "CASCADE"
                )
                .field(name = "distance", notNull = true)
                .field(name = "value", affinity = "TEXT"),

            TableMigrationBuilder("Signature")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(
                            name = "trainingId",
                            columnName = "trainingId",
                            affinity = "INTEGER",
                            notNull = true,
                            refTable = "Training",
                            refColumn = "id",
                            onDelete = "CASCADE"
                )
                .field(name = "type", affinity = "INTEGER", notNull = true)
                .field(name = "index", affinity = "INTEGER", notNull = true)
                .field(name = "name", notNull = true)
                .field(name = "bitmap", affinity = "BLOB"),

            TableMigrationBuilder("Training")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(name = "title", notNull = true)
                .field(name = "date", notNull = true)
                .field(
                    name = "standardRoundId",
                    columnName = "standardRoundId",
                    affinity = "INTEGER",
                    refTable = "StandardRound",
                    refColumn = "id",
                    onDelete = "SET NULL"
                )
                .field(
                    name = "bowId",
                    columnName = "bowId",
                    affinity = "INTEGER",
                    refTable = "Bow",
                    refColumn = "id",
                    onDelete = "SET NULL"
                )
                .field(
                    name = "arrowId",
                    columnName = "arrowId",
                    affinity = "INTEGER",
                    refTable = "Arrow",
                    refColumn = "id",
                    onDelete = "SET NULL"
                )
                .field(name = "arrowNumbering", affinity = "INTEGER", notNull = true)
                .field(name = "comment", notNull = true)
                .field(name = "indoor", affinity = "INTEGER", notNull = true)
                .field(name = "weather", affinity = "INTEGER", notNull = true)
                .field(name = "windSpeed", affinity = "INTEGER", notNull = true)
                .field(name = "windDirection", affinity = "INTEGER", notNull = true)
                .field(name = "location", notNull = true)
                .field(name = "reachedPoints", affinity = "INTEGER", notNull = true)
                .field(name = "totalPoints", affinity = "INTEGER", notNull = true)
                .field(name = "shotCount", affinity = "INTEGER", notNull = true),

            TableMigrationBuilder("Round")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(
                    name = "trainingId",
                    columnName = "trainingId",
                    affinity = "INTEGER",
                    refTable = "Training",
                    refColumn = "id",
                    onDelete = "CASCADE"
                )
                .field(name = "index", affinity = "INTEGER", notNull = true)
                .field(name = "shotsPerEnd", affinity = "INTEGER", notNull = true)
                .field(name = "maxEndCount", affinity = "INTEGER")
                .field(name = "distance", notNull = true)
                .field(name = "comment", notNull = true)
                .field(
                    name = "targetId",
                    affinity = "INTEGER",
                    notNull = true
                )
                .field(
                    name = "targetScoringStyleIndex",
                    columnName = "targetScoringStyleIndex",
                    affinity = "INTEGER",
                    notNull = true
                )
                .field(name = "targetDiameter", notNull = true)
                .field(name = "reachedPoints", affinity = "INTEGER", notNull = true)
                .field(name = "totalPoints", affinity = "INTEGER", notNull = true)
                .field(name = "shotCount", affinity = "INTEGER", notNull = true),

            TableMigrationBuilder("End")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(name = "index", affinity = "INTEGER", notNull = true)
                .field(
                    name = "roundId",
                    columnName = "roundId",
                    affinity = "INTEGER",
                    refTable = "Round",
                    refColumn = "id",
                    onDelete = "CASCADE"
                )
                .field(name = "exact", affinity = "INTEGER", notNull = true)
                .field(name = "saveTime", affinity = "TEXT")
                .field(name = "comment", notNull = true)
                .field(name = "reachedPoints", affinity = "INTEGER", notNull = true)
                .field(name = "totalPoints", affinity = "INTEGER", notNull = true)
                .field(name = "shotCount", affinity = "INTEGER", notNull = true),

            TableMigrationBuilder("EndImage")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(name = "fileName", notNull = true)
                .field(
                    name = "endId",
                    columnName = "endId",
                    affinity = "INTEGER",
                    refTable = "End",
                    refColumn = "id",
                    onDelete = "CASCADE"
                ),

            TableMigrationBuilder("Shot")
                .field(
                    name = "id",
                    columnName = "id",
                    affinity = "INTEGER PRIMARY KEY AUTOINCREMENT",
                    notNull = true
                )
                .field(name = "index", affinity = "INTEGER", notNull = true)
                .field(
                    name = "endId",
                    columnName = "endId",
                    affinity = "INTEGER",
                    refTable = "End",
                    refColumn = "id",
                    onDelete = "CASCADE"
                )
                .field(name = "x", affinity = "REAL", notNull = true)
                .field(name = "y", affinity = "REAL", notNull = true)
                .field(name = "scoringRing", affinity = "INTEGER", notNull = true)
                .field(name = "arrowNumber", affinity = "TEXT")
        )

        for (migration in migrations) {
            migration.createNewTable(database)
        }
        for (migration in migrations) {
            migration.copyData(database)
        }
        for (migration in migrations) {
            migration.replaceOldTableWithNewTable(database)
        }
    }
}
