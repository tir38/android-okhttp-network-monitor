package com.example.networkmonitorexample;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {MainModule.class})
/**
 * Dagger component for main package
 */
public interface MainComponent {
    void inject(MainActivity activity);
}
