package com.example.networkmonitorexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;

public class MainActivity extends AppCompatActivity {

    @Inject
    protected GithubWebService mGithubWebService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((ExampleApplication) getApplication()).getComponent().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mGithubWebService.getPublicEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(users -> {
                    // onNext
                    Toast.makeText(MainActivity.this, "Successful Request", Toast.LENGTH_SHORT).show();
                }, throwable -> {
                    // onError
                    if (throwable instanceof NoNetworkException) {
                        // handle 'no network'
                        Toast.makeText(MainActivity.this, "No Network Connection", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Some Other Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
