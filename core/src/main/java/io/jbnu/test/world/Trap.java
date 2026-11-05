package io.jbnu.test.world;

import com.badlogic.gdx.math.Rectangle;

/** 일정 주기로 켜지고 꺼지는 바닥 함정입니다. */
public final class Trap {
    public final Rectangle bounds;
    public final float period, activeTime;
    public float timer;

    Trap(float x, float y, float width, float period, float activeTime, float phase) {
        bounds = new Rectangle(x, y, width, 24);
        this.period = period;
        this.activeTime = activeTime;
        timer = phase;
    }

    public boolean active() {
        return timer < activeTime;
    }

    /** 현재 상태가 끝날 때까지 남은 비율(1~0)입니다. */
    public float remaining() {
        return active() ? (activeTime - timer) / activeTime : (period - timer) / (period - activeTime);
    }

    void update(float dt) {
        timer = (timer + dt) % period;
    }
}
