package org.classapp.molediver;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

public class OptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        Switch switchMusic     = findViewById(R.id.switchMusic);
        Switch switchSfx       = findViewById(R.id.switchSound);
        Switch switchVibration = findViewById(R.id.switchVibration);

        // Load persisted values before attaching listeners to avoid spurious saves.
        switchMusic.setChecked(PlayerData.isMusicEnabled(this));
        switchSfx.setChecked(PlayerData.isSfxEnabled(this));
        switchVibration.setChecked(PlayerData.isVibrationEnabled(this));

        switchMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PlayerData.setMusicEnabled(this, isChecked);
            // TODO: AudioManager.setMusicEnabled(isChecked)
        });

        switchSfx.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PlayerData.setSfxEnabled(this, isChecked);
            // TODO: AudioManager.setSfxEnabled(isChecked)
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PlayerData.setVibrationEnabled(this, isChecked);
            // TODO: check this flag before any Vibrator.vibrate() call in GameView
        });
    }
}
