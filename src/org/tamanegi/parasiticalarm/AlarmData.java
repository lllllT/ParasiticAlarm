package org.tamanegi.parasiticalarm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;

public abstract class AlarmData
{
    private static List<AlarmData> alarmDataCache = null;
    private static Map<String, AlarmData> alarmMapCache;

    static {
        alarmMapCache = new HashMap<String, AlarmData>();
    }

    protected String name;
    protected Uri icon;
    protected Uri[] alert_audio;
    protected Uri[] alert_image;
    protected String alert_message;
    protected Uri[] after_audio;
    protected Uri[] after_image;
    protected Uri[] background;

    public String getName()
    {
        return name;
    }

    public Uri getIcon()
    {
        return icon;
    }

    public Uri[] getAlertAudio()
    {
        return alert_audio;
    }

    public Uri[] getAlertImage()
    {
        return alert_image;
    }

    public String getAlertMessage()
    {
        return alert_message;
    }

    public Uri[] getAfterAudio()
    {
        return after_audio;
    }

    public Uri[] getAfterImage()
    {
        return after_image;
    }

    public Uri[] getBackground()
    {
        return background;
    }

    public abstract String flattenToString();

    public static AlarmData unflattenFromString(Context context, String str)
    {
        if(alarmMapCache.containsKey(str)) {
            return alarmMapCache.get(str);
        }

        AlarmData data = unflattenFromStringNoCached(context, str);
        if(data != null) {
            alarmMapCache.put(str, data);
        }

        return data;
    }

