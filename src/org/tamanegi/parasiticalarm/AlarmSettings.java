package org.tamanegi.parasiticalarm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.text.format.DateFormat;

public class AlarmSettings
{
    private static final String PREFERENCES_NAME = "alarms";
    private static final String PREF_PREFIX = "alarm%d.";
    private static final Pattern PREF_PATTERN =
        Pattern.compile("^alarm(\\d+)\\.(.*)$");

    public static final int SETTING_COUNT = 8;

    public static final String PREF_ID = "id";
    public static final String PREF_ONOFF = "onoff";
    public static final String PREF_DAY = "day";
    public static final String PREF_TIME = "time";
    public static final String PREF_TIME_HOUR = "time.hour";
    public static final String PREF_TIME_MINUTE = "time.minute";
    public static final String PREF_ALARMS = "alarms";
    public static final String PREF_VOLUME = "volume";
    public static final String PREF_SNOOZE_ENABLED = "snooze.enabled";
    public static final String PREF_SNOOZE_INTERVAL = "snooze.interval";
    public static final String PREF_SNOOZE_TIMEOUT = "snooze.timeout";
    public static final String PREF_VIBRATION = "vibration";

    public enum DayOfWeek
    {
        MONDAY(R.string.day_of_week_abbr_mon,
               R.string.day_of_week_full_mon, Calendar.MONDAY),
        TUESDAY(R.string.day_of_week_abbr_tue,
                R.string.day_of_week_full_tue, Calendar.TUESDAY),
        WEDNESDAY(R.string.day_of_week_abbr_wed,
                  R.string.day_of_week_full_wed, Calendar.WEDNESDAY),
        THURSDAY(R.string.day_of_week_abbr_thu,
                 R.string.day_of_week_full_thu, Calendar.THURSDAY),
        FRIDAY(R.string.day_of_week_abbr_fri,
               R.string.day_of_week_full_fri, Calendar.FRIDAY),
        SATURDAY(R.string.day_of_week_abbr_sat,
                 R.string.day_of_week_full_sat, Calendar.SATURDAY),
        SUNDAY(R.string.day_of_week_abbr_sun,
               R.string.day_of_week_full_sun, Calendar.SUNDAY);

        private final int abbrId;
        private final int fullId;
        private final int calVal;

        private DayOfWeek(int abbrId, int fullId, int calVal)
        {
            this.abbrId = abbrId;
            this.fullId = fullId;
            this.calVal = calVal;
        }

        public String getAbbrName(Context context)
        {
            return context.getString(abbrId);
        }

        public String getFullName(Context context)
        {
            return context.getString(fullId);
        }

        public int getCalendarValue()
        {
            return calVal;
        }
    }

    public static interface OnSettingChangeListener
    {
        public void onSettingChanged(
            AlarmSettings setting, int index, String key);
    }

    private Context context;
    private List<? extends Map<String, Object>> list;
    private SharedPreferences pref;

