package io.jbnu.test.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/** 상태 기계 AI: 맴돌기 → 조준(경고) → 급강하 → 복귀 순서로 움직이는 매입니다. */
public final class Hawk extends Enemy {
    public enum State { HOVER, AIM, DIVE, RETURN }

    private static final float DETECT_X = 300, DETECT_BELOW = 420, DETECT_ABOVE = 40;
    private static final float AIM_TIME = 0.75f, DIVE_SPEED = 400, RETURN_SPEED = 165, COOLDOWN = 1.3f;
    private static final float HOVER_RANGE = 70;

    public State state = State.HOVER;
    private final float homeX, homeY;
    private float stateTime = COOLDOWN, targetX, targetY, dirX, dirY;

    public Hawk(float x, float y) {
        super(x, y, 40, 26);
        homeX = x;
        homeY = y;
    }

    @Override
    public Kind kind() {
        return Kind.HAWK;
    }

    @Override
    public boolean stompable() {
        return true;
    }

    @Override
    protected void think(float dt, GameWorld world) {
        stateTime = Math.max(0, stateTime - dt);
        Rectangle player = world.player.bounds;
        switch (state) {
            case HOVER:
                hover(player);
                break;
            case AIM:
                // 조준 중에는 플레이어를 계속 따라 보며, 경고 후 위치를 고정합니다.
                targetX = player.x + player.width / 2;
                targetY = player.y + player.height / 2;
                facing = targetX >= bounds.x ? 1 : -1;
                if (stateTime <= 0) startDive();
                break;
            case DIVE:
                dive(dt, world);
                break;
            default:
                returnHome(dt);
                break;
        }
    }

    private void hover(Rectangle player) {
        float offset = MathUtils.sin(animTime * 1.3f) * HOVER_RANGE;
        facing = MathUtils.cos(animTime * 1.3f) >= 0 ? 1 : -1;
        bounds.x = homeX + offset;
        bounds.y = homeY + MathUtils.sin(animTime * 3f) * 8;
        float dx = Math.abs(player.x - bounds.x);
        float dy = bounds.y - player.y;
        if (stateTime <= 0 && dx < DETECT_X && dy < DETECT_BELOW && dy > DETECT_ABOVE) {
            state = State.AIM;
            stateTime = AIM_TIME;
        }
    }

    private void startDive() {
        float dx = targetX - (bounds.x + bounds.width / 2);
        float dy = targetY - (bounds.y + bounds.height / 2);
        float length = Math.max(1, (float) Math.sqrt(dx * dx + dy * dy));
        dirX = dx / length;
        dirY = dy / length;
        state = State.DIVE;
    }

    private void dive(float dt, GameWorld world) {
        bounds.x += dirX * DIVE_SPEED * dt;
        bounds.y += dirY * DIVE_SPEED * dt;
        boolean arrived = bounds.y + bounds.height / 2 <= targetY - 40;
        for (Rectangle solid : world.solids) {
            if (bounds.overlaps(solid)) arrived = true;
        }
        if (arrived) state = State.RETURN;
    }

    private void returnHome(float dt) {
        float dx = homeX - bounds.x;
        float dy = homeY - bounds.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float move = RETURN_SPEED * dt;
        if (length <= move) {
            bounds.setPosition(homeX, homeY);
            state = State.HOVER;
            stateTime = COOLDOWN;
            animTime = 0;
            return;
        }
        bounds.x += dx / length * move;
        bounds.y += dy / length * move;
        facing = dx >= 0 ? 1 : -1;
    }

    @Override
    void reset() {
        super.reset();
        state = State.HOVER;
        stateTime = COOLDOWN;
        animTime = 0;
    }
}
