package org.classapp.molediver;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    private static final String TAG = "GameView";

    // ─── Sprite dimensions ────────────────────────────────────────────────────
    private static final int ITEM_SIZE  = 40;
    private static final int CANISTER_W = 30;
    private static final int CANISTER_H = 50;
    private static final int COIN_SIZE  = 32;
    private static final int TILE_SIZE  = 128; // on-screen tile size (source PNGs are 64x64)

    // ─── Terrain layer table ──────────────────────────────────────────────────
    // Columns: minDepth, maxDepth, topR, topG, topB, botR, botG, botB
    private static final int[][] LAYERS = {
        {    0,   100,  101, 67, 33,   80, 50, 25 },  // 0 Topsoil
        {  100,   300,   80, 50, 25,  140, 90, 60 },  // 1 Clay
        {  300,   600,  140, 90, 60,   80, 80, 85 },  // 2 Rock
        {  600,  1000,   80, 80, 85,   45, 45, 50 },  // 3 Deep Rock
        { 1000,  1500,   45, 45, 50,   25, 20, 22 },  // 4 Bedrock
        { 1500, 999999,  25, 20, 22,   15,  5,  5 },  // 5 Core
    };
    private static final String[] LAYER_NAMES = {
        "TOPSOIL", "CLAY LAYER", "ROCK LAYER", "DEEP ROCK", "BEDROCK", "THE CORE"
    };
    private static final int[] TILE_RES_IDS = {
        R.drawable.tile_topsoil, R.drawable.tile_clay,  R.drawable.tile_rock,
        R.drawable.tile_deep_rock, R.drawable.tile_bedrock, R.drawable.tile_core,
    };

    // ─── Enemy constants ──────────────────────────────────────────────────────
    private static final float[] ENEMY_RADIUS    = { 15f,  20f,  25f };
    private static final float[] ENEMY_SPEED     = { 2.0f, 3.0f, 4.0f };
    private static final int[]   ENEMY_DAMAGE    = { 15,   25,   40 };
    private static final float[] ENEMY_MIN_DEPTH = { 50f,  200f, 500f }; // metres
    private static final int[]   ENEMY_COLORS    = {
        0xFFCC3333, // Worm — red
        0xFFCC8833, // Beetle — orange
        0xFF8833CC, // Rock Crawler — purple
    };
    private static final String[] ENEMY_NAMES = { "Worm", "Beetle", "Rock Crawler" };
    private static final int MAX_ENEMIES = 8;

    // Enemy sprite resources and on-screen sizes (indexed by type 0/1/2)
    private static final int[] ENEMY_RES_IDS  = {
        R.drawable.enemy_worm, R.drawable.enemy_beetle, R.drawable.enemy_rock_crawler,
    };
    private static final int[] ENEMY_SPRITE_W = { 48, 56, 64 };
    private static final int[] ENEMY_SPRITE_H = { 48, 56, 64 };

    // ─── Context ─────────────────────────────────────────────────────────────
    private final Context context;

    // ─── Game loop ───────────────────────────────────────────────────────────
    private Thread           thread;
    private volatile boolean playing;
    private int              frameCount;

    // ─── Screen ──────────────────────────────────────────────────────────────
    private int screenW, screenH;

    // ─── Mole ────────────────────────────────────────────────────────────────
    private static final float MOLE_RADIUS = 28f;
    private static final float MAX_ANGLE   = 75f;

    private float moveSpeed, turnSpeed;
    private float moleWorldX, moleWorldY, moleAngle;

    // ─── Touch ───────────────────────────────────────────────────────────────
    private volatile boolean pressingLeft, pressingRight;

    // ─── Camera / depth ──────────────────────────────────────────────────────
    private float cameraY, depthMetres;

    // ─── Gas ─────────────────────────────────────────────────────────────────
    private static final float GAS_SPEED = 1.2f;
    private float gasWorldY;

    // ─── Oxygen ──────────────────────────────────────────────────────────────
    private static final float OXY_DRAIN = 0.03f;
    private float maxOxygen, oxygen;

    // ─── O2 canisters ────────────────────────────────────────────────────────
    private final ArrayList<float[]> canisters = new ArrayList<>();
    private float lastCanisterSpawnY, o2RestoreAmount, collectRange;

    // ─── Coin pickups ────────────────────────────────────────────────────────
    private final ArrayList<float[]> coinItems = new ArrayList<>();
    private float lastCoinSpawnY;
    private int   runCoins;

    // ─── Collectible item nodes ───────────────────────────────────────────────
    private static final class ItemNode {
        float worldX, worldY;
        ItemCatalogue.Item item;
        ItemNode(float x, float y, ItemCatalogue.Item item) {
            this.worldX = x; this.worldY = y; this.item = item;
        }
    }
    private final ArrayList<ItemNode> itemNodes = new ArrayList<>();
    private float lastItemSpawnY;
    private int   rarityBoostLevel;

    // "NEW!" item flash
    private static final int FLASH_DURATION = 75;
    private int    newItemFlashTimer;
    private String newItemFlashName;

    // ─── Terrain layer transition ─────────────────────────────────────────────
    private int    currentLayerIdx = -1;
    private int    layerFlashTimer;
    private String layerFlashName;

    // ─── Enemies ─────────────────────────────────────────────────────────────
    private static final class Enemy {
        float worldX, worldY;
        float velocityX;
        int   type;
        float radius;
        float speed;
        int   damage;
    }
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private float lastEnemySpawnY;
    private int   clawLevel;
    private int   hitFlashTimer;

    // ─── Game-over stats ─────────────────────────────────────────────────────
    private volatile boolean gameOver;
    private String  deathCause;
    private boolean isNewRecord;
    private int     newItemsThisRun;

    // ─── Cached bitmaps ──────────────────────────────────────────────────────
    private final SparseArray<Bitmap> itemBitmaps = new SparseArray<>();
    private final Bitmap[] tileBitmaps        = new Bitmap[TILE_RES_IDS.length];
    private final Bitmap[] enemyBitmapsRight  = new Bitmap[3]; // facing right (normal)
    private final Bitmap[] enemyBitmapsLeft   = new Bitmap[3]; // facing left (h-flipped)
    private Bitmap bmpCoin;
    private Bitmap bmpCanister;

    // ─── Paints ──────────────────────────────────────────────────────────────
    private final Paint tilePaint         = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint molePaint         = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyePaint          = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gasPaint          = new Paint();
    private final Paint hudPaint          = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coinHudPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint         = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint oxyBgPaint        = new Paint();
    private final Paint oxyFillPaint      = new Paint();
    private final Paint oxyLabelPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint newItemFlashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint layerFlashPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fallbackPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyBodyPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyLabelPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyHudPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyDotPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hitFlashPaint     = new Paint();
    // Game-over overlay
    private final Paint dimPaint          = new Paint();
    private final Paint goTitlePaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goCausePaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goDepthPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goNewRecordPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goStatPaint       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goNewItemsPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goButtonPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goButtonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameView(Context context) {
        super(context);
        this.context = context.getApplicationContext();
        getHolder().addCallback(this);
        setFocusable(true);

        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        molePaint.setColor(Color.rgb(101, 60, 20));
        eyePaint.setColor(Color.WHITE);
        gasPaint.setColor(Color.argb(120, 80, 200, 80));

        hudPaint.setColor(Color.WHITE);
        hudPaint.setTextSize(sp(dm, 48f));
        hudPaint.setTextAlign(Paint.Align.CENTER);
        hudPaint.setFakeBoldText(true);

        coinHudPaint.setColor(Color.rgb(245, 197, 66));
        coinHudPaint.setTextSize(sp(dm, 20f));
        coinHudPaint.setTextAlign(Paint.Align.LEFT);
        coinHudPaint.setFakeBoldText(true);

        hintPaint.setColor(Color.WHITE);
        hintPaint.setTextSize(sp(dm, 18f));

        oxyBgPaint.setColor(Color.rgb(40, 40, 40));

        oxyLabelPaint.setColor(Color.WHITE);
        oxyLabelPaint.setTextSize(sp(dm, 14f));
        oxyLabelPaint.setTextAlign(Paint.Align.CENTER);

        newItemFlashPaint.setColor(Color.parseColor("#4CAF50"));
        newItemFlashPaint.setTextSize(sp(dm, 20f));
        newItemFlashPaint.setTextAlign(Paint.Align.CENTER);
        newItemFlashPaint.setFakeBoldText(true);

        layerFlashPaint.setColor(Color.WHITE);
        layerFlashPaint.setTextSize(sp(dm, 28f));
        layerFlashPaint.setTextAlign(Paint.Align.CENTER);
        layerFlashPaint.setFakeBoldText(true);

        fallbackPaint.setStyle(Paint.Style.FILL);

        // Enemy paints (body used as fallback when bitmap is null)
        enemyBodyPaint.setStyle(Paint.Style.FILL);

        enemyLabelPaint.setTextSize(sp(dm, 10f));
        enemyLabelPaint.setTextAlign(Paint.Align.CENTER);

        enemyHudPaint.setColor(Color.WHITE);
        enemyHudPaint.setTextSize(sp(dm, 14f));
        enemyHudPaint.setTextAlign(Paint.Align.RIGHT);
        enemyHudPaint.setFakeBoldText(true);

        enemyDotPaint.setColor(Color.RED);
        enemyDotPaint.setStyle(Paint.Style.FILL);

        hitFlashPaint.setColor(Color.argb(80, 255, 0, 0));

        // Game-over overlay paints
        dimPaint.setColor(Color.argb(180, 0, 0, 0));

        goTitlePaint.setColor(Color.RED);
        goTitlePaint.setTextSize(sp(dm, 48f));
        goTitlePaint.setTextAlign(Paint.Align.CENTER);
        goTitlePaint.setFakeBoldText(true);

        goCausePaint.setColor(Color.WHITE);
        goCausePaint.setTextSize(sp(dm, 18f));
        goCausePaint.setTextAlign(Paint.Align.CENTER);

        goDepthPaint.setColor(Color.rgb(245, 197, 66));
        goDepthPaint.setTextSize(sp(dm, 24f));
        goDepthPaint.setTextAlign(Paint.Align.CENTER);
        goDepthPaint.setFakeBoldText(true);

        goNewRecordPaint.setColor(Color.parseColor("#4CAF50"));
        goNewRecordPaint.setTextSize(sp(dm, 20f));
        goNewRecordPaint.setTextAlign(Paint.Align.CENTER);
        goNewRecordPaint.setFakeBoldText(true);

        goStatPaint.setColor(Color.rgb(245, 197, 66));
        goStatPaint.setTextSize(sp(dm, 20f));
        goStatPaint.setTextAlign(Paint.Align.CENTER);

        goNewItemsPaint.setTextSize(sp(dm, 18f));
        goNewItemsPaint.setTextAlign(Paint.Align.CENTER);

        goButtonPaint.setStyle(Paint.Style.FILL);

        goButtonTextPaint.setColor(Color.WHITE);
        goButtonTextPaint.setTextSize(sp(dm, 14f));
        goButtonTextPaint.setTextAlign(Paint.Align.CENTER);
        goButtonTextPaint.setFakeBoldText(true);
    }

    private static float sp(DisplayMetrics dm, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm);
    }

    // ─── Terrain helpers ──────────────────────────────────────────────────────

    private static int layerIndex(float depth) {
        for (int i = 0; i < LAYERS.length; i++) {
            if (depth < LAYERS[i][1]) return i;
        }
        return LAYERS.length - 1;
    }

    private static int terrainFallbackColor(float depth) {
        for (int[] L : LAYERS) {
            if (depth < L[1]) {
                float t = Math.max(0f, Math.min(1f,
                    (depth - L[0]) / (float)(L[1] - L[0])));
                return Color.rgb(lerp(L[2], L[5], t),
                                 lerp(L[3], L[6], t),
                                 lerp(L[4], L[7], t));
            }
        }
        int[] core = LAYERS[LAYERS.length - 1];
        return Color.rgb(core[5], core[6], core[7]);
    }

    private static int darkenColor(int c) {
        return Color.rgb(
            (int)(Color.red(c)   * 0.5f),
            (int)(Color.green(c) * 0.5f),
            (int)(Color.blue(c)  * 0.5f));
    }

    // ─── Tile rendering ───────────────────────────────────────────────────────

    /** Tiles a bitmap across the full screen, scrolled to match cameraY. */
    private void drawTiledBackground(Canvas canvas, Bitmap tile) {
        if (tile == null) return;
        // First tile row that intersects or is above the screen top
        float startY = (float)(Math.floor(cameraY / TILE_SIZE) * TILE_SIZE) - cameraY;
        for (float ty = startY; ty < screenH; ty += TILE_SIZE) {
            for (float tx = 0; tx < screenW; tx += TILE_SIZE) {
                canvas.drawBitmap(tile, tx, ty, tilePaint);
            }
        }
    }

    // ─── Bitmap loading / recycling ───────────────────────────────────────────

    private void loadBitmaps() {
        recycleBitmaps();
        Resources res = context.getResources();

        // Terrain tiles
        for (int i = 0; i < TILE_RES_IDS.length; i++) {
            tileBitmaps[i] = scale(BitmapFactory.decodeResource(res, TILE_RES_IDS[i]),
                                   TILE_SIZE, TILE_SIZE);
        }

        // Pickup sprites
        bmpCoin     = scale(BitmapFactory.decodeResource(res, R.drawable.coin),
                            COIN_SIZE, COIN_SIZE);
        bmpCanister = scale(BitmapFactory.decodeResource(res, R.drawable.item_51_pickup_oxygen_tank),
                            CANISTER_W, CANISTER_H);

        // Enemy sprites — load, scale, then pre-create h-flipped copies
        Matrix flipMatrix = new Matrix();
        flipMatrix.preScale(-1, 1);
        for (int t = 0; t < 3; t++) {
            Bitmap right = scale(BitmapFactory.decodeResource(res, ENEMY_RES_IDS[t]),
                                 ENEMY_SPRITE_W[t], ENEMY_SPRITE_H[t]);
            enemyBitmapsRight[t] = right;
            if (right != null) {
                enemyBitmapsLeft[t] = Bitmap.createBitmap(
                    right, 0, 0, right.getWidth(), right.getHeight(), flipMatrix, false);
            }
        }

        // Collectible item sprites
        for (ItemCatalogue.Item item : ItemCatalogue.ALL_ITEMS) {
            Bitmap raw = BitmapFactory.decodeResource(res, item.spriteResId);
            if (raw != null) itemBitmaps.put(item.id, scale(raw, ITEM_SIZE, ITEM_SIZE));
        }
    }

    private static Bitmap scale(Bitmap src, int w, int h) {
        if (src == null) return null;
        Bitmap scaled = Bitmap.createScaledBitmap(src, w, h, true);
        if (scaled != src) src.recycle();
        return scaled;
    }

    private void recycleBitmaps() {
        for (int i = 0; i < tileBitmaps.length; i++) {
            if (tileBitmaps[i] != null && !tileBitmaps[i].isRecycled()) {
                tileBitmaps[i].recycle();
            }
            tileBitmaps[i] = null;
        }
        if (bmpCoin     != null) { bmpCoin.recycle();     bmpCoin     = null; }
        if (bmpCanister != null) { bmpCanister.recycle(); bmpCanister = null; }
        for (int t = 0; t < 3; t++) {
            if (enemyBitmapsRight[t] != null && !enemyBitmapsRight[t].isRecycled()) {
                enemyBitmapsRight[t].recycle();
            }
            enemyBitmapsRight[t] = null;
            if (enemyBitmapsLeft[t] != null && !enemyBitmapsLeft[t].isRecycled()) {
                enemyBitmapsLeft[t].recycle();
            }
            enemyBitmapsLeft[t] = null;
        }
        for (int i = 0; i < itemBitmaps.size(); i++) {
            Bitmap b = itemBitmaps.valueAt(i);
            if (b != null && !b.isRecycled()) b.recycle();
        }
        itemBitmaps.clear();
    }

    // ─── Enemy helpers ────────────────────────────────────────────────────────

    private void spawnEnemy() {
        // Collect eligible types for current depth
        int typeCount = 0;
        int[] types = new int[3];
        for (int t = 0; t < ENEMY_MIN_DEPTH.length; t++) {
            if (depthMetres >= ENEMY_MIN_DEPTH[t]) types[typeCount++] = t;
        }
        if (typeCount == 0) return;

        int type = types[(int)(Math.random() * typeCount)];
        Enemy e  = new Enemy();
        e.type      = type;
        e.radius    = ENEMY_RADIUS[type];
        e.speed     = ENEMY_SPEED[type];
        e.damage    = ENEMY_DAMAGE[type];
        e.worldX    = e.radius + (float)(Math.random() * (screenW - 2 * e.radius));
        e.worldY    = moleWorldY + screenH * 0.85f; // spawn below visible area
        e.velocityX = Math.random() < 0.5f ? e.speed : -e.speed;
        enemies.add(e);
    }

    // ─── State reset ─────────────────────────────────────────────────────────

    private void resetGame() {
        if (screenW == 0) return;

        int oxyLevel     = PlayerData.getUpgradeOxygen(context);
        int steerLevel   = PlayerData.getUpgradeSteer(context);
        int digLevel     = PlayerData.getUpgradeDig(context);
        int rangeLevel   = PlayerData.getUpgradeRange(context);
        int refillLevel  = PlayerData.getUpgradeO2Refill(context);
        rarityBoostLevel = PlayerData.getUpgradeRarity(context);
        clawLevel        = PlayerData.getUpgradeClaw(context);

        maxOxygen       = 100f + (oxyLevel    - 1) * 25f;
        turnSpeed       = 2f   + (steerLevel  - 1) * 0.5f;
        moveSpeed       = 5f   + (digLevel    - 1) * 1.5f;
        collectRange    = 50f  + (rangeLevel  - 1) * 15f;
        o2RestoreAmount = 10f  + (refillLevel - 1) * 5f;

        moleWorldX  = screenW / 2f;
        moleWorldY  = 200f;
        moleAngle   = 0f;
        cameraY     = moleWorldY - screenH * 0.4f;
        gasWorldY   = -300f;
        oxygen      = maxOxygen;
        depthMetres = 0f;
        runCoins    = 0;
        frameCount  = 0;

        canisters.clear();
        coinItems.clear();
        itemNodes.clear();
        enemies.clear();

        lastCanisterSpawnY = moleWorldY;
        lastCoinSpawnY     = moleWorldY;
        lastItemSpawnY     = moleWorldY;
        lastEnemySpawnY    = moleWorldY;

        newItemFlashTimer = 0;
        newItemFlashName  = null;
        newItemsThisRun   = 0;

        currentLayerIdx = -1;
        layerFlashTimer = 0;
        layerFlashName  = null;

        hitFlashTimer = 0;
        deathCause    = null;
        isNewRecord   = false;

        pressingLeft  = false;
        pressingRight = false;
        gameOver      = false;
    }

    // ─── Game loop ───────────────────────────────────────────────────────────

    @Override
    public void run() {
        while (playing) {
            long start = System.currentTimeMillis();
            if (!gameOver) update();
            draw();
            long sleepMs = 16L - (System.currentTimeMillis() - start);
            if (sleepMs > 0) {
                try { Thread.sleep(sleepMs); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    private void update() {
        if (screenW == 0) return;
        frameCount++;

        // Steering
        if (pressingLeft)  moleAngle = Math.max(moleAngle - turnSpeed, -MAX_ANGLE);
        if (pressingRight) moleAngle = Math.min(moleAngle + turnSpeed,  MAX_ANGLE);

        // Movement
        double rad = Math.toRadians(moleAngle);
        moleWorldX += (float)(Math.sin(rad) * moveSpeed);
        moleWorldY += (float)(Math.cos(rad) * moveSpeed);
        moleWorldX = Math.max(MOLE_RADIUS, Math.min(moleWorldX, screenW - MOLE_RADIUS));

        cameraY     = moleWorldY - screenH * 0.4f;
        depthMetres = Math.max(cameraY / 10f, 0f);

        // Terrain layer transition detection
        int newLayerIdx = layerIndex(depthMetres);
        if (newLayerIdx != currentLayerIdx) {
            if (currentLayerIdx >= 0) {
                layerFlashTimer = 90;
                layerFlashName  = LAYER_NAMES[newLayerIdx];
            }
            currentLayerIdx = newLayerIdx;
        }

        // Gas
        gasWorldY += GAS_SPEED;
        if (gasWorldY >= moleWorldY) {
            Log.d(TAG, "Game over: gas at depth " + (int)depthMetres + "m");
            deathCause = "GAS CAUGHT YOU";
            saveRunResults(); return;
        }

        // Oxygen
        oxygen = Math.max(oxygen - OXY_DRAIN, 0f);
        if (oxygen <= 0f) {
            Log.d(TAG, "Game over: oxygen depleted at depth " + (int)depthMetres + "m");
            deathCause = "OXYGEN DEPLETED";
            saveRunResults(); return;
        }

        float rangeSq = collectRange * collectRange;

        // ── Spawning ─────────────────────────────────────────────────────────

        if (moleWorldY - lastCanisterSpawnY >= 400f) {
            lastCanisterSpawnY = moleWorldY;
            float x = MOLE_RADIUS + (float)(Math.random() * (screenW - 2 * MOLE_RADIUS));
            canisters.add(new float[]{x, moleWorldY + screenH * 0.85f});
        }

        if (moleWorldY - lastCoinSpawnY >= 150f) {
            lastCoinSpawnY = moleWorldY;
            float x = MOLE_RADIUS + (float)(Math.random() * (screenW - 2 * MOLE_RADIUS));
            coinItems.add(new float[]{x, moleWorldY + screenH * 0.65f});
        }

        if (moleWorldY - lastItemSpawnY >= 200f) {
            lastItemSpawnY = moleWorldY;
            ItemCatalogue.Item item = ItemCatalogue.rollRandomItem(rarityBoostLevel);
            float x = MOLE_RADIUS + (float)(Math.random() * (screenW - 2 * MOLE_RADIUS));
            itemNodes.add(new ItemNode(x, moleWorldY + screenH * 0.75f, item));
        }

        if (moleWorldY - lastEnemySpawnY >= 300f && enemies.size() < MAX_ENEMIES) {
            lastEnemySpawnY = moleWorldY;
            int count = Math.random() < 0.4 ? 2 : 1;
            for (int i = 0; i < count && enemies.size() < MAX_ENEMIES; i++) spawnEnemy();
        }

        // ── Collect O2 canisters ──────────────────────────────────────────────
        Iterator<float[]> canIter = canisters.iterator();
        while (canIter.hasNext()) {
            float[] c  = canIter.next();
            float   dx = moleWorldX - c[0], dy = moleWorldY - c[1];
            if (dx*dx + dy*dy < rangeSq) {
                canIter.remove();
                oxygen = Math.min(oxygen + o2RestoreAmount, maxOxygen);
            }
        }

        // ── Collect coins ─────────────────────────────────────────────────────
        boolean multiplier = PlayerData.hasCoinMultiplier(context);
        Iterator<float[]> coinIter = coinItems.iterator();
        while (coinIter.hasNext()) {
            float[] c  = coinIter.next();
            float   dx = moleWorldX - c[0], dy = moleWorldY - c[1];
            if (dx*dx + dy*dy < rangeSq) {
                coinIter.remove();
                runCoins += multiplier ? 2 : 1;
            }
        }

        // ── Collect item nodes ────────────────────────────────────────────────
        Iterator<ItemNode> itemIter = itemNodes.iterator();
        while (itemIter.hasNext()) {
            ItemNode node = itemIter.next();
            float dx = moleWorldX - node.worldX, dy = moleWorldY - node.worldY;
            if (dx*dx + dy*dy < rangeSq) {
                itemIter.remove();
                boolean isNew = !PlayerData.isItemCollected(context, node.item.id);
                PlayerData.markItemCollected(context, node.item.id);
                int value = node.item.coinValue;
                if (node.item.rarity == ItemCatalogue.Rarity.COMMON
                        && PlayerData.hasCommonDoubler(context)) value *= 2;
                if (multiplier) value *= 2;
                runCoins += value;
                if (isNew) {
                    newItemsThisRun++;
                    newItemFlashTimer = FLASH_DURATION;
                    newItemFlashName  = node.item.name;
                    checkAndClaimSets();
                }
            }
        }

        // ── Update enemies ────────────────────────────────────────────────────
        Iterator<Enemy> enemyIter = enemies.iterator();
        int eIdx = 0;
        while (enemyIter.hasNext()) {
            Enemy e = enemyIter.next();

            // Cull enemies that have scrolled far above the camera
            if (e.worldY < cameraY - 800f) {
                enemyIter.remove();
                continue;
            }

            // Horizontal patrol with wall bounce
            e.worldX += e.velocityX;
            if (e.worldX < 50f) {
                e.worldX = 50f;
                e.velocityX = Math.abs(e.velocityX);
            } else if (e.worldX > screenW - 50f) {
                e.worldX = screenW - 50f;
                e.velocityX = -Math.abs(e.velocityX);
            }

            // Vertical wobble
            e.worldY += (float)(Math.sin(frameCount * 0.05 + eIdx) * 0.3);

            // Collision with mole
            float dx   = moleWorldX - e.worldX;
            float dy   = moleWorldY - e.worldY;
            float minD = MOLE_RADIUS + e.radius;
            if (dx*dx + dy*dy < minD * minD) {
                enemyIter.remove();
                int dmg = Math.max(0, e.damage - (clawLevel - 1) * 3);
                oxygen = Math.max(0f, oxygen - dmg);
                hitFlashTimer = 15;
                continue;
            }

            eIdx++;
        }

        // ── Timers ───────────────────────────────────────────────────────────
        if (newItemFlashTimer > 0) newItemFlashTimer--;
        if (layerFlashTimer   > 0) layerFlashTimer--;
        if (hitFlashTimer     > 0) hitFlashTimer--;
    }

    private void saveRunResults() {
        float prevBest = PlayerData.getBestDepth(context);
        isNewRecord = depthMetres > prevBest;
        PlayerData.addCoins(context, runCoins);
        if (isNewRecord) PlayerData.setBestDepth(context, depthMetres);
        gameOver = true;
    }

    private void checkAndClaimSets() {
        for (SetManager.SetDef set : SetManager.ALL_SETS) {
            if (!PlayerData.isSetRewardClaimed(context, set.name)
                    && SetManager.isSetComplete(context, set)) {
                PlayerData.claimSetReward(context, set.name);
            }
        }
    }

    // ─── Draw ────────────────────────────────────────────────────────────────

    private void draw() {
        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;

        try {
            // ── Terrain background ────────────────────────────────────────────
            int layerIdx = layerIndex(depthMetres);

            if (tileBitmaps[layerIdx] == null) {
                // Fallback: solid interpolated colour
                canvas.drawColor(terrainFallbackColor(depthMetres));
            } else {
                // Determine crossfade to next layer (within 50m of boundary)
                float layerMaxDepth   = LAYERS[layerIdx][1];
                float depthToNext     = layerMaxDepth - depthMetres;
                boolean nearBoundary  = depthToNext < 50f && layerIdx < LAYERS.length - 1;

                if (!nearBoundary) {
                    tilePaint.setAlpha(255);
                    drawTiledBackground(canvas, tileBitmaps[layerIdx]);
                } else {
                    // Blend progress: 0 when 50m from boundary, 1 at boundary
                    float blend = (50f - depthToNext) / 50f;
                    // Primary fades slightly (255 → ~200), secondary fades in (0 → ~55)
                    int primAlpha = Math.max(0, (int)(255 - blend * 55f));
                    int secAlpha  = Math.min(255, (int)(blend * 55f));

                    tilePaint.setAlpha(primAlpha);
                    drawTiledBackground(canvas, tileBitmaps[layerIdx]);

                    if (tileBitmaps[layerIdx + 1] != null && secAlpha > 0) {
                        tilePaint.setAlpha(secAlpha);
                        drawTiledBackground(canvas, tileBitmaps[layerIdx + 1]);
                    }
                }

                // Depth darkening overlay within each layer (0 → alpha 40)
                int[] L = LAYERS[layerIdx];
                float layerT = L[1] < 999999
                    ? Math.max(0f, Math.min(1f, (depthMetres - L[0]) / (float)(L[1] - L[0])))
                    : 1f;
                int darkAlpha = (int)(layerT * 40f);
                if (darkAlpha > 0) canvas.drawColor(Color.argb(darkAlpha, 0, 0, 0));
            }

            // ── Gas cloud ─────────────────────────────────────────────────────
            float gasScreenY = gasWorldY - cameraY;
            if (gasScreenY > 0) canvas.drawRect(0, 0, screenW, gasScreenY, gasPaint);

            // ── O2 canisters ──────────────────────────────────────────────────
            for (float[] c : canisters) {
                float sx = c[0], sy = c[1] - cameraY;
                if (sy < -CANISTER_H || sy > screenH + CANISTER_H) continue;
                if (bmpCanister != null)
                    canvas.drawBitmap(bmpCanister, sx - CANISTER_W / 2f, sy - CANISTER_H / 2f, null);
            }

            // ── Coin pickups ──────────────────────────────────────────────────
            for (float[] c : coinItems) {
                float sx = c[0], sy = c[1] - cameraY;
                if (sy < -COIN_SIZE || sy > screenH + COIN_SIZE) continue;
                if (bmpCoin != null)
                    canvas.drawBitmap(bmpCoin, sx - COIN_SIZE / 2f, sy - COIN_SIZE / 2f, null);
            }

            // ── Collectible item nodes ────────────────────────────────────────
            for (ItemNode node : itemNodes) {
                float sx = node.worldX, sy = node.worldY - cameraY;
                if (sy < -ITEM_SIZE || sy > screenH + ITEM_SIZE) continue;
                Bitmap bmp = itemBitmaps.get(node.item.id);
                if (bmp != null) {
                    canvas.drawBitmap(bmp, sx - ITEM_SIZE / 2f, sy - ITEM_SIZE / 2f, null);
                } else {
                    fallbackPaint.setColor(ItemCatalogue.getBorderColor(node.item.rarity));
                    canvas.drawCircle(sx, sy, 16f, fallbackPaint);
                }
            }

            // ── Enemies ───────────────────────────────────────────────────────
            for (Enemy e : enemies) {
                float sx = e.worldX, sy = e.worldY - cameraY;
                int   sw = ENEMY_SPRITE_W[e.type], sh = ENEMY_SPRITE_H[e.type];
                if (sy < -(sh * 2) || sy > screenH + sh * 2) continue;

                // Pick facing bitmap; fall back to a filled circle if not loaded
                Bitmap bmp = e.velocityX >= 0
                    ? enemyBitmapsRight[e.type]
                    : enemyBitmapsLeft[e.type];
                if (bmp != null) {
                    canvas.drawBitmap(bmp, sx - sw / 2f, sy - sh / 2f, null);
                } else {
                    enemyBodyPaint.setColor(ENEMY_COLORS[e.type]);
                    canvas.drawCircle(sx, sy, e.radius, enemyBodyPaint);
                }

                // Name label — only when close to mole
                float dxE = moleWorldX - e.worldX, dyE = moleWorldY - e.worldY;
                if (dxE*dxE + dyE*dyE < 200f * 200f) {
                    enemyLabelPaint.setColor(ENEMY_COLORS[e.type]);
                    canvas.drawText(ENEMY_NAMES[e.type],
                        sx, sy + sh / 2f + enemyLabelPaint.getTextSize(), enemyLabelPaint);
                }
            }

            // ── Mole ─────────────────────────────────────────────────────────
            canvas.save();
            canvas.translate(moleWorldX, screenH * 0.4f);
            canvas.rotate(moleAngle);
            canvas.drawCircle(0, 0, MOLE_RADIUS, molePaint);
            canvas.drawCircle(-10f, 12f, 5f, eyePaint);
            canvas.drawCircle( 10f, 12f, 5f, eyePaint);
            canvas.restore();

            // ── Hit flash overlay ─────────────────────────────────────────────
            if (hitFlashTimer > 0) {
                canvas.drawRect(0, 0, screenW, screenH, hitFlashPaint);
            }

            // ── Oxygen bar ───────────────────────────────────────────────────
            int barW = 20, barH = 200, barMargin = 24;
            int barX      = screenW - barMargin - barW;
            int barTop    = screenH / 2 - barH / 2;
            int barBottom = barTop + barH;

            canvas.drawRect(barX, barTop, barX + barW, barBottom, oxyBgPaint);
            float fillFrac = oxygen / maxOxygen;
            oxyFillPaint.setColor(oxyBarColor(fillFrac));
            canvas.drawRect(barX, (int)(barBottom - barH * fillFrac),
                barX + barW, barBottom, oxyFillPaint);
            canvas.drawText("O2", barX + barW / 2f, barTop - 8f, oxyLabelPaint);

            // ── HUD: depth + coins ────────────────────────────────────────────
            canvas.drawText((int)depthMetres + "m",
                screenW / 2f, hudPaint.getTextSize() + 16f, hudPaint);
            canvas.drawText("● " + runCoins,
                24f, coinHudPaint.getTextSize() + 20f, coinHudPaint);

            // ── Enemy counter (bottom-right) ──────────────────────────────────
            float dotY = screenH - 30f;
            canvas.drawCircle(screenW - 66f, dotY, 6f, enemyDotPaint);
            canvas.drawText(String.valueOf(enemies.size()),
                screenW - 24f, dotY + enemyHudPaint.getTextSize() / 3f, enemyHudPaint);

            // ── "NEW ITEM!" flash ─────────────────────────────────────────────
            if (newItemFlashTimer > 0 && newItemFlashName != null) {
                float a = newItemFlashTimer > 20 ? 1f : newItemFlashTimer / 20f;
                newItemFlashPaint.setAlpha((int)(a * 255));
                canvas.drawText("NEW!  " + newItemFlashName,
                    screenW / 2f, screenH * 0.6f, newItemFlashPaint);
            }

            // ── Layer name flash ──────────────────────────────────────────────
            if (layerFlashTimer > 0 && layerFlashName != null) {
                float a = layerFlashTimer > 30 ? 1f : layerFlashTimer / 30f;
                layerFlashPaint.setAlpha((int)(a * 255));
                canvas.drawText(layerFlashName,
                    screenW / 2f, screenH * 0.5f, layerFlashPaint);
            }

            // ── Zone hints (first 20m) ────────────────────────────────────────
            if (depthMetres < 20f) {
                int alpha = depthMetres <= 15f
                    ? 180 : (int)(180f * (1f - (depthMetres - 15f) / 5f));
                hintPaint.setAlpha(alpha);
                hintPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("< LEFT", 40f, screenH - 60f, hintPaint);
                hintPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("RIGHT >", screenW - 40f, screenH - 60f, hintPaint);
            }

            // ── Game-over overlay ─────────────────────────────────────────────
            if (gameOver) {
                canvas.drawRect(0, 0, screenW, screenH, dimPaint);

                float cx = screenW / 2f;
                float y  = screenH * 0.10f;

                y += goTitlePaint.getTextSize();
                canvas.drawText("GAME OVER", cx, y, goTitlePaint);

                y += 16 + goCausePaint.getTextSize();
                canvas.drawText(deathCause != null ? deathCause : "", cx, y, goCausePaint);

                y += 24 + goDepthPaint.getTextSize();
                canvas.drawText("DEPTH: " + (int)depthMetres + "m", cx, y, goDepthPaint);

                if (isNewRecord) {
                    y += 8 + goNewRecordPaint.getTextSize();
                    canvas.drawText("NEW RECORD!", cx, y, goNewRecordPaint);
                }

                y += 16 + goStatPaint.getTextSize();
                canvas.drawText("COINS: +" + runCoins, cx, y, goStatPaint);

                y += 8 + goNewItemsPaint.getTextSize();
                goNewItemsPaint.setColor(newItemsThisRun > 0
                    ? Color.parseColor("#4CAF50") : Color.parseColor("#888888"));
                canvas.drawText("NEW ITEMS: " + newItemsThisRun, cx, y, goNewItemsPaint);

                y += 32;
                float btnH  = 60f;
                float btnMg = 20f;

                RectF leftBtn  = new RectF(btnMg,                y, screenW / 2f - btnMg, y + btnH);
                RectF rightBtn = new RectF(screenW / 2f + btnMg, y, screenW - btnMg,      y + btnH);

                float textOffY = goButtonTextPaint.getTextSize() / 3f;

                goButtonPaint.setColor(Color.parseColor("#1A4D1A"));
                canvas.drawRoundRect(leftBtn, 10f, 10f, goButtonPaint);
                canvas.drawText("TAP LEFT TO RETRY",
                    leftBtn.centerX(), leftBtn.centerY() + textOffY, goButtonTextPaint);

                goButtonPaint.setColor(Color.parseColor("#4D1A1A"));
                canvas.drawRoundRect(rightBtn, 10f, 10f, goButtonPaint);
                canvas.drawText("TAP RIGHT FOR MENU",
                    rightBtn.centerX(), rightBtn.centerY() + textOffY, goButtonTextPaint);
            }

        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static int lerp(int a, int b, float t) { return (int)(a + (b - a) * t); }

    private static int oxyBarColor(float f) {
        if (f > 0.5f) { float t = (f - 0.5f) / 0.5f; return Color.rgb((int)(255*(1-t)), 200, 0); }
        else          { float t = f / 0.5f;            return Color.rgb(255, (int)(200*t), 0); }
    }

    // ─── Touch ───────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (event.getX() < screenW / 2f) {
                    resetGame();
                } else {
                    ((Activity) getContext()).finish();
                }
            }
            return true;
        }

        int action = event.getActionMasked(), idx = event.getActionIndex();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                registerPress(event.getX(idx)); break;
            case MotionEvent.ACTION_MOVE:
                pressingLeft = pressingRight = false;
                for (int i = 0; i < event.getPointerCount(); i++) registerPress(event.getX(i));
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                registerRelease(event.getX(idx)); break;
            case MotionEvent.ACTION_CANCEL:
                pressingLeft = pressingRight = false; break;
        }
        return true;
    }

    private void registerPress  (float x) { if (x < screenW/2f) pressingLeft=true;  else pressingRight=true;  }
    private void registerRelease(float x) { if (x < screenW/2f) pressingLeft=false; else pressingRight=false; }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    public void resume() {
        playing = true;
        thread  = new Thread(this);
        thread.start();
    }

    public void pause() {
        playing = false;
        try { thread.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Override public void surfaceCreated(SurfaceHolder holder) { }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenW = width;
        screenH = height;
        loadBitmaps();
        resetGame();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        recycleBitmaps();
    }
}
