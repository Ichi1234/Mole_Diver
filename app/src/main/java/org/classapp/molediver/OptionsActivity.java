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

        // Hook up toggles — save state with SharedPreferences later
        Switch switchSound     = findViewById(R.id.switchSound);
        Switch switchMusic     = findViewById(R.id.switchMusic);
        Switch switchVibration = findViewById(R.id.switchVibration);

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: toggle sound engine on/off
        });

        switchMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: toggle background music on/off
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: toggle haptic feedback on/off
        });
    }
}
