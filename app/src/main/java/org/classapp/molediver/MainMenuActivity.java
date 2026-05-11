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

        tvBestScore = findViewById(R.id.tvBestScore);

        Button btnPlay         = findViewById(R.id.btnPlay);
        Button btnShop         = findViewById(R.id.btnShop);
        Button btnCollection   = findViewById(R.id.btnCollection);
        Button btnOptions      = findViewById(R.id.btnOptions);
        Button btnAchievements = findViewById(R.id.btnAchievements);
        tvBestScore = findViewById(R.id.tvBestScore);
        updateBestDepthUi();

        btnPlay.setOnClickListener(v ->
            startActivity(new Intent(this, GameActivity.class)));

        btnShop.setOnClickListener(v ->
            startActivity(new Intent(this, ShopActivity.class)));

        btnCollection.setOnClickListener(v ->
            startActivity(new Intent(this, CollectionActivity.class)));

        btnOptions.setOnClickListener(v ->
            startActivity(new Intent(this, OptionsActivity.class)));

        btnAchievements.setOnClickListener(v ->
            startActivity(new Intent(this, AchievementsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBestDepthUi();

        float best = PlayerData.getBestDepth(this);
        tvBestScore.setText(best > 0
            ? "BEST DEPTH: " + (int) best + "m ★"
            : "BEST DEPTH: --- ★");
    }

    private void updateBestDepthUi() {
        int bestDepthMetres = Math.round(PlayerData.getBestDepth(this));
        tvBestScore.setText(getString(R.string.best_depth_format, bestDepthMetres));
    }
}
