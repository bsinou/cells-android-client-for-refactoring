package com.pydio.android.client.gui.components;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Theme;

public class EmptyContentComponent extends Component{

    private View rootView;
    private TextView text;
    private ImageView icon;
    private Button button;

    public EmptyContentComponent(View rootView) {
        this.rootView = rootView;
        text = rootView.findViewById(R.id.empty_content_text);
        icon = rootView.findViewById(R.id.empty_content_icon);
        button = rootView.findViewById(R.id.empty_content_button);

        if (Application.customTheme() != null) {
            final Theme theme = Application.customTheme();
            button.setTextColor(theme.getMainColor());
        }
    }

    public void setText(String text) {
        this.text.setText(text);
    }

    public void setText(int string) {
        this.text.setText(string);
    }

    public void setIcon(int icon) {
        this.icon.setImageResource(icon);
    }

    public void setIcon(Bitmap bitmap) {
        this.icon.setImageBitmap(bitmap);
    }

    public void setButtonText(String text) {
        this.button.setText(text);
    }

    public void setButtonText(int string) {
        this.button.setText(string);
    }

    public void setButtonClickListener(View.OnClickListener listener) {
        if (button != null) {
            button.setOnClickListener(listener);
        }
    }

    public void hideActionButton() {
        button.setVisibility(View.GONE);
    }

    public void showActionButton() {
        button.setVisibility(View.VISIBLE);
    }

    public View getView(){
        return rootView;
    }
}
