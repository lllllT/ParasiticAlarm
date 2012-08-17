package org.tamanegi.parasiticalarm;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;

public abstract class BackgroundImageActivity extends FragmentActivity
{
    private static final int INTERVAL = 1000 * 10;
    private static final int TRANSITION = 500;

    private static List<Uri> backgrounds = null;
    private static int backgroundIndex = 0;

    private TransitionDrawable bgDrawable = null;
    private Drawable lastDrawable;
    private ImageView bgImage;
    private Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        lastDrawable = getResources().getDrawable(R.drawable.empty_background);
        handler = new Handler(backgroundCallback);

        if(backgrounds == null) {
            HashSet<Uri> uriSet = new HashSet<Uri>();
            for(AlarmData data : AlarmData.getAllAvailableAlarmData(this)) {
                if(data.getBackground() == null) {
                    continue;
                }

                for(Uri uri : data.getBackground()) {
                    uriSet.add(uri);
                }
            }

            backgrounds = new ArrayList<Uri>(uriSet);
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
        Uri uri = backgrounds.get(backgroundIndex);
        backgroundIndex += 1;
        backgroundIndex %= backgrounds.size();

        Drawable d;
        try {
            d = Drawable.createFromStream(
                getContentResolver().openInputStream(uri),
                uri.toString());
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        Drawable[] drawables = new Drawable[2];
        drawables[0] = lastDrawable;
        drawables[1] = d;
        bgDrawable = new TransitionDrawable(drawables);

        bgDrawable.setId(0, 0);
        bgDrawable.setId(1, 1);
        bgImage.setImageDrawable(bgDrawable);

        bgDrawable.startTransition(TRANSITION);
        lastDrawable = d;

        handler.removeMessages(0);
        handler.sendEmptyMessageDelayed(0, INTERVAL);
    }
}
