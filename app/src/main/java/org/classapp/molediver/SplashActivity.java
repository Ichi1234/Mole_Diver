package org.classapp.molediver;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2800; // ms before going to main menu

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(android.view.Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);

        View splashContent = findViewById(R.id.splashContent);
        TextView tvLoading  = findViewById(R.id.tvLoading);

        // --- Animate the logo/title in ---
        // Fade + slight scale up from 0.85 → 1.0
        AnimationSet contentAnim = new AnimationSet(true);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(900);
        fadeIn.setFillAfter(true);

        ScaleAnimation scaleUp = new ScaleAnimation(
            0.85f, 1f,
            0.85f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleUp.setDuration(900);
        scaleUp.setFillAfter(true);

        contentAnim.addAnimation(fadeIn);
        contentAnim.addAnimation(scaleUp);
        contentAnim.setFillAfter(true);

        splashContent.startAnimation(contentAnim);
        splashContent.setAlpha(1f); // needed so setFillAfter sticks

        // --- Animate the loading dots in after a short delay ---
        AlphaAnimation dotsAnim = new AlphaAnimation(0f, 1f);
        dotsAnim.setDuration(500);
        dotsAnim.setStartOffset(800);
        dotsAnim.setFillAfter(true);
        tvLoading.startAnimation(dotsAnim);
        tvLoading.setAlpha(1f);

        // --- Navigate to MainMenuActivity after delay ---
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainMenuActivity.class);
            startActivity(intent);
            finish(); // removes splash from back stack so back button doesn't return here
        }, SPLASH_DURATION);
    }

    @Override
    public void onBackPressed() {
        // Disable back button during splash
    }
}
