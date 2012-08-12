package org.tamanegi.parasiticalarm;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class AlarmDetailActivity extends FragmentActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alarm_detail);

        if(savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putString(
                AlarmDetailFragment.ARG_ITEM_ID,
                getIntent().getStringExtra(AlarmDetailFragment.ARG_ITEM_ID));

            AlarmDetailFragment fragment = new AlarmDetailFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                .add(R.id.alarm_detail_container, fragment)
                .commit();
        }
    }
}
