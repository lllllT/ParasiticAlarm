package org.tamanegi.parasiticalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InitReceiver extends BroadcastReceiver
{
    public static final String ALARM_SCHEME = "alarm";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
           Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) ||
           Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction()) ||
           Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction()) ||
           (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction()) &&
            intent.getData() != null &&
            context.getPackageName().equals(
                intent.getData().getEncodedSchemeSpecificPart()))) {
            Intent service_intent = new Intent(context, AlarmService.class)
                .setAction(AlarmService.ACTION_SETUP);
            context.startService(service_intent);
        }
    }
}
