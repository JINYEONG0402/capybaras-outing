package io.jbnu.test.world;

/** 구간을 왕복하며, 밟으면 친구가 되는 멧돼지입니다. */
public final class Boar extends PatrolEnemy {
    public Boar(float x, float y, float min, float max) {
        super(x, y, 42, 30, min, max, 75);
    }

    @Override
    public Kind kind() {
        return Kind.BOAR;
    }

    @Override
    public boolean stompable() {
        return true;
    }

    @Override
    protected void think(float dt, GameWorld world) {
        patrol(dt);
    }
}
