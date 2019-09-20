package com.example.countme;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Open an activity to capture image
        new Handler().postDelayed(new Runnable() {



            @Override
            public void run() {

                Intent i = new Intent(MainActivity.this, CaptureActivity.class);
                startActivity(i);

                // close this activity
                finish();
            }
        }, 100);


    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}

