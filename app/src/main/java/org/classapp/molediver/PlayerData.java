package org.classapp.molediver;

import android.content.Context;
import android.content.SharedPreferences;

public class PlayerData {

    private static final String PREFS_NAME = "mole_diver_prefs";

    private static final String KEY_MUSIC_ENABLED      = "music_enabled";
    private static final String KEY_SFX_ENABLED        = "sfx_enabled";
    private static final String KEY_VIBRATION_ENABLED  = "vibration_enabled";
    private static final String KEY_BEST_DEPTH         = "best_depth";
    private static final String KEY_TOTAL_COINS        = "total_coins";

    private static final String KEY_UPGRADE_OXYGEN     = "upgrade_oxygen";
    private static final String KEY_UPGRADE_STEER      = "upgrade_steer_speed";
    private static final String KEY_UPGRADE_DIG        = "upgrade_dig_speed";
    private static final String KEY_UPGRADE_RANGE      = "upgrade_range";
    private static final String KEY_UPGRADE_O2_REFILL  = "upgrade_o2_refill";
    private static final String KEY_UPGRADE_CLAW       = "upgrade_claw";
    private static final String KEY_UPGRADE_RARITY     = "upgrade_rarity";

    private static final String KEY_COLLECTED_PREFIX   = "collected_";
    private static final String KEY_SET_REWARD_PREFIX  = "set_reward_";

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

    public static int getUpgradeSteer(Context ctx) {
        return prefs(ctx).getInt(KEY_UPGRADE_STEER, 1);
    }

    public static void setUpgradeSteer(Context ctx, int level) {
        prefs(ctx).edit().putInt(KEY_UPGRADE_STEER, Math.min(level, 5)).apply();
    }

    public static int getUpgradeDig(Context ctx) {
        return prefs(ctx).getInt(KEY_UPGRADE_DIG, 1);
    }

    public static void setUpgradeDig(Context ctx, int level) {
        prefs(ctx).edit().putInt(KEY_UPGRADE_DIG, Math.min(level, 5)).apply();
    }

    public static int getUpgradeRange(Context ctx) {
        return prefs(ctx).getInt(KEY_UPGRADE_RANGE, 1);
    }

    public static void setUpgradeRange(Context ctx, int level) {
        prefs(ctx).edit().putInt(KEY_UPGRADE_RANGE, Math.min(level, 5)).apply();
    }

    public static int getUpgradeO2Refill(Context ctx) {
        return prefs(ctx).getInt(KEY_UPGRADE_O2_REFILL, 1);
    }

    public static void setUpgradeO2Refill(Context ctx, int level) {
        prefs(ctx).edit().putInt(KEY_UPGRADE_O2_REFILL, Math.min(level, 5)).apply();
    }

    public static int getUpgradeClaw(Context ctx) {
        return prefs(ctx).getInt(KEY_UPGRADE_CLAW, 1);
    }

    public static void setUpgradeClaw(Context ctx, int level) {
        prefs(ctx).edit().putInt(KEY_UPGRADE_CLAW, Math.min(level, 5)).apply();
    }

    public static int getUpgradeRarity(Context ctx) {
        return prefs(ctx).getInt(KEY_UPGRADE_RARITY, 1);
    }

    public static void setUpgradeRarity(Context ctx, int level) {
        prefs(ctx).edit().putInt(KEY_UPGRADE_RARITY, Math.min(level, 5)).apply();
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

    // -------------------------------------------------------------------------
    // Set rewards
    // -------------------------------------------------------------------------

    /** Sanitises the set name into a stable SharedPreferences key. */
    private static String setRewardKey(String setName) {
        return KEY_SET_REWARD_PREFIX + setName.toLowerCase()
                .replace(' ', '_')
                .replace('\'', '_');
    }

    public static boolean isSetRewardClaimed(Context ctx, String setName) {
        return prefs(ctx).getBoolean(setRewardKey(setName), false);
    }

    public static void claimSetReward(Context ctx, String setName) {
        prefs(ctx).edit().putBoolean(setRewardKey(setName), true).apply();
    }

    /** True when "Legend of the Mole" set reward is active (2x coin multiplier). */
    public static boolean hasCoinMultiplier(Context ctx) {
        return isSetRewardClaimed(ctx, "Legend of the Mole");
    }

    /** True when "Fool's Hoard" set reward is active (common items worth 2x). */
    public static boolean hasCommonDoubler(Context ctx) {
        return isSetRewardClaimed(ctx, "Fool's Hoard");
    }
}
