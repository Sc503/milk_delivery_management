package com.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.R;

public class SplashActivity extends AppCompatActivity {

    ImageView imgSplash;
    TextView txtMoving, txtLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  Prevent duplicate splash
        if (!isTaskRoot()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_splash);

        imgSplash = findViewById(R.id.imgSplash);
        txtMoving = findViewById(R.id.txtMoving);
        txtLoading = findViewById(R.id.txtLoading);

        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        Animation move = AnimationUtils.loadAnimation(this, R.anim.right_to_left);
        Animation blink = AnimationUtils.loadAnimation(this, R.anim.blink);

        imgSplash.startAnimation(pulse);
        txtMoving.startAnimation(move);
        txtLoading.startAnimation(blink);

        new Handler().postDelayed(() -> {
            //  Clear top flag to prevent multiple instances
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }, 3000);
    }
}