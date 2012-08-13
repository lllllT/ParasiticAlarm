package org.tamanegi.parasiticalarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class AlertActivity extends Activity
{
    private static final long[] VIBRATE_PATTERN = { 500, 500 };

    private static final int AUDIO_MSGID = 1;
    private static final long AUDIO_INTERVAL = 1000;

    private Vibrator vibrator;
    private MediaPlayer mediaplayer;
    private Handler handler;

    private boolean isInAlert;
    private boolean isAfter;

    private int alarmId;
    private boolean snoozeEnabled;
    private List<Uri> alertAudio;
    private int alertAudioCur;
    private Uri afterAudio;
    private Uri afterImage;
    private boolean vibrationEnabled;

    private ImageView image;
    private TextView messageText;
    private View alertArea;
    private View alertStopLine;
    private View alertDismissLine;
    private View alertSnooze;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);

        mediaplayer = new MediaPlayer();
        mediaplayer.setOnPreparedListener(onPlayerPreparedListener);
        mediaplayer.setOnCompletionListener(onPlayerCompletionListener);
        mediaplayer.setOnErrorListener(onPlayerErrorListener);

        handler = new Handler(delayedPlayCallback);

        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        setContentView(R.layout.activity_alert);
        image = (ImageView)findViewById(R.id.alert_image);
        messageText = (TextView)findViewById(R.id.alert_message);
        alertArea = findViewById(R.id.alert_area);
        alertStopLine = findViewById(R.id.alert_stop_button);
        alertDismissLine = findViewById(R.id.alert_dimiss_line);
        alertSnooze = findViewById(R.id.alert_snooze_button);

        findViewById(R.id.alert_stop_button)
            .setOnClickListener(onClickStopListener);
        findViewById(R.id.alert_dismiss_button)
            .setOnClickListener(onClickDismissListener);
        findViewById(R.id.alert_snooze_button)
            .setOnClickListener(onClickSnoozeListener);

        registerReceiver(screenOffReceiver,
                         new IntentFilter(Intent.ACTION_SCREEN_OFF));

        applyParameters(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        AlertWakeLock.release();     // acquired at AlarmService#alarm

        applyParameters(intent);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(isInAlert) {
            startAlert();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        handler.removeMessages(AUDIO_MSGID);
        mediaplayer.reset();

        if(vibrationEnabled) {
            vibrator.cancel();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        unregisterReceiver(screenOffReceiver);
        mediaplayer.release();

        AlertWakeLock.release();     // acquired at AlarmService#alarm
    }

    @Override
    public void onBackPressed()
    {
        if(isInAlert) {
            // do nothing: ignore back key
            return;
        }

        finish();
    }

    private void applyParameters(Intent intent)
    {
        isInAlert = true;
        isAfter = false;

        alarmId = intent.getIntExtra(AlarmService.EXTRA_ALARM_ID, -1);
        snoozeEnabled =
            intent.getBooleanExtra(AlarmService.EXTRA_SNOOZE_ENABLED, false);
        {
            Parcelable[] alertAudioArray = (Parcelable[])
                intent.getParcelableArrayExtra(AlarmService.EXTRA_ALERT_AUDIO);
            alertAudio = new ArrayList<Uri>();
            for(Parcelable uri : alertAudioArray) {
                alertAudio.add((Uri)uri);
            }

            Collections.shuffle(alertAudio);
            alertAudioCur = 0;
        }
        afterAudio = (Uri)
            intent.getParcelableExtra(AlarmService.EXTRA_AFTER_AUDIO);
        afterImage = (Uri)
            intent.getParcelableExtra(AlarmService.EXTRA_AFTER_IMAGE);
        Uri background = (Uri)
            intent.getParcelableExtra(AlarmService.EXTRA_BACKGROUND); // todo:
        vibrationEnabled =
            intent.getBooleanExtra(AlarmService.EXTRA_VIBRATION_ENABLED, false);

        Uri alertImage = (Uri)
            intent.getParcelableExtra(AlarmService.EXTRA_ALERT_IMAGE);
        image.setImageURI(alertImage);

        String message =
            intent.getStringExtra(AlarmService.EXTRA_ALERT_MESSAGE);
        if(message != null) {
            messageText.setText(message);
        }
        else {
            messageText.setVisibility(View.GONE);
        }
    }

    private void startAlert()
    {
        isInAlert = true;
        isAfter = false;

        alertArea.setVisibility(View.VISIBLE);
        alertStopLine.setVisibility(View.VISIBLE);
        alertDismissLine.setVisibility(View.GONE);

        // todo: set volume
        playAudio(getNextAlertAudio());

        if(vibrationEnabled) {
            vibrator.vibrate(VIBRATE_PATTERN, 0);
        }

        // todo: set stop timer
    }

    private void stopAlert()
    {
        isInAlert = false;

        alertArea.setVisibility(View.VISIBLE);
        alertStopLine.setVisibility(View.GONE);
        alertDismissLine.setVisibility(View.VISIBLE);
        alertSnooze.setEnabled(snoozeEnabled);

        handler.removeMessages(AUDIO_MSGID);
        mediaplayer.reset();

        if(vibrationEnabled) {
            vibrator.cancel();
        }

        if(! snoozeEnabled) {
            startAfter();
        }
    }

    private void cancelSnooze()
    {
        startService(new Intent(this, AlarmService.class)
                     .setAction(AlarmService.ACTION_CANCEL_SNOOZE)
                     .putExtra(AlarmService.EXTRA_ALARM_ID, alarmId));
        startAfter();
    }

    private void startAfter()
    {
        isAfter = true;
        alertArea.setVisibility(View.GONE);

        image.setImageURI(afterImage);
        playAudio(afterAudio);
    }

    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };

    private View.OnClickListener onClickStopListener =
        new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAlert();
            }
        };

    private View.OnClickListener onClickDismissListener =
        new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertArea.setVisibility(View.GONE);
            }
        };

    private View.OnClickListener onClickSnoozeListener =
        new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelSnooze();
            }
        };

    private Uri getNextAlertAudio()
    {
        Uri uri = alertAudio.get(alertAudioCur);
        alertAudioCur = (alertAudioCur + 1) % alertAudio.size();

        return uri;
    }

    private void playAudio(Uri uri)
    {
        mediaplayer.reset();

        if(uri == null) {
            return;
        }

        try {
            mediaplayer.setDataSource(this, uri);
            mediaplayer.prepareAsync();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private MediaPlayer.OnPreparedListener onPlayerPreparedListener =
        new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        };

    private MediaPlayer.OnCompletionListener onPlayerCompletionListener =
        new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(! isAfter) {
                    handler.sendMessageDelayed(
                        Message.obtain(
                            handler, AUDIO_MSGID, getNextAlertAudio()),
                        AUDIO_INTERVAL);
                }
                else {
                    mp.reset();
                }
            }
        };

    private MediaPlayer.OnErrorListener onPlayerErrorListener =
        new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mp.reset();
                return true;
            }
        };

    private Handler.Callback delayedPlayCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                playAudio((Uri)msg.obj);
                return true;
            }
        };
}
