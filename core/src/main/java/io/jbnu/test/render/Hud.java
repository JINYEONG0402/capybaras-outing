package io.jbnu.test.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import io.jbnu.test.world.GameWorld;
import io.jbnu.test.world.Tuning;

/** 점수, 귤, Zen 게이지, 콤보, 다음 목표 화살표를 화면에 표시합니다. */
public final class Hud {
    private static final Color WHITE = new Color(1, 1, 1, 1);
    private static final Color GOLD = new Color(1, 0.84f, 0.25f, 1);
    private static final Color ORANGE = new Color(1, 0.6f, 0.15f, 1);
    private static final Color RED = new Color(1, 0.35f, 0.3f, 1);

    private final GameAssets assets;
    private final Color fade = new Color();

    public Hud(GameAssets assets) {
        this.assets = assets;
    }

    public void render(GameWorld world, int round, int roundCount, OrthographicCamera camera, float time,
                       String message, float messageAlpha) {
        drawTopBar(world, round, roundCount);
        drawZenGauge(world, time);
        drawKids(world, roundCount);
        drawCombo(world, time);
        drawObjectiveArrow(world, camera, time);
        drawTideWarning(world, camera, time);
        if (message != null && messageAlpha > 0) {
            fade.set(WHITE);
            fade.a = Math.min(1, messageAlpha);
            assets.textCenter(message, Tuning.VIEW_W / 2, 150, 1.5f, fade);
        }
    }

    private void drawTopBar(GameWorld world, int round, int roundCount) {
        float top = Tuning.VIEW_H - 12;
        assets.rect(0, Tuning.VIEW_H - 44, Tuning.VIEW_W, 44, 0, 0, 0, 0.35f);
        assets.text("ROUND " + (round + 1) + "/" + roundCount, 14, top - 4, 1.2f, WHITE, Align.left);

        assets.batch.draw(assets.tangerine, 145, top - 26, 26, 26);
        boolean ready = world.flagUnlocked();
        assets.text(world.collected + " / " + world.required, 176, top - 4, 1.35f, ready ? GOLD : WHITE, Align.left);

        assets.text(String.format("SCORE %06d", world.run.score), Tuning.VIEW_W / 2, top - 4, 1.35f, WHITE, Align.center);
        int seconds = (int) world.run.time;
        assets.text(String.format("%d:%02d   Falls %d", seconds / 60, seconds % 60, world.run.deaths),
            Tuning.VIEW_W - 58, top - 4, 1.2f, WHITE, Align.right);
    }

    private void drawZenGauge(GameWorld world, float time) {
        float x = 14, y = Tuning.VIEW_H - 74, w = 170, h = 16;
        boolean active = world.zenActive();
        float ratio = active ? world.zenTimer / Tuning.ZEN_DURATION : world.run.zen;
        assets.rect(x - 2, y - 2, w + 4, h + 4, 0, 0, 0, 0.6f);
        boolean full = !active && world.run.zen >= 1;
        float flash = full ? 0.75f + 0.25f * MathUtils.sin(time * 9) : 1;
        assets.rect(x, y, w * ratio, h, 0.45f * flash, 0.9f * flash, 0.65f * flash, 1);
        String label = active ? "ZEN!" : full ? "ZEN READY!" : "ZEN";
        assets.text(label, x + 6, y + 13, 0.85f, WHITE, Align.left);
        if (active || full) assets.batch.setColor(GOLD);
        else assets.batch.setColor(1, 1, 1, 0.55f);
        assets.batch.draw(assets.iconShield, x + w + 6, y - 3, 22, 22);
        assets.batch.setColor(Color.WHITE);

        // 친구 수는 아이콘으로 표시합니다.
        assets.batch.setColor(Color.WHITE);
        for (int i = 0; i < Tuning.MAX_FRIENDS; i++) {
            Texture t = assets.boar[0];
            if (i >= world.run.friends) assets.batch.setColor(0, 0, 0, 0.35f);
            assets.batch.draw(t, x + i * 34, y - 30, 30, 21);
            assets.batch.setColor(Color.WHITE);
        }
    }

    /** 찾은 자식 수를 아이콘으로 표시합니다. 아직 못 찾은 자식은 그림자로 보입니다. */
    private void drawKids(GameWorld world, int total) {
        float y = Tuning.VIEW_H - 136;
        for (int i = 0; i < total; i++) {
            if (i < world.run.children) assets.batch.setColor(Color.WHITE);
            else assets.batch.setColor(0, 0, 0, 0.4f);
            assets.batch.draw(assets.baby, 14 + i * 34, y, 30, 27);
        }
        assets.batch.setColor(Color.WHITE);
    }

    private void drawCombo(GameWorld world, float time) {
        if (world.combo < 2) return;
        float pulse = 1.6f + Math.min(world.combo, 9) * 0.12f + 0.08f * MathUtils.sin(time * 12);
        fade.set(world.combo >= 5 ? RED : ORANGE);
        fade.a = MathUtils.clamp(world.comboTimer * 2, 0, 1);
        assets.text(world.combo + " COMBO  x" + world.combo, Tuning.VIEW_W - 66, Tuning.VIEW_H - 62, pulse, fade, Align.right);
    }

    /** 목표가 화면 밖이면 가장자리에 방향을 표시합니다. */
    private void drawObjectiveArrow(GameWorld world, OrthographicCamera camera, float time) {
        Rectangle target = world.nextObjective();
        if (target == null) return;
        float halfW = Tuning.VIEW_W / 2 * camera.zoom, halfH = Tuning.VIEW_H / 2 * camera.zoom;
        float dx = target.x + target.width / 2 - camera.position.x;
        float dy = target.y + target.height / 2 - camera.position.y;
        if (Math.abs(dx) < halfW - 10 && Math.abs(dy) < halfH - 10) return;

        float margin = 46;
        float limitX = Tuning.VIEW_W / 2 - margin, limitY = Tuning.VIEW_H / 2 - margin - 30;
        float scale = Math.min(limitX / Math.max(1, Math.abs(dx)), limitY / Math.max(1, Math.abs(dy)));
        float sx = Tuning.VIEW_W / 2 + dx * scale;
        float sy = Tuning.VIEW_H / 2 + dy * scale - 12;
        float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float size = 30 + 4 * MathUtils.sin(time * 6);

        boolean flag = world.flagUnlocked();
        Color color = flag ? GOLD : ORANGE;
        assets.batch.setColor(color.r, color.g, color.b, 0.95f);
        assets.batch.draw(assets.arrow, sx - size / 2, sy - size / 2, size / 2, size / 2, size, size, 1, 1, angle,
            0, 0, assets.arrow.getWidth(), assets.arrow.getHeight(), false, false);
        assets.batch.setColor(Color.WHITE);
    }

    private void drawTideWarning(GameWorld world, OrthographicCamera camera, float time) {
        if (!world.def.hasTide) return;
        float distance = world.player.bounds.y - world.tideY;
        if (distance < 130) {
            float alpha = 0.3f + 0.25f * MathUtils.sin(time * 10);
            assets.rect(0, 0, Tuning.VIEW_W, 16, 1, 0.2f, 0.1f, alpha);
            assets.rect(0, Tuning.VIEW_H - 16, Tuning.VIEW_W, 16, 1, 0.2f, 0.1f, alpha);
            assets.textCenter("BOILING WATER!", Tuning.VIEW_W / 2, 60, 1.5f, RED);
        }
    }
}
