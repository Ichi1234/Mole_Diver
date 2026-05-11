package org.classapp.molediver;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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

    // ─── Sprite dimensions (pixels) ───────────────────────────────────────────
    private static final int ITEM_SIZE   = 40;
    private static final int CANISTER_W  = 30;
    private static final int CANISTER_H  = 50;
    private static final int COIN_SIZE   = 32;

    // ─── Context ─────────────────────────────────────────────────────────────
    private final Context context;

    // ─── Game loop ───────────────────────────────────────────────────────────
    private Thread           thread;
    private volatile boolean playing;

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
    private static final float GAS_SPEED = 5.2f;
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

    // "NEW!" flash
    private static final int FLASH_DURATION = 75;
    private int    newItemFlashTimer;
    private String newItemFlashName;

    // ─── Game state ──────────────────────────────────────────────────────────
    private volatile boolean isIntro = true;
    private volatile boolean isTransitioning = false;

    private volatile boolean gameOver;
    private int newItemsFoundThisRun = 0;
    private boolean newBestDepthThisRun = false;
    private boolean retryButtonHovered = false;
    private boolean menuButtonHovered = false;

    // ─── Background constants ────────────────────────────────────────────────
    private static final int BG_R_START = 101, BG_G_START = 67, BG_B_START = 33;
    private static final int BG_R_END   = 20,  BG_G_END   = 12, BG_B_END   = 6;
    private static final int GRASS_COLOR = Color.rgb(34, 139, 34);
    private static final int SKY_COLOR   = Color.rgb(135, 206, 235);
    private static final float GRASS_HEIGHT = 100f; // World height of the grass layer

    // ─── Cached bitmaps ──────────────────────────────────────────────────────
    private final SparseArray<Bitmap> itemBitmaps = new SparseArray<>();
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

    // ─── Paints ──────────────────────────────────────────────────────────────
    private final Paint bgPaint           = new Paint();
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
    private final Paint dimPaint          = new Paint();
    private final Paint gameOverPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gameOverSubPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardPaint         = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardStrokePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statsLabelPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statsValuePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonTextPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint retryButtonPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint menuButtonPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint introTextPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Fallback when a bitmap failed to load
    private final Paint fallbackPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);

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

        dimPaint.setColor(Color.argb(160, 0, 0, 0));

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
    }

    private static float sp(DisplayMetrics dm, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm);
    }

    // ─── Bitmap loading / recycling ───────────────────────────────────────────

    private void loadBitmaps() {
        recycleBitmaps(); // safe to call on first load (no-op if empty)

        Resources res = context.getResources();

        bmpCoin     = scale(BitmapFactory.decodeResource(res, R.drawable.coin),
                            COIN_SIZE, COIN_SIZE);
        bmpCanister = scale(BitmapFactory.decodeResource(res, R.drawable.item_51_pickup_oxygen_tank),
                            CANISTER_W, CANISTER_H);
        bmpDead     = scale(BitmapFactory.decodeResource(res, R.drawable.tunn_dead),
                            124, 124);

        for (ItemCatalogue.Item item : ItemCatalogue.ALL_ITEMS) {
            Bitmap raw = BitmapFactory.decodeResource(res, item.spriteResId);
            // scale() recycles raw internally when it creates a new Bitmap
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

    /**
     * Scales src to (w x h) and recycles the original if a new Bitmap was created.
     * Returns null if src is null.
     */
    private static Bitmap scale(Bitmap src, int w, int h) {
        if (src == null) return null;
        Bitmap scaled = Bitmap.createScaledBitmap(src, w, h, true);
        if (scaled != src) src.recycle();
        return scaled;
    }

    private void recycleBitmaps() {
        if (bmpCoin     != null) { bmpCoin.recycle();     bmpCoin     = null; }
        if (bmpCanister != null) { bmpCanister.recycle(); bmpCanister = null; }
        if (bmpDead     != null) { bmpDead.recycle();     bmpDead     = null; }
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

    // ─── State reset ─────────────────────────────────────────────────────────

    private void resetGame() {
        if (screenW == 0) return;

        int oxyLevel     = PlayerData.getUpgradeOxygen(context);
        int steerLevel   = PlayerData.getUpgradeSteer(context);
        int digLevel     = PlayerData.getUpgradeDig(context);
        int rangeLevel   = PlayerData.getUpgradeRange(context);
        int refillLevel  = PlayerData.getUpgradeO2Refill(context);
        rarityBoostLevel = PlayerData.getUpgradeRarity(context);

        maxOxygen       = 100f + (oxyLevel    - 1) * 25f;  // L1=100 … L5=200
        turnSpeed       = 2f   + (steerLevel  - 1) * 0.5f; // L1=2   … L5=4
        moveSpeed       = 5f   + (digLevel    - 1) * 1.5f; // L1=5   … L5=11
        collectRange    = 50f  + (rangeLevel  - 1) * 15f;  // L1=50  … L5=110
        o2RestoreAmount = 10f  + (refillLevel - 1) * 5f;   // L1=10  … L5=30

        moleWorldX  = screenW / 2f;
        moleWorldY = 0f; // grass surface, mole feet touch it
        moleAngle   = 0f;
        cameraY     = moleWorldY - screenH * 0.4f;
        gasWorldY   = -300f;
        oxygen      = maxOxygen;
        depthMetres = 0f;
        runCoins    = 0;

        canisters.clear();
        coinItems.clear();
        itemNodes.clear();

        lastCanisterSpawnY = moleWorldY;
        lastCoinSpawnY     = moleWorldY;
        lastItemSpawnY     = moleWorldY;

        newItemFlashTimer = 0;
        newItemFlashName  = null;

        pressingLeft  = false;
        pressingRight = false;
        gameOver      = false;
        isIntro       = true;
        isTransitioning = false;

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
                try { Thread.sleep(sleepMs); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    private void update() {
        if (screenW == 0) return;

        if (isIntro) {
            if (moleIntroSheet != null) {
                introFrameTimer++;
                if (introFrameTimer >= 8) {
                    introFrameTimer = 0;
                    introFrameIndex = (introFrameIndex + 1) % INTRO_SHEET_FRAMES;
                }
            }
            if (isTransitioning) {
                moleWorldY += 4f;
                if (moleWorldY >= GRASS_HEIGHT) {
                    moleWorldY = GRASS_HEIGHT;
                    isIntro = false;
                    isTransitioning = false;
                }
            }
            // NOT transitioning = mole stands still, waiting for tap
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
        moleWorldX -= (float)(Math.sin(rad) * moveSpeed);
        moleWorldY += (float)(Math.cos(rad) * moveSpeed);
        moleWorldX = Math.max(MOLE_RADIUS, Math.min(moleWorldX, screenW - MOLE_RADIUS));

        cameraY     = moleWorldY - screenH * 0.4f;
        depthMetres = Math.max((moleWorldY - GRASS_HEIGHT) / 10f, 0f);

        // Gas
        gasWorldY += GAS_SPEED;
        if (gasWorldY >= moleWorldY) {
            Log.d(TAG, "Game over: gas at depth " + (int)depthMetres + "m");
            saveRunResults(); return;
        }

        // Oxygen
        oxygen = Math.max(oxygen - OXY_DRAIN, 0f);
        if (oxygen <= 0f) {
            Log.d(TAG, "Game over: oxygen depleted at depth " + (int)depthMetres + "m");
            saveRunResults(); return;
        }

        float rangeSq = collectRange * collectRange;

        // Spawn O2 canisters
        if (moleWorldY - lastCanisterSpawnY >= 400f) {
            lastCanisterSpawnY = moleWorldY;
            float x = MOLE_RADIUS + (float)(Math.random() * (screenW - 2 * MOLE_RADIUS));
            canisters.add(new float[]{x, moleWorldY + screenH * 0.85f});
        }

        // Spawn coin pickups
        if (moleWorldY - lastCoinSpawnY >= 150f) {
            lastCoinSpawnY = moleWorldY;
            float x = MOLE_RADIUS + (float)(Math.random() * (screenW - 2 * MOLE_RADIUS));
            coinItems.add(new float[]{x, moleWorldY + screenH * 0.65f});
        }

        // Spawn collectible item nodes
        if (moleWorldY - lastItemSpawnY >= 200f) {
            lastItemSpawnY = moleWorldY;
            ItemCatalogue.Item item = ItemCatalogue.rollRandomItem(rarityBoostLevel);
            float x = MOLE_RADIUS + (float)(Math.random() * (screenW - 2 * MOLE_RADIUS));
            itemNodes.add(new ItemNode(x, moleWorldY + screenH * 0.75f, item));
        }

        // Collect O2 canisters
        Iterator<float[]> canIter = canisters.iterator();
        while (canIter.hasNext()) {
            float[] c  = canIter.next();
            float   dx = moleWorldX - c[0], dy = moleWorldY - c[1];
            if (dx*dx + dy*dy < rangeSq) {
                canIter.remove();
                oxygen = Math.min(oxygen + o2RestoreAmount, maxOxygen);
            }
        }

        // Collect coins
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

        // Collect item nodes
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
                    newItemsFoundThisRun++;
                    newItemFlashTimer = FLASH_DURATION;
                    newItemFlashName  = node.item.name;
                    checkAndClaimSets();
                }
            }
        }

        if (newItemFlashTimer > 0) newItemFlashTimer--;

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
        PlayerData.addCoins(context, runCoins);
        float best = PlayerData.getBestDepth(context);
        newBestDepthThisRun = depthMetres > best;
        if (newBestDepthThisRun) PlayerData.setBestDepth(context, depthMetres);
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
                // Mole stays FIXED on screen - only camera/background moves
                moleScreenY = screenH * 0.65f;
                effectiveCameraY = moleWorldY - moleScreenY;
            } else {
                effectiveCameraY = cameraY;
                moleScreenY = screenH * 0.4f;
            }

            // Background (Soil, Sky, Grass)
            drawWorldBackground(canvas, effectiveCameraY);

            if (isIntro) {
                // Intro Mole
                drawIntroMole(canvas, moleScreenY);

                // Intro Text
                if (!isTransitioning) {
                    canvas.drawText("Press The Screen To Start The game", screenW / 2f, screenH - 100f, introTextPaint);
                }
            } else {
                // Gameplay elements
                drawGameEntities(canvas, effectiveCameraY);
                drawMole(canvas, moleScreenY);
                drawHUD(canvas);
                if (gameOver) drawGameOver(canvas);
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
            // Feet are at screenY
            RectF dst = new RectF(mx - moleSize / 2, screenY - moleSize, mx + moleSize / 2, screenY);
            canvas.drawBitmap(moleIntroSheet, src, dst, null);
        }
    }

    private void drawMole(Canvas canvas, float screenY) {
        canvas.save();
        canvas.translate(moleWorldX, screenY);
        canvas.rotate(moleAngle);
        canvas.rotate(180f);
        if (moleSpriteSheet != null && moleFrameSrcW > 0 && moleFrameSrcH > 0) {
            int frameLeft = moleFrameIndex * moleFrameSrcW;
            if (frameLeft + moleFrameSrcW <= moleSpriteSheet.getWidth()) {
                Rect src = new Rect(frameLeft, 0, frameLeft + moleFrameSrcW, moleFrameSrcH);
                float dstSize = MOLE_RADIUS * 2f;
                RectF dst = new RectF(-dstSize / 2f, -dstSize / 2f, dstSize / 2f, dstSize / 2f);
                canvas.drawBitmap(moleSpriteSheet, src, dst, null);
            }
        } else {
            canvas.drawCircle(0, 0, MOLE_RADIUS, molePaint);
            canvas.drawCircle(-10f, 12f, 5f, eyePaint);
            canvas.drawCircle( 10f, 12f, 5f, eyePaint);
        }
        canvas.restore();
    }

    private void drawGameEntities(Canvas canvas, float effectiveCameraY) {
        float gasScreenY = gasWorldY - effectiveCameraY;
        if (gasScreenY > 0) canvas.drawRect(0, 0, screenW, gasScreenY, gasPaint);

        for (float[] c : canisters) {
            float sx = c[0], sy = c[1] - effectiveCameraY;
            if (sy < -CANISTER_H || sy > screenH + CANISTER_H) continue;
            if (bmpCanister != null) {
                canvas.drawBitmap(bmpCanister, sx - CANISTER_W / 2f, sy - CANISTER_H / 2f, null);
            }
        }

        for (float[] c : coinItems) {
            float sx = c[0], sy = c[1] - effectiveCameraY;
            if (sy < -COIN_SIZE || sy > screenH + COIN_SIZE) continue;
            if (bmpCoin != null) {
                canvas.drawBitmap(bmpCoin, sx - COIN_SIZE / 2f, sy - COIN_SIZE / 2f, null);
            }
        }

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

        canvas.drawText((int)depthMetres + "m",
                screenW / 2f, hudPaint.getTextSize() + 16f, hudPaint);
        canvas.drawText("● " + runCoins,
                24f, coinHudPaint.getTextSize() + 20f, coinHudPaint);

        if (newItemFlashTimer > 0 && newItemFlashName != null) {
            float a = newItemFlashTimer > 20 ? 1f : newItemFlashTimer / 20f;
            newItemFlashPaint.setAlpha((int)(a * 255));
            canvas.drawText("NEW!  " + newItemFlashName,
                    screenW / 2f, screenH * 0.6f, newItemFlashPaint);
        }

        if (depthMetres < 20f) {
            int alpha = depthMetres <= 15f ? 180 : (int)(180f * (1f - (depthMetres - 15f) / 5f));
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
        float rowY    = stats.top + sp(dm, 26f);
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
        statsValuePaint.setColor(Color.WHITE);
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

    private static int lerp(int a, int b, float t) { return (int)(a + (b - a) * t); }

    private static int oxyBarColor(float f) {
        if (f > 0.5f) { float t = (f - 0.5f) / 0.5f; return Color.rgb((int)(255*(1-t)), 200, 0); }
        else          { float t = f / 0.5f;            return Color.rgb(255, (int)(200*t), 0); }
    }

    // ─── Touch ───────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isIntro) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (!isTransitioning) isTransitioning = true;
            }
            return true;
        }

        if (gameOver) {
            float x = event.getX();
            DisplayMetrics dm = getResources().getDisplayMetrics();
            float btnMarginSide = sp(dm, 20f), btnWidth = (screenW - 2f * btnMarginSide - 12f) / 2f;
            float retryLeft = btnMarginSide, retryRight = retryLeft + btnWidth;
            float menuLeft = retryLeft + btnWidth + sp(dm, 12f), menuRight = menuLeft + btnWidth;

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (x >= retryLeft && x <= retryRight) {
                    post(this::resetGame);
                } else if (x >= menuLeft && x <= menuRight) {
                    Context ctx = getContext();
                    if (ctx instanceof GameActivity) ((GameActivity) ctx).finish();
                }
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                retryButtonHovered = (x >= retryLeft && x <= retryRight);
                menuButtonHovered = (x >= menuLeft && x <= menuRight);
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                retryButtonHovered = menuButtonHovered = false;
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
                registerRelease(event.getX(idx));
                if (action == MotionEvent.ACTION_UP) performClick();
                break;
            case MotionEvent.ACTION_CANCEL:
                pressingLeft = pressingRight = false; break;
        }
        return true;
    }

    @Override public boolean performClick() { return super.performClick(); }

    private void registerPress(float x) {
        if (x < screenW / 2f) pressingLeft = true;
        else pressingRight = true;
    }

    private void registerRelease(float x) {
        if (x < screenW / 2f) pressingLeft = false;
        else pressingRight = false;
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    public void resume() { playing = true; thread = new Thread(this); thread.start(); }
    public void pause() {
        playing = false;
        try { thread.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    @Override public void surfaceCreated(SurfaceHolder holder) { }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenW = width; screenH = height; loadBitmaps(); resetGame();
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { recycleBitmaps(); }
}
