package org.classapp.molediver;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class AchievementManager {

    public static class Achievement {
        public final int    id;
        public final String name;
        public final String description;
        public final String category; // "Depth","Collection","Coins","Upgrades","Survival","Sets"

        public Achievement(int id, String name, String description, String category) {
            this.id          = id;
            this.name        = name;
            this.description = description;
            this.category    = category;
        }
    }

    public static final Achievement[] ALL_ACHIEVEMENTS = {
        // ── Depth ────────────────────────────────────────────────────────────
        new Achievement( 1, "First Steps",        "Reach 50m depth",                         "Depth"),
        new Achievement( 2, "Getting Deeper",      "Reach 200m depth",                        "Depth"),
        new Achievement( 3, "Into the Rock",       "Reach 300m depth",                        "Depth"),
        new Achievement( 4, "Deep Diver",          "Reach 600m depth",                        "Depth"),
        new Achievement( 5, "Bedrock Breaker",     "Reach 1,000m depth",                      "Depth"),
        new Achievement( 6, "Core Reached",        "Reach 1,500m depth",                      "Depth"),
        // ── Collection ───────────────────────────────────────────────────────
        new Achievement( 7, "First Find",          "Collect your first item",                 "Collection"),
        new Achievement( 8, "Budding Collector",   "Collect 10 unique items",                 "Collection"),
        new Achievement( 9, "Halfway There",       "Collect 25 unique items",                 "Collection"),
        new Achievement(10, "Museum Curator",      "Collect 40 unique items",                 "Collection"),
        new Achievement(11, "Completionist",       "Collect all 50 items",                    "Collection"),
        new Achievement(12, "Rare Finder",         "Collect your first Rare item",            "Collection"),
        new Achievement(13, "Epic Discovery",      "Collect your first Epic item",            "Collection"),
        new Achievement(14, "Legendary Pull",      "Collect your first Legendary item",       "Collection"),
        // ── Coins ────────────────────────────────────────────────────────────
        new Achievement(15, "Pocket Change",       "Earn 100 total coins",                    "Coins"),
        new Achievement(16, "Saving Up",           "Earn 1,000 total coins",                  "Coins"),
        new Achievement(17, "Rich Mole",           "Earn 10,000 total coins",                 "Coins"),
        new Achievement(18, "Mole Millionaire",    "Earn 100,000 total coins",                "Coins"),
        new Achievement(19, "Big Haul",            "Earn 500+ coins in a single run",         "Coins"),
        // ── Upgrades ─────────────────────────────────────────────────────────
        new Achievement(20, "First Upgrade",       "Purchase any upgrade",                    "Upgrades"),
        new Achievement(21, "Fully Loaded",        "Max out any single upgrade to level 5",   "Upgrades"),
        new Achievement(22, "Jack of All Trades",  "Upgrade all 7 stats at least once",       "Upgrades"),
        new Achievement(23, "Maxed Out",           "Max out all 7 upgrades to level 5",       "Upgrades"),
        // ── Survival ─────────────────────────────────────────────────────────
        new Achievement(24, "Close Call",          "Survive with less than 5% oxygen",        "Survival"),
        new Achievement(25, "Oxygen Hoarder",      "Collect 10 O2 canisters in one run",      "Survival"),
        new Achievement(26, "Untouchable",         "Reach 200m without hitting any enemy",    "Survival"),
        new Achievement(27, "Bug Squasher",        "Hit 50 enemies total (lifetime)",         "Survival"),
        // ── Sets ─────────────────────────────────────────────────────────────
        new Achievement(28, "Set Starter",         "Complete your first item set",            "Sets"),
        new Achievement(29, "Set Collector",       "Complete 5 item sets",                    "Sets"),
        new Achievement(30, "Legend",              "Complete the Legend of the Mole set",     "Sets"),
    };

    public static boolean isUnlocked(Context ctx, int id) {
        return PlayerData.isAchievementUnlocked(ctx, id);
    }

    public static int getUnlockedCount(Context ctx) {
        SharedPreferences prefs = PlayerData.prefs(ctx);
        int count = 0;
        for (Achievement a : ALL_ACHIEVEMENTS) {
            if (prefs.getBoolean("achievement_" + a.id, false)) count++;
        }
        return count;
    }

    public static int getCompletionPercent(Context ctx) {
        return (getUnlockedCount(ctx) * 100) / ALL_ACHIEVEMENTS.length;
    }

    /**
     * Checks all 30 achievements against current PlayerData state.
     * Unlocks any that are newly earned and returns their IDs.
     * Reads SharedPreferences only once for efficiency.
     */
    public static List<Integer> checkAndUnlockAll(Context ctx) {
        SharedPreferences prefs = PlayerData.prefs(ctx);

        // ── Read all data once ────────────────────────────────────────────────
        float   bestDepth       = prefs.getFloat("best_depth", 0f);
        long    lifetimeCoins   = prefs.getLong("lifetime_coins", 0L);
        int     lifetimeEnemies = prefs.getInt("lifetime_enemies_hit", 0);
        boolean closeCall       = prefs.getBoolean("close_call_triggered", false);
        boolean untouchable     = prefs.getBoolean("untouchable_triggered", false);
        int     maxRunCoins     = prefs.getInt("max_run_coins", 0);
        int     maxRunCanisters = prefs.getInt("max_run_canisters", 0);

        int oxyLv    = prefs.getInt("upgrade_oxygen",     1);
        int steerLv  = prefs.getInt("upgrade_steer_speed",1);
        int digLv    = prefs.getInt("upgrade_dig_speed",  1);
        int rangeLv  = prefs.getInt("upgrade_range",      1);
        int refillLv = prefs.getInt("upgrade_o2_refill",  1);
        int clawLv   = prefs.getInt("upgrade_claw",       1);
        int rarityLv = prefs.getInt("upgrade_rarity",     1);
        int[] upgrades = { oxyLv, steerLv, digLv, rangeLv, refillLv, clawLv, rarityLv };

        int     collectedCount = 0;
        boolean hasRare        = false;
        boolean hasEpic        = false;
        boolean hasLegendary   = false;
        for (ItemCatalogue.Item item : ItemCatalogue.ALL_ITEMS) {
            if (prefs.getBoolean("collected_" + item.id, false)) {
                collectedCount++;
                switch (item.rarity) {
                    case RARE:      hasRare      = true; break;
                    case EPIC:      hasEpic      = true; break;
                    case LEGENDARY: hasLegendary = true; break;
                    default: break;
                }
            }
        }

        int     completedSets = SetManager.getCompletedSetCount(ctx);
        boolean legendDone    = SetManager.isLegendOfTheMoleComplete(ctx);

        boolean anyUpgraded = false, anyMaxed = false;
        boolean allUpgraded = true,  allMaxed  = true;
        for (int lv : upgrades) {
            if (lv >= 2) anyUpgraded = true; else allUpgraded = false;
            if (lv >= 5) anyMaxed   = true;  else allMaxed    = false;
        }

        // ── Check and batch-write newly unlocked ──────────────────────────────
        List<Integer>        newlyUnlocked = new ArrayList<>();
        SharedPreferences.Editor editor   = prefs.edit();

        tryUnlock(prefs, editor, newlyUnlocked,  1, bestDepth >= 50f);
        tryUnlock(prefs, editor, newlyUnlocked,  2, bestDepth >= 200f);
        tryUnlock(prefs, editor, newlyUnlocked,  3, bestDepth >= 300f);
        tryUnlock(prefs, editor, newlyUnlocked,  4, bestDepth >= 600f);
        tryUnlock(prefs, editor, newlyUnlocked,  5, bestDepth >= 1000f);
        tryUnlock(prefs, editor, newlyUnlocked,  6, bestDepth >= 1500f);

        tryUnlock(prefs, editor, newlyUnlocked,  7, collectedCount >= 1);
        tryUnlock(prefs, editor, newlyUnlocked,  8, collectedCount >= 10);
        tryUnlock(prefs, editor, newlyUnlocked,  9, collectedCount >= 25);
        tryUnlock(prefs, editor, newlyUnlocked, 10, collectedCount >= 40);
        tryUnlock(prefs, editor, newlyUnlocked, 11, collectedCount >= 50);
        tryUnlock(prefs, editor, newlyUnlocked, 12, hasRare);
        tryUnlock(prefs, editor, newlyUnlocked, 13, hasEpic);
        tryUnlock(prefs, editor, newlyUnlocked, 14, hasLegendary);

        tryUnlock(prefs, editor, newlyUnlocked, 15, lifetimeCoins >= 100L);
        tryUnlock(prefs, editor, newlyUnlocked, 16, lifetimeCoins >= 1_000L);
        tryUnlock(prefs, editor, newlyUnlocked, 17, lifetimeCoins >= 10_000L);
        tryUnlock(prefs, editor, newlyUnlocked, 18, lifetimeCoins >= 100_000L);
        tryUnlock(prefs, editor, newlyUnlocked, 19, maxRunCoins >= 500);

        tryUnlock(prefs, editor, newlyUnlocked, 20, anyUpgraded);
        tryUnlock(prefs, editor, newlyUnlocked, 21, anyMaxed);
        tryUnlock(prefs, editor, newlyUnlocked, 22, allUpgraded);
        tryUnlock(prefs, editor, newlyUnlocked, 23, allMaxed);

        tryUnlock(prefs, editor, newlyUnlocked, 24, closeCall);
        tryUnlock(prefs, editor, newlyUnlocked, 25, maxRunCanisters >= 10);
        tryUnlock(prefs, editor, newlyUnlocked, 26, untouchable);
        tryUnlock(prefs, editor, newlyUnlocked, 27, lifetimeEnemies >= 50);

        tryUnlock(prefs, editor, newlyUnlocked, 28, completedSets >= 1);
        tryUnlock(prefs, editor, newlyUnlocked, 29, completedSets >= 5);
        tryUnlock(prefs, editor, newlyUnlocked, 30, legendDone);

        if (!newlyUnlocked.isEmpty()) editor.apply();
        return newlyUnlocked;
    }

    private static void tryUnlock(SharedPreferences prefs, SharedPreferences.Editor editor,
                                   List<Integer> list, int id, boolean condition) {
        if (!prefs.getBoolean("achievement_" + id, false) && condition) {
            editor.putBoolean("achievement_" + id, true);
            list.add(id);
        }
    }
}
