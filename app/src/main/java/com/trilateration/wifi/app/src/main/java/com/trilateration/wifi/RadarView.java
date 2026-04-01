package com.trilateration.wifi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class RadarView extends View {

    private final Paint bgPaint      = new Paint();
    private final Paint ringPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint apPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint apLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rangePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint posPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint posGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint distLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<WifiScanner.AccessPoint> aps;
    private Trilateration.Point estimatedPos;

    private float scale;
    private float originX, originY;

    public RadarView(Context ctx) { super(ctx); init(); }
    public RadarView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }

    private void init() {
        bgPaint.setColor(Color.parseColor("#060F1E"));
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.parseColor("#1A3A5C"));
        ringPaint.setStrokeWidth(1.5f);
        apPaint.setStyle(Paint.Style.FILL);
        apPaint.setColor(Color.parseColor("#00CFFF"));
        apLabelPaint.setColor(Color.parseColor("#80DFFF"));
        apLabelPaint.setTextSize(26f);
        rangePaint.setStyle(Paint.Style.STROKE);
        rangePaint.setColor(Color.parseColor("#004466"));
        rangePaint.setStrokeWidth(2f);
        rangePaint.setPathEffect(new DashPathEffect(new float[]{14, 8}, 0));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.parseColor("#33AADD"));
        linePaint.setStrokeWidth(1.5f);
        linePaint.setPathEffect(new DashPathEffect(new float[]{6, 6}, 0));
        posPaint.setStyle(Paint.Style.FILL);
        posPaint.setColor(Color.parseColor("#FF3333"));
        posGlowPaint.setStyle(Paint.Style.FILL);
        posGlowPaint.setColor(Color.parseColor("#44FF3333"));
        distLabelPaint.setColor(Color.parseColor("#AAAAAA"));
        distLabelPaint.setTextSize(22f);
    }

    public void update(List<WifiScanner.AccessPoint> aps, Trilateration.Point pos) {
        this.aps          = aps;
        this.estimatedPos = pos;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        canvas.drawRect(0, 0, w, h, bgPaint);
        if (aps == null || aps.size() < 3) {
            drawPlaceholder(canvas, w, h);
            return;
        }
        computeTransform(w, h);
        drawRings(canvas, w, h);
        drawRangeCircles(canvas);
        drawConnectingLines(canvas);
        drawAccessPoints(canvas);
        if (estimatedPos != null) drawEstimatedPosition(canvas);
    }

    private void drawPlaceholder(Canvas canvas, int w, int h) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor("#335577"));
        p.setTextSize(36f);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Scan for WiFi networks", w / 2f, h / 2f, p);
    }

    private void drawRings(Canvas canvas, int w, int h) {
        float cx = w / 2f, cy = h / 2f;
        float maxR = Math.min(cx, cy) * 0.9f;
        for (int i = 1; i <= 4; i++) {
            canvas.drawCircle(cx, cy, maxR * i / 4f, ringPaint);
        }
        canvas.drawLine(cx, cy - maxR, cx, cy + maxR, ringPaint);
        canvas.drawLine(cx - maxR, cy, cx + maxR, cy, ringPaint);
    }

    private void drawRangeCircles(Canvas canvas) {
        for (WifiScanner.AccessPoint ap : aps) {
            float sx = worldToScreenX((float) ap.x);
            float sy = worldToScreenY((float) ap.y);
            float sr = (float) (ap.distance * scale);
            canvas.drawCircle(sx, sy, sr, rangePaint);
        }
    }

    private void drawConnectingLines(Canvas canvas) {
        if (estimatedPos == null) return;
        float px = worldToScreenX((float) estimatedPos.x);
        float py = worldToScreenY((float) estimatedPos.y);
        for (WifiScanner.AccessPoint ap : aps) {
            float sx = worldToScreenX((float) ap.x);
            float sy = worldToScreenY((float) ap.y);
            canvas.drawLine(px, py, sx, sy, linePaint);
        }
    }

    private void drawAccessPoints(Canvas canvas) {
        int i = 1;
        for (WifiScanner.AccessPoint ap : aps) {
            float sx = worldToScreenX((float) ap.x);
            float sy = worldToScreenY((float) ap.y);
            canvas.drawCircle(sx, sy, 22f, posGlowPaint);
            canvas.drawCircle(sx, sy, 12f, apPaint);
            canvas.drawText("AP" + i + ": " + ap.label(), sx + 16f, sy - 16f, apLabelPaint);
            canvas.drawText(String.format("%.1f m", ap.distance), sx + 16f, sy + 8f, distLabelPaint);
            i++;
        }
    }

    private void drawEstimatedPosition(Canvas canvas) {
        float px = worldToScreenX((float) estimatedPos.x);
        float py = worldToScreenY((float) estimatedPos.y);
        canvas.drawCircle(px, py, 30f, posGlowPaint);
        canvas.drawCircle(px, py, 14f, posPaint);
        Paint labelP = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelP.setColor(Color.WHITE);
        labelP.setTextSize(28f);
        labelP.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("YOU", px, py - 22f, labelP);
    }

    private void computeTransform(int w, int h) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (WifiScanner.AccessPoint ap : aps) {
            minX = Math.min(minX, ap.x); maxX = Math.max(maxX, ap.x);
            minY = Math.min(minY, ap.y); maxY = Math.max(maxY, ap.y);
        }
        if (estimatedPos != null) {
            minX = Math.min(minX, estimatedPos.x); maxX = Math.max(maxX, estimatedPos.x);
            minY = Math.min(minY, estimatedPos.y); maxY = Math.max(maxY, estimatedPos.y);
        }
        double range = Math.max(maxX - minX, maxY - minY);
        if (range < 1.0) range = 1.0;
        scale   = (float) (Math.min(w, h) * 0.7 / range);
        originX = w / 2f - (float) ((minX + maxX) / 2.0 * scale);
        originY = h / 2f + (float) ((minY + maxY) / 2.0 * scale);
    }

    private float worldToScreenX(float wx) { return originX + wx * scale; }
    private float worldToScreenY(float wy) { return originY - wy * scale; }
}
