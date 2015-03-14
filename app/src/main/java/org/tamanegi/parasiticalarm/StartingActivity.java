package org.tamanegi.parasiticalarm;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;

public class StartingActivity extends FragmentActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_starting);

        getSupportLoaderManager().initLoader(0, null, cacheLoader);
    }

    private void startNextActivity()
    {
        Intent intent =
            new Intent(StartingActivity.this, AlarmListActivity.class);
        startActivity(intent);
        finish();
    }

    private LoaderManager.LoaderCallbacks<List<AlarmData>> cacheLoader =
        new LoaderManager.LoaderCallbacks<List<AlarmData>>() {
            @Override
            public Loader<List<AlarmData>> onCreateLoader(int id, Bundle args)
            {
                return new AsyncCacheLoader(StartingActivity.this);
            }

            @Override
            public void onLoadFinished(
                Loader<List<AlarmData>> loader, List<AlarmData> data)
            {
                startNextActivity();
            }

            @Override
            public void onLoaderReset(Loader<List<AlarmData>> loader)
            {
                // do nothing
            }
    };

    private static class AsyncCacheLoader
        extends AsyncTaskLoader<List<AlarmData>>
    {
        private AsyncCacheLoader(Context context)
        {
            super(context);
        }

        @Override
        protected void onStartLoading()
        {
            forceLoad();
        }

        @Override
        public List<AlarmData> loadInBackground()
        {
            int savedPriority = Process.getThreadPriority(Process.myTid());
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            AlarmData.clearCache();
            List<AlarmData> ret =
                AlarmData.getAllAvailableAlarmData(getContext());

            Process.setThreadPriority(savedPriority);

            return ret;
        }
    }
}
