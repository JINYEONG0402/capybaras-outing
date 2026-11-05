package io.jbnu.test.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.jbnu.test.render.GameAssets;
import io.jbnu.test.world.InputState;

/** 휴대폰용 화면 버튼입니다. 여러 손가락을 동시에 인식해 달리면서 점프할 수 있습니다. */
public final class TouchControls {
    private static final int POINTERS = 5;
    private static final float SIZE = 96, PAD = 18;

    // 손가락이 살짝 벗어나도 눌리도록 판정 영역을 그림보다 넓게 둡니다.
    private final Rectangle left = new Rectangle(PAD, PAD, SIZE, SIZE);
    private final Rectangle right = new Rectangle(PAD * 2 + SIZE, PAD, SIZE, SIZE);
    private final Rectangle jump = new Rectangle(960 - PAD - SIZE, PAD, SIZE, SIZE);
    private final Rectangle zen = new Rectangle(960 - PAD * 2 - SIZE * 1.8f, PAD, SIZE * 0.8f, SIZE * 0.8f);
    private boolean leftDown, rightDown, jumpDown, zenDown;

    /** 터치 상태를 읽어 게임 입력에 더합니다. 눌림 순간의 입력은 물리 스텝이 처리할 때까지 유지됩니다. */
    public void apply(Viewport viewport, InputState input) {
        boolean wasJump = jumpDown, wasZen = zenDown;
        leftDown = rightDown = jumpDown = zenDown = false;
        for (int p = 0; p < POINTERS; p++) {
            if (!Gdx.input.isTouched(p)) continue;
            float x = UiMouse.toViewX(viewport, Gdx.input.getX(p));
            float y = UiMouse.toViewY(viewport, Gdx.input.getY(p));
            if (inside(left, x, y)) leftDown = true;
            if (inside(right, x, y)) rightDown = true;
            if (inside(jump, x, y)) jumpDown = true;
            if (inside(zen, x, y)) zenDown = true;
        }
        input.left |= leftDown;
        input.right |= rightDown;
        input.jumpHeld |= jumpDown;
        input.jumpPressed |= jumpDown && !wasJump;
        input.zenPressed |= zenDown && !wasZen;
    }

    private static boolean inside(Rectangle r, float x, float y) {
        float margin = 10;
        return x >= r.x - margin && x <= r.x + r.width + margin && y >= r.y - margin && y <= r.y + r.height + margin;
    }

    public void draw(GameAssets assets, boolean zenReady) {
        drawButton(assets, left, leftDown, 180);
        drawButton(assets, right, rightDown, 0);
        drawButton(assets, jump, jumpDown, 90);
        drawCircle(assets, zen, zenDown, zenReady ? 0.85f : 0.3f);
        // 게이지가 차면 방패가 금색으로 밝아집니다.
        float size = zen.width * 0.56f;
        if (zenReady) assets.batch.setColor(1f, 0.84f, 0.25f, 1);
        else assets.batch.setColor(0.8f, 0.8f, 0.8f, 0.6f);
        assets.batch.draw(assets.iconShield, zen.x + (zen.width - size) / 2, zen.y + (zen.height - size) / 2, size, size);
        assets.batch.setColor(Color.WHITE);
    }

    private void drawButton(GameAssets assets, Rectangle r, boolean down, float rotation) {
        drawCircle(assets, r, down, 0.3f);
        float size = r.width * 0.46f;
        assets.batch.setColor(1, 1, 1, down ? 1f : 0.8f);
        assets.batch.draw(assets.arrow, r.x + (r.width - size) / 2, r.y + (r.height - size) / 2, size / 2, size / 2,
            size, size, 1, 1, rotation, 0, 0, assets.arrow.getWidth(), assets.arrow.getHeight(), false, false);
        assets.batch.setColor(Color.WHITE);
    }

    private void drawCircle(GameAssets assets, Rectangle r, boolean down, float greenBoost) {
        float alpha = down ? 0.65f : 0.34f;
        assets.batch.setColor(0.08f, 0.1f + greenBoost * 0.4f, 0.14f, alpha);
        assets.batch.draw(assets.circle, r.x, r.y, r.width, r.height);
        assets.batch.setColor(Color.WHITE);
    }
}
