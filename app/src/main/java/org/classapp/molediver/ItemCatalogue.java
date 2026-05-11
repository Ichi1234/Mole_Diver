package org.classapp.molediver;

import android.graphics.Color;

import java.util.ArrayList;

public class ItemCatalogue {

    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }

    // ─── Item data class ─────────────────────────────────────────────────────

    public static class Item {
        public final int    id;
        public final String name;
        public final String category;
        public final Rarity rarity;
        public final int    coinValue;
        public final int    spriteResId;
        public final boolean isHidden; // hidden items never drop from normal rolls

        public Item(int id, String name, String category, Rarity rarity,
                    int coinValue, int spriteResId, boolean isHidden) {
            this.id          = id;
            this.name        = name;
            this.category    = category;
            this.rarity      = rarity;
            this.coinValue   = coinValue;
            this.spriteResId = spriteResId;
            this.isHidden    = isHidden;
        }
    }

    // ─── Full catalogue (50 items) ────────────────────────────────────────────

    public static final Item[] ALL_ITEMS = {
        // ── Minerals & Gems ──────────────────────────────────────────────────
        new Item(1,  "Coal Chunk",           "Minerals & Gems", Rarity.COMMON,    10,   R.drawable.item_01_coal_chunk,             false),
        new Item(2,  "Iron Ore",             "Minerals & Gems", Rarity.COMMON,    10,   R.drawable.item_02_iron_ore,               false),
        new Item(3,  "Copper Nugget",        "Minerals & Gems", Rarity.COMMON,    10,   R.drawable.item_03_copper_nugget,          false),
        new Item(4,  "Quartz Crystal",       "Minerals & Gems", Rarity.COMMON,    10,   R.drawable.item_04_quartz_crystal,         false),
        new Item(5,  "Fool's Gold",          "Minerals & Gems", Rarity.COMMON,    10,   R.drawable.item_05_fools_gold,             false),
        new Item(6,  "Granite Chunk",        "Minerals & Gems", Rarity.COMMON,    10,   R.drawable.item_06_granite_chunk,          false),
        new Item(7,  "Flint Shard",          "Minerals & Gems", Rarity.COMMON,    10,   R.drawable.item_07_flint_shard,            false),
        new Item(8,  "Limestone Block",      "Minerals & Gems", Rarity.COMMON,    10,   R.drawable.item_08_limestone_block,        false),
        new Item(9,  "Obsidian Block",       "Minerals & Gems", Rarity.COMMON,    10,   R.drawable.item_09_obsidian_block,         false),
        new Item(10, "Sulfur Crystal",       "Minerals & Gems", Rarity.COMMON,    10,   R.drawable.item_10_sulfur_crystal,         false),
        new Item(11, "Ruby Shard",           "Minerals & Gems", Rarity.UNCOMMON,  30,   R.drawable.item_11_ruby_shard,             false),
        new Item(12, "Sapphire Fragment",    "Minerals & Gems", Rarity.UNCOMMON,  30,   R.drawable.item_12_sapphire_fragment,      false),
        new Item(13, "Emerald Sliver",       "Minerals & Gems", Rarity.UNCOMMON,  30,   R.drawable.item_13_emerald_sliver,         false),
        new Item(14, "Amethyst Chunk",       "Minerals & Gems", Rarity.UNCOMMON,  30,   R.drawable.item_14_amethyst_chunk,         false),
        new Item(15, "Lava Stone",           "Minerals & Gems", Rarity.UNCOMMON,  30,   R.drawable.item_15_lava_stone,             false),
        new Item(16, "Magnetite Ore",        "Minerals & Gems", Rarity.UNCOMMON,  30,   R.drawable.item_16_magnetite_ore,          false),
        new Item(17, "Malachite",            "Minerals & Gems", Rarity.UNCOMMON,  30,   R.drawable.item_17_malachite,              false),
        new Item(18, "Geode",                "Minerals & Gems", Rarity.RARE,      100,  R.drawable.item_18_geode,                  false),
        new Item(19, "Diamond",              "Minerals & Gems", Rarity.RARE,      100,  R.drawable.item_19_diamond,                false),
        new Item(20, "Topaz Fragment",       "Minerals & Gems", Rarity.RARE,      100,  R.drawable.item_20_topaz_fragment,         false),
        new Item(21, "Black Opal",           "Minerals & Gems", Rarity.RARE,      100,  R.drawable.item_21_black_opal,             false),
        new Item(22, "Moon Rock",            "Minerals & Gems", Rarity.EPIC,      300,  R.drawable.item_22_moon_rock,              false),
        new Item(23, "Blood Ruby",           "Minerals & Gems", Rarity.EPIC,      300,  R.drawable.item_23_blood_ruby,             false),
        new Item(24, "Void Shard",           "Minerals & Gems", Rarity.EPIC,      300,  R.drawable.item_24_void_shard,             false),
        new Item(25, "Mole Jesus Fragment",  "Minerals & Gems", Rarity.LEGENDARY, 1000, R.drawable.item_25_mole_jesus_fragment,    false),

        // ── Fossils ──────────────────────────────────────────────────────────
        new Item(26, "Dino Bone",            "Fossils", Rarity.UNCOMMON,  30,   R.drawable.item_26_dino_bone,              false),
        new Item(27, "Ancient Skull",        "Fossils", Rarity.RARE,      100,  R.drawable.item_27_ancient_skull,          false),
        new Item(28, "Petrified Egg",        "Fossils", Rarity.RARE,      100,  R.drawable.item_28_petrified_egg,          false),
        new Item(29, "Amber with Bug",       "Fossils", Rarity.RARE,      100,  R.drawable.item_29_amber_with_bug,         false),
        new Item(30, "Fossilized Claw",      "Fossils", Rarity.UNCOMMON,  30,   R.drawable.item_30_fossilized_claw,        false),
        new Item(31, "Stone Fish",           "Fossils", Rarity.COMMON,    10,   R.drawable.item_31_stone_fish,             false),
        new Item(32, "Root Fossil",          "Fossils", Rarity.COMMON,    10,   R.drawable.item_32_root_fossil,            false),
        new Item(33, "Ancient Coin",         "Fossils", Rarity.UNCOMMON,  30,   R.drawable.item_33_ancient_coin,           false),
        new Item(34, "Trilobite Shell",      "Fossils", Rarity.COMMON,    10,   R.drawable.item_34_trilobite_shell,        false),
        new Item(35, "Mammoth Tusk Chip",    "Fossils", Rarity.RARE,      100,  R.drawable.item_35_mammoth_tusk_chip,      false),

        // ── Artifacts ────────────────────────────────────────────────────────
        new Item(36, "Rusted Gear",          "Artifacts", Rarity.COMMON,    10,   R.drawable.item_36_rusted_gear,            false),
        new Item(37, "Broken Watch",         "Artifacts", Rarity.UNCOMMON,  30,   R.drawable.item_37_broken_watch,           false),
        new Item(38, "Old Boot",             "Artifacts", Rarity.COMMON,    10,   R.drawable.item_38_old_boot,               false),
        new Item(39, "Buried Treasure Chest","Artifacts", Rarity.EPIC,      300,  R.drawable.item_39_buried_treasure_chest,  false),
        new Item(40, "Strange Idol",         "Artifacts", Rarity.RARE,      100,  R.drawable.item_40_strange_idol,           false),
        new Item(41, "Cracked Compass",      "Artifacts", Rarity.UNCOMMON,  30,   R.drawable.item_41_cracked_compass,        false),
        new Item(42, "Underground Shrine",   "Artifacts", Rarity.EPIC,      300,  R.drawable.item_42_underground_shrine,     false),
        new Item(43, "Mole Helmet",          "Artifacts", Rarity.RARE,      100,  R.drawable.item_43_mole_helmet,            false),
        new Item(44, "Mystery Box",          "Artifacts", Rarity.EPIC,      300,  R.drawable.item_44_mystery_box,            false),
        new Item(45, "Mole Jesus Relic",     "Artifacts", Rarity.LEGENDARY, 1000, R.drawable.item_45_mole_jesus_relic,       false),

        // ── Hidden (never drop from normal rolls) ─────────────────────────────
        new Item(46, "Riccardo Statue",      "Hidden", Rarity.LEGENDARY, 1000, R.drawable.item_46_riccardo_statue,        true),
        new Item(47, "Kasidet Statue",       "Hidden", Rarity.LEGENDARY, 1000, R.drawable.item_47_kasidet_statue,         true),
        new Item(48, "Snow Dog Plushie",     "Hidden", Rarity.LEGENDARY, 1000, R.drawable.item_48_snow_dog_plushie,       true),
        new Item(49, "White Cat Plushie",    "Hidden", Rarity.LEGENDARY, 1000, R.drawable.item_49_white1_cat_plushie,     true),
        new Item(50, "Mole Plushie",         "Hidden", Rarity.LEGENDARY, 1000, R.drawable.item_50_mole_plushie,           true),
    };

    // ─── Lookups ─────────────────────────────────────────────────────────────

    /** Returns the Item for a 1-indexed id (1–50). */
    public static Item getItem(int id) {
        return ALL_ITEMS[id - 1];
    }

    public static int getCoinValue(Rarity r) {
        switch (r) {
            case UNCOMMON:  return 30;
            case RARE:      return 100;
            case EPIC:      return 300;
            case LEGENDARY: return 1000;
            default:        return 10; // COMMON
        }
    }

    public static int getBorderColor(Rarity r) {
        switch (r) {
            case UNCOMMON:  return Color.parseColor("#4CAF50");
            case RARE:      return Color.parseColor("#2196F3");
            case EPIC:      return Color.parseColor("#9C27B0");
            case LEGENDARY: return Color.parseColor("#FFD700");
            default:        return Color.parseColor("#888888"); // COMMON
        }
    }

    // ─── Random roll ─────────────────────────────────────────────────────────

    /**
     * Rolls a random non-hidden item adjusted for the given rarity boost level.
     * Rarity boost level 1 = default table; each level above 1 shifts -2% from
     * Common and distributes +0.5% to each other tier.
     *
     * Drop table:
     * L1: Common=50 / Uncommon=30 / Rare=15 / Epic=4 / Legendary=1
     * L5: Common=42 / Uncommon=32 / Rare=17 / Epic=6  / Legendary=3
     */
    public static Item rollRandomItem(int rarityBoostLevel) {
        int   bonus      = Math.max(0, rarityBoostLevel - 1);
        float legendary  = 1f  + bonus * 0.5f;
        float epic       = 4f  + bonus * 0.5f;
        float rare       = 15f + bonus * 0.5f;
        float uncommon   = 30f + bonus * 0.5f;
        // common = remainder; not needed explicitly

        float roll = (float)(Math.random() * 100f);
        Rarity tier;
        if (roll < legendary) {
            tier = Rarity.LEGENDARY;
        } else if (roll < legendary + epic) {
            tier = Rarity.EPIC;
        } else if (roll < legendary + epic + rare) {
            tier = Rarity.RARE;
        } else if (roll < legendary + epic + rare + uncommon) {
            tier = Rarity.UNCOMMON;
        } else {
            tier = Rarity.COMMON;
        }

        // Collect non-hidden items of the rolled tier
        ArrayList<Item> pool = new ArrayList<>();
        for (Item item : ALL_ITEMS) {
            if (!item.isHidden && item.rarity == tier) {
                pool.add(item);
            }
        }

        // Fallback: if the pool is somehow empty, return any non-hidden item
        if (pool.isEmpty()) {
            for (Item item : ALL_ITEMS) {
                if (!item.isHidden) pool.add(item);
            }
        }

        return pool.get((int)(Math.random() * pool.size()));
    }
}
