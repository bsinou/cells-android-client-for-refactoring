package com.pydio.android.client.data.auth;

import com.pydio.sdk.core.security.Passwords;

public class AppCredentials implements com.pydio.sdk.core.security.Credentials {

    private String url;
    private String login;
    private String captcha;
    private String seed;

    public AppCredentials(String url) {
        this.url = url;
        this.seed = "-1";
    }

    @Override
    public String getLogin() {
        return this.login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @Override
    public String getPassword() {
        return Passwords.load(this.url, this.login);
    }

    @Override
    public String getCaptcha() {
        return this.captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public String getSeed() {
        return this.seed;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }
}
