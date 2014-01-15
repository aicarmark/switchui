package com.android.contacts.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import com.android.contacts.R;

public class InteractiveTitleBarView extends LinearLayout {

    private TextView label;

    public InteractiveTitleBarView(Context context, AttributeSet attrs) {

        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.interactive_title_bar, this);

        label = (TextView)findViewById(R.id.interactive_title_bar_label);
    }

    /**
     * Gets a button defined in the bar or <code>null</code> if the <code>id</code>
     * does not refer to a defined button.
     * @return The button or <code>null</code> if the button named
     * by <code>id</code> does not exist or if that view is not an
     * <code>ImageButton</code> element.
     */
    public ImageButton getImageButton(final int id) {

        View view = findViewById(id);
        if (view instanceof ImageButton) {
            return (ImageButton)view;
        }

        return null;
    }

    /**
     * Sets the title text.
     * @param title The text used for the title. Use the empty string or
     * <code>null</code> to clear the title.
     */
    public void setTitle(String title){
        if (label != null) {
            label.setText((title != null) ? title : "");
        }
    }

}

