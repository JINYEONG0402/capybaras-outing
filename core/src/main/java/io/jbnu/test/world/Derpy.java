package io.jbnu.test.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * 플레이어를 끝까지 쫓아오지만 아주 멍청한 몬스터입니다. 상태 기계로 행동합니다.
 * 잘 미끄러져 지나치고, 이유 없이 뛰고, 벽에 머리를 박고, 쫓던 대상을 잊고, 구덩이에 스스로 빠집니다.
 * 친구가 되지 않으며, 밟으면 잠시 어지러워할 뿐입니다.
 */
public final class Derpy extends Enemy {
    public enum State { ASLEEP, CHASE, CONFUSED, DIZZY, DANCE }

    private static final float WAKE_TIME = 3f, DIZZY_TIME = 2.6f, BONK_TIME = 1.1f, CONFUSED_TIME = 0.9f, DANCE_TIME = 1.5f;
    private static final float MAX_SPEED = 170, ACCEL = 260, FRICTION = 500;
    private static final float HOP_SPEED = 560;
    private static final float FAR_X = 700, FAR_Y = 350, FAR_PATIENCE = 3f;
    private static final float DROP_DISTANCE = 420, DROP_HEIGHT = 480;

    public State state = State.ASLEEP;
    /** 떨어지는 중인지 표시합니다. 비명을 보여 줄 때 씁니다. */
    public boolean falling;
    private float vx, vy, stateTime = WAKE_TIME, hopTimer = 2f, confuseTimer = 4f, farTimer;
    private boolean grounded;

    public Derpy(float x, float y) {
        super(x, y, 48, 44);
    }

    @Override
    public Kind kind() {
        return Kind.DERPY;
    }

    @Override
    public boolean stompable() {
        return true;
    }

    @Override
    public boolean becomesFriend() {
        return false;
    }

    @Override
    public boolean harmless() {
        return state == State.ASLEEP || state == State.DIZZY || state == State.DANCE;
    }

    @Override
    void stun() {
        enter(State.DIZZY, DIZZY_TIME);
    }

    /** 플레이어를 맞히면 기뻐서 춤추느라 잠시 멈춥니다. */
    @Override
    void onHitPlayer() {
        enter(State.DANCE, DANCE_TIME);
    }

    private void enter(State next, float seconds) {
        state = next;
        stateTime = seconds;
    }

    @Override
    protected void think(float dt, GameWorld world) {
        stateTime -= dt;
        Rectangle player = world.player.bounds;
        switch (state) {
            case CHASE:
                chase(dt, world, player);
                break;
            case ASLEEP:
                if (stateTime <= 0) enter(State.CHASE, 0);
                break;
            default:
                // 어지럽거나 춤추거나 헷갈리는 동안에는 미끄러지다 멈춥니다.
                vx = approach(vx, 0, FRICTION * dt);
                if (stateTime <= 0) enter(State.CHASE, 0);
                break;
        }
        applyPhysics(dt, world);
        if (bounds.y < -120) dropNearPlayer(world);
    }

    private void chase(float dt, GameWorld world, Rectangle player) {
        float dx = player.x - bounds.x;
        // 가속이 낮아 방향을 바꾸는 데 오래 걸리고, 플레이어를 지나쳐 미끄러집니다.
        vx = approach(vx, Math.signum(dx) * MAX_SPEED, ACCEL * dt);
        if (Math.abs(vx) > 10) facing = vx > 0 ? 1 : -1;

        hopTimer -= dt;
        if (hopTimer <= 0 && grounded) {
            vy = HOP_SPEED; // 아무 이유 없이 뜁니다.
            grounded = false;
            hopTimer = MathUtils.random(2f, 4.5f);
        }
        confuseTimer -= dt;
        if (confuseTimer <= 0) {
            confuseTimer = MathUtils.random(3.5f, 7f);
            facing = -facing; // 뭘 쫓고 있었는지 잊습니다.
            enter(State.CONFUSED, CONFUSED_TIME);
        }
        // 멀어지면 포기하지 않고 하늘에서 떨어져 돌아옵니다.
        boolean far = Math.abs(dx) > FAR_X || Math.abs(player.y - bounds.y) > FAR_Y;
        farTimer = far ? farTimer + dt : 0;
        if (farTimer > FAR_PATIENCE) dropNearPlayer(world);
    }

    private void applyPhysics(float dt, GameWorld world) {
        vy = Math.max(vy - Tuning.GRAVITY * dt, -Tuning.MAX_FALL);
        bounds.x += vx * dt;
        for (Rectangle solid : world.solids) {
            if (!bounds.overlaps(solid)) continue;
            bounds.x = vx > 0 ? solid.x - bounds.width : solid.x + solid.width;
            hitWall();
        }
        bounds.x = MathUtils.clamp(bounds.x, 0, world.def.worldWidth - bounds.width);

        float dy = vy * dt;
        bounds.y += dy;
        grounded = false;
        for (Rectangle solid : world.solids) {
            if (!bounds.overlaps(solid)) continue;
            if (dy < 0) {
                bounds.y = solid.y + solid.height;
                grounded = true;
            } else {
                bounds.y = solid.y - bounds.height;
            }
            vy = 0;
        }
        falling = vy < -500;
    }

    /** 벽을 만나면 절반쯤은 넘어 보려 하고, 나머지는 그대로 머리를 박습니다. */
    private void hitWall() {
        vx = 0;
        if (state != State.CHASE || !grounded) return;
        if (MathUtils.randomBoolean(0.55f)) {
            vy = HOP_SPEED;
            grounded = false;
        } else {
            enter(State.DIZZY, BONK_TIME);
        }
    }

    private void dropNearPlayer(GameWorld world) {
        Rectangle player = world.player.bounds;
        float behind = player.x - world.player.facing * DROP_DISTANCE;
        float ahead = player.x + world.player.facing * DROP_DISTANCE;
        float limit = world.def.worldWidth - bounds.width;
        float x = MathUtils.clamp(behind, 0, limit);
        if (!hasGroundBelow(world, x, player.y + DROP_HEIGHT)) {
            float other = MathUtils.clamp(ahead, 0, limit);
            if (hasGroundBelow(world, other, player.y + DROP_HEIGHT)) x = other;
        }
        bounds.setPosition(x, player.y + DROP_HEIGHT);
        vx = vy = 0;
        farTimer = 0;
        enter(State.CHASE, 0);
    }

    private boolean hasGroundBelow(GameWorld world, float x, float y) {
        for (Rectangle solid : world.statics) {
            boolean under = x + bounds.width > solid.x && x < solid.x + solid.width;
            if (under && solid.y + solid.height <= y) return true;
        }
        return false;
    }

    private static float approach(float value, float target, float amount) {
        return value < target ? Math.min(value + amount, target) : Math.max(value - amount, target);
    }

    @Override
    void reset() {
        super.reset();
        vx = vy = 0;
        farTimer = 0;
        hopTimer = 2f;
        confuseTimer = 4f;
        enter(State.ASLEEP, WAKE_TIME);
    }
}
