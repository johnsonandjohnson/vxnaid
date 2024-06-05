package com.jnj.vaccinetracker.common.data.database.migrations

import androidx.room.migration.AutoMigrationSpec
import androidx.room.RenameColumn

@RenameColumn.Entries(
        RenameColumn(tableName = "participant",
        fromColumnName = "isBirthDateAnApproximation",
        toColumnName = "isBirthDateEstimated"
        ),
        RenameColumn(
                tableName = "draft_participant",
                fromColumnName = "isBirthDateAnApproximation",
                toColumnName = "isBirthDateEstimated"
        ),
)
class ParticipantAutoMigrationSpec12to13 : AutoMigrationSpec
