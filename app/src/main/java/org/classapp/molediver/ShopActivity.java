package org.classapp.molediver;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ShopActivity extends AppCompatActivity {

    // ─── Views (bound once in onCreate) ──────────────────────────────────────
    private TextView tvCoinBalance;

    private TextView     tvLevelOxygen, tvLevelSteer, tvLevelDig, tvLevelRange,
                         tvLevelRefill, tvLevelClaw,  tvLevelRarity;
    private LinearLayout pipsOxygen,    pipsSteer,    pipsDig,    pipsRange,
                         pipsRefill,    pipsClaw,     pipsRarity;
    private Button       btnUpgradeOxygen, btnUpgradeSteer, btnUpgradeDig, btnUpgradeRange,
                         btnUpgradeRefill, btnUpgradeClaw,  btnUpgradeRarity;

    // ─── Functional interface for the generic purchase helper ─────────────────
    private interface LevelSetter { void set(int newLevel); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // ── Bind views ───────────────────────────────────────────────────────
        tvCoinBalance  = findViewById(R.id.tvCoinBalance);

        tvLevelOxygen  = findViewById(R.id.tvLevelOxygen);
        tvLevelSteer   = findViewById(R.id.tvLevelSteer);
        tvLevelDig     = findViewById(R.id.tvLevelDig);
        tvLevelRange   = findViewById(R.id.tvLevelRange);
        tvLevelRefill  = findViewById(R.id.tvLevelRefill);
        tvLevelClaw    = findViewById(R.id.tvLevelClaw);
        tvLevelRarity  = findViewById(R.id.tvLevelRarity);

        pipsOxygen     = findViewById(R.id.pipsOxygen);
        pipsSteer      = findViewById(R.id.pipsSteer);
        pipsDig        = findViewById(R.id.pipsDig);
        pipsRange      = findViewById(R.id.pipsRange);
        pipsRefill     = findViewById(R.id.pipsRefill);
        pipsClaw       = findViewById(R.id.pipsClaw);
        pipsRarity     = findViewById(R.id.pipsRarity);

        btnUpgradeOxygen = findViewById(R.id.btnUpgradeOxygen);
        btnUpgradeSteer  = findViewById(R.id.btnUpgradeSteer);
        btnUpgradeDig    = findViewById(R.id.btnUpgradeDig);
        btnUpgradeRange  = findViewById(R.id.btnUpgradeRange);
        btnUpgradeRefill = findViewById(R.id.btnUpgradeRefill);
        btnUpgradeClaw   = findViewById(R.id.btnUpgradeClaw);
        btnUpgradeRarity = findViewById(R.id.btnUpgradeRarity);

        // ── Buy listeners ────────────────────────────────────────────────────
        btnUpgradeOxygen.setOnClickListener(v -> buyUpgrade(
            PlayerData.getUpgradeOxygen(this),
            lv -> PlayerData.setUpgradeOxygen(this, lv)
        ));

        btnUpgradeSteer.setOnClickListener(v -> buyUpgrade(
            PlayerData.getUpgradeSteer(this),
            lv -> PlayerData.setUpgradeSteer(this, lv)
        ));

        btnUpgradeDig.setOnClickListener(v -> buyUpgrade(
            PlayerData.getUpgradeDig(this),
            lv -> PlayerData.setUpgradeDig(this, lv)
        ));

        btnUpgradeRange.setOnClickListener(v -> buyUpgrade(
            PlayerData.getUpgradeRange(this),
            lv -> PlayerData.setUpgradeRange(this, lv)
        ));

        btnUpgradeRefill.setOnClickListener(v -> buyUpgrade(
            PlayerData.getUpgradeO2Refill(this),
            lv -> PlayerData.setUpgradeO2Refill(this, lv)
        ));

        btnUpgradeClaw.setOnClickListener(v -> buyUpgrade(
            PlayerData.getUpgradeClaw(this),
            lv -> PlayerData.setUpgradeClaw(this, lv)
            // TODO: gameplay effect not yet implemented (Claw Level)
        ));

        btnUpgradeRarity.setOnClickListener(v -> buyUpgrade(
            PlayerData.getUpgradeRarity(this),
            lv -> PlayerData.setUpgradeRarity(this, lv)
            // TODO: gameplay effect not yet implemented (Rarity Boost)
        ));

        refreshUI();
    }

    // ─── Purchase helper ─────────────────────────────────────────────────────

    private void buyUpgrade(int currentLevel, LevelSetter setter) {
        int cost = PlayerData.getUpgradeCost(currentLevel);
        if (cost == -1) return;                            // already max level
        if (!PlayerData.spendCoins(this, cost)) return;   // insufficient funds
        setter.set(currentLevel + 1);
        refreshUI();
    }

    // ─── Refresh all UI elements from PlayerData ─────────────────────────────

    private void refreshUI() {
        int coins = PlayerData.getTotalCoins(this);
        tvCoinBalance.setText("● " + coins);

        refreshCard(tvLevelOxygen, pipsOxygen, btnUpgradeOxygen, PlayerData.getUpgradeOxygen(this),    coins);
        refreshCard(tvLevelSteer,  pipsSteer,  btnUpgradeSteer,  PlayerData.getUpgradeSteer(this),     coins);
        refreshCard(tvLevelDig,    pipsDig,    btnUpgradeDig,    PlayerData.getUpgradeDig(this),       coins);
        refreshCard(tvLevelRange,  pipsRange,  btnUpgradeRange,  PlayerData.getUpgradeRange(this),     coins);
        refreshCard(tvLevelRefill, pipsRefill, btnUpgradeRefill, PlayerData.getUpgradeO2Refill(this),  coins);
        refreshCard(tvLevelClaw,   pipsClaw,   btnUpgradeClaw,   PlayerData.getUpgradeClaw(this),      coins);
        refreshCard(tvLevelRarity, pipsRarity, btnUpgradeRarity, PlayerData.getUpgradeRarity(this),    coins);
    }

    private void refreshCard(TextView lvlLabel, LinearLayout pips, Button btn, int level, int coins) {
        lvlLabel.setText("LVL " + level + "/5");

        for (int i = 0; i < pips.getChildCount(); i++) {
            pips.getChildAt(i).setBackgroundColor(
                i < level ? Color.parseColor("#2B6B2B") : Color.parseColor("#1A2E1A")
            );
        }

        int cost = PlayerData.getUpgradeCost(level);
        if (cost == -1) {
            btn.setText("MAX");
            btn.setEnabled(false);
            btn.setAlpha(0.45f);
        } else {
            btn.setText("UPGRADE  ● " + cost);
            btn.setEnabled(true);
            btn.setAlpha(coins >= cost ? 1.0f : 0.4f);
        }
    }
}
