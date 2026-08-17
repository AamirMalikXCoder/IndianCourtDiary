package com.malik.indiancourtdiary

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

object DailyCaseSyncScheduler {
 fun cancel(context:Context){WorkManager.getInstance(context).cancelUniqueWork("daily-court-sync")}
 fun schedule(context:Context){
  val constraints=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
  val request=PeriodicWorkRequestBuilder<DailyCaseSyncWorker>(24,TimeUnit.HOURS).setConstraints(constraints).build()
  WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily-court-sync",ExistingPeriodicWorkPolicy.UPDATE,request)
 }
}

class DailyCaseSyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
 override suspend fun doWork():Result{
  val app=applicationContext as CourtDiaryApp
  val dao=app.database.courtCaseDao()
  var temporaryFailure=false
  dao.activeCases().forEach{old->
   try{
    val fresh=CourtApiProvider.api.getCase(old.cnr)
    val changed=old.nextHearingDate!=fresh.nextHearingDate&&fresh.nextHearingDate!=null
    dao.save(old.copy(caseTitle=fresh.caseTitle,courtName=fresh.courtName,nextHearingDate=fresh.nextHearingDate,stage=fresh.stage,hearingHistoryJson=Gson().toJson(fresh.hearingHistory),updatedAt=System.currentTimeMillis()))
    HearingReminderScheduler.schedule(applicationContext,fresh.cnr,fresh.caseTitle,fresh.nextHearingDate)
    if(changed)notifyChanged(fresh)
   }catch(e:Exception){temporaryFailure=true}
  }
  return if(temporaryFailure)Result.retry() else Result.success()
 }

 private fun notifyChanged(item:CaseResponse){
  val manager=applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)as NotificationManager
  manager.createNotificationChannel(NotificationChannel("case_updates","Case updates",NotificationManager.IMPORTANCE_HIGH))
  if(android.os.Build.VERSION.SDK_INT>=33&&applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
  val text="New hearing: "+(item.nextHearingDate?:"Date unavailable")+" • "+item.caseTitle
  val notification=NotificationCompat.Builder(applicationContext,"case_updates")
   .setSmallIcon(android.R.drawable.ic_dialog_info)
   .setContentTitle("Hearing date changed")
   .setContentText(text)
   .setStyle(NotificationCompat.BigTextStyle().bigText(text+" • CNR "+item.cnr))
   .setPriority(NotificationCompat.PRIORITY_HIGH)
   .setAutoCancel(true)
   .build()
  NotificationManagerCompat.from(applicationContext).notify(("update-"+item.cnr).hashCode(),notification)
 }
}
