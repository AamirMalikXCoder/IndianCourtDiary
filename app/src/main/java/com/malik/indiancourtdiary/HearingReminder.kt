package com.malik.indiancourtdiary

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object HearingReminderScheduler {
    fun schedule(context: Context, cnr: String, title: String, hearingDate: String?) {
        if (hearingDate.isNullOrBlank()) return
        val date = runCatching { LocalDate.parse(hearingDate.take(10)) }.getOrNull() ?: return
        val days = AppPreferences.reminderDays(context).toLong()
        val hour = AppPreferences.reminderHour(context).coerceIn(0, 23)
        val reminderAt = LocalDateTime.of(date.minusDays(days), LocalTime.of(hour, 0))
        val delay = Duration.between(LocalDateTime.now(), reminderAt).toMillis()
        if (delay <= 0) return

        val data = Data.Builder()
            .putString("cnr", cnr)
            .putString("title", title)
            .putString("hearingDate", hearingDate)
            .build()

        val request = OneTimeWorkRequestBuilder<HearingReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "hearing-reminder-$cnr",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, cnr: String) {
        WorkManager.getInstance(context).cancelUniqueWork("hearing-reminder-$cnr")
    }
}

class HearingReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val cnr = inputData.getString("cnr") ?: return Result.failure()
        val title = inputData.getString("title") ?: "Court case"
        val date = inputData.getString("hearingDate") ?: "tomorrow"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel("hearing_reminders", "Hearing reminders", NotificationManager.IMPORTANCE_HIGH)
        )

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val notification = NotificationCompat.Builder(applicationContext, "hearing_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Court hearing reminder")
            .setContentText("$title • $date • CNR $cnr")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title is listed for hearing on $date. CNR: $cnr"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(cnr.hashCode(), notification)
        return Result.success()
    }
}
