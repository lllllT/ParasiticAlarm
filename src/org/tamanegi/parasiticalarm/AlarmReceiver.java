package org.tamanegi.parasiticalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver
{
    public static final String ALARM_SCHEME = "alarm";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(AlarmService.ACTION_ALARM.equals(intent.getAction())) {
            Intent service_intent = new Intent(intent)
                .setClass(context, AlarmService.class);

            AlertWakeLock.acquire(context);
            context.startService(service_intent);
        }
    }
}
