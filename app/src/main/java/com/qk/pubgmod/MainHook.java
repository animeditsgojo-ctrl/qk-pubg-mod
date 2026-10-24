package com.qk.pubgmod;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET = "com.tencent.ig";

    private final Paint boxPaint  = makePaint(Color.RED,   3f);
    private final Paint textPaint = makePaint(Color.WHITE, 1f);

    private static Paint makePaint(int color, float stroke) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStrokeWidth(stroke);
        p.setStyle(Paint.Style.STROKE);
        p.setTextSize(28f);
        return p;
    }

    static class Enemy {
        float screenX, screenY, health, distance;
        boolean visible;
    }

    private final Enemy[] enemies    = new Enemy[100];
    private volatile int  enemyCount = 0;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpp) {
        if (!TARGET.equals(lpp.packageName)) return;
        XposedBridge.log("[QK] Yüklendi");
        hookRenderer(lpp.classLoader);
        hookGameLoop(lpp.classLoader);
    }

    private void hookRenderer(ClassLoader cl) {
        XposedHelpers.findAndHookMethod(
            "android.view.View", cl, "draw", Canvas.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Canvas canvas = (Canvas) param.args[0];
                    if (canvas == null) return;
                    drawESP(canvas);
                }
            }
        );
    }

    private void drawESP(Canvas canvas) {
        int count = enemyCount;
        for (int i = 0; i < count; i++) {
            Enemy e = enemies[i];
            if (e == null || !e.visible) continue;
            float x = e.screenX, y = e.screenY;
            float w = 60f, h = 120f;
            canvas.drawRect(x - w/2, y - h, x + w/2, y, boxPaint);
            canvas.drawText(
                String.format("%.0fm HP:%.0f", e.distance, e.health),
                x - w/2, y - h - 8, textPaint
            );
        }
    }

    private void hookGameLoop(ClassLoader cl) {
        try {
            Class<?> gm = XposedHelpers.findClass(
                "com.tencent.pubgmobile.GameManager", cl
            );
            XposedHelpers.findAndHookMethod(gm, "update",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param)
                            throws Throwable {
                        updateEnemies(param.thisObject);
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("[QK] GameManager hook hata: " + t.getMessage());
        }
    }

    private void updateEnemies(Object gm) {
        try {
            Object list = XposedHelpers.getObjectField(gm, "entityList");
            if (list == null) return;
            int count   = (int) XposedHelpers.callMethod(list, "size");
            int written = 0;
            for (int i = 0; i < count && written < 100; i++) {
                Object entity = XposedHelpers.callMethod(list, "get", i);
                if (entity == null) continue;
                boolean alive = (boolean) XposedHelpers.getObjectField(entity, "isAlive");
                if (!alive) continue;
                Enemy e    = new Enemy();
                e.health   = (float) XposedHelpers.getObjectField(entity, "health");
                e.distance = (float) XposedHelpers.getObjectField(entity, "distance");
                e.screenX  = (float) XposedHelpers.getObjectField(entity, "screenX");
                e.screenY  = (float) XposedHelpers.getObjectField(entity, "screenY");
                e.visible  = true;
                enemies[written++] = e;
            }
            enemyCount = written;
        } catch (Throwable t) {
            XposedBridge.log("[QK] updateEnemies hata: " + t.getMessage());
        }
    }
}
