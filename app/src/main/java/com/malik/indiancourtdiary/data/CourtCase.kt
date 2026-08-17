package com.malik.indiancourtdiary.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "court_cases")
data class CourtCase(
 @PrimaryKey val cnr:String,
 val caseTitle:String="Case details pending",
 val courtName:String="Court will appear after sync",
 val nextHearingDate:String?=null,
 val stage:String="Not synced",
 val hearingHistoryJson:String="[]",
 val clientName:String="",
 val clientPhone:String="",
 val notes:String="",
 val isPinned:Boolean=false,
 val isArchived:Boolean=false,
 val updatedAt:Long=System.currentTimeMillis()
)
