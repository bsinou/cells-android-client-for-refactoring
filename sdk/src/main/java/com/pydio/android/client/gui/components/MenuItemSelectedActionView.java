package com.pydio.android.client.gui.components;

import android.view.View;
import android.widget.ImageView;

import com.pydio.android.client.R;

public class MenuItemSelectedActionView extends Component {

    private  View rootView;
    private ImageView icon;
    private boolean selected, deselectedVisible;

    private int selectedColor, deselectedColor;

    MenuItemSelectedActionView(View rootView) {
        this.rootView = rootView;
        this.icon = this.rootView.findViewById(R.id.icon);
        this.deselectedVisible = false;
        this.selected = false;
        this.selectedColor = R.color.white2;
        this.deselectedColor = R.color.transparent;
        this.icon.setImageResource(R.drawable.ic_checkbox_marked_circle_grey600_48dp);
        this.icon.setScaleX((float) 0.8);
        this.icon.setScaleY((float) 0.8);
    }

    public void setDeselectedVisible(boolean v){
        this.deselectedVisible = v;
    }

    public void setSelected(boolean s) {
        this.selected = s;
        if (s) {
            icon.setColorFilter(rootView.getResources().getColor(selectedColor));
            this.show();

        } else {
            icon.setColorFilter(rootView.getResources().getColor(deselectedColor));
            if(deselectedVisible){
                this.show();
            } else {
                this.hide();
            }
        }
    }
    @Override
    public View getView() {
        return icon;
    }
}
