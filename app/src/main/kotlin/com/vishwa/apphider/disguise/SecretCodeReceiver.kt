package com.vishwa.apphider.disguise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.vishwa.apphider.MainActivity

/** Opens the real App Hider when the dialer secret code is entered. */
class SecretCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_SECRET_CODE) return
        if (intent.data?.host != DisguiseManager.SECRET_CODE) return

        val open = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_SECRET_UNLOCK
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        }
        context.startActivity(open)
    }
}
