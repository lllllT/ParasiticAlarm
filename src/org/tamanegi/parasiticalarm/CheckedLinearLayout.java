package org.tamanegi.parasiticalarm;

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

    public CheckedLinearLayout(
        Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
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

    private Checkable getCheckable()
    {
        return (Checkable)findViewById(R.id.item_check);
    }
}
