package org.tamanegi.parasiticalarm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckedLinearLayout
    extends LinearLayout implements Checkable
{
    public CheckedLinearLayout(Context context)
    {
        super(context);
    }

    public CheckedLinearLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public boolean isChecked()
    {
        return getCheckable().isChecked();
    }

    @Override
    public void setChecked(boolean checked)
    {
        getCheckable().setChecked(checked);
    }

    @Override
    public void toggle()
    {
        getCheckable().toggle();
    }

    @SuppressLint("WrongViewCast")
    private Checkable getCheckable()
    {
        return (Checkable)findViewById(R.id.item_check);
    }
}
