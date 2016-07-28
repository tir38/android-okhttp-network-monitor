Monitor the state of a network with Retrofit/OkHttp and RxJava
---


How can we monitor the state of our network connection with Retrofit? How can we do it seamlessly and in a single place? Let's find out!

First we create an interface for monitoring network. An interface will let us do dependency injection magic to supply a test double during testing:


```
public interface NetworkMonitor {
    boolean isConnected();
}
```

Then we implement the interface and call into the system's `ConnectivityManager`. If you're already doing network monitoring you're likely doing this somewhere (or everywhere!?!?):

```
public class LiveNetworkMonitor implements NetworkMonitor {

    private final Context applicationContext;

    public LiveNetworkMonitor(Context context) {
        applicationContext = context.getApplicationContext();
    }

    public boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
}
```

If you're even asking about monitoring network connectivity then you've already got Retrofit handling your web requests and you're using RxJava to communicate asynchronously between UI and service layer. In our example here we are getting public events from Github's API:

```
public interface GithubWebService {

    @GET("events")
    Observable<List<Event>> getPublicEvents();
}
```

```
public class MainActivity extends AppCompatActivity {

    @Inject
    protected GithubWebService mGithubWebService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((ExampleApplication) getApplication()).getComponent().inject(this);

        mGithubWebService.getPublicEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(events -> {
                    // TODO onNext
                }, throwable -> {
                    // TODO onError
                });
    }
}
```

So where can we call `NetworkMonitor.isConnected()` so that a network check happens seamlessly every time we make a web request? We'll use an [OkHttp Interceptor](https://github.com/square/okhttp/wiki/Interceptors)! Let's peek into our Dagger module, where we created our Retrofit object in the first place:

```
@Provides
@Singleton
GithubWebService provideWebService() {

    String baseUrl = "https://api.github.com";

    Retrofit.Builder builder = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()));

    OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();

    return builder.client(okHttpClientBuilder.build())
            .build()
            .create(GithubWebService.class);
}
```

We just need to take in a `NetworkMonitor`, create an `Interceptor`, and check the state of the network:

```
@Provides
@Singleton
GithubWebService provideWebService(NetworkMonitor networkMonitor) {

    String baseUrl = "https://api.github.com";

    Retrofit.Builder builder = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()));

    OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();

    // LOOK HERE !!!! add network monitor interceptor
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
```

We add our interceptor to the interceptor chain and it will make a network check before the rest of the OkHttp chain completes. We either proceed with the chain or throw a custom `NoNetworkException`. 

The last thing we need to do (and unfortunately we have to do this everywhere) is catch this specific exception in our `onError` method:

```
mGithubWebService.getPublicEvents()
        .subscribeOn(AndroidSchedulers.mainThread())
        .subscribe(events -> {
            // TODO onNext
        }, throwable -> {
            // on Error
            if (throwable instanceof NoNetworkException) {
                // TODO handle 'no network'
            } else {
            	// TODO handle some other error
            }
        });
```


In this example, I've decided to use an *Application* Interceptor. I'll leave it up to you to decide if you need an *Application* Interceptor or a *Network* Interceptor. 

With an *Application* Interceptor, you don't need to worry about network checks on every redirect and intermediate response. However, you'll perform an unnecessary network check even if OkHttp retrieves your request from cache, which partially negates the benefit of the cache.

With an *Network* Interceptor, you'll have the opposite: You won't make network checks when OkHttp decides to supply a cached response. However, you'll have the network check firing for redirects and retries, which might be overkill.

Check out [the working example](https://github.com/tir38/android-okhttp-network-monitor) and happy coding!

