package com.qk.pubgmod;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainHook extends Activity {

    private static final String TAG = "QKMod";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "QK Mod loaded");
    }

    public static void init() {
        Log.i(TAG, "QK Mod init called");
    }
}
