package io.jbnu.test.world;

import com.badlogic.gdx.math.Rectangle;

/** 두 지점을 왕복하는 발판입니다. */
public final class MovingPlatform {
    public final Rectangle bounds;
    public final boolean vertical;
    private final float min, max, speed;
    private float direction = 1;

    MovingPlatform(float x, float y, float width, float min, float max, float speed, boolean vertical) {
        bounds = new Rectangle(x, y, width, 20);
        this.min = min;
        this.max = max;
        this.speed = speed;
        this.vertical = vertical;
    }

    /** 이동하고 위치 변화량을 돌려줍니다. */
    float advance(float dt) {
        float old = vertical ? bounds.y : bounds.x;
        float position = old + direction * speed * dt;
        if (position >= max) {
            position = max;
            direction = -1;
        } else if (position <= min) {
            position = min;
            direction = 1;
        }
        if (vertical) bounds.y = position;
        else bounds.x = position;
        return position - old;
    }
}
