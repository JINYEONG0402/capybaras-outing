package io.jbnu.test.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.jbnu.test.world.Tuning;

/**
 * 마우스와 터치를 960x540 화면 좌표로 바꿉니다. 창 크기가 바뀌고 여백이 생겨도 정확합니다.
 * 다른 손가락으로 이동 버튼을 누른 채 새로 탭해도 그 탭의 위치를 잡아냅니다.
 */
public final class UiMouse {
    private static final int POINTERS = 5;

    public float x, y;
    public boolean clicked;

    private final boolean[] wasDown = new boolean[POINTERS];
    private boolean started;

    public void update(Viewport viewport) {
        clicked = false;
        setPosition(viewport, 0);
        for (int p = 0; p < POINTERS; p++) {
            boolean down = Gdx.input.isTouched(p);
            // 첫 프레임의 눌림은 이전 화면에서 누른 것이므로 클릭으로 치지 않습니다.
            if (started && down && !wasDown[p]) {
                clicked = true;
                setPosition(viewport, p);
            }
            wasDown[p] = down;
        }
        started = true;
    }

    private void setPosition(Viewport viewport, int pointer) {
        x = toViewX(viewport, Gdx.input.getX(pointer));
        y = toViewY(viewport, Gdx.input.getY(pointer));
    }

    static float toViewX(Viewport viewport, int screenX) {
        return (screenX - viewport.getScreenX()) * Tuning.VIEW_W / viewport.getScreenWidth();
    }

    static float toViewY(Viewport viewport, int screenY) {
        float local = Gdx.graphics.getHeight() - screenY - viewport.getScreenY();
        return local * Tuning.VIEW_H / viewport.getScreenHeight();
    }
}
