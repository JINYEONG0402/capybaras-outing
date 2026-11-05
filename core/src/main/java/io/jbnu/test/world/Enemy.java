package io.jbnu.test.world;

import com.badlogic.gdx.math.Rectangle;

/** 모든 적의 공통 동작입니다. 종류별 행동은 하위 클래스가 다형성으로 구현합니다. */
public abstract class Enemy {
    public enum Kind { BOAR, HEDGEHOG, HAWK, DERPY }

    public final Rectangle bounds;
    public boolean alive = true;
    public int facing = 1;
    public float animTime;
    private final float spawnX, spawnY;

    protected Enemy(float x, float y, float width, float height) {
        bounds = new Rectangle(x, y, width, height);
        spawnX = x;
        spawnY = y;
    }

    public abstract Kind kind();

    /** 밟아서 친구로 만들 수 있으면 true입니다. */
    public abstract boolean stompable();

    /** 밟았을 때 친구가 되면 true, 기절만 하면 false입니다. */
    public boolean becomesFriend() {
        return true;
    }

    /** 지금 닿아도 다치지 않으면 true입니다. */
    public boolean harmless() {
        return false;
    }

    void stun() {
    }

    void onHitPlayer() {
    }

    final void update(float dt, GameWorld world) {
        animTime += dt;
        think(dt, world);
    }

    protected abstract void think(float dt, GameWorld world);

    void reset() {
        bounds.setPosition(spawnX, spawnY);
        alive = true;
        facing = 1;
    }
}
