package com.aliyun.homeshell.views;

import com.aliyun.homeshell.R;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class AliRadioButtonPreference extends CheckBoxPreference {
    public interface OnClickListener {
        public abstract void onRadioButtonClicked(
                AliRadioButtonPreference emiter);
    }

    private OnClickListener mListener = null;

    public AliRadioButtonPreference(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
    }

    public AliRadioButtonPreference(Context context, AttributeSet attrs) {
        this(context, attrs,
                android.R.attr.checkBoxPreferenceStyle);
    }

    public AliRadioButtonPreference(Context context) {
        this(context, null);
    }

    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick() {
        if (mListener != null) {
            mListener.onRadioButtonClicked(this);
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView title = (TextView) view.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
            title.setMaxLines(3);
        }
    }

}
