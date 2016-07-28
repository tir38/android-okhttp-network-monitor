package com.example.networkmonitorexample;

import android.app.Application;

public final class ExampleApplication extends Application {

    private MainComponent component;

    @Override
    public void onCreate() {
        super.onCreate();

        component = DaggerMainComponent.builder()
                .mainModule(new MainModule(this))
                .build();
    }

    public MainComponent getComponent() {
        return component;
    }
}