    private Handler handler;
    private OnSettingChangeListener changeListener = null;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences pref,
                                                  String key) {
                Matcher matcher = PREF_PATTERN.matcher(key);
                if(! matcher.matches()) {
                    return;
                }

                final String subkey = matcher.group(2);
                int index;
                try {
                    index = Integer.parseInt(matcher.group(1));
                }
                catch(NumberFormatException e) {
                    return;
                }

                fillInSetting(index);
                if(changeListener != null) {
                    final int idx = index;
                    handler.post(new Runnable() {
                            @Override
                            public void run() {
                                changeListener.onSettingChanged(
                                    AlarmSettings.this, idx, subkey);
                            }
                        });
                }
            }
        };

    private Method preferencesApply = null;

    public AlarmSettings(Context context)
    {
        this.context = context;
        ArrayList<HashMap<String, Object>> list =
            new ArrayList<HashMap<String, Object>>(SETTING_COUNT);

        for(int i = 0; i < SETTING_COUNT; i++) {
            list.add(new HashMap<String, Object>());
        }
        this.list = list;

        pref = context.getSharedPreferences(PREFERENCES_NAME, 0);
        for(int i = 0; i < SETTING_COUNT; i++) {
            fillInSetting(i);
        }

        handler = new Handler();
        pref.registerOnSharedPreferenceChangeListener(prefListener);
    }

    public void setOnSettingChangeListener(OnSettingChangeListener listener)
    {
        changeListener = listener;
    }

    private void fillInSetting(int index)
    {
        Map<String, Object> setting = list.get(index);
        String prefix = getPrefix(index);

        // id
        setting.put(PREF_ID, index);

        // on-off
        setting.put(PREF_ONOFF, pref.getBoolean(prefix + PREF_ONOFF, false));

        // day
        String daystr = pref.getString(prefix + PREF_DAY, "");
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for(String day : daystr.split(",")) {
            try {
                days.add(DayOfWeek.valueOf(day));
            }
            catch(IllegalArgumentException e) {
                // ignore
            }
        }
        setting.put(PREF_DAY, days);

        // time
        {
            int hour = pref.getInt(prefix + PREF_TIME_HOUR, 0);
            int minute = pref.getInt(prefix + PREF_TIME_MINUTE, 0);
            setting.put(PREF_TIME_HOUR, hour);
            setting.put(PREF_TIME_MINUTE, minute);
            setting.put(PREF_TIME, formatTime(hour, minute));
        }

        // alarms
        {
            String alarmstr = pref.getString(prefix + PREF_ALARMS, "");
            String [] alarms = (alarmstr.contains(",") ?
                                alarmstr.split(",") : new String[0]);
            setting.put(PREF_ALARMS, alarms);
        }

        // volume
        setting.put(PREF_VOLUME, pref.getInt(prefix + PREF_VOLUME, -1));

        // snooze
        setting.put(PREF_SNOOZE_ENABLED,
                    pref.getBoolean(prefix + PREF_SNOOZE_ENABLED, true));
        setting.put(PREF_SNOOZE_INTERVAL,
                    pref.getInt(prefix + PREF_SNOOZE_INTERVAL, 5));
        setting.put(PREF_SNOOZE_TIMEOUT,
                    pref.getInt(prefix + PREF_SNOOZE_TIMEOUT, 30));

        // vibration
        setting.put(PREF_VIBRATION,
                    pref.getBoolean(prefix + PREF_VIBRATION, true));
    }

    private String formatTime(int hour, int minute)
    {
        Date time = new Date();
        time.setHours(hour);
        time.setMinutes(minute);
        return DateFormat.getTimeFormat(context).format(time);
    }

    public List<? extends Map<String, Object>> getList()
    {
        return list;
    }

    public int getId(int index)
    {
        return (Integer)list.get(index).get(PREF_ID);
    }

    public boolean getOnOff(int index)
    {
        return (Boolean)list.get(index).get(PREF_ONOFF);
    }

    public void setOnOff(int index, boolean val)
    {
        list.get(index).put(PREF_ONOFF, val);
        apply(pref.edit().putBoolean(getPrefix(index) + PREF_ONOFF, val));
    }

    @SuppressWarnings("unchecked")
    public EnumSet<DayOfWeek> getDay(int index)
    {
        return (EnumSet<DayOfWeek>)list.get(index).get(PREF_DAY);
    }

    public CharSequence getDayText(Context context, int index)
    {
        return getDayText(context, getDay(index));
    }

    public static CharSequence getDayText(
        Context context, EnumSet<AlarmSettings.DayOfWeek> days)
    {
        if(days.isEmpty()) {
            return context.getString(R.string.day_once);
        }
        else {
            String sep = context.getString(R.string.day_of_week_separator);
            StringBuilder sb = new StringBuilder();

            for(AlarmSettings.DayOfWeek day : days) {
                if(sb.length() != 0) {
                    sb.append(sep);
                }

                sb.append(day.getAbbrName(context));
            }

            return sb;
        }
    }

    public void setDay(int index, EnumSet<DayOfWeek> val)
    {
        list.get(index).put(PREF_DAY, val);

        StringBuilder sb = new StringBuilder();
        for(DayOfWeek day : val) {
            if(sb.length() != 0) {
                sb.append(",");
            }

            sb.append(day);
        }
        apply(pref.edit().putString(getPrefix(index) + PREF_DAY,
                                    sb.toString()));
    }

    public String getTime(int index)
    {
        return (String)list.get(index).get(PREF_TIME);
    }

    public int getTimeHour(int index)
    {
        return (Integer)list.get(index).get(PREF_TIME_HOUR);
    }

    public int getTimeMinute(int index)
    {
        return (Integer)list.get(index).get(PREF_TIME_MINUTE);
    }

    public void setTime(int index, int hour, int minute)
    {
        Map<String, Object> setting = list.get(index);
        setting.put(PREF_TIME_HOUR, hour);
        setting.put(PREF_TIME_MINUTE, minute);
        setting.put(PREF_TIME, formatTime(hour, minute));

        apply(pref.edit()
              .putInt(getPrefix(index) + PREF_TIME_HOUR, hour)
              .putInt(getPrefix(index) + PREF_TIME_MINUTE, minute));
    }

    public void setTime(int index, String val)
    {
        list.get(index).put(PREF_TIME, val);
        apply(pref.edit().putString(getPrefix(index) + PREF_TIME, val));
    }

    public String[] getAlarms(int index)
    {
        return (String[])list.get(index).get(PREF_ALARMS);
    }

    public void setAlarms(int index, String[] val)
    {
        list.get(index).put(PREF_ALARMS, val);

        StringBuilder sb = new StringBuilder();
        for(String alarm : val) {
            if(sb.length() != 0) {
                sb.append(",");
            }

            sb.append(alarm);
        }
        apply(pref.edit().putString(getPrefix(index) + PREF_ALARMS,
                                    sb.toString()));
    }

    public int getVolume(int index)
    {
        return (Integer)list.get(index).get(PREF_VOLUME);
    }

    public String getVolumeString(Context context, int index)
    {
        int val = getVolume(index);
        if(val < 0) {
            return context.getString(R.string.volume_use_system);
        }
        else {
            return context.getString(R.string.pref_summary_volume, val);
        }
    }

    public void setVolume(int index, int val)
    {
        list.get(index).put(PREF_VOLUME, val);
        apply(pref.edit().putInt(getPrefix(index) + PREF_VOLUME, val));
    }

    public boolean isSnoozeEnabled(int index)
    {
        return (Boolean)list.get(index).get(PREF_SNOOZE_ENABLED);
    }

    public void setSnoozeEnabled(int index, boolean val)
    {
        list.get(index).put(PREF_SNOOZE_ENABLED, val);
        apply(pref.edit().putBoolean(getPrefix(index) + PREF_SNOOZE_ENABLED,
                                     val));
    }

    public int getSnoozeInterval(int index)
    {
        return (Integer)list.get(index).get(PREF_SNOOZE_INTERVAL);
    }

    public String getSnoozeIntervalString(Context context, int index)
    {
        return context.getResources().getQuantityString(
            R.plurals.pref_summary_snooze_interval,
            getSnoozeInterval(index),
            getSnoozeInterval(index));
    }

    public void setSnoozeInterval(int index, int val)
    {
        list.get(index).put(PREF_SNOOZE_INTERVAL, val);
        apply(pref.edit().putInt(getPrefix(index) + PREF_SNOOZE_INTERVAL, val));
    }

    public int getSnoozeTimeout(int index)
    {
        return (Integer)list.get(index).get(PREF_SNOOZE_TIMEOUT);
    }

    public String getSnoozeTimeoutString(Context context, int index)
    {
        return context.getResources().getQuantityString(
            R.plurals.pref_summary_snooze_timeout,
            getSnoozeTimeout(index),
            getSnoozeTimeout(index));
    }

    public void setSnoozeTimeout(int index, int val)
    {
        list.get(index).put(PREF_SNOOZE_TIMEOUT, val);
        apply(pref.edit().putInt(getPrefix(index) + PREF_SNOOZE_TIMEOUT, val));
    }

    public boolean getVibration(int index)
    {
        return (Boolean)list.get(index).get(PREF_VIBRATION);
    }

    public void setVibration(int index, boolean val)
    {
        list.get(index).put(PREF_VIBRATION, val);
        apply(pref.edit().putBoolean(getPrefix(index) + PREF_VIBRATION, val));
    }

    private String getPrefix(int index)
    {
        return String.format(PREF_PREFIX, index);
    }

    private void apply(SharedPreferences.Editor editor)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            try {
                if(preferencesApply == null) {
                    preferencesApply =
                        SharedPreferences.Editor.class.getMethod("apply");
                }
                preferencesApply.invoke(editor);
                return;
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }

        editor.commit();
    }
}
