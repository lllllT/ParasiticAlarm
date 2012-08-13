package org.tamanegi.parasiticalarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class NoAlarmsDialogFragment extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedState)
    {
        return new AlertDialog.Builder(getActivity())
            .setTitle(R.string.pref_desc_onoff)
            .setMessage(R.string.msg_no_alarms)
            .setPositiveButton(android.R.string.ok, null)
            .create();
    }
}
