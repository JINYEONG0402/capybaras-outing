package io.jbnu.test.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import io.jbnu.test.world.Tuning;

/** 카메라보다 느리게 움직이는 다층 배경을 코드로 그립니다. 화면 좌표계에서 호출합니다. */
public final class ParallaxBackground {
    private static final int COLUMN = 6, SKY_BANDS = 36, STAR_COUNT = 70;

    private final GameAssets assets;
    private final Theme theme;
    private final Color band = new Color();

    public ParallaxBackground(GameAssets assets, Theme theme) {
        this.assets = assets;
        this.theme = theme;
    }

    public void render(float camX, float camY, float time) {
        drawSky();
        float lift = (camY - Tuning.VIEW_H / 2);
        if (theme.stars) drawStars(camX, time);
        drawSun(lift);
        if (theme.clouds) drawClouds(camX, time);
        drawHills(camX, lift * 0.25f, 0.18f, 170, 44, 0.008f, theme.hillFar);
        drawHills(camX, lift * 0.45f, 0.38f, 105, 34, 0.013f, theme.hillNear);
    }

    private void drawSky() {
        float bandHeight = Tuning.VIEW_H / SKY_BANDS;
        for (int i = 0; i < SKY_BANDS; i++) {
            band.set(theme.skyBottom).lerp(theme.skyTop, i / (SKY_BANDS - 1f));
            assets.batch.setColor(band);
            assets.batch.draw(assets.white, 0, i * bandHeight, Tuning.VIEW_W, bandHeight + 1);
        }
        assets.batch.setColor(Color.WHITE);
    }

    private void drawStars(float camX, float time) {
        for (int i = 0; i < STAR_COUNT; i++) {
            // 고정된 해시값으로 항상 같은 위치에 별을 찍습니다.
            float hx = (i * 7919 % 1000) / 1000f;
            float hy = (i * 6271 % 1000) / 1000f;
            float x = (hx * Tuning.VIEW_W * 1.2f - camX * 0.05f) % Tuning.VIEW_W;
            if (x < 0) x += Tuning.VIEW_W;
            float y = Tuning.VIEW_H * (0.35f + hy * 0.65f);
            float alpha = 0.45f + 0.45f * MathUtils.sin(time * 1.7f + i);
            float size = i % 5 == 0 ? 3 : 2;
            assets.rect(x, y, size, size, 1, 1, 0.92f, alpha);
        }
    }

    private void drawSun(float lift) {
        float radius = theme.stars ? 36 : 44;
        float x = 860;
        float y = Tuning.VIEW_H - 120 - lift * 0.08f;
        assets.batch.setColor(theme.sun.r, theme.sun.g, theme.sun.b, 0.25f);
        assets.batch.draw(assets.circle, x - radius * 1.5f, y - radius * 1.5f, radius * 3, radius * 3);
        assets.batch.setColor(theme.sun);
        assets.batch.draw(assets.circle, x - radius, y - radius, radius * 2, radius * 2);
        assets.batch.setColor(Color.WHITE);
    }

    private void drawClouds(float camX, float time) {
        assets.batch.setColor(1, 1, 1, 0.75f);
        for (int i = 0; i < 6; i++) {
            float span = Tuning.VIEW_W + 300;
            float x = (i * 230 - camX * 0.1f - time * (6 + i)) % span;
            if (x < 0) x += span;
            x -= 150;
            float y = 360 + (i * 53 % 130);
            float scale = 0.8f + (i % 3) * 0.25f;
            assets.batch.draw(assets.circle, x, y, 70 * scale, 34 * scale);
            assets.batch.draw(assets.circle, x + 30 * scale, y + 8 * scale, 80 * scale, 44 * scale);
            assets.batch.draw(assets.circle, x + 76 * scale, y, 64 * scale, 30 * scale);
        }
        assets.batch.setColor(Color.WHITE);
    }

    private void drawHills(float camX, float lift, float factor, float base, float amp, float freq, Color color) {
        assets.batch.setColor(color);
        float offset = camX * factor;
        for (int sx = 0; sx < Tuning.VIEW_W; sx += COLUMN) {
            float wx = sx + offset;
            float h;
            if (theme.peaks) {
                // 삼각파를 겹쳐 뾰족한 화산 능선을 만듭니다.
                float a = Math.abs(((wx * freq * 0.5f) % 2) - 1);
                float b = Math.abs(((wx * freq * 1.3f + 0.4f) % 2) - 1);
                h = base + (a * 2.4f + b * 0.8f) * amp;
            } else {
                h = base + MathUtils.sin(wx * freq) * amp + MathUtils.sin(wx * freq * 2.3f + 1) * amp * 0.35f;
            }
            float top = h - lift;
            if (top > 0) assets.batch.draw(assets.white, sx, 0, COLUMN, top);
        }
        assets.batch.setColor(Color.WHITE);
    }
}
