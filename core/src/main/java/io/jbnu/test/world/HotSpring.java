package io.jbnu.test.world;

import com.badlogic.gdx.math.Rectangle;

/** 체크포인트이자, 가만히 있으면 Zen 게이지를 채워 주는 온천입니다. */
public final class HotSpring {
    public static final float W = 70, H = 16;

    public final Rectangle bounds;
    public boolean activated;

    HotSpring(float x, float y) {
        bounds = new Rectangle(x, y, W, H);
    }
}
