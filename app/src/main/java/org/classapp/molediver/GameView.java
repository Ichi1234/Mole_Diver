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
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.graphics.Rect;
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
    private static final int ITEM_SIZE = 40;
    private static final int CANISTER_W = 30;
    private static final int CANISTER_H = 50;
    private static final int COIN_SIZE = 32;
    private static final int TILE_SIZE = 128; // on-screen tile size (source PNGs are 64x64)

    // ─── Terrain layer table ──────────────────────────────────────────────────
    // Columns: minDepth, maxDepth, topR, topG, topB, botR, botG, botB
    private static final int[][] LAYERS = {
            {0, 100, 101, 67, 33, 80, 50, 25},  // 0 Topsoil
            {100, 300, 80, 50, 25, 140, 90, 60},  // 1 Clay
            {300, 600, 140, 90, 60, 80, 80, 85},  // 2 Rock
            {600, 1000, 80, 80, 85, 45, 45, 50},  // 3 Deep Rock
            {1000, 1500, 45, 45, 50, 25, 20, 22},  // 4 Bedrock
            {1500, 999999, 25, 20, 22, 15, 5, 5},  // 5 Core
    };
    private static final String[] LAYER_NAMES = {
            "TOPSOIL", "CLAY LAYER", "ROCK LAYER", "DEEP ROCK", "BEDROCK", "THE CORE"
    };
    private static final int[] TILE_RES_IDS = {
            R.drawable.tile_topsoil, R.drawable.tile_clay, R.drawable.tile_rock,
            R.drawable.tile_deep_rock, R.drawable.tile_bedrock, R.drawable.tile_core,
    };

    // ─── Enemy constants ──────────────────────────────────────────────────────
    private static final float[] ENEMY_RADIUS = {15f, 20f, 25f};
    private static final float[] ENEMY_SPEED = {2.0f, 3.0f, 4.0f};
    private static final int[] ENEMY_DAMAGE = {15, 25, 40};
    private static final float[] ENEMY_MIN_DEPTH = {50f, 200f, 500f}; // metres
    private static final int[] ENEMY_COLORS = {
            0xFFCC3333, // Worm — red
            0xFFCC8833, // Beetle — orange
            0xFF8833CC, // Rock Crawler — purple
    };
    private static final String[] ENEMY_NAMES = {"Worm", "Beetle", "Rock Crawler"};
    private static final int MAX_ENEMIES = 8;

    // Enemy sprite resources and on-screen sizes (indexed by type 0/1/2)
    private static final int[] ENEMY_RES_IDS = {
            R.drawable.enemy_worm, R.drawable.enemy_beetle, R.drawable.enemy_rock_crawler,
    };
    private static final int[] ENEMY_SPRITE_W = {48, 56, 64};
    private static final int[] ENEMY_SPRITE_H = {48, 56, 64};

    // ─── Context ─────────────────────────────────────────────────────────────
    private final Context context;

    // ─── Game loop ───────────────────────────────────────────────────────────
    private Thread thread;
    private volatile boolean playing;
    private int frameCount;

    // ─── Screen ──────────────────────────────────────────────────────────────
    private int screenW, screenH;

    // ─── Mole ────────────────────────────────────────────────────────────────
    private static final float MOLE_RADIUS = 50f;

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
    private int runCoins;

    // ─── Collectible item nodes ───────────────────────────────────────────────
    private static final class ItemNode {
        float worldX, worldY;
        ItemCatalogue.Item item;

        ItemNode(float x, float y, ItemCatalogue.Item item) {
            this.worldX = x;
            this.worldY = y;
            this.item = item;
        }
    }

    private final ArrayList<ItemNode> itemNodes = new ArrayList<>();
    private float lastItemSpawnY;
    private int rarityBoostLevel;

    // "NEW!" item flash
    private static final int FLASH_DURATION = 75;
    private int newItemFlashTimer;
    private String newItemFlashName;

    // ─── Terrain layer transition ─────────────────────────────────────────────
    private int currentLayerIdx = -1;
    private int layerFlashTimer;
    private String layerFlashName;

    // ─── Enemies ─────────────────────────────────────────────────────────────
    private static final class Enemy {
        float worldX, worldY;
        float velocityX;
        int type;
        float radius;
        float speed;
        int damage;
    }

    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private float lastEnemySpawnY;
    private int clawLevel;
    private int hitFlashTimer;

    // ─── Game-over stats ─────────────────────────────────────────────────────
    private volatile boolean gameOver;
    private String deathCause;
    private boolean isNewRecord;
    private int newItemsThisRun;
    // ─── Game state ──────────────────────────────────────────────────────────
    private volatile boolean isIntro = true;
    private volatile boolean isTransitioning = false;
    private float transitionProgress = 0f; // 0.0 to 1.0

    private int newItemsFoundThisRun = 0;
    private boolean newBestDepthThisRun = false;
    private boolean retryButtonHovered = false;
    private boolean menuButtonHovered = false;

    // ─── Background constants ────────────────────────────────────────────────
    private static final int BG_R_START = 101, BG_G_START = 67, BG_B_START = 33;
    private static final int BG_R_END = 20, BG_G_END = 12, BG_B_END = 6;
    private static final int GRASS_COLOR = Color.rgb(34, 139, 34);
    private static final int SKY_COLOR = Color.rgb(135, 206, 235);
    private static final float GRASS_HEIGHT = 200f; // World height of the grass layer

    // ─── Cached bitmaps ──────────────────────────────────────────────────────
    private final SparseArray<Bitmap> itemBitmaps = new SparseArray<>();
    private final Bitmap[] tileBitmaps = new Bitmap[TILE_RES_IDS.length];
    private final Bitmap[] enemyBitmapsRight = new Bitmap[3]; // facing right (normal)
    private final Bitmap[] enemyBitmapsLeft = new Bitmap[3]; // facing left (h-flipped)
    private Bitmap bmpCoin;
    private Bitmap bmpCanister;
    private Bitmap bmpDead;

    // Mole animation sheet (8 frames laid out horizontally).
    private static final int MOLE_SHEET_FRAMES = 8;
    private Bitmap moleSpriteSheet;
    private int moleFrameSrcW = 0;
    private int moleFrameSrcH = 0;
    private int moleFrameIndex = 0;
    private int moleFrameTimer = 0;
    private int moleFrameDelay = 6;   // ticks per frame (roughly 96ms at 60fps)

    // Intro mole animation (4 frames, 144x36 -> each 36x36)
    private static final int INTRO_SHEET_FRAMES = 4;
    private Bitmap moleIntroSheet;
    private int introFrameIndex = 0;
    private int introFrameTimer = 0;

    // ─── Audio ───────────────────────────────────────────────────────────────
    private MediaPlayer mediaPlayer;
    private SoundPool soundPool;
    private int pickupSoundId = -1;

    // ─── Paints ──────────────────────────────────────────────────────────────
    private final Paint bgPaint = new Paint();
    private final Paint tilePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint molePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gasPaint = new Paint();
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coinHudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint oxyBgPaint = new Paint();
    private final Paint oxyFillPaint = new Paint();
    private final Paint oxyLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint newItemFlashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint layerFlashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gameOverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gameOverSubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statsLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statsValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint retryButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint menuButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint introTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Fallback when a bitmap failed to load
    private final Paint fallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyHudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hitFlashPaint = new Paint();
    // Game-over overlay
    private final Paint dimPaint = new Paint();
    private final Paint goTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goCausePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goDepthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goNewRecordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goStatPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goNewItemsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

        gameOverPaint.setColor(Color.RED);
        gameOverPaint.setTextSize(sp(dm, 56f));
        gameOverPaint.setTextAlign(Paint.Align.CENTER);
        gameOverPaint.setFakeBoldText(true);

        gameOverSubPaint.setColor(Color.rgb(200, 200, 200));
        gameOverSubPaint.setTextSize(sp(dm, 17f));
        gameOverSubPaint.setTextAlign(Paint.Align.CENTER);

        cardPaint.setColor(Color.rgb(18, 20, 12));
        cardStrokePaint.setStyle(Paint.Style.STROKE);
        cardStrokePaint.setStrokeWidth(sp(dm, 1.5f));
        cardStrokePaint.setColor(Color.rgb(60, 80, 40));

        statsLabelPaint.setColor(Color.rgb(170, 190, 120));
        statsLabelPaint.setTextSize(sp(dm, 15f));
        statsLabelPaint.setTextAlign(Paint.Align.LEFT);
        statsLabelPaint.setFakeBoldText(true);

        statsValuePaint.setColor(Color.WHITE);
        statsValuePaint.setTextSize(sp(dm, 15f));
        statsValuePaint.setTextAlign(Paint.Align.RIGHT);
        statsValuePaint.setFakeBoldText(true);

        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextSize(sp(dm, 14f));
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
        buttonTextPaint.setFakeBoldText(true);

        retryButtonPaint.setColor(Color.rgb(36, 75, 126));
        menuButtonPaint.setColor(Color.rgb(121, 84, 18));
        buttonStrokePaint.setStyle(Paint.Style.STROKE);
        buttonStrokePaint.setStrokeWidth(sp(dm, 1f));
        buttonStrokePaint.setColor(Color.rgb(255, 200, 70));

        introTextPaint.setColor(Color.WHITE);
        introTextPaint.setTextSize(sp(dm, 22f));
        introTextPaint.setTextAlign(Paint.Align.CENTER);
        introTextPaint.setFakeBoldText(true);
        introTextPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK);

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

        // ── Audio init ────────────────────────────────────────────────────────
        try {
            mediaPlayer = MediaPlayer.create(this.context, R.raw.background);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(0.4f, 0.4f);
            }
        } catch (Exception e) {
            Log.e(TAG, "MediaPlayer init failed", e);
        }

        try {
            soundPool = new SoundPool.Builder().setMaxStreams(4).build();
            pickupSoundId = soundPool.load(this.context, R.raw.item_pickup, 1);
        } catch (Exception e) {
            Log.e(TAG, "SoundPool init failed", e);
        }
    }

    private static float sp(DisplayMetrics dm, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm);
    }

    // ─── Audio helpers ────────────────────────────────────────────────────────

    private void playSfx() {
        try {
            if (soundPool != null && pickupSoundId > 0
                    && PlayerData.isSfxEnabled(context)) {
                soundPool.play(pickupSoundId, 0.6f, 0.6f, 1, 0, 1.0f);
            }
        } catch (Exception e) {
            Log.e(TAG, "SoundPool play failed", e);
        }
    }

    /**
     * Call from GameActivity.onDestroy() to free MediaPlayer and SoundPool.
     */
    public void release() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "MediaPlayer release failed", e);
        }
        try {
            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "SoundPool release failed", e);
        }
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
                        (depth - L[0]) / (float) (L[1] - L[0])));
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
                (int) (Color.red(c) * 0.5f),
                (int) (Color.green(c) * 0.5f),
                (int) (Color.blue(c) * 0.5f));
    }

    // ─── Tile rendering ───────────────────────────────────────────────────────

    /**
     * Tiles a bitmap across the full screen, scrolled to match cameraY.
     */
    private void drawTiledBackground(Canvas canvas, Bitmap tile) {
        if (tile == null) return;
        // First tile row that intersects or is above the screen top
        float startY = (float) (Math.floor(cameraY / TILE_SIZE) * TILE_SIZE) - cameraY;
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
        bmpCoin = scale(BitmapFactory.decodeResource(res, R.drawable.coin),
                COIN_SIZE, COIN_SIZE);
        bmpCanister = scale(BitmapFactory.decodeResource(res, R.drawable.item_51_pickup_oxygen_tank),
                CANISTER_W, CANISTER_H);
        bmpDead = scale(BitmapFactory.decodeResource(res, R.drawable.tunn_dead),
                124, 124);

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

        // Load the mole walking animation sheet and slice it dynamically.
        moleSpriteSheet = BitmapFactory.decodeResource(res, R.drawable.mole_walk_animation);
        if (moleSpriteSheet != null) {
            if (moleSpriteSheet.getWidth() >= MOLE_SHEET_FRAMES) {
                moleFrameSrcW = moleSpriteSheet.getWidth() / MOLE_SHEET_FRAMES;
                moleFrameSrcH = moleSpriteSheet.getHeight();
                if (moleFrameSrcW <= 0 || moleFrameSrcH <= 0) {
                    Log.w(TAG, "Invalid mole sprite sheet size: "
                            + moleSpriteSheet.getWidth() + "x" + moleSpriteSheet.getHeight());
                    moleSpriteSheet.recycle();
                    moleSpriteSheet = null;
                } else if (moleSpriteSheet.getWidth() % MOLE_SHEET_FRAMES != 0) {
                    Log.w(TAG, "Mole sprite sheet width is not evenly divisible by frame count: "
                            + moleSpriteSheet.getWidth() + " / " + MOLE_SHEET_FRAMES);
                }
            } else {
                Log.w(TAG, "Mole sprite sheet is too narrow: " + moleSpriteSheet.getWidth());
                moleSpriteSheet.recycle();
                moleSpriteSheet = null;
            }
        }

        // Load intro mole
        moleIntroSheet = BitmapFactory.decodeResource(res, R.drawable.mole_intro);
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
        if (bmpCoin != null) {
            bmpCoin.recycle();
            bmpCoin = null;
        }
        if (bmpCanister != null) {
            bmpCanister.recycle();
            bmpCanister = null;
        }
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
        if (bmpDead != null) {
            bmpDead.recycle();
            bmpDead = null;
        }
        for (int i = 0; i < itemBitmaps.size(); i++) {
            Bitmap b = itemBitmaps.valueAt(i);
            if (b != null && !b.isRecycled()) b.recycle();
        }
        itemBitmaps.clear();

        if (moleSpriteSheet != null) {
            moleSpriteSheet.recycle();
            moleSpriteSheet = null;
        }
        if (moleIntroSheet != null) {
            moleIntroSheet.recycle();
            moleIntroSheet = null;
        }
        moleFrameSrcW = 0;
        moleFrameSrcH = 0;
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

        int type = types[(int) (Math.random() * typeCount)];
        Enemy e = new Enemy();
        e.type = type;
        e.radius = ENEMY_RADIUS[type];
        e.speed = ENEMY_SPEED[type];
        e.damage = ENEMY_DAMAGE[type];
        e.worldX = e.radius + (float) (Math.random() * (screenW - 2 * e.radius));
        e.worldY = moleWorldY + screenH * 0.85f; // spawn below visible area
        e.velocityX = Math.random() < 0.5f ? e.speed : -e.speed;
        enemies.add(e);
    }

    // ─── State reset ─────────────────────────────────────────────────────────

    private void resetGame() {
        if (screenW == 0) return;

        int oxyLevel = PlayerData.getUpgradeOxygen(context);
        int steerLevel = PlayerData.getUpgradeSteer(context);
        int digLevel = PlayerData.getUpgradeDig(context);
        int rangeLevel = PlayerData.getUpgradeRange(context);
        int refillLevel = PlayerData.getUpgradeO2Refill(context);
        rarityBoostLevel = PlayerData.getUpgradeRarity(context);
        clawLevel = PlayerData.getUpgradeClaw(context);

        maxOxygen = 100f + (oxyLevel - 1) * 25f;
        turnSpeed = 2f + (steerLevel - 1) * 0.5f;
        moveSpeed = 5f + (digLevel - 1) * 1.5f;
        collectRange = 50f + (rangeLevel - 1) * 15f;
        o2RestoreAmount = 10f + (refillLevel - 1) * 5f;

        moleWorldX = screenW / 2f;
        moleWorldY = GRASS_HEIGHT; // Mole starts sitting on the soil, boundary of grass
        moleAngle = 0f;
        cameraY = moleWorldY - screenH * 0.4f;
        gasWorldY = -300f;
        oxygen = maxOxygen;
        depthMetres = 0f;
        runCoins = 0;
        frameCount = 0;

        canisters.clear();
        coinItems.clear();
        itemNodes.clear();
        enemies.clear();

        lastCanisterSpawnY = moleWorldY;
        lastCoinSpawnY = moleWorldY;
        lastItemSpawnY = moleWorldY;
        lastEnemySpawnY = moleWorldY;

        newItemFlashTimer = 0;
        newItemFlashName = null;
        newItemsThisRun = 0;

        currentLayerIdx = -1;
        layerFlashTimer = 0;
        layerFlashName = null;

        hitFlashTimer = 0;
        deathCause = null;
        isNewRecord = false;

        pressingLeft = false;
        pressingRight = false;
        gameOver = false;

        // Restore music volume and resume playback on retry
        try {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(0.4f, 0.4f);
                if (!mediaPlayer.isPlaying() && PlayerData.isMusicEnabled(context)) {
                    mediaPlayer.start();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "MediaPlayer retry restart failed", e);
        }
        isIntro = true;
        isTransitioning = false;
        transitionProgress = 0f;

        // Reset run stats
        moleFrameIndex = 0;
        moleFrameTimer = 0;
        introFrameIndex = 0;
        introFrameTimer = 0;
        newItemsFoundThisRun = 0;
        newBestDepthThisRun = false;
    }

    // ─── Game loop ───────────────────────────────────────────────────────────

    @Override
    public void run() {
        while (playing) {
            long start = System.currentTimeMillis();
            update();
            draw();
            long sleepMs = 16L - (System.currentTimeMillis() - start);
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    private void update() {
        if (screenW == 0) return;
        frameCount++;

        if (isIntro) {
            // Update intro animation
            if (moleIntroSheet != null) {
                introFrameTimer++;
                if (introFrameTimer >= 8) {
                    introFrameTimer = 0;
                    introFrameIndex = (introFrameIndex + 1) % INTRO_SHEET_FRAMES;
                }
            }
            if (isTransitioning) {
                transitionProgress += 0.02f; // Smooth transition progress
                if (transitionProgress >= 1f) {
                    isIntro = false;
                    isTransitioning = false;
                    transitionProgress = 0f;
                    // Actual gameplay starts here
                }
            }
            return;
        }

        if (gameOver) return;

        // Steering: allow full rotation and wrap heading to [-180, 180].
        if (pressingLeft) {
            moleAngle -= turnSpeed;
            if (moleAngle < -180f) moleAngle += 360f;
        }
        if (pressingRight) {
            moleAngle += turnSpeed;
            if (moleAngle > 180f) moleAngle -= 360f;
        }

        // Movement
        double rad = Math.toRadians(moleAngle);
        // Invert X to match the current sprite orientation after rotation adjustments.
        moleWorldX -= (float) (Math.sin(rad) * moveSpeed);
        moleWorldY += (float) (Math.cos(rad) * moveSpeed);
        moleWorldX = Math.max(MOLE_RADIUS, Math.min(moleWorldX, screenW - MOLE_RADIUS));

        cameraY = moleWorldY - screenH * 0.4f;
        depthMetres = Math.max((moleWorldY - GRASS_HEIGHT) / 10f, 0f);

        // Terrain layer transition detection
        int newLayerIdx = layerIndex(depthMetres);
        if (newLayerIdx != currentLayerIdx) {
            if (currentLayerIdx >= 0) {
                layerFlashTimer = 90;
                layerFlashName = LAYER_NAMES[newLayerIdx];
            }
            currentLayerIdx = newLayerIdx;
        }

        // Gas
        gasWorldY += GAS_SPEED;
        if (gasWorldY >= moleWorldY) {
            Log.d(TAG, "Game over: gas at depth " + (int) depthMetres + "m");
            deathCause = "GAS CAUGHT YOU";
            saveRunResults();
            return;
        }

        // Oxygen
        oxygen = Math.max(oxygen - OXY_DRAIN, 0f);
        if (oxygen <= 0f) {
            Log.d(TAG, "Game over: oxygen depleted at depth " + (int) depthMetres + "m");
            deathCause = "OXYGEN DEPLETED";
            saveRunResults();
            return;
        }

        float rangeSq = collectRange * collectRange;

        // ── Spawning ─────────────────────────────────────────────────────────

        if (moleWorldY - lastCanisterSpawnY >= 400f) {
            lastCanisterSpawnY = moleWorldY;
            float x = MOLE_RADIUS + (float) (Math.random() * (screenW - 2 * MOLE_RADIUS));
            canisters.add(new float[]{x, moleWorldY + screenH * 0.85f});
        }

        if (moleWorldY - lastCoinSpawnY >= 150f) {
            lastCoinSpawnY = moleWorldY;
            float x = MOLE_RADIUS + (float) (Math.random() * (screenW - 2 * MOLE_RADIUS));
            coinItems.add(new float[]{x, moleWorldY + screenH * 0.65f});
        }

        if (moleWorldY - lastItemSpawnY >= 200f) {
            lastItemSpawnY = moleWorldY;
            ItemCatalogue.Item item = ItemCatalogue.rollRandomItem(rarityBoostLevel);
            float x = MOLE_RADIUS + (float) (Math.random() * (screenW - 2 * MOLE_RADIUS));
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
            float[] c = canIter.next();
            float dx = moleWorldX - c[0], dy = moleWorldY - c[1];
            if (dx * dx + dy * dy < rangeSq) {
                canIter.remove();
                oxygen = Math.min(oxygen + o2RestoreAmount, maxOxygen);
                playSfx();
            }
        }

        // ── Collect coins ─────────────────────────────────────────────────────
        boolean multiplier = PlayerData.hasCoinMultiplier(context);
        Iterator<float[]> coinIter = coinItems.iterator();
        while (coinIter.hasNext()) {
            float[] c = coinIter.next();
            float dx = moleWorldX - c[0], dy = moleWorldY - c[1];
            if (dx * dx + dy * dy < rangeSq) {
                coinIter.remove();
                runCoins += multiplier ? 2 : 1;
                playSfx();
            }
        }

        // ── Collect item nodes ────────────────────────────────────────────────
        Iterator<ItemNode> itemIter = itemNodes.iterator();
        while (itemIter.hasNext()) {
            ItemNode node = itemIter.next();
            float dx = moleWorldX - node.worldX, dy = moleWorldY - node.worldY;
            if (dx * dx + dy * dy < rangeSq) {
                itemIter.remove();
                boolean isNew = !PlayerData.isItemCollected(context, node.item.id);
                PlayerData.markItemCollected(context, node.item.id);
                int value = node.item.coinValue;
                if (node.item.rarity == ItemCatalogue.Rarity.COMMON
                        && PlayerData.hasCommonDoubler(context)) value *= 2;
                if (multiplier) value *= 2;
                runCoins += value;
                playSfx();
                if (isNew) {
                    newItemsThisRun++;
                    newItemsFoundThisRun++;
                    newItemFlashTimer = FLASH_DURATION;
                    newItemFlashName = node.item.name;
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
            e.worldY += (float) (Math.sin(frameCount * 0.05 + eIdx) * 0.3);

            // Collision with mole
            float dx = moleWorldX - e.worldX;
            float dy = moleWorldY - e.worldY;
            float minD = MOLE_RADIUS + e.radius;
            if (dx * dx + dy * dy < minD * minD) {
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
        if (layerFlashTimer > 0) layerFlashTimer--;
        if (hitFlashTimer > 0) hitFlashTimer--;

        // --- advance mole animation ---
        if (moleSpriteSheet != null) {
            moleFrameTimer++;
            if (moleFrameTimer >= moleFrameDelay) {
                moleFrameTimer = 0;
                moleFrameIndex = (moleFrameIndex + 1) % MOLE_SHEET_FRAMES;
            }
        }
    }

    private void saveRunResults() {
        float prevBest = PlayerData.getBestDepth(context);
        isNewRecord = depthMetres > prevBest;
        newBestDepthThisRun = isNewRecord;
        PlayerData.addCoins(context, runCoins);
        if (isNewRecord) PlayerData.setBestDepth(context, depthMetres);
        try {
            if (mediaPlayer != null) mediaPlayer.setVolume(0.15f, 0.15f);
        } catch (Exception e) { Log.e(TAG, "MediaPlayer volume failed", e); }
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

            float effectiveCameraY;
            float moleScreenY;

            if (isIntro) {
                // Intro animation camera
                float startY = screenH * 0.7f;
                float endY = screenH * 0.4f;

                moleScreenY = startY + (endY - startY) * transitionProgress;
                effectiveCameraY = GRASS_HEIGHT - moleScreenY;
            } else {
                effectiveCameraY = cameraY;
                moleScreenY = screenH * 0.4f;
            }

            // ── Terrain background ────────────────────────────────────────────
            int layerIdx = layerIndex(depthMetres);

            if (tileBitmaps[layerIdx] == null) {

                // fallback color
                canvas.drawColor(terrainFallbackColor(depthMetres));

            } else {

                float layerMaxDepth = LAYERS[layerIdx][1];
                float depthToNext = layerMaxDepth - depthMetres;

                boolean nearBoundary =
                        depthToNext < 50f &&
                                layerIdx < LAYERS.length - 1;

                if (!nearBoundary) {

                    tilePaint.setAlpha(255);
                    drawTiledBackground(canvas, tileBitmaps[layerIdx]);

                } else {

                    float blend = (50f - depthToNext) / 50f;

                    int primAlpha =
                            Math.max(0, (int) (255 - blend * 55f));

                    int secAlpha =
                            Math.min(255, (int) (blend * 55f));

                    tilePaint.setAlpha(primAlpha);
                    drawTiledBackground(canvas, tileBitmaps[layerIdx]);

                    if (tileBitmaps[layerIdx + 1] != null && secAlpha > 0) {
                        tilePaint.setAlpha(secAlpha);
                        drawTiledBackground(canvas, tileBitmaps[layerIdx + 1]);
                    }
                }

                // darkening overlay
                int[] L = LAYERS[layerIdx];

                float layerT =
                        L[1] < 999999
                                ? Math.max(
                                0f,
                                Math.min(
                                        1f,
                                        (depthMetres - L[0]) /
                                                (float) (L[1] - L[0])
                                )
                        )
                                : 1f;

                int darkAlpha = (int) (layerT * 40f);

                if (darkAlpha > 0) {
                    canvas.drawColor(Color.argb(darkAlpha, 0, 0, 0));
                }
            }

            // ── Intro mode ────────────────────────────────────────────────────
            if (isIntro) {

                drawIntroMole(canvas, moleScreenY);

                if (!isTransitioning) {
                    canvas.drawText(
                            "Press The Screen To Start The game",
                            screenW / 2f,
                            screenH - 100f,
                            introTextPaint
                    );
                }

            } else {

                // ── Gas cloud ────────────────────────────────────────────────
                float gasScreenY = gasWorldY - effectiveCameraY;

                if (gasScreenY > 0) {
                    canvas.drawRect(0, 0, screenW, gasScreenY, gasPaint);
                }

                // ── O2 canisters ────────────────────────────────────────────
                for (float[] c : canisters) {

                    float sx = c[0];
                    float sy = c[1] - effectiveCameraY;

                    if (sy < -CANISTER_H || sy > screenH + CANISTER_H) {
                        continue;
                    }

                    if (bmpCanister != null) {
                        canvas.drawBitmap(
                                bmpCanister,
                                sx - CANISTER_W / 2f,
                                sy - CANISTER_H / 2f,
                                null
                        );
                    }
                }

                // ── Coin pickups ────────────────────────────────────────────
                for (float[] c : coinItems) {

                    float sx = c[0];
                    float sy = c[1] - effectiveCameraY;

                    if (sy < -COIN_SIZE || sy > screenH + COIN_SIZE) {
                        continue;
                    }

                    if (bmpCoin != null) {
                        canvas.drawBitmap(
                                bmpCoin,
                                sx - COIN_SIZE / 2f,
                                sy - COIN_SIZE / 2f,
                                null
                        );
                    }
                }

                // ── Item nodes ──────────────────────────────────────────────
                for (ItemNode node : itemNodes) {

                    float sx = node.worldX;
                    float sy = node.worldY - effectiveCameraY;

                    if (sy < -ITEM_SIZE || sy > screenH + ITEM_SIZE) {
                        continue;
                    }

                    Bitmap bmp = itemBitmaps.get(node.item.id);

                    if (bmp != null) {

                        canvas.drawBitmap(
                                bmp,
                                sx - ITEM_SIZE / 2f,
                                sy - ITEM_SIZE / 2f,
                                null
                        );

                    } else {

                        fallbackPaint.setColor(
                                ItemCatalogue.getBorderColor(node.item.rarity)
                        );

                        canvas.drawCircle(sx, sy, 16f, fallbackPaint);
                    }
                }

                // ── Enemies ─────────────────────────────────────────────────
                for (Enemy e : enemies) {

                    float sx = e.worldX;
                    float sy = e.worldY - effectiveCameraY;

                    int sw = ENEMY_SPRITE_W[e.type];
                    int sh = ENEMY_SPRITE_H[e.type];

                    if (sy < -(sh * 2) || sy > screenH + sh * 2) {
                        continue;
                    }

                    Bitmap bmp =
                            e.velocityX >= 0
                                    ? enemyBitmapsRight[e.type]
                                    : enemyBitmapsLeft[e.type];

                    if (bmp != null) {

                        canvas.drawBitmap(
                                bmp,
                                sx - sw / 2f,
                                sy - sh / 2f,
                                null
                        );

                    } else {

                        enemyBodyPaint.setColor(ENEMY_COLORS[e.type]);
                        canvas.drawCircle(sx, sy, e.radius, enemyBodyPaint);
                    }

                    // enemy label
                    float dxE = moleWorldX - e.worldX;
                    float dyE = moleWorldY - e.worldY;

                    if (dxE * dxE + dyE * dyE < 200f * 200f) {

                        enemyLabelPaint.setColor(ENEMY_COLORS[e.type]);

                        canvas.drawText(
                                ENEMY_NAMES[e.type],
                                sx,
                                sy + sh / 2f + enemyLabelPaint.getTextSize(),
                                enemyLabelPaint
                        );
                    }
                }

                // ── Mole ────────────────────────────────────────────────────
                drawMole(canvas, moleScreenY);

                // ── Hit flash ───────────────────────────────────────────────
                if (hitFlashTimer > 0) {
                    canvas.drawRect(0, 0, screenW, screenH, hitFlashPaint);
                }

                // ── HUD ─────────────────────────────────────────────────────
                drawHUD(canvas);

                // ── Enemy counter ───────────────────────────────────────────
                float dotY = screenH - 30f;

                canvas.drawCircle(screenW - 66f, dotY, 6f, enemyDotPaint);

                canvas.drawText(
                        String.valueOf(enemies.size()),
                        screenW - 24f,
                        dotY + enemyHudPaint.getTextSize() / 3f,
                        enemyHudPaint
                );

                // ── New item flash ──────────────────────────────────────────
                if (newItemFlashTimer > 0 && newItemFlashName != null) {

                    float a =
                            newItemFlashTimer > 20
                                    ? 1f
                                    : newItemFlashTimer / 20f;

                    newItemFlashPaint.setAlpha((int) (a * 255));

                    canvas.drawText(
                            "NEW!  " + newItemFlashName,
                            screenW / 2f,
                            screenH * 0.6f,
                            newItemFlashPaint
                    );
                }

                // ── Layer flash ─────────────────────────────────────────────
                if (layerFlashTimer > 0 && layerFlashName != null) {

                    float a =
                            layerFlashTimer > 30
                                    ? 1f
                                    : layerFlashTimer / 30f;

                    layerFlashPaint.setAlpha((int) (a * 255));

                    canvas.drawText(
                            layerFlashName,
                            screenW / 2f,
                            screenH * 0.5f,
                            layerFlashPaint
                    );
                }

                // ── Tutorial hints ──────────────────────────────────────────
                if (depthMetres < 20f) {

                    int alpha =
                            depthMetres <= 15f
                                    ? 180
                                    : (int) (
                                    180f *
                                            (1f - (depthMetres - 15f) / 5f)
                            );

                    hintPaint.setAlpha(alpha);

                    hintPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText(
                            "< LEFT",
                            40f,
                            screenH - 60f,
                            hintPaint
                    );

                    hintPaint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(
                            "RIGHT >",
                            screenW - 40f,
                            screenH - 60f,
                            hintPaint
                    );
                }

                // ── Game over ───────────────────────────────────────────────
                if (gameOver) {
                    drawGameOver(canvas);
                }
            }

        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawWorldBackground(Canvas canvas, float effectiveCameraY) {
        float surfaceScreenY = -effectiveCameraY; // World Y=0 is the surface (top of grass)

        // 1. Soil (The deep part)
        float t = Math.min(depthMetres / 1000f, 1f);
        bgPaint.setColor(Color.rgb(
                lerp(BG_R_START, BG_R_END, t),
                lerp(BG_G_START, BG_G_END, t),
                lerp(BG_B_START, BG_B_END, t)
        ));
        canvas.drawRect(0, 0, screenW, screenH, bgPaint);

        // 2. Sky (Light Blue)
        if (surfaceScreenY > 0) {
            bgPaint.setColor(SKY_COLOR);
            canvas.drawRect(0, 0, screenW, surfaceScreenY, bgPaint);
        }

        // 3. Grass (Green layer from World Y=0 to World Y=GRASS_HEIGHT)
        float grassBottomScreenY = surfaceScreenY + GRASS_HEIGHT;
        if (grassBottomScreenY > 0 && surfaceScreenY < screenH) {
            bgPaint.setColor(GRASS_COLOR);
            canvas.drawRect(0, Math.max(0, surfaceScreenY), screenW, Math.min(screenH, grassBottomScreenY), bgPaint);
        }
    }

    private void drawIntroMole(Canvas canvas, float screenY) {
        if (moleIntroSheet != null) {
            int srcW = moleIntroSheet.getWidth() / INTRO_SHEET_FRAMES;
            int srcH = moleIntroSheet.getHeight();
            int left = introFrameIndex * srcW;
            Rect src = new Rect(left, 0, left + srcW, srcH);

            float moleSize = MOLE_RADIUS * 2.5f;
            float mx = screenW / 2f;
            // Intro mole sits such that its feet are at screenY
            RectF dst = new RectF(mx - moleSize / 2, screenY - moleSize, mx + moleSize / 2, screenY);
            canvas.drawBitmap(moleIntroSheet, src, dst, null);
        }
    }

    private void drawMole(Canvas canvas, float screenY) {
        canvas.save();
        canvas.translate(moleWorldX, screenY);
        canvas.rotate(moleAngle);
        canvas.rotate(180f); // Face upward
        if (moleSpriteSheet != null && moleFrameSrcW > 0 && moleFrameSrcH > 0) {
            int frameLeft = moleFrameIndex * moleFrameSrcW;
            if (frameLeft + moleFrameSrcW <= moleSpriteSheet.getWidth()) {
                Rect src = new Rect(frameLeft, 0, frameLeft + moleFrameSrcW, moleFrameSrcH);
                float dstSize = MOLE_RADIUS * 2f;
                RectF dst = new RectF(-dstSize / 2f, -dstSize / 2f, dstSize / 2f, dstSize / 2f);
                canvas.drawBitmap(moleSpriteSheet, src, dst, null);
            }
        } else {
            // Fallback
            canvas.drawCircle(0, 0, MOLE_RADIUS, molePaint);
            canvas.drawCircle(-10f, 12f, 5f, eyePaint);
            canvas.drawCircle(10f, 12f, 5f, eyePaint);
        }
        canvas.restore();
    }

    private void drawGameEntities(Canvas canvas, float effectiveCameraY) {
        // Gas cloud
        float gasScreenY = gasWorldY - effectiveCameraY;
        if (gasScreenY > 0) canvas.drawRect(0, 0, screenW, gasScreenY, gasPaint);

        // O2 canisters
        for (float[] c : canisters) {
            float sx = c[0], sy = c[1] - effectiveCameraY;
            if (sy < -CANISTER_H || sy > screenH + CANISTER_H) continue;
            if (bmpCanister != null) {
                canvas.drawBitmap(bmpCanister, sx - CANISTER_W / 2f, sy - CANISTER_H / 2f, null);
            }
        }

        // Coin pickups
        for (float[] c : coinItems) {
            float sx = c[0], sy = c[1] - effectiveCameraY;
            if (sy < -COIN_SIZE || sy > screenH + COIN_SIZE) continue;
            if (bmpCoin != null) {
                canvas.drawBitmap(bmpCoin, sx - COIN_SIZE / 2f, sy - COIN_SIZE / 2f, null);
            }
        }

        // Collectible item nodes
        for (ItemNode node : itemNodes) {
            float sx = node.worldX, sy = node.worldY - effectiveCameraY;
            if (sy < -ITEM_SIZE || sy > screenH + ITEM_SIZE) continue;
            Bitmap bmp = itemBitmaps.get(node.item.id);
            if (bmp != null) {
                canvas.drawBitmap(bmp, sx - ITEM_SIZE / 2f, sy - ITEM_SIZE / 2f, null);
            } else {
                fallbackPaint.setColor(ItemCatalogue.getBorderColor(node.item.rarity));
                canvas.drawCircle(sx, sy, 16f, fallbackPaint);
            }
        }
    }

    private void drawHUD(Canvas canvas) {
        // Oxygen bar
        int barW = 20, barH = 200, barMargin = 24;
        int barX = screenW - barMargin - barW;
        int barTop = screenH / 2 - barH / 2;
        int barBottom = barTop + barH;

        canvas.drawRect(barX, barTop, barX + barW, barBottom, oxyBgPaint);
        float fillFrac = oxygen / maxOxygen;
        oxyFillPaint.setColor(oxyBarColor(fillFrac));
        canvas.drawRect(barX, (int) (barBottom - barH * fillFrac),
                barX + barW, barBottom, oxyFillPaint);
        canvas.drawText("O2", barX + barW / 2f, barTop - 8f, oxyLabelPaint);

        // HUD: depth + coins
        canvas.drawText((int) depthMetres + "m",
                screenW / 2f, hudPaint.getTextSize() + 16f, hudPaint);
        canvas.drawText("● " + runCoins,
                24f, coinHudPaint.getTextSize() + 20f, coinHudPaint);

        // "NEW!" flash
        if (newItemFlashTimer > 0 && newItemFlashName != null) {
            float a = newItemFlashTimer > 20 ? 1f : newItemFlashTimer / 20f;
            newItemFlashPaint.setAlpha((int) (a * 255));
            canvas.drawText("NEW!  " + newItemFlashName,
                    screenW / 2f, screenH * 0.6f, newItemFlashPaint);
        }

        // Zone hints
        if (depthMetres < 20f) {
            int alpha = depthMetres <= 15f ? 180 : (int) (180f * (1f - (depthMetres - 15f) / 5f));
            hintPaint.setAlpha(alpha);
            hintPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("< LEFT", 40f, screenH - 60f, hintPaint);
            hintPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("RIGHT >", screenW - 40f, screenH - 60f, hintPaint);
        }
    }

    private void drawGameOver(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        float screenCenterX = screenW / 2f;
        float screenCenterY = screenH / 2f;
        DisplayMetrics dm = getResources().getDisplayMetrics();

        float deadSize = Math.min(screenW * 0.55f, 280f);
        float deadTop = screenCenterY - (screenH * 0.30f);
        RectF deadRect = new RectF(screenCenterX - deadSize / 2f, deadTop, screenCenterX + deadSize / 2f, deadTop + deadSize);
        if (bmpDead != null) canvas.drawBitmap(bmpDead, null, deadRect, null);

        float titleY = deadRect.bottom + sp(dm, 60f);
        float subtitleY = titleY + sp(dm, 28f);

        canvas.drawText("GAME OVER", screenCenterX, titleY, gameOverPaint);
        gameOverSubPaint.setColor(Color.rgb(160, 230, 70));
        canvas.drawText("POISON GAS REACHED YOU", screenCenterX, subtitleY, gameOverSubPaint);

        float statsLeft = sp(dm, 16f);
        float statsTop = subtitleY + sp(dm, 32f);
        float statsWidth = screenW - sp(dm, 32f);
        float statsHeight = sp(dm, 230f);
        RectF stats = new RectF(statsLeft, statsTop, statsLeft + statsWidth, statsTop + statsHeight);

        canvas.drawRoundRect(stats, 20f, 20f, cardPaint);
        canvas.drawRoundRect(stats, 20f, 20f, cardStrokePaint);

        float labelX = statsLeft + sp(dm, 16f);
        float valueX = statsLeft + statsWidth - sp(dm, 16f);
        float rowY = stats.top + sp(dm, 26f);
        float rowGap = sp(dm, 40f);
        int bestDepth = Math.round(PlayerData.getBestDepth(context));

        canvas.drawText("DEPTH", labelX, rowY, statsLabelPaint);
        statsValuePaint.setColor(Color.WHITE);
        canvas.drawText(((int) depthMetres) + " m", valueX, rowY, statsValuePaint);

        rowY += rowGap;
        canvas.drawText("BEST DEPTH", labelX, rowY, statsLabelPaint);
        if (newBestDepthThisRun) statsValuePaint.setColor(Color.rgb(255, 170, 40));
        canvas.drawText(bestDepth + " m", valueX, rowY, statsValuePaint);

        rowY += rowGap;
        canvas.drawText("MATERIALS EARNED", labelX, rowY, statsLabelPaint);
        statsValuePaint.setColor(Color.WHITE);
        canvas.drawText(newItemsFoundThisRun + " items", valueX, rowY, statsValuePaint);

        rowY += rowGap;
        canvas.drawText("COINS EARNED", labelX, rowY, statsLabelPaint);
        canvas.drawText("+" + runCoins + " coins", valueX, rowY, statsValuePaint);

        rowY += rowGap;
        canvas.drawText("ENEMIES DODGED", labelX, rowY, statsLabelPaint);
        canvas.drawText("0", valueX, rowY, statsValuePaint);

        float btnMarginSide = sp(dm, 20f);
        float btnWidth = (screenW - 2f * btnMarginSide - 12f) / 2f;
        float retryLeft = btnMarginSide;
        float menuLeft = btnMarginSide + btnWidth + sp(dm, 12f);
        float btnTop = stats.bottom + sp(dm, 28f);
        float btnBottom = btnTop + sp(dm, 48f);

        RectF retry = new RectF(retryLeft, btnTop, retryLeft + btnWidth, btnBottom);
        RectF menu = new RectF(menuLeft, btnTop, menuLeft + btnWidth, btnBottom);

        retryButtonPaint.setColor(retryButtonHovered ? Color.rgb(46, 95, 156) : Color.rgb(36, 75, 126));
        canvas.drawRoundRect(retry, 16f, 16f, retryButtonPaint);
        buttonStrokePaint.setColor(Color.rgb(90, 125, 185));
        canvas.drawRoundRect(retry, 16f, 16f, buttonStrokePaint);

        menuButtonPaint.setColor(menuButtonHovered ? Color.rgb(151, 114, 28) : Color.rgb(121, 84, 18));
        canvas.drawRoundRect(menu, 16f, 16f, menuButtonPaint);
        buttonStrokePaint.setColor(Color.rgb(230, 180, 50));
        canvas.drawRoundRect(menu, 16f, 16f, buttonStrokePaint);

        float btnTextY = retry.centerY() + sp(dm, 4f);
        canvas.drawText("▶ RETRY", retry.centerX(), btnTextY, buttonTextPaint);
        canvas.drawText("⌂ MENU", menu.centerX(), btnTextY, buttonTextPaint);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static int lerp(int a, int b, float t) {
        return (int) (a + (b - a) * t);
    }

    private static int oxyBarColor(float f) {
        if (f > 0.5f) {
            float t = (f - 0.5f) / 0.5f;
            return Color.rgb((int) (255 * (1 - t)), 200, 0);
        } else {
            float t = f / 0.5f;
            return Color.rgb(255, (int) (200 * t), 0);
        }
    }

    // ─── Touch ───────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // ── Intro screen ─────────────────────────────────────────────
        if (isIntro) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                if (!isTransitioning) {
                    isTransitioning = true;
                }
            }

            return true;
        }

        // ── Game over buttons ───────────────────────────────────────
        if (gameOver) {

            float x = event.getX();

            DisplayMetrics dm = getResources().getDisplayMetrics();

            float btnMarginSide = sp(dm, 20f);

            float btnWidth =
                    (screenW - 2f * btnMarginSide - 12f) / 2f;

            float retryLeft = btnMarginSide;
            float retryRight = retryLeft + btnWidth;

            float menuLeft =
                    retryLeft + btnWidth + sp(dm, 12f);

            float menuRight = menuLeft + btnWidth;

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {

                if (x >= retryLeft && x <= retryRight) {

                    resetGame();

                } else if (x >= menuLeft && x <= menuRight) {

                    Context ctx = getContext();

                    if (ctx instanceof GameActivity) {
                        ((GameActivity) ctx).finish();
                    }
                }

            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {

                retryButtonHovered =
                        (x >= retryLeft && x <= retryRight);

                menuButtonHovered =
                        (x >= menuLeft && x <= menuRight);

            } else if (
                    event.getActionMasked() == MotionEvent.ACTION_UP ||
                            event.getActionMasked() == MotionEvent.ACTION_CANCEL
            ) {

                retryButtonHovered = false;
                menuButtonHovered = false;
            }

            return true;
        }

        // ── Gameplay controls ───────────────────────────────────────
        int action = event.getActionMasked();
        int idx = event.getActionIndex();

        switch (action) {

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:

                registerPress(event.getX(idx));
                break;

            case MotionEvent.ACTION_MOVE:

                pressingLeft = false;
                pressingRight = false;

                for (int i = 0; i < event.getPointerCount(); i++) {
                    registerPress(event.getX(i));
                }

                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:

                registerRelease(event.getX(idx));

                if (action == MotionEvent.ACTION_UP) {
                    performClick();
                }

                break;

            case MotionEvent.ACTION_CANCEL:

                pressingLeft = false;
                pressingRight = false;

                break;
        }

        return true;

    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void registerPress(float x) {
        if (x < screenW / 2f) pressingLeft = true;
        else pressingRight = true;
    }

    private void registerRelease(float x) {
        if (x < screenW / 2f) pressingLeft = false;
        else pressingRight = false;
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

    public void resume() {
        playing = true;
        thread = new Thread(this);
        thread.start();
        try {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()
                    && PlayerData.isMusicEnabled(context)) {
                mediaPlayer.start();
            }
        } catch (Exception e) { Log.e(TAG, "MediaPlayer resume failed", e); }
    }

    public void pause() {
        playing = false;
        try { thread.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
        } catch (Exception e) { Log.e(TAG, "MediaPlayer pause failed", e); }
    }
}
