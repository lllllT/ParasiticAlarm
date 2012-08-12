package org.tamanegi.parasiticalarm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class AlarmListActivity
    extends FragmentActivity implements AlarmListFragment.Callbacks
{
    private boolean mTwoPane;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alarm_list);

        if(findViewById(R.id.alarm_detail_container) != null) {
            mTwoPane = true;
            ((AlarmListFragment)getSupportFragmentManager().findFragmentById(
                R.id.alarm_list)).setActivateOnItemClick(true);
        }
    }

    @Override
    public void onItemSelected(int id)
    {
        if(mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putInt(AlarmDetailFragment.ARG_ITEM_ID, id);

            AlarmDetailFragment fragment = new AlarmDetailFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                .replace(R.id.alarm_detail_container, fragment)
                .commit();
        }
        else {
            Intent detailIntent = new Intent(this, AlarmDetailActivity.class);
            detailIntent.putExtra(AlarmDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }
}
