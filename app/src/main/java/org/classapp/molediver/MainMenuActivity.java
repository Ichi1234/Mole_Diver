package org.classapp.molediver;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {

    private TextView tvBestScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        Button btnPlay        = findViewById(R.id.btnPlay);
        Button btnShop        = findViewById(R.id.btnShop);
        Button btnCollection  = findViewById(R.id.btnCollection);
        Button btnOptions     = findViewById(R.id.btnOptions);
        Button btnAchievements = findViewById(R.id.btnAchievements);
        tvBestScore = findViewById(R.id.tvBestScore);
        updateBestDepthUi();

        // PLAY — launches the main game
        btnPlay.setOnClickListener(v -> {
            Intent intent = new Intent(this, GameActivity.class);
            startActivity(intent);
        });

        // SHOP
        btnShop.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShopActivity.class);
            startActivity(intent);
        });

        // COLLECTION
        btnCollection.setOnClickListener(v -> {
            Intent intent = new Intent(this, CollectionActivity.class);
            startActivity(intent);
        });

        // OPTIONS
        btnOptions.setOnClickListener(v -> {
            Intent intent = new Intent(this, OptionsActivity.class);
            startActivity(intent);
        });

        // ACHIEVEMENTS
        btnAchievements.setOnClickListener(v -> {
            Intent intent = new Intent(this, AchievementsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBestDepthUi();
    }

    private void updateBestDepthUi() {
        int bestDepthMetres = Math.round(PlayerData.getBestDepth(this));
        tvBestScore.setText(getString(R.string.best_depth_format, bestDepthMetres));
    }
}
