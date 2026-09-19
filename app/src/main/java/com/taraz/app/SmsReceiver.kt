package com.taraz.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return
        val bundle = intent.extras ?: return
        val pdus = bundle.getSerializable("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")
        val messages = pdus.mapNotNull { pdu ->
            try { SmsMessage.createFromPdu(pdu as ByteArray, format) } catch (_: Exception) { null }
        }
        if (messages.isEmpty()) return

        val sender = messages.first().displayOriginatingAddress.orEmpty()
        val body = messages.joinToString("") { it.displayMessageBody.orEmpty() }
        val account = Db(context).loadAccounts().firstOrNull {
            normalizeSender(it.senderNumber) == normalizeSender(sender)
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "bank_sms"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "پیامک‌های بانکی", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "اعلان دریافت پیامک از سرشماره‌های بانکی ثبت‌شده"
                }
            )
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_sms", true)
        }
        val pending = PendingIntent.getActivity(
            context,
            account.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snippet = body.replace("\\s+".toRegex(), " ").trim().take(120)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("پیامک بانکی جدید • " + account.bankName)
            .setContentText(snippet.ifBlank { "یک پیامک از سرشماره ثبت‌شده دریافت شد." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun normalizeSender(value: String): String {
        val digits = value
            .replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4')
            .replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9')
            .filter { it.isDigit() }
        return when {
            digits.startsWith("0098") -> digits.removePrefix("0098")
            digits.startsWith("98") && digits.length > 8 -> digits.removePrefix("98")
            digits.startsWith("0") && digits.length > 8 -> digits.removePrefix("0")
            else -> digits
        }
    }
}
