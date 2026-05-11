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

    // Achievement keys
    private static final String KEY_ACHIEVEMENT_PREFIX  = "achievement_";
    private static final String KEY_LIFETIME_COINS      = "lifetime_coins";
    private static final String KEY_LIFETIME_ENEMIES    = "lifetime_enemies_hit";
    private static final String KEY_CLOSE_CALL          = "close_call_triggered";
    private static final String KEY_UNTOUCHABLE         = "untouchable_triggered";
    private static final String KEY_MAX_RUN_COINS       = "max_run_coins";
    private static final String KEY_MAX_RUN_CANISTERS   = "max_run_canisters";

    private PlayerData() {}

    static SharedPreferences prefs(Context ctx) {
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

    /** True when all 50 items have been collected (unlocks the golden mole skin). */
    public static boolean hasGoldenSkin(Context ctx) {
        SharedPreferences p = prefs(ctx);
        for (int i = 1; i <= 50; i++) {
            if (!p.getBoolean(KEY_COLLECTED_PREFIX + i, false)) return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Achievements
    // -------------------------------------------------------------------------

    public static boolean isAchievementUnlocked(Context ctx, int id) {
        return prefs(ctx).getBoolean(KEY_ACHIEVEMENT_PREFIX + id, false);
    }

    public static void unlockAchievement(Context ctx, int id) {
        prefs(ctx).edit().putBoolean(KEY_ACHIEVEMENT_PREFIX + id, true).apply();
    }

    // -------------------------------------------------------------------------
    // Lifetime stats (never decrease)
    // -------------------------------------------------------------------------

    public static long getLifetimeCoins(Context ctx) {
        return prefs(ctx).getLong(KEY_LIFETIME_COINS, 0L);
    }

    public static void addLifetimeCoins(Context ctx, int amount) {
        SharedPreferences p = prefs(ctx);
        p.edit().putLong(KEY_LIFETIME_COINS, p.getLong(KEY_LIFETIME_COINS, 0L) + amount).apply();
    }

    public static int getLifetimeEnemiesHit(Context ctx) {
        return prefs(ctx).getInt(KEY_LIFETIME_ENEMIES, 0);
    }

    public static void addLifetimeEnemiesHit(Context ctx, int count) {
        SharedPreferences p = prefs(ctx);
        p.edit().putInt(KEY_LIFETIME_ENEMIES, p.getInt(KEY_LIFETIME_ENEMIES, 0) + count).apply();
    }

    /** Best single-run coin total ever recorded. Used for the "Big Haul" achievement. */
    public static int getMaxRunCoins(Context ctx) {
        return prefs(ctx).getInt(KEY_MAX_RUN_COINS, 0);
    }

    public static void updateMaxRunCoins(Context ctx, int runCoins) {
        if (runCoins > getMaxRunCoins(ctx)) {
            prefs(ctx).edit().putInt(KEY_MAX_RUN_COINS, runCoins).apply();
        }
    }

    /** Best single-run O2 canister count ever recorded. Used for "Oxygen Hoarder". */
    public static int getMaxRunCanisters(Context ctx) {
        return prefs(ctx).getInt(KEY_MAX_RUN_CANISTERS, 0);
    }

    public static void updateMaxRunCanisters(Context ctx, int count) {
        if (count > getMaxRunCanisters(ctx)) {
            prefs(ctx).edit().putInt(KEY_MAX_RUN_CANISTERS, count).apply();
        }
    }

    // -------------------------------------------------------------------------
    // In-game triggers (written during a run, read by AchievementManager)
    // -------------------------------------------------------------------------

    public static boolean isCloseCallTriggered(Context ctx) {
        return prefs(ctx).getBoolean(KEY_CLOSE_CALL, false);
    }

    public static void setCloseCallTriggered(Context ctx) {
        prefs(ctx).edit().putBoolean(KEY_CLOSE_CALL, true).apply();
    }

    public static boolean isUntouchableTriggered(Context ctx) {
        return prefs(ctx).getBoolean(KEY_UNTOUCHABLE, false);
    }

    public static void setUntouchableTriggered(Context ctx) {
        prefs(ctx).edit().putBoolean(KEY_UNTOUCHABLE, true).apply();
    }
}
