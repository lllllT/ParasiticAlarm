package org.tamanegi.parasiticalarm;

import android.content.Context;
import android.os.PowerManager;

class AlertWakeLock
{
    private static PowerManager.WakeLock wakelock = null;

    static synchronized void acquire(Context context)
    {
        if(wakelock == null) {
            PowerManager pm = (PowerManager)context.getApplicationContext()
                .getSystemService(Context.POWER_SERVICE);
            wakelock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "AlertWakeLock");
        }

        wakelock.acquire();
    }

    static void release()
    {
        if(wakelock == null) {
            return;
        }

        wakelock.release();
    }
}
