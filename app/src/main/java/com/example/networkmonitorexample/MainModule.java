package com.example.networkmonitorexample;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

@Module
public final class MainModule {

    private Context context;

    public MainModule(Context context) {
        this.context = context.getApplicationContext();
    }

    @Provides
    @Singleton
    Context provideContext() {
        return context;
    }

    @Provides
    @Singleton
    GithubWebService provideWebService(NetworkMonitor networkMonitor) {

        String baseUrl = "https://api.github.com";

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()));

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();

        // add network monitor interceptor
        okHttpClientBuilder.addInterceptor(chain -> {
            boolean connected = networkMonitor.isConnected();
            if (connected) {
                return chain.proceed(chain.request());
            } else {
                throw new NoNetworkException();
            }
        });

        return builder.client(okHttpClientBuilder.build())
                .build()
                .create(GithubWebService.class);
    }

    @Provides
    @Singleton
    NetworkMonitor provideNetworkMonitor(Context context) {
        return new LiveNetworkMonitor(context);
    }
}
