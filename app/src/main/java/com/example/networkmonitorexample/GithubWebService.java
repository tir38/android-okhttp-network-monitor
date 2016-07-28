package com.example.networkmonitorexample;

import java.util.List;

import retrofit2.http.GET;
import rx.Observable;

public interface GithubWebService {

    @GET("events")
    Observable<List<Event>> getPublicEvents();
}
