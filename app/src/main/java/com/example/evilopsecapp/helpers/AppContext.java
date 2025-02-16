package com.example.evilopsecapp.helpers;

import android.content.Context;

public class AppContext {
    private static Context context;

    public static void initialize(Context ctx) {
        if (context == null) {
            context = ctx.getApplicationContext();
        }
    }

    public static Context getContext() {
        return context;
    }
}
