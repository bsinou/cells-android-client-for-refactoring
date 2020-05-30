package com.pydio.android.client.accounts;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;

import com.pydio.android.client.utils.Background;
import com.pydio.sdk.core.auth.OauthConfig;
import com.pydio.sdk.core.auth.Token;
import com.pydio.sdk.core.common.http.HttpClient;
import com.pydio.sdk.core.common.http.HttpRequest;
import com.pydio.sdk.core.common.http.HttpResponse;
import com.pydio.sdk.core.common.http.Method;
import com.pydio.sdk.core.model.ServerNode;
import com.pydio.sdk.core.utils.Params;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Accounts {

    public static Accounts manager;
    private Map<String, OauthConfig> states;
    private AuthenticationEventHandler authenticationCallback;
    private final Object lock = new Object();

    public static void init() {
        manager = new Accounts();
    }

    private Accounts() {
        states = new HashMap<>();
    }

    public void handlerURL(Uri uri) {
        String errorDescription = null, error = null, code = null, state = null, scope = null;

        String query = uri.getQuery();
        if (query == null) {
            return;
        }

        String[] splitQuery = query.split("&");
        for (String part : splitQuery) {
            String[] pair = part.split("=");
            if (pair.length > 1) {
                switch (pair[0]) {
                    case "code":
                        code = pair[1];
                        break;

                    case "scope":
                        scope = pair[1];
                        break;

                    case "error":
                        error = pair[1];
                        break;

                    case "error_description":
                        errorDescription = pair[1];
                        break;

                    case "state":
                        state = pair[1];
                        break;
                }
            }
        }

        if (state == null) {
            return;
        }

        if (authenticationCallback == null) {
            return;
        }

        if (error != null || errorDescription != null) {
            authenticationCallback.onError(error, errorDescription);
        }

        this.getToken(state, code, scope);
    }

    public void authorize(OauthConfig cfg, AuthenticationEventHandler handler) {
        states.put(cfg.state, cfg);
        // handlers.put(cfg.state, handler);
        this.authenticationCallback = handler;

        Uri.Builder builder = Uri.parse(cfg.authorizeEndpoint).buildUpon();
        builder = builder.appendQueryParameter("state", cfg.state)
                .appendQueryParameter("scope", cfg.scope)
                .appendQueryParameter("client_id", cfg.clientID)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", cfg.redirectURI);

        if (cfg.audience != null && !"".equals(cfg.audience)) {
            builder.appendQueryParameter("audience_id", cfg.audience);
        }

        Uri uri = builder.build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        handler.startIntent(intent);
    }

    private void getToken(String state, String code, String scope) {
        OauthConfig cfg = this.states.get(state);
        if (cfg == null) {
            return;
        }

        cfg.scope = scope;
        cfg.code = code;

        HttpRequest request = new HttpRequest();
        Params params = Params.create("state", state)
                .set("code", code)
                .set("grant_type", "authorization_code")
                .set("redirect_uri", cfg.redirectURI);
        request.setParams(params);
        String auth = new String(Base64.encode((cfg.clientID + ":" + cfg.clientSecret).getBytes(), Base64.DEFAULT));
        request.setHeaders(Params.create("Authorization", "Basic " + auth));
        request.setEndpoint(cfg.tokenEndpoint);
        request.setMethod(Method.POST);

        Background.go(() -> {
            try {
                HttpResponse response = HttpClient.request(request);
                String jwt = response.getString();
                this.authenticationCallback.handleToken(jwt);
            } catch (Exception e) {
                e.printStackTrace();
                this.authenticationCallback.onError("could not get authentication token", e.getMessage());
            }
        });
    }

    public Token refreshToken(ServerNode server, Token token) throws IOException {
        OauthConfig cfg = OauthConfig.fromJSON(server.getOIDCInfo(), "");

        HttpRequest request = new HttpRequest();
        Params params = Params.create("grant_type", "refresh_token").set("refresh_token", token.refreshToken);

        request.setParams(params);
        String auth = new String(Base64.encode((cfg.clientID + ":" + cfg.clientSecret).getBytes(), Base64.DEFAULT));
        request.setHeaders(Params.create("Authorization", "Basic " + auth));
        request.setEndpoint(cfg.tokenEndpoint);
        request.setMethod(Method.POST);

        HttpResponse response = HttpClient.request(request);
        String jwt = response.getString();
        return Token.decode(jwt);
    }
}
