package io.jbnu.test.world;

/** 게임 밸런스와 조작감을 결정하는 수치를 한곳에 모읍니다. */
public final class Tuning {
    public static final float VIEW_W = 960;
    public static final float VIEW_H = 540;
    public static final float FIXED_STEP = 1f / 120f;

    public static final float MOVE_SPEED = 260;
    public static final float GROUND_ACCEL = 2800;
    public static final float AIR_ACCEL = 2000;
    public static final float JUMP_SPEED = 650;
    public static final float GRAVITY = 1700;
    public static final float MAX_FALL = 900;
    public static final float JUMP_CUT = 0.5f;
    public static final float COYOTE_TIME = 0.09f;
    public static final float JUMP_BUFFER = 0.12f;

    public static final float STOMP_BOUNCE = 470;
    public static final float STOMP_BOUNCE_HELD = 640;
    public static final float INVULN_TIME = 1.6f;
    public static final float DEATH_TIME = 0.8f;
    public static final int START_LIVES = 3;

    public static final float COMBO_WINDOW = 2.6f;
    public static final int COMBO_CAP = 9;
    public static final int POINTS_TANGERINE = 100;
    public static final int POINTS_STOMP = 300;
    public static final int POINTS_GOLDEN = 1000;
    public static final int POINTS_CLEAR = 1000;
    public static final int POINTS_LIFE_BONUS = 200;

    public static final float ZEN_DURATION = 4.5f;
    public static final float ZEN_WORLD_SCALE = 0.35f;
    public static final float ZEN_PER_TANGERINE = 0.08f;
    public static final float ZEN_PER_STOMP = 0.15f;
    public static final float ZEN_PER_CHECKPOINT = 0.25f;
    public static final float SOAK_DELAY = 0.5f;
    public static final float SOAK_RATE = 0.16f;

    public static final float CRUMBLE_SHAKE = 0.6f;
    public static final float CRUMBLE_RESPAWN = 2.8f;
    public static final float TIDE_RESPAWN_GAP = 280;

    private Tuning() {
    }
}
