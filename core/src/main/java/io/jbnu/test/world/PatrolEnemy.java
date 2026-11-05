package io.jbnu.test.world;

/** 정해진 구간을 왕복하는 지상 적의 공통 이동입니다. */
abstract class PatrolEnemy extends Enemy {
    private final float min, max, speed;

    PatrolEnemy(float x, float y, float width, float height, float min, float max, float speed) {
        super(x, y, width, height);
        this.min = min;
        this.max = max;
        this.speed = speed;
    }

    protected final void patrol(float dt) {
        bounds.x += facing * speed * dt;
        if (bounds.x + bounds.width >= max) {
            bounds.x = max - bounds.width;
            facing = -1;
        } else if (bounds.x <= min) {
            bounds.x = min;
            facing = 1;
        }
    }
}
