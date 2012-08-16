package org.tamanegi.parasiticalarm;

import java.util.EnumSet;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class AlarmListFragment extends ListFragment
{
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    private Callbacks mCallbacks = sDummyCallbacks;
    private int mActivatedPosition = ListView.INVALID_POSITION;

    private AlarmListAdapter adapter;
    private AlarmSettings settings;

    public interface Callbacks
    {
        public void onItemSelected(int id);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
            @Override
            public void onItemSelected(int id) {}
        };

    public AlarmListFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        settings = new AlarmSettings(getActivity());
        settings.setOnSettingChangeListener(settingChangeListener);

        adapter = new AlarmListAdapter();

        setListAdapter(adapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        getListView().setSelector(R.drawable.list_background_selector);
        getListView().setBackgroundResource(0);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        settings.setOnSettingChangeListener(null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        if(savedInstanceState != null &&
           savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(
                savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        if(!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                "Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks)activity;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView,
                                View view, int position, long id)
    {
        super.onListItemClick(listView, view, position, id);

        mCallbacks.onItemSelected(settings.getId(position));
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if(mActivatedPosition != ListView.INVALID_POSITION) {
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    public void setActivateOnItemClick(boolean activateOnItemClick)
    {
        getListView().setChoiceMode(
            activateOnItemClick ?
            ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
    }

    public void setActivatedPosition(int position)
    {
        if(position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        }
        else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    private AlarmSettings.OnSettingChangeListener settingChangeListener =
        new AlarmSettings.OnSettingChangeListener() {
            @Override
            public void onSettingChanged(
                AlarmSettings setting, int index, String key) {
                if(key.equals(AlarmSettings.PREF_ONOFF) ||
                   key.equals(AlarmSettings.PREF_TIME_HOUR) ||
                   key.equals(AlarmSettings.PREF_TIME_MINUTE) ||
                   key.equals(AlarmSettings.PREF_DAY)) {
                    adapter.notifyDataSetChanged();
                }
            }
        };

    private class AlarmListAdapter
        extends SimpleAdapter
        implements SimpleAdapter.ViewBinder, View.OnClickListener
    {
        private AlarmListAdapter()
        {
            super(getActivity(),
                  settings.getList(),
                  R.layout.list_alarm_item,
                  new String[] {
                      AlarmSettings.PREF_ONOFF,
                      AlarmSettings.PREF_TIME,
                      AlarmSettings.PREF_DAY,
                  },
                  new int[] {
                      R.id.alarm_onoff,
                      R.id.alarm_time,
                      R.id.alarm_day,
                  });
            setViewBinder(this);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View view = super.getView(position, convertView, parent);

            View viewOnOff = view.findViewById(R.id.alarm_onoff);
            viewOnOff.setTag(Integer.valueOf(position));
            viewOnOff.setOnClickListener(this);

            return view;
        }

        @Override
        public boolean setViewValue(View view, Object data, String text)
        {
            if(view.getId() == R.id.alarm_day) {
                @SuppressWarnings("unchecked")
                EnumSet<AlarmSettings.DayOfWeek> days =
                    (EnumSet<AlarmSettings.DayOfWeek>)data;
                ((TextView)view).setText(
                    AlarmSettings.getDayText(getActivity(), days));

                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void onClick(View v)
        {
            Checkable check = (Checkable)v;
            boolean val = check.isChecked();
            int id = (Integer)v.getTag();

            if(val && settings.getAlarms(id).length == 0) {
                check.setChecked(false);
                NoAlarmsDialogFragment f = new NoAlarmsDialogFragment();
                f.show(getFragmentManager(), "NoAlarms");
                return;
            }

            settings.setOnOff(id, val);
            AlarmService.startSetupAlarms(getActivity());
        }
    }
}
