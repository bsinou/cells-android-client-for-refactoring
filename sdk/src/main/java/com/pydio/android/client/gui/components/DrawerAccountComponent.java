package com.pydio.android.client.gui.components;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Session;

public class DrawerAccountComponent extends Component {

    private View rootView;
    private Session session;
    private ImageView icon;
    private TextView username, email;
    private LinearLayout switchAccount;
    private LinearLayout logout;
    private ImageView switchAccountIcon;

    public DrawerAccountComponent(View rootView) {
        this.rootView = rootView;
        initView();
    }

    public void setData(Session session){
        this.session = session;
        setAccountName(session.user);
        setSecondText(session.server.label());
    }

    private void setAccountName(String name){
        username.setText(name);
    }

    private void setSecondText(String text){
        email.setText(text);
    }

    public void setSwitchAccountButtonClickListener(View.OnClickListener listener){
        switchAccount.setOnClickListener(listener);
    }

    public void setLogoutActionClickListener(View.OnClickListener listener){
        logout.setOnClickListener(listener);
    }

    public void setOptionIcon(int res){
        switchAccountIcon.setImageResource(res);
    }

    public void showSwitchAccountButton(){
        switchAccount.setVisibility(View.VISIBLE);
    }

    public void hideSwitchAccountButton(){
        switchAccount.setVisibility(View.GONE);
    }

    @Override
    public View getView() {
        return rootView;
    }

    protected void initView() {
        super.initView();
        icon = rootView.findViewById(R.id.account_icon);
        username = rootView.findViewById(R.id.account_main_text);
        email = rootView.findViewById(R.id.account_second_text);
        switchAccount = rootView.findViewById(R.id.switch_account_button);
        switchAccountIcon = rootView.findViewById(R.id.switch_account_icon);
        logout = rootView.findViewById(R.id.logout_layout);

        if(Application.customTheme() != null) {
            icon.setColorFilter(Application.customTheme().getMainColor());
            username.setTextColor(Application.customTheme().getSecondaryColor());
            email.setTextColor(Application.customTheme().getSecondaryColor());
            ((ImageView)logout.getChildAt(0)).setColorFilter(Application.customTheme().getSecondaryColor());
            switchAccountIcon.setColorFilter(Application.customTheme().getSecondaryColor());
            rootView.setBackgroundColor(Application.customTheme().getMainColor());
        }
    }
}
