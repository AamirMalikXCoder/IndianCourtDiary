package com.malik.indiancourtdiary.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
@Database(entities=[CourtCase::class],version=2,exportSchema=false)
abstract class CourtDatabase:RoomDatabase(){
 abstract fun courtCaseDao():CourtCaseDao
 companion object{fun create(c:Context)=Room.databaseBuilder(c,CourtDatabase::class.java,"court-diary.db").fallbackToDestructiveMigration().build()}
}
