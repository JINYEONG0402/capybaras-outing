package io.jbnu.test.world;

/** 걷다가 가시를 세우며 멈추는 고슴도치입니다. 밟을 수 없어 뛰어넘어야 합니다. */
public final class Hedgehog extends PatrolEnemy {
    private static final float WALK_TIME = 2.2f, CURL_TIME = 1.0f;

    private float cycle;

    public Hedgehog(float x, float y, float min, float max) {
        super(x, y, 38, 28, min, max, 48);
    }

    public boolean curled() {
        return cycle > WALK_TIME;
    }

    @Override
    public Kind kind() {
        return Kind.HEDGEHOG;
    }

    @Override
    public boolean stompable() {
        return false;
    }

    @Override
    protected void think(float dt, GameWorld world) {
        cycle = (cycle + dt) % (WALK_TIME + CURL_TIME);
        if (!curled()) patrol(dt);
    }

    @Override
    void reset() {
        super.reset();
        cycle = 0;
    }
}
