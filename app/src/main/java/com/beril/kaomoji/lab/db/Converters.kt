package com.beril.kaomoji.lab.db

import androidx.room.TypeConverter
import com.beril.kaomoji.lab.model.ContextKind
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipType
import com.beril.kaomoji.lab.model.ScheduleStatus
import java.time.Instant

class Converters {
    @TypeConverter fun instantToEpochMilli(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun epochMilliToInstant(v: Long?): Instant? = v?.let { Instant.ofEpochMilli(it) }

    @TypeConverter fun objectKindToString(v: ObjectKind): String = v.name
    @TypeConverter fun stringToObjectKind(v: String): ObjectKind = ObjectKind.valueOf(v)

    @TypeConverter fun relationshipTypeToString(v: RelationshipType): String = v.name
    @TypeConverter fun stringToRelationshipType(v: String): RelationshipType = RelationshipType.valueOf(v)

    @TypeConverter fun contextKindToString(v: ContextKind): String = v.name
    @TypeConverter fun stringToContextKind(v: String): ContextKind = ContextKind.valueOf(v)

    @TypeConverter fun scheduleStatusToString(v: ScheduleStatus): String = v.name
    @TypeConverter fun stringToScheduleStatus(v: String): ScheduleStatus = ScheduleStatus.valueOf(v)

    @TypeConverter fun stringSetToString(v: Set<String>): String = v.joinToString("\u0001")
    @TypeConverter fun stringToStringSet(v: String): Set<String> =
        if (v.isEmpty()) emptySet() else v.split("\u0001").toSet()
}
