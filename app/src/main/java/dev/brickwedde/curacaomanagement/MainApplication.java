package dev.brickwedde.curacaomanagement;

import android.app.Application;

public class MainApplication extends Application {
    static private CcApi api;

    static public CcApi getApi() {
        return api;
    }

    public MainApplication() {
        super();
        api = new CcApi("https://andrew.nfsroot.de/api/method", this);
    }
}
