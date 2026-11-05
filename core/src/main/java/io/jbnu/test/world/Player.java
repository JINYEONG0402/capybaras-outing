package io.jbnu.test.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** 카피바라의 이동과 지형 충돌을 담당합니다. */
public final class Player {
    public static final float W = 32, H = 44;

    public final Rectangle bounds = new Rectangle(0, 0, W, H);
    public float vx, vy;
    public int facing = 1;
    /** 지금 밟고 있는 발판입니다. 공중이면 null입니다. */
    public Rectangle ground;
    public float invuln, landSquash, walkTime;

    private float coyote, jumpBuffer, knockTimer;
    private boolean jumping;

    void place(float x, float y) {
        bounds.setPosition(x, y);
        vx = vy = 0;
        ground = null;
        coyote = jumpBuffer = knockTimer = 0;
        jumping = false;
    }

    boolean grounded() {
        return ground != null;
    }

    void update(float dt, InputState in, Array<Rectangle> solids, float worldWidth, WorldListener events) {
        invuln = Math.max(0, invuln - dt);
        landSquash = Math.max(0, landSquash - dt * 5f);
        knockTimer = Math.max(0, knockTimer - dt);

        move(dt, in);
        jump(dt, in, events);

        vy = Math.max(vy - Tuning.GRAVITY * dt, -Tuning.MAX_FALL);
        moveX(dt, solids, worldWidth);
        float impact = -vy;
        boolean wasAirborne = coyote < Tuning.COYOTE_TIME;
        moveY(dt, solids);
        if (ground != null) {
            coyote = Tuning.COYOTE_TIME;
            jumping = false;
            if (wasAirborne && impact > 220) {
                landSquash = Math.min(1, impact / 800f);
                events.onLand(bounds.x + W / 2, bounds.y, impact);
            }
            if (Math.abs(vx) > 20) walkTime += dt;
        } else {
            coyote = Math.max(0, coyote - dt);
        }
    }

    private void move(float dt, InputState in) {
        if (knockTimer > 0) return; // 넉백 중에는 조작이 잠시 막힙니다.
        float wish = 0;
        if (in.left) wish -= Tuning.MOVE_SPEED;
        if (in.right) wish += Tuning.MOVE_SPEED;
        if (wish != 0) facing = wish > 0 ? 1 : -1;
        float accel = ground != null ? Tuning.GROUND_ACCEL : Tuning.AIR_ACCEL;
        float delta = wish - vx;
        vx += MathUtils.clamp(delta, -accel * dt, accel * dt);
    }

    private void jump(float dt, InputState in, WorldListener events) {
        // 코요테 타임과 입력 버퍼로 '눌렀는데 안 뛴' 느낌을 없앱니다.
        jumpBuffer = in.jumpPressed ? Tuning.JUMP_BUFFER : Math.max(0, jumpBuffer - dt);
        if (jumpBuffer > 0 && coyote > 0 && knockTimer <= 0) {
            vy = Tuning.JUMP_SPEED;
            jumpBuffer = 0;
            coyote = 0;
            ground = null;
            jumping = true;
            events.onJump();
        }
        // 버튼을 일찍 떼면 낮게 뜁니다.
        if (jumping && !in.jumpHeld && vy > 0) {
            vy *= Tuning.JUMP_CUT;
            jumping = false;
        }
    }

    private void moveX(float dt, Array<Rectangle> solids, float worldWidth) {
        bounds.x += vx * dt;
        for (Rectangle block : solids) {
            if (!bounds.overlaps(block)) continue;
            if (vx > 0) bounds.x = block.x - W;
            else if (vx < 0) bounds.x = block.x + block.width;
            vx = 0;
        }
        bounds.x = MathUtils.clamp(bounds.x, 0, worldWidth - W);
    }

    private void moveY(float dt, Array<Rectangle> solids) {
        float dy = vy * dt;
        bounds.y += dy;
        ground = null;
        for (Rectangle block : solids) {
            if (!bounds.overlaps(block)) continue;
            if (dy < 0) {
                bounds.y = block.y + block.height;
                ground = block;
            } else if (dy > 0) {
                bounds.y = block.y - H;
                jumping = false;
            }
            vy = 0;
        }
    }

    void stompBounce(boolean jumpHeld) {
        vy = jumpHeld ? Tuning.STOMP_BOUNCE_HELD : Tuning.STOMP_BOUNCE;
        jumping = false;
        coyote = 0;
        ground = null;
    }

    void knockback(int direction) {
        vx = direction * 260;
        vy = 340;
        ground = null;
        coyote = 0;
        jumping = false;
        knockTimer = Tuning.KNOCKBACK_TIME;
        invuln = Tuning.INVULN_TIME;
    }
}