    public static AlarmData unflattenFromStringNoCached(
        Context context, String str)
    {
        String[] params = str.split("/");

        try {
            String packageName = params[0];

            if(BiglobeAlarmData.PACKAGE_NAME_PATTERN
               .matcher(packageName).matches()) {
                return new BiglobeAlarmData(
                    context, packageName, Integer.parseInt(params[1]));
            }
            if(packageName.equals(KmbAlarmData.PACKAGE_NAME)) {
                return new KmbAlarmData(context, Integer.parseInt(params[1]));
            }
            if(packageName.equals(SankareaAlarmData.PACKAGE_NAME)) {
                return new SankareaAlarmData(context);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void clearCache()
    {
        alarmDataCache = null;
        alarmMapCache.clear();
    }

    public static List<AlarmData> getAllAvailableAlarmData(Context context)
    {
        if(alarmDataCache != null) {
            return alarmDataCache;
        }

        List<AlarmData> list = new ArrayList<AlarmData>();

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        Collections.sort(apps, new ApplicationInfo.DisplayNameComparator(pm));

        for(ApplicationInfo app : apps) {
            AlarmData[] data = getAvailableAlarmData(context, app.packageName);
            if(data == null) {
                continue;
            }

            for(AlarmData d : data) {
                list.add(d);
                alarmMapCache.put(d.flattenToString(), d);
            }
        }

        alarmDataCache = list;
        return list;
    }

    private static AlarmData[] getAvailableAlarmData(
        Context context, String packageName)
    {
        try {
            if(BiglobeAlarmData.PACKAGE_NAME_PATTERN
               .matcher(packageName).matches()) {
                ArrayList<AlarmData> list = new ArrayList<AlarmData>();
                int idx = 0;
                try {
                    while(true) {
                        list.add(
                            new BiglobeAlarmData(context, packageName, idx));
                        idx += 1;
                    }
                }
                catch(AlarmDataNotFoundException e) {
                    // ignore
                }

                return list.toArray(new AlarmData[list.size()]);
            }
            if(packageName.equals(KmbAlarmData.PACKAGE_NAME)) {
                return new AlarmData[] {
                    new KmbAlarmData(context, 0),
                    new KmbAlarmData(context, 1),
                    new KmbAlarmData(context, 2),
                };
            }
            if(packageName.equals(SankareaAlarmData.PACKAGE_NAME)) {
                return new AlarmData[] {
                    new SankareaAlarmData(context),
                };
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @SuppressWarnings("serial")
    private static class AlarmDataNotFoundException extends RuntimeException
    {
        private AlarmDataNotFoundException()
        {
        }

        private AlarmDataNotFoundException(String msg)
        {
            super(msg);
        }

        private AlarmDataNotFoundException(String msg, Throwable cause)
        {
            super(msg, cause);
        }

        private AlarmDataNotFoundException(Throwable cause)
        {
            super(cause);
        }
    }

    private static class BiglobeAlarmData extends AlarmData
    {
        private static final Pattern PACKAGE_NAME_PATTERN =
            Pattern.compile("^jp\\.ne\\.biglobe\\..*");

        private String packageName;
        private int idx;

        public BiglobeAlarmData(Context context, String packageName, int idx)
        {
            this.packageName = packageName;
            this.idx = idx;

            PackageManager pm = context.getPackageManager();
            AssetManager am;
            CharSequence appname;
            try {
                am = pm.getResourcesForApplication(packageName).getAssets();
                appname = pm.getApplicationLabel(
                    pm.getApplicationInfo(packageName, 0));
            }
            catch(PackageManager.NameNotFoundException ex) {
                throw new IllegalStateException(ex);
            }

            String[] message;
            Map<String, String[]> relation;
            try {
                message = readMessageCsv(am, idx);
                relation = readVoiceRelation(am);
            }
            catch(IOException ex) {
                throw new IllegalStateException(ex);
            }

            // name
            name = appname + " - " + message[11];

            // icon
            icon = buildResourceUri(packageName, "drawable/disptypeicon" + idx);

            // alert_audio, alert_image, after_image
            {
                ArrayList<Uri> audio = new ArrayList<Uri>();
                ArrayList<Uri> image = new ArrayList<Uri>();
                HashSet<String> names = new HashSet<String>();

                for(int i = 0; i < 6; i++) {
                    String[] rel = relation.get(message[i]);
                    if(rel == null) {
                        throw new AlarmDataNotFoundException(
                            "picture-audio relation not found");
                    }

                    for(int j = 1; j < rel.length; j++) {
                        String name = rel[j];
                        if(names.contains(name)) {
                            continue;
                        }

                        names.add(name);
                        audio.add(buildAssetUri(packageName, "/voice/" + name));
                    }

                    image.add(
                        buildAssetUri(packageName, "/data/" + message[i]));
                }
                image.add(buildAssetUri(packageName, "/data/" + message[6]));

                alert_audio = audio.toArray(new Uri[audio.size()]);
                alert_image = image.toArray(new Uri[image.size()]);
                after_image = image.toArray(new Uri[image.size()]);
            }

            // alert_message
            alert_message = message[7];

            // after_audio
            {
                ArrayList<Uri> audio = new ArrayList<Uri>();

                for(int i = 8; i < 11; i++) {
                    audio.add(
                        buildAssetUri(packageName, "/voice/" + message[i]));
                }

                after_audio = audio.toArray(new Uri[audio.size()]);
            }

            // background
            background = new Uri[] {
                buildResourceUri(packageName, "drawable/backgroundmenue") };
        }

        @Override
        public String flattenToString()
        {
            return packageName + "/" + idx;
        }

        private String[] readMessageCsv(AssetManager am, int idx)
            throws IOException
        {
            BufferedReader buf =
                new BufferedReader(
                    new InputStreamReader(am.open("csv/message.csv"),
                                          "utf-8"));

            for(int i = 0; i < idx; i++) {
                if(buf.readLine() == null) {
                    throw new AlarmDataNotFoundException(
                        "Specified index not found in message.csv: " +
                        packageName + ", " + idx);
                }
            }

            String line = buf.readLine();
            if(line == null) {
                throw new AlarmDataNotFoundException(
                    "Specified index not found in message.csv: " +
                    packageName + ", " + idx);
            }

            String[] elems = line.split(",");
            if(elems.length != 12) {
                throw new IllegalStateException(
                    "Unknown format message.csv: " +
                    packageName + ", " + idx);
            }

            return elems;
        }

        private Map<String, String[]> readVoiceRelation(AssetManager am)
            throws IOException
        {
            BufferedReader buf =
                new BufferedReader(
                    new InputStreamReader(am.open("csv/voicerelation_data.csv"),
                                          "utf-8"));

            Map<String, String[]> rel = new HashMap<String, String[]>();
            while(true) {
                String line = buf.readLine();
                if(line == null) {
                    break;
                }

                String[] vals = line.split(",");
                if(vals.length < 2) {
                    continue;
                }

                rel.put(vals[0], vals);
            }

            return rel;
        }
    }

    private static class KmbAlarmData extends AlarmData
    {
        private static final String PACKAGE_NAME =
            "jp.co.tbs.killmebabyalarm.android";

        private static final String[] NAMES = {
            "やすなボイス", "ソーニャボイス", "やすな&ソーニャボイス" };
        private static final String[][] AUDIOS = {
            { "akasaki_1", "akasaki_2", "akasaki_3",
              "akasaki_4", "akasaki_5", "akasaki_6",
              "akasaki_7", "akasaki_8", "akasaki_11",
              "akasaki_12", "akasaki_13", "akasaki_14" },
            { "tamura_1", "tamura_2", "tamura_3",
              "tamura_4", "tamura_5", "tamura_6",
              "tamura_7", "tamura_8", "tamura_9",
              "tamura_10", "tamura_13", "tamura_14" },
            { "kakeai_1", "kakeai_3", "kakeai_4",
              "kakeai_5", "kakeai_6", "kakeai_9" }
        };

        private int idx;

        public KmbAlarmData(Context context, int idx)
        {
            this.idx = idx;

            PackageManager pm = context.getPackageManager();
            CharSequence appname;
            try {
                appname = pm.getApplicationLabel(
                    pm.getApplicationInfo(PACKAGE_NAME, 0));
            }
            catch(PackageManager.NameNotFoundException ex) {
                throw new IllegalStateException(ex);
            }

            // name
            name = appname + " - " + NAMES[idx];

            // icon
            icon = buildResourceUri(PACKAGE_NAME, "drawable/icon");

            // alert_audio
            alert_audio = new Uri[AUDIOS[idx].length];
            for(int i = 0; i < AUDIOS[idx].length; i++) {
                alert_audio[i] =
                    buildResourceUri(PACKAGE_NAME, "raw/" + AUDIOS[idx][i]);
            }

            // alert_image
            alert_image = new Uri[] {
                buildResourceUri(PACKAGE_NAME, "drawable/snooze_char"),
                buildResourceUri(PACKAGE_NAME, "drawable/title"),
            };

            // alert_message
            alert_message = null;

            // after_audio
            after_audio = null;

            // after_image
            after_image = alert_image;

            // background
            background = new Uri[] {
                buildResourceUri(PACKAGE_NAME, "drawable/splash") };
        }

        @Override
        public String flattenToString()
        {
            return PACKAGE_NAME + "/" + idx;
        }
    }

    private static class SankareaAlarmData extends AlarmData
    {
        private static final String PACKAGE_NAME =
            "jp.co.tbs.sankareaalarm.android";

        private static final String AUDIONAME_FORMAT = "alarm%d";
        private static final int AUDIO_COUNT = 14;

        public SankareaAlarmData(Context context)
        {
            PackageManager pm = context.getPackageManager();
            CharSequence appname;
            try {
                appname = pm.getApplicationLabel(
                    pm.getApplicationInfo(PACKAGE_NAME, 0));
            }
            catch(PackageManager.NameNotFoundException ex) {
                throw new IllegalStateException(ex);
            }

            // name
            name = appname.toString();

            // icon
            icon = buildResourceUri(PACKAGE_NAME, "drawable/icon");

            // alert_audio
            alert_audio = new Uri[AUDIO_COUNT];
            for(int i = 0; i < AUDIO_COUNT; i++) {
                alert_audio[i] =
                    buildResourceUri(
                        PACKAGE_NAME,
                        "raw/" + String.format(AUDIONAME_FORMAT, i));
            }

            // alert_image
            alert_image = new Uri[] {
                buildResourceUri(PACKAGE_NAME, "drawable/snooze_bg_a"),
                buildResourceUri(PACKAGE_NAME, "drawable/snooze_bg_b"),
                buildResourceUri(PACKAGE_NAME, "drawable/snooze_bg_c"),
                buildResourceUri(PACKAGE_NAME, "drawable/splash_bg"),
            };

            // alert_message
            alert_message = null;

            // after_audio
            after_audio = null;

            // after_image
            after_image = alert_image;

            // background
            background = new Uri[] {
                buildResourceUri(PACKAGE_NAME, "drawable/alarmlist_bg"),
                buildResourceUri(PACKAGE_NAME, "drawable/alarmsetting_bg"),
            };
        }

        @Override
        public String flattenToString()
        {
            return PACKAGE_NAME;
        }
    }

    private static Uri buildAssetUri(String packageName, String path)
    {
        return new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AssetProxyProvider.AUTHORITY)
            .appendEncodedPath(packageName + path)
            .build();
    }

    private static Uri buildResourceUri(String packageName, String path)
    {
        return new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(ResourceProxyProvider.AUTHORITY)
            .appendEncodedPath(packageName + "/" + path)
            .build();
    }
}
