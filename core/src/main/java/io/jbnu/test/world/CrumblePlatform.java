package io.jbnu.test.world;

import com.badlogic.gdx.math.Rectangle;

/** 밟으면 흔들리다가 무너지고, 잠시 뒤 돌아오는 발판입니다. */
public final class CrumblePlatform {
    public enum State { IDLE, SHAKING, FALLEN }

    public final Rectangle bounds;
    public State state = State.IDLE;
    public float timer;

    CrumblePlatform(float x, float y, float width) {
        bounds = new Rectangle(x, y, width, 20);
    }

    void restore() {
        state = State.IDLE;
        timer = 0;
    }
}
