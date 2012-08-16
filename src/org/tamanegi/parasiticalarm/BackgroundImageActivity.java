package org.tamanegi.parasiticalarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;

public abstract class BackgroundImageActivity extends FragmentActivity
{
    private static final int INTERVAL = 1000 * 10;

    private static List<Uri> backgrounds = null;
    private static int backgroundIndex = 0;

    private ImageView bgImage;
    private Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        handler = new Handler(backgroundCallback);

        if(backgrounds == null) {
            backgrounds = new ArrayList<Uri>();
            for(AlarmData data : AlarmData.getAllAvailableAlarmData(this)) {
                if(data.getBackground() == null) {
                    continue;
                }

                for(Uri uri : data.getBackground()) {
                    backgrounds.add(uri);
                }
            }
            Collections.shuffle(backgrounds);
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        bgImage = (ImageView)findViewById(R.id.background_image);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(backgrounds.size() != 0) {
            updateBackground();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        handler.removeMessages(0);
    }

    private Handler.Callback backgroundCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                updateBackground();
                return true;
            }
        };

    private void updateBackground()
    {
        bgImage.setImageURI(backgrounds.get(backgroundIndex));
        backgroundIndex += 1;
        backgroundIndex %= backgrounds.size();

        handler.removeMessages(0);
        handler.sendEmptyMessageDelayed(0, INTERVAL);
    }
}
