package io.jbnu.test.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import io.jbnu.test.world.GameWorld;
import io.jbnu.test.world.Player;
import io.jbnu.test.world.Tuning;

/**
 * 플레이어와 1:1로 묶이지 않는 카메라입니다.
 * 데드존, 진행 방향 앞보기, 발판 기준 수직 추적, 화면 흔들림, Zen 줌, 연출용 시점 이동을 지원합니다.
 */
public final class GameCamera {
    private static final float DEAD_ZONE = 36, LOOK_AHEAD = 90;

    public final OrthographicCamera camera = new OrthographicCamera();
    private float x, y, lookAhead, anchorY, zoom = 1, zoomTarget = 1;
    private float shakeTime, shakeLength, shakePower;
    private float focusX, focusY;
    private boolean focused;
    private float rise;

    public GameCamera() {
        camera.setToOrtho(false, Tuning.VIEW_W, Tuning.VIEW_H);
    }

    public void snapTo(GameWorld world) {
        x = world.player.bounds.x + Player.W / 2;
        anchorY = world.player.bounds.y;
        y = anchorY + Tuning.VIEW_H * 0.28f;
        lookAhead = 0;
        focused = false;
        rise = 0;
        zoom = zoomTarget = 1;
        clamp(world);
        apply(0);
    }

    public void shake(float power, float seconds) {
        if (power < shakePower && shakeTime > 0) return;
        shakePower = power;
        shakeTime = shakeLength = seconds;
    }

    public void setFocus(float focusX, float focusY) {
        this.focusX = focusX;
        this.focusY = focusY;
        focused = true;
    }

    public void clearFocus() {
        focused = false;
    }

    public void setZoomTarget(float zoomTarget) {
        this.zoomTarget = zoomTarget;
    }

    /** 클리어 연출처럼 월드 경계를 넘어 위로 올라가는 양입니다. */
    public void setRise(float rise) {
        this.rise = rise;
    }

    public void update(float dt, GameWorld world) {
        if (focused) {
            float k = 1 - (float) Math.exp(-6 * dt);
            x += (focusX - x) * k;
            y += (focusY - y) * k;
        } else {
            follow(dt, world);
        }
        zoom += (zoomTarget - zoom) * (1 - (float) Math.exp(-5 * dt));
        clamp(world);
        apply(dt);
    }

    /** 인트로 연출에서 위치를 직접 지정합니다. */
    public void setPosition(float newX, float newY, GameWorld world) {
        x = newX;
        y = newY;
        clamp(world);
        apply(0);
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    private void follow(float dt, GameWorld world) {
        Player player = world.player;
        float px = player.bounds.x + Player.W / 2;
        float py = player.bounds.y;

        float lookTarget = Math.abs(player.vx) > 20 ? player.facing * LOOK_AHEAD : lookAhead;
        lookAhead += (lookTarget - lookAhead) * (1 - (float) Math.exp(-2.5f * dt));
        float tx = px + lookAhead;
        float desiredX = x;
        if (tx > x + DEAD_ZONE) desiredX = tx - DEAD_ZONE;
        else if (tx < x - DEAD_ZONE) desiredX = tx + DEAD_ZONE;
        x += (desiredX - x) * (1 - (float) Math.exp(-9 * dt));

        // 점프 중에는 수직 흔들림을 줄이고, 착지하거나 크게 벗어날 때만 따라갑니다.
        if (player.ground != null) anchorY = py;
        else if (py < anchorY - 30) anchorY = py + 30;
        else if (py > anchorY + 170) anchorY = py - 170;
        float targetY = anchorY + Tuning.VIEW_H * 0.28f;
        y += (targetY - y) * (1 - (float) Math.exp(-5 * dt));
    }

    private void clamp(GameWorld world) {
        float halfW = Tuning.VIEW_W / 2 * zoom;
        float halfH = Tuning.VIEW_H / 2 * zoom;
        x = MathUtils.clamp(x, halfW, Math.max(halfW, world.def.worldWidth - halfW));
        y = MathUtils.clamp(y, halfH, Math.max(halfH, world.def.worldHeight - halfH));
    }

    private void apply(float dt) {
        float ox = 0, oy = 0;
        if (shakeTime > 0) {
            shakeTime = Math.max(0, shakeTime - dt);
            float amount = shakePower * (shakeTime / shakeLength);
            ox = MathUtils.random(-amount, amount);
            oy = MathUtils.random(-amount, amount);
        }
        camera.zoom = zoom;
        camera.position.set(x + ox, y + oy + rise, 0);
        camera.update();
    }
}
