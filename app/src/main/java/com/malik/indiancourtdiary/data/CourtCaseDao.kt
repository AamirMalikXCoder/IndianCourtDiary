package com.malik.indiancourtdiary.data
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
@Dao
interface CourtCaseDao{
 @Query("SELECT * FROM court_cases ORDER BY CASE WHEN nextHearingDate IS NULL THEN 1 ELSE 0 END, nextHearingDate")
 fun observeAll():Flow<List<CourtCase>>
 @Query("SELECT * FROM court_cases WHERE isArchived=0")
 suspend fun activeCases():List<CourtCase>
 @Query("SELECT * FROM court_cases WHERE cnr=:cnr LIMIT 1")
 suspend fun find(cnr:String):CourtCase?
 @Upsert suspend fun save(item:CourtCase)
 @Query("DELETE FROM court_cases WHERE cnr=:cnr")
 suspend fun delete(cnr:String)
}
