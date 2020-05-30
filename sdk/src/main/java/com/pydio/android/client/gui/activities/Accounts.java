package com.pydio.android.client.gui.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pydio.android.client.R;
import com.pydio.android.client.data.Application;
import com.pydio.android.client.data.Session;
import com.pydio.android.client.data.State;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.gui.components.ConfirmDialogComponent;
import com.pydio.android.client.gui.dialogs.models.DialogData;

import java.util.ArrayList;
import java.util.List;

public class Accounts extends AppCompatActivity {


    class Holder extends RecyclerView.ViewHolder {
        View v;
        public Holder(@NonNull View itemView) {
            super(itemView);
            v = itemView;
        }
    }

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_accounts_layout);
        recyclerView = findViewById(R.id.recycler_view);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((view) -> {
            Intent intent = new Intent(Accounts.this, Application.newServerClass);
            Accounts.this.startActivity(intent);
        });
        if (Application.customTheme() != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(Application.customTheme().getMainColor()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fab.setImageTintList(ColorStateList.valueOf(Application.customTheme().getSecondaryColor()));
            }
        }

        final List<Session> sessions = new ArrayList<>(Application.sessions);

        adapter = new RecyclerView.Adapter<Holder>() {
            @NonNull
            @Override
            public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(Accounts.this);
                View v = inflater.inflate(R.layout.view_session_cell_layout, null, false);
                v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) Accounts.this.getResources().getDimension(R.dimen.account_cell_height) + 1));
                return new Holder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull Holder holder, int i) {
                Session session = sessions.get(i);
                View v = holder.v;

                ImageView icon = v.findViewById(R.id.icon);

                TextView mtv = v.findViewById(R.id.main_text);
                mtv.setText(session.user);

                TextView stv = v.findViewById(R.id.secondary_text);
                stv.setText(session.server.getOriginalURL().replaceFirst("pyd://", ""));

                v.findViewById(R.id.action_option_layout).setOnClickListener((view) -> {
                    DialogData data = DialogData.confirm(Accounts.this, session.user, () -> {
                        Database.deleteSession(session.id());
                        sessions.remove(i);
                        Application.deleteSession(i);

                        if(sessions.size() > 0){
                            Accounts.this.adapter.notifyDataSetChanged();
                        } else {
                            Intent intent = new Intent(Accounts.this, Application.newServerClass);
                            Accounts.this.startActivity(intent);
                            Accounts.this.finish();
                        }
                    });
                    ConfirmDialogComponent cd = new ConfirmDialogComponent(Accounts.this, data);
                    cd.show();
                });

                v.setOnClickListener((view) -> {
                    State state = new State();
                    state.setSaver((x, err) -> Application.setPreference(Application.PREF_APP_STATE, x));
                    state.session = session.id();
                    Application.saveState(state);
                    Application.onEnterSession(session);

                    Intent intent = new Intent(Accounts.this, Browser.class);
                    intent.putExtra("state", state.toString());
                    Accounts.this.startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    Accounts.this.finish();
                });

                if(Application.customTheme() != null) {
                    icon.setColorFilter(Application.customTheme().getMainColor());
                    mtv.setTextColor(Application.customTheme().getMainColor());
                    stv.setTextColor(Application.customTheme().getMainColor());
                    ((ImageView)((LinearLayout)v.findViewById(R.id.action_option_layout)).getChildAt(0)).setColorFilter(Application.customTheme().getMainColor());
                } else {
                    icon.setColorFilter(Accounts.this.getResources().getColor(R.color.main_color));
                }
            }

            @Override
            public int getItemCount() {
                return sessions.size();
            }
        };
        recyclerView.setAdapter(adapter);
        RecyclerView.LayoutManager lm = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(lm);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.notifyDataSetChanged();
    }
}
