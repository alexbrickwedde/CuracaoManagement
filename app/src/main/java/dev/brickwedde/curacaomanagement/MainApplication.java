package dev.brickwedde.curacaomanagement;

import android.app.Application;

public class MainApplication extends Application {
    static private CcApi api;

    static public CcApi getApi() {
        return api;
    }

    public MainApplication() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        api = new CcApi("https://andrew.nfsroot.de/api/method", getApplicationContext());
    }
}
