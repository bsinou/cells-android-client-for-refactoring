package com.pydio.android.client.data;


import androidx.annotation.NonNull;

import com.pydio.sdk.core.common.callback.StringCompletion;

import java.io.Serializable;

public class State implements Serializable {
    public String session;
    public String workspace;

    private StringCompletion saver;

    public void setSaver(StringCompletion s){
        this.saver = s;
    }

    public void save(){
        if(saver != null){
            saver.onComplete(toString(), null);
        } else {
            Application.saveState(this);
        }
    }

    @NonNull
    public String toString(){
        return String.format("%s:%s", session, workspace);
    }

    public static State parse(String str) {
        if (str != null && str.length() == 0) {
            return null;
        }

        String[] parts = str.split(":");
        State state = new State();

        state.session = parts[0];
        state.workspace = parts[1];
        if("null".equals(state.workspace)){
            state.workspace = null;
        }
        return state;
    }
}
