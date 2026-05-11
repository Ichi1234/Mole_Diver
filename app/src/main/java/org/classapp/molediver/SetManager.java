package org.classapp.molediver;

import android.content.Context;

public class SetManager {

    // ─── Set definition ───────────────────────────────────────────────────────

    public static class SetDef {
        public final String name;
        public final String description;
        public final int[]  itemIds;
        public final String reward;

        public SetDef(String name, String description, int[] itemIds, String reward) {
            this.name        = name;
            this.description = description;
            this.itemIds     = itemIds;
            this.reward      = reward;
        }
    }

    // ─── All 7 sets ───────────────────────────────────────────────────────────

    public static final SetDef[] ALL_SETS = {
        new SetDef(
            "Legend of the Mole",
            "The holy trinity of mole worship.",
            new int[]{25, 45, 50},
            "2x permanent coin multiplier"
        ),
        new SetDef(
            "The Dev Collection",
            "They built this game in the dark.",
            new int[]{46, 47, 48, 49},
            "Unlock dev colorway for mole (cosmetic)"    // TODO: implement reward
        ),
        new SetDef(
            "Deep Earth",
            "Treasures forged in the deepest layers.",
            new int[]{21, 22, 23, 24},
            "+15% material drop rate"                    // TODO: implement reward
        ),
        new SetDef(
            "The Digger's Toolkit",
            "Every mole needs the right tools.",
            new int[]{36, 41, 37, 43},
            "+10% upgrade discount"                      // TODO: implement reward
        ),
        new SetDef(
            "Fool's Hoard",
            "Worth more than anyone thinks.",
            new int[]{1, 2, 3, 5},
            "Common items worth 2x coins"
        ),
        new SetDef(
            "The Ancient World",
            "Echoes of prehistoric life.",
            new int[]{26, 27, 33, 35},
            "Fossil detector highlight"                  // TODO: implement reward
        ),
        new SetDef(
            "Collector's Nightmare",
            "All 50 items. Good luck.",
            buildAllIds(),
            "Golden mole skin + title"                   // TODO: implement reward
        ),
    };

    private static int[] buildAllIds() {
        int[] ids = new int[50];
        for (int i = 0; i < 50; i++) ids[i] = i + 1;
        return ids;
    }

    // ─── Query helpers ────────────────────────────────────────────────────────

    public static boolean isSetComplete(Context ctx, SetDef set) {
        for (int id : set.itemIds) {
            if (!PlayerData.isItemCollected(ctx, id)) return false;
        }
        return true;
    }

    /** How many items in the set the player has collected. */
    public static int getSetProgress(Context ctx, SetDef set) {
        int count = 0;
        for (int id : set.itemIds) {
            if (PlayerData.isItemCollected(ctx, id)) count++;
        }
        return count;
    }

    public static int getCompletedSetCount(Context ctx) {
        int count = 0;
        for (SetDef set : ALL_SETS) {
            if (isSetComplete(ctx, set)) count++;
        }
        return count;
    }

    /** Convenience: checks only the 2x coin multiplier set. */
    public static boolean isLegendOfTheMoleComplete(Context ctx) {
        return isSetComplete(ctx, ALL_SETS[0]);
    }
}
