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

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    private static final String TAG = "GameView";

    // ─── Game loop ───────────────────────────────────────────────────────────
    private Thread           thread;
    private volatile boolean playing;

    // ─── Screen ──────────────────────────────────────────────────────────────
    private int screenW, screenH;

    // ─── Mole ────────────────────────────────────────────────────────────────
    private static final float MOVE_SPEED  = 5f;
    private static final float TURN_SPEED  = 2f;
    private static final float MOLE_RADIUS = 28f;
    private static final float MAX_ANGLE   = 75f;

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

    // ─── Oxygen ──────────────────────────────────────────────────────────────
    private static final float MAX_OXYGEN = 100f;
    private static final float OXY_DRAIN  = 0.03f;
    private float oxygen;

    // ─── Game state ──────────────────────────────────────────────────────────
    private volatile boolean gameOver;

    // ─── Background gradient endpoints ───────────────────────────────────────
    private static final int BG_R_START = 101, BG_G_START = 67, BG_B_START = 33;
    private static final int BG_R_END   = 20,  BG_G_END   = 12, BG_B_END   = 6;

    // ─── Paints (allocated once, never inside the draw loop) ─────────────────
    private final Paint bgPaint       = new Paint();
    private final Paint molePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyePaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gasPaint      = new Paint();
    private final Paint hudPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint oxyBgPaint    = new Paint();
    private final Paint oxyFillPaint  = new Paint();
    private final Paint oxyLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint      = new Paint();
    private final Paint gameOverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameView(Context context) {
        super(context);
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

        hintPaint.setColor(Color.WHITE);
        hintPaint.setTextSize(sp(dm, 18f));

        oxyBgPaint.setColor(Color.rgb(40, 40, 40));

        oxyLabelPaint.setColor(Color.WHITE);
        oxyLabelPaint.setTextSize(sp(dm, 14f));
        oxyLabelPaint.setTextAlign(Paint.Align.CENTER);

        dimPaint.setColor(Color.argb(150, 0, 0, 0));

        gameOverPaint.setColor(Color.RED);
        gameOverPaint.setTextSize(sp(dm, 72f));
        gameOverPaint.setTextAlign(Paint.Align.CENTER);
        gameOverPaint.setFakeBoldText(true);
    }

    private static float sp(DisplayMetrics dm, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm);
    }

    // ─── State reset ─────────────────────────────────────────────────────────

    private void resetGame() {
        if (screenW == 0) return; // surface dimensions not yet available
        moleWorldX    = screenW / 2f;
        moleWorldY    = 200f;
        moleAngle     = 0f;
        cameraY       = moleWorldY - screenH * 0.4f;
        gasWorldY     = -300f;
        oxygen        = MAX_OXYGEN;
        depthMetres   = 0f;
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

            long elapsed  = System.currentTimeMillis() - start;
            long sleepMs  = 16L - elapsed;
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
        if (pressingLeft)  moleAngle = Math.max(moleAngle - TURN_SPEED, -MAX_ANGLE);
        if (pressingRight) moleAngle = Math.min(moleAngle + TURN_SPEED,  MAX_ANGLE);

        // ── Movement ─────────────────────────────────────────────────────────
        double rad = Math.toRadians(moleAngle);
        float  vx  = (float)(Math.sin(rad) * MOVE_SPEED);
        float  vy  = (float)(Math.cos(rad) * MOVE_SPEED); // positive = downward

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
            gameOver = true;
            return;
        }

        // ── Oxygen ───────────────────────────────────────────────────────────
        oxygen = Math.max(oxygen - OXY_DRAIN, 0f);
        if (oxygen <= 0f) {
            Log.d(TAG, "Game over: oxygen depleted at depth " + (int) depthMetres + "m");
            gameOver = true;
            return;
        }

        // ── PLACEHOLDER: systems to add here ─────────────────────────────────
        // - Terrain tile generation and rendering
        // - Material/item spawning and proximity collection
        // - Enemy spawning and collision
        // - O2 canister pickup spawning
        // - Coin conversion on run end
        // - Upgrade stat application from PlayerData
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

            // ── Gas cloud (fills screen top down to gas line) ─────────────────
            float gasScreenY = gasWorldY - cameraY;
            if (gasScreenY > 0) {
                canvas.drawRect(0, 0, screenW, gasScreenY, gasPaint);
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

            float fillFrac = oxygen / MAX_OXYGEN;
            int   fillTop  = (int)(barBottom - barH * fillFrac);
            oxyFillPaint.setColor(oxyBarColor(fillFrac));
            canvas.drawRect(barX, fillTop, barX + barW, barBottom, oxyFillPaint);

            canvas.drawText("O2", barX + barW / 2f, barTop - 8f, oxyLabelPaint);

            // ── Depth counter (top centre) ────────────────────────────────────
            canvas.drawText((int) depthMetres + "m", screenW / 2f, hudPaint.getTextSize() + 16f, hudPaint);

            // ── Zone hints (fade from 15m, invisible by 20m) ──────────────────
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
                canvas.drawText("GAME OVER", screenW / 2f, screenH / 2f, gameOverPaint);
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
