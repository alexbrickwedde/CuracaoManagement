package dev.brickwedde.curacaomanagement;

import android.app.Application;
import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public class MainApplication extends Application {
    static private CcApi api = null;

    static public void changeHost(Context context, String host) {
        api = new CcApi("" + host + "/api/method", context);
    }

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
