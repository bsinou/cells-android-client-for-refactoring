package com.pydio.android.client.data;

public class Theme {
    private int mainColor;
    private int backgroundColor;
    private int secondaryColor;
    private String splashImagePath;
    private String icon;


    public int getMainColor() {
        return mainColor;
    }

    public void setMainColor(int mainColor) {
        this.mainColor = mainColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(int secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getSplashImagePath() {
        return splashImagePath;
    }

    public void setSplashImagePath(String splashImagePath) {
        this.splashImagePath = splashImagePath;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
