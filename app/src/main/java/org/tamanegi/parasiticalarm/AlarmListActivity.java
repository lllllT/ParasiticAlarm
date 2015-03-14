package org.tamanegi.parasiticalarm;

import android.content.Intent;
import android.os.Bundle;

public class AlarmListActivity
    extends BackgroundImageActivity implements AlarmListFragment.Callbacks
{
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alarm_list);

        if(findViewById(R.id.alarm_detail_container) != null) {
            mTwoPane = true;
            AlarmListFragment f = (AlarmListFragment)
                getSupportFragmentManager().findFragmentById(R.id.alarm_list);
            f.setActivateOnItemClick(true);
            f.setActivatedPosition(0);
            onItemSelected(0);
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
