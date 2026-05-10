package org.classapp.molediver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    private static final String TAG = "GameView";

    // ─── Context (held as application context to avoid Activity leak) ─────────
    private final Context context;

    // ─── Game loop ───────────────────────────────────────────────────────────
    private Thread           thread;
    private volatile boolean playing;

    // ─── Screen ──────────────────────────────────────────────────────────────
    private int screenW, screenH;

    // ─── Mole (constants) ────────────────────────────────────────────────────
    private static final float MOLE_RADIUS = 28f;
    private static final float MAX_ANGLE   = 75f;

    // ─── Mole (upgrade-derived, recalculated each run in resetGame) ──────────
    private float moveSpeed;
    private float turnSpeed;

    private float moleWorldX;
    private float moleWorldY;
    private float moleAngle; // degrees: 0 = straight down, negative = left, positive = right

    // ─── Touch ───────────────────────────────────────────────────────────────
    private volatile boolean pressingLeft;
    private volatile boolean pressingRight;

    // ─── Camera / depth ──────────────────────────────────────────────────────
    private float cameraY;
    private float depthMetres;

    // ─── Gas ─────────────────────────────────────────────────────────────────
    private static final float GAS_SPEED = 1.2f;
    private float gasWorldY;

    // ─── Oxygen (upgrade-derived) ─────────────────────────────────────────────
    private static final float OXY_DRAIN = 0.03f;
    private float maxOxygen;
    private float oxygen;

    // ─── O2 canisters ────────────────────────────────────────────────────────
    private final ArrayList<float[]> canisters = new ArrayList<>();
    private float lastCanisterSpawnY;
    private float o2RestoreAmount;
    private float collectRange;

    // ─── Coin items ──────────────────────────────────────────────────────────
    private final ArrayList<float[]> coinItems = new ArrayList<>();
    private float lastCoinSpawnY;
    private int   runCoins;

    // ─── Game state ──────────────────────────────────────────────────────────
    private volatile boolean gameOver;

    // ─── Background gradient endpoints ───────────────────────────────────────
    private static final int BG_R_START = 101, BG_G_START = 67, BG_B_START = 33;
    private static final int BG_R_END   = 20,  BG_G_END   = 12, BG_B_END   = 6;

    // ─── Paints (allocated once, never inside the draw loop) ─────────────────
    private final Paint bgPaint          = new Paint();
    private final Paint molePaint        = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyePaint         = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gasPaint         = new Paint();
    private final Paint hudPaint         = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coinHudPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint        = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint oxyBgPaint       = new Paint();
    private final Paint oxyFillPaint     = new Paint();
    private final Paint oxyLabelPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint canisterPaint    = new Paint();
    private final Paint canisterCrossPaint = new Paint();
    private final Paint coinFillPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coinOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint         = new Paint();
    private final Paint gameOverPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gameOverSubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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

        canisterPaint.setColor(Color.rgb(30, 140, 50));
        canisterPaint.setStyle(Paint.Style.FILL);

        canisterCrossPaint.setColor(Color.WHITE);
        canisterCrossPaint.setStyle(Paint.Style.FILL);

        coinFillPaint.setColor(Color.rgb(245, 197, 66));
        coinFillPaint.setStyle(Paint.Style.FILL);

        coinOutlinePaint.setColor(Color.rgb(150, 110, 20));
        coinOutlinePaint.setStyle(Paint.Style.STROKE);
        coinOutlinePaint.setStrokeWidth(2f);
        coinOutlinePaint.setAntiAlias(true);

        dimPaint.setColor(Color.argb(160, 0, 0, 0));

        gameOverPaint.setColor(Color.RED);
        gameOverPaint.setTextSize(sp(dm, 72f));
        gameOverPaint.setTextAlign(Paint.Align.CENTER);
        gameOverPaint.setFakeBoldText(true);

        gameOverSubPaint.setColor(Color.rgb(200, 200, 200));
        gameOverSubPaint.setTextSize(sp(dm, 22f));
        gameOverSubPaint.setTextAlign(Paint.Align.CENTER);
    }

    private static float sp(DisplayMetrics dm, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm);
    }

    // ─── State reset ─────────────────────────────────────────────────────────

    private void resetGame() {
        if (screenW == 0) return; // surface dimensions not yet available

        // Read upgrade levels — applied for the duration of this run
        int oxyLevel    = PlayerData.getUpgradeOxygen(context);
        int steerLevel  = PlayerData.getUpgradeSteer(context);
        int digLevel    = PlayerData.getUpgradeDig(context);
        int rangeLevel  = PlayerData.getUpgradeRange(context);
        int refillLevel = PlayerData.getUpgradeO2Refill(context);

        maxOxygen       = 100f + (oxyLevel    - 1) * 25f;  // L1=100  L5=200
        turnSpeed       = 2f   + (steerLevel  - 1) * 0.5f; // L1=2    L5=4
        moveSpeed       = 5f   + (digLevel    - 1) * 1.5f; // L1=5    L5=11
        collectRange    = 50f  + (rangeLevel  - 1) * 15f;  // L1=50   L5=110
        o2RestoreAmount = 10f  + (refillLevel - 1) * 5f;   // L1=10   L5=30

        moleWorldX    = screenW / 2f;
        moleWorldY    = 200f;
        moleAngle     = 0f;
        cameraY       = moleWorldY - screenH * 0.4f;
        gasWorldY     = -300f;
        oxygen        = maxOxygen;
        depthMetres   = 0f;
        runCoins      = 0;

        canisters.clear();
        coinItems.clear();
        lastCanisterSpawnY = moleWorldY;
        lastCoinSpawnY     = moleWorldY;

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

            long elapsed = System.currentTimeMillis() - start;
            long sleepMs = 16L - elapsed;
            if (sleepMs > 0) {
                try { Thread.sleep(sleepMs); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    private void update() {
        if (screenW == 0) return; // surface not ready yet

        // ── Steering ─────────────────────────────────────────────────────────
        if (pressingLeft)  moleAngle = Math.max(moleAngle - turnSpeed, -MAX_ANGLE);
        if (pressingRight) moleAngle = Math.min(moleAngle + turnSpeed,  MAX_ANGLE);

        // ── Movement ─────────────────────────────────────────────────────────
        double rad = Math.toRadians(moleAngle);
        float  vx  = (float)(Math.sin(rad) * moveSpeed);
        float  vy  = (float)(Math.cos(rad) * moveSpeed); // positive = downward

        moleWorldX += vx;
        moleWorldY += vy;
        moleWorldX = Math.max(MOLE_RADIUS, Math.min(moleWorldX, screenW - MOLE_RADIUS));

        // ── Camera ───────────────────────────────────────────────────────────
        cameraY     = moleWorldY - screenH * 0.4f;
        depthMetres = Math.max(cameraY / 10f, 0f); // placeholder conversion

        // ── Gas ──────────────────────────────────────────────────────────────
        gasWorldY += GAS_SPEED;
        if (gasWorldY >= moleWorldY) {
            Log.d(TAG, "Game over: gas reached mole at depth " + (int) depthMetres + "m");
            PlayerData.addCoins(context, runCoins);
            gameOver = true;
            return;
        }

        // ── Oxygen ───────────────────────────────────────────────────────────
        oxygen = Math.max(oxygen - OXY_DRAIN, 0f);
        if (oxygen <= 0f) {
            Log.d(TAG, "Game over: oxygen depleted at depth " + (int) depthMetres + "m");
            PlayerData.addCoins(context, runCoins);
            gameOver = true;
            return;
        }

        // ── Spawn O2 canisters (every ~400 world units below mole) ───────────
        if (moleWorldY - lastCanisterSpawnY >= 400f) {
            lastCanisterSpawnY = moleWorldY;
            float spawnX = MOLE_RADIUS + (float)(Math.random() * (screenW - 2 * MOLE_RADIUS));
            float spawnY = moleWorldY + screenH * 0.85f; // spawn below visible area
            canisters.add(new float[]{spawnX, spawnY});
        }

        // ── Spawn coins (every ~150 world units below mole) ──────────────────
        if (moleWorldY - lastCoinSpawnY >= 150f) {
            lastCoinSpawnY = moleWorldY;
            float spawnX = MOLE_RADIUS + (float)(Math.random() * (screenW - 2 * MOLE_RADIUS));
            float spawnY = moleWorldY + screenH * 0.65f;
            coinItems.add(new float[]{spawnX, spawnY});
        }

        // ── Collect canisters ─────────────────────────────────────────────────
        float rangeSq = collectRange * collectRange;
        Iterator<float[]> canIter = canisters.iterator();
        while (canIter.hasNext()) {
            float[] c  = canIter.next();
            float   dx = moleWorldX - c[0];
            float   dy = moleWorldY - c[1];
            if (dx * dx + dy * dy < rangeSq) {
                canIter.remove();
                oxygen = Math.min(oxygen + o2RestoreAmount, maxOxygen);
            }
        }

        // ── Collect coins ─────────────────────────────────────────────────────
        Iterator<float[]> coinIter = coinItems.iterator();
        while (coinIter.hasNext()) {
            float[] c  = coinIter.next();
            float   dx = moleWorldX - c[0];
            float   dy = moleWorldY - c[1];
            if (dx * dx + dy * dy < rangeSq) {
                coinIter.remove();
                runCoins++;
            }
        }

        // ── PLACEHOLDER: systems to add here ─────────────────────────────────
        // - Terrain tile generation and rendering
        // - Material/item spawning and proximity collection
        // - Enemy spawning and collision
        // - Coin conversion on run end
        // - Upgrade stat application from PlayerData (claw, rarity)
        // ─────────────────────────────────────────────────────────────────────
    }

    // ─── Draw ────────────────────────────────────────────────────────────────

    private void draw() {
        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;

        try {
            // ── Background (darkens linearly toward 1000m) ────────────────────
            float t = Math.min(depthMetres / 1000f, 1f);
            bgPaint.setColor(Color.rgb(
                lerp(BG_R_START, BG_R_END, t),
                lerp(BG_G_START, BG_G_END, t),
                lerp(BG_B_START, BG_B_END, t)
            ));
            canvas.drawRect(0, 0, screenW, screenH, bgPaint);

            // ── Gas cloud (fills from screen top down to gas line) ─────────────
            float gasScreenY = gasWorldY - cameraY;
            if (gasScreenY > 0) {
                canvas.drawRect(0, 0, screenW, gasScreenY, gasPaint);
            }

            // ── O2 canisters ──────────────────────────────────────────────────
            for (float[] c : canisters) {
                float sx = c[0];
                float sy = c[1] - cameraY;
                if (sy < -40 || sy > screenH + 40) continue;
                // Canister body (20×30)
                canvas.drawRect(sx - 10, sy - 15, sx + 10, sy + 15, canisterPaint);
                // Plus sign: horizontal bar
                canvas.drawRect(sx - 7,  sy - 2,  sx + 7,  sy + 2,  canisterCrossPaint);
                // Plus sign: vertical bar
                canvas.drawRect(sx - 2,  sy - 9,  sx + 2,  sy + 9,  canisterCrossPaint);
            }

            // ── Coin items ────────────────────────────────────────────────────
            for (float[] c : coinItems) {
                float sx = c[0];
                float sy = c[1] - cameraY;
                if (sy < -20 || sy > screenH + 20) continue;
                canvas.drawCircle(sx, sy, 10f, coinFillPaint);
                canvas.drawCircle(sx, sy, 10f, coinOutlinePaint);
            }

            // ── Mole (rotated to face dig direction) ──────────────────────────
            canvas.save();
            canvas.translate(moleWorldX, screenH * 0.4f);
            canvas.rotate(moleAngle);
            canvas.drawCircle(0, 0, MOLE_RADIUS, molePaint);
            canvas.drawCircle(-10f, 12f, 5f, eyePaint); // left eye
            canvas.drawCircle( 10f, 12f, 5f, eyePaint); // right eye
            canvas.restore();

            // ── Oxygen bar (right side, vertically centred) ───────────────────
            int barW      = 20;
            int barH      = 200;
            int barMargin = 24;
            int barX      = screenW - barMargin - barW;
            int barTop    = screenH / 2 - barH / 2;
            int barBottom = barTop + barH;

            canvas.drawRect(barX, barTop, barX + barW, barBottom, oxyBgPaint);

            float fillFrac = oxygen / maxOxygen;
            int   fillTop  = (int)(barBottom - barH * fillFrac);
            oxyFillPaint.setColor(oxyBarColor(fillFrac));
            canvas.drawRect(barX, fillTop, barX + barW, barBottom, oxyFillPaint);

            canvas.drawText("O2", barX + barW / 2f, barTop - 8f, oxyLabelPaint);

            // ── HUD: depth (top centre) ───────────────────────────────────────
            canvas.drawText((int) depthMetres + "m", screenW / 2f, hudPaint.getTextSize() + 16f, hudPaint);

            // ── HUD: coin counter (top left) ──────────────────────────────────
            canvas.drawText("● " + runCoins, 24f, coinHudPaint.getTextSize() + 20f, coinHudPaint);

            // ── HUD: zone hints (fade from 15m, invisible by 20m) ─────────────
            if (depthMetres < 20f) {
                int alpha = depthMetres <= 15f
                    ? 180
                    : (int)(180f * (1f - (depthMetres - 15f) / 5f));
                hintPaint.setAlpha(alpha);
                hintPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("< LEFT", 40f, screenH - 60f, hintPaint);
                hintPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("RIGHT >", screenW - 40f, screenH - 60f, hintPaint);
            }

            // ── Game over overlay ─────────────────────────────────────────────
            if (gameOver) {
                canvas.drawRect(0, 0, screenW, screenH, dimPaint);
                float cy = screenH / 2f;
                canvas.drawText("GAME OVER",       screenW / 2f, cy - 40f, gameOverPaint);
                canvas.drawText("COINS: +" + runCoins, screenW / 2f, cy + 50f, gameOverSubPaint);
                canvas.drawText("tap to play again",   screenW / 2f, cy + 90f, gameOverSubPaint);
            }

        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static int lerp(int start, int end, float t) {
        return (int)(start + (end - start) * t);
    }

    private static int oxyBarColor(float fraction) {
        if (fraction > 0.5f) {
            // Green → yellow as fraction falls from 1.0 to 0.5
            float t = (fraction - 0.5f) / 0.5f; // 1.0 at full, 0.0 at half
            return Color.rgb((int)(255 * (1f - t)), 200, 0);
        } else {
            // Yellow → red as fraction falls from 0.5 to 0.0
            float t = fraction / 0.5f; // 1.0 at half, 0.0 at empty
            return Color.rgb(255, (int)(200 * t), 0);
        }
    }

    // ─── Touch ───────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                resetGame();
            }
            return true;
        }

        int action       = event.getActionMasked();
        int pointerIndex = event.getActionIndex();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                registerPress(event.getX(pointerIndex));
                break;

            case MotionEvent.ACTION_MOVE:
                // Re-evaluate every active pointer so held fingers aren't lost
                pressingLeft  = false;
                pressingRight = false;
                for (int i = 0; i < event.getPointerCount(); i++) {
                    registerPress(event.getX(i));
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                registerRelease(event.getX(pointerIndex));
                break;

            case MotionEvent.ACTION_CANCEL:
                pressingLeft  = false;
                pressingRight = false;
                break;
        }
        return true;
    }

    private void registerPress(float x) {
        if (x < screenW / 2f) pressingLeft  = true;
        else                   pressingRight = true;
    }

    private void registerRelease(float x) {
        if (x < screenW / 2f) pressingLeft  = false;
        else                   pressingRight = false;
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    public void resume() {
        playing = true;
        thread  = new Thread(this);
        thread.start();
    }

    public void pause() {
        playing = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override public void surfaceCreated(SurfaceHolder holder) { }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenW = width;
        screenH = height;
        resetGame();
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) { }
}
