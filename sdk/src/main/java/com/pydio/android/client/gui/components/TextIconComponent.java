package com.pydio.android.client.gui.components;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pydio.android.client.R;

public class TextIconComponent extends Component {

    private View rootView;
    private String text;
    private TextView textView;
    private LinearLayout background;

    public TextIconComponent(View view) {
        rootView = view;
        textView = rootView.findViewById(R.id.text);
        background = rootView.findViewById(R.id.text_layout);
    }

    public TextIconComponent(Context context, String text) {
        LayoutInflater inflater = LayoutInflater.from(context);
        rootView = inflater.inflate(R.layout.view_circle_text_layout, null, false);
        textView = rootView.findViewById(R.id.text);
        background = rootView.findViewById(R.id.text_layout);
        setText(text);
    }

    public TextIconComponent(Context context){
        LayoutInflater inflater = LayoutInflater.from(context);
        rootView = inflater.inflate(R.layout.view_circle_text_layout, null, false);
        textView = rootView.findViewById(R.id.text);
        background = rootView.findViewById(R.id.text_layout);
    }

    public void setText(String text){
        this.text = text;
        this.textView.setText(text);
    }

    public void setText(int res){
        this.setText(rootView.getContext().getString(res));
    }

    public void setBackground(int res){
        background.setBackgroundResource(res);
    }

    public void setTint(int resColor) {
        textView.setTextColor(resColor);
    }

    public void setTextBold(boolean bold){
        if(bold){
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        } else {
            textView.setTypeface(textView.getTypeface(), Typeface.NORMAL);
        }
    }

    public String getText(){
        return text;
    }

    @Override
    public View getView() {
        return rootView;
    }
}
