package org.classapp.molediver;

import android.content.Context;
import android.content.SharedPreferences;

public class PlayerData {

    private static final String PREFS_NAME = "mole_diver_prefs";

    private static final String KEY_MUSIC_ENABLED     = "music_enabled";
    private static final String KEY_SFX_ENABLED       = "sfx_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    private static final String KEY_BEST_DEPTH        = "best_depth";
    private static final String KEY_TOTAL_COINS       = "total_coins";
    private static final String KEY_UPGRADE_OXYGEN    = "upgrade_oxygen";
    private static final String KEY_UPGRADE_SPEED     = "upgrade_speed";
    private static final String KEY_UPGRADE_RANGE     = "upgrade_range";
    private static final String KEY_COLLECTED_PREFIX  = "collected_";

    private PlayerData() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // -------------------------------------------------------------------------
    // Settings
    // -------------------------------------------------------------------------

    public static boolean isMusicEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_MUSIC_ENABLED, true);
    }

    public static void setMusicEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply();
    }

    public static boolean isSfxEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SFX_ENABLED, true);
    }

    public static void setSfxEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_SFX_ENABLED, enabled).apply();
    }

    public static boolean isVibrationEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    public static void setVibrationEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }

    // -------------------------------------------------------------------------
    // Player progress
    // -------------------------------------------------------------------------

    public static float getBestDepth(Context ctx) {
        return prefs(ctx).getFloat(KEY_BEST_DEPTH, 0f);
    }

    public static void setBestDepth(Context ctx, float depth) {
        prefs(ctx).edit().putFloat(KEY_BEST_DEPTH, depth).apply();
    }

    public static int getTotalCoins(Context ctx) {
        return prefs(ctx).getInt(KEY_TOTAL_COINS, 0);
    }

    public static void setTotalCoins(Context ctx, int coins) {
        prefs(ctx).edit().putInt(KEY_TOTAL_COINS, coins).apply();
    }

    public static void addCoins(Context ctx, int amount) {
        setTotalCoins(ctx, getTotalCoins(ctx) + amount);
    }

    /** Returns true and deducts coins only if the balance is sufficient. */
    public static boolean spendCoins(Context ctx, int amount) {
        int current = getTotalCoins(ctx);
        if (current < amount) return false;
        setTotalCoins(ctx, current - amount);
        return true;
    }

    // -------------------------------------------------------------------------
    // Upgrades
    // -------------------------------------------------------------------------

    public static int getUpgradeOxygen(Context ctx) {
        return prefs(ctx).getInt(KEY_UPGRADE_OXYGEN, 1);
    }

    public static void setUpgradeOxygen(Context ctx, int level) {
        prefs(ctx).edit().putInt(KEY_UPGRADE_OXYGEN, Math.min(level, 5)).apply();
    }

    public static int getUpgradeSpeed(Context ctx) {
        return prefs(ctx).getInt(KEY_UPGRADE_SPEED, 1);
    }

    public static void setUpgradeSpeed(Context ctx, int level) {
        prefs(ctx).edit().putInt(KEY_UPGRADE_SPEED, Math.min(level, 5)).apply();
    }

    public static int getUpgradeRange(Context ctx) {
        return prefs(ctx).getInt(KEY_UPGRADE_RANGE, 1);
    }

    public static void setUpgradeRange(Context ctx, int level) {
        prefs(ctx).edit().putInt(KEY_UPGRADE_RANGE, Math.min(level, 5)).apply();
    }

    /**
     * Returns the coin cost to advance from currentLevel to currentLevel+1.
     * Returns -1 if already at max level (5).
     */
    public static int getUpgradeCost(int currentLevel) {
        switch (currentLevel) {
            case 1: return 100;
            case 2: return 300;
            case 3: return 700;
            case 4: return 1500;
            default: return -1;
        }
    }

    // -------------------------------------------------------------------------
    // Collection (items 1–50)
    // -------------------------------------------------------------------------

    public static boolean isItemCollected(Context ctx, int id) {
        return prefs(ctx).getBoolean(KEY_COLLECTED_PREFIX + id, false);
    }

    public static void markItemCollected(Context ctx, int id) {
        prefs(ctx).edit().putBoolean(KEY_COLLECTED_PREFIX + id, true).apply();
    }
}
