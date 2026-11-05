package io.jbnu.test.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import io.jbnu.test.world.GameWorld;
import io.jbnu.test.world.Player;

/**
 * 찾은 자식들이 아빠가 지나간 길을 따라 통통 뛰어오게 합니다.
 * 플레이어의 경로를 원형 버퍼에 기록해, 각 자식이 일정 거리 뒤의 지점을 따라갑니다.
 * 게임 규칙에는 영향을 주지 않는 시각 효과입니다.
 */
public final class FamilyTrail {
    private static final int CAPACITY = 256;
    private static final float POINT_GAP = 3;
    private static final int SPACING_POINTS = 15; // 자식 사이 간격 45px

    private final float[] xs = new float[CAPACITY];
    private final float[] ys = new float[CAPACITY];
    private int head;

    /** 라운드 시작이나 부활 때 경로를 플레이어 바로 뒤로 다시 만듭니다. */
    public void reset(GameWorld world) {
        Player player = world.player;
        float x = player.bounds.x + Player.W / 2;
        float y = player.bounds.y;
        head = CAPACITY - 1;
        for (int i = 0; i < CAPACITY; i++) {
            xs[CAPACITY - 1 - i] = Math.max(16, x - i * POINT_GAP);
            ys[CAPACITY - 1 - i] = y;
        }
    }

    public void record(GameWorld world) {
        float x = world.player.bounds.x + Player.W / 2;
        float y = world.player.bounds.y;
        float dx = x - xs[head], dy = y - ys[head];
        if (dx * dx + dy * dy < POINT_GAP * POINT_GAP) return;
        head = (head + 1) % CAPACITY;
        xs[head] = x;
        ys[head] = y;
    }

    public void draw(GameAssets assets, GameWorld world, float time) {
        // 클리어 순간에 찾은 자식은 아직 깃발 앞에 있으므로 빼고 그립니다.
        int count = world.run.children - (world.phase == GameWorld.Phase.CLEARED ? 1 : 0);
        boolean moving = Math.abs(world.player.vx) > 20 || world.player.ground == null;
        Texture t = assets.baby;
        assets.batch.setColor(Color.WHITE);
        for (int i = 0; i < count; i++) {
            int index = (head - SPACING_POINTS * (i + 1) + CAPACITY * 2) % CAPACITY;
            float x = xs[index], y = ys[index];
            int next = (index + SPACING_POINTS / 2) % CAPACITY;
            boolean faceLeft = xs[next] < x - 0.5f;
            float hop = moving ? Math.abs(MathUtils.sin(time * 11 + i * 1.2f)) * 13
                : Math.abs(MathUtils.sin(time * 2.5f + i)) * 2;
            assets.batch.draw(t, x - 19, y + hop, 38, 34, 0, 0, t.getWidth(), t.getHeight(), faceLeft, false);
        }
    }
}
