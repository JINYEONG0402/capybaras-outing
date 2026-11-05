package io.jbnu.test.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/** 객체 풀로 입자와 점수 팝업을 재사용해 프레임 중 메모리 할당을 줄입니다. */
public final class ParticleSystem {
    private static final class Particle implements Pool.Poolable {
        float x, y, vx, vy, life, maxLife, size, gravity;
        boolean puff;
        final Color color = new Color();

        @Override
        public void reset() {
            life = 0;
        }
    }

    private static final class Popup implements Pool.Poolable {
        float x, y, life, scale;
        String text;
        final Color color = new Color();

        @Override
        public void reset() {
            text = null;
        }
    }

    private final Array<Particle> particles = new Array<>(false, 256);
    private final Array<Popup> popups = new Array<>(false, 32);
    private final Pool<Particle> particlePool = new Pool<Particle>(256) {
        @Override
        protected Particle newObject() {
            return new Particle();
        }
    };
    private final Pool<Popup> popupPool = new Pool<Popup>(32) {
        @Override
        protected Popup newObject() {
            return new Popup();
        }
    };

    public void spawn(float x, float y, float vx, float vy, float life, float size, float gravity,
                      float r, float g, float b) {
        Particle p = particlePool.obtain();
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.life = p.maxLife = life;
        p.size = size;
        p.gravity = gravity;
        p.puff = false;
        p.color.set(r, g, b, 1);
        particles.add(p);
    }

    /** 달릴 때 발 뒤에 퐁 하고 피어오르는 구름 모양 먼지입니다. direction은 달리는 쪽입니다. */
    public void cloudPuff(float x, float y, int direction) {
        float[][] blobs = {{0, 0, 15}, {-direction * 9f, 4, 11}, {direction * 8f, 5, 10}};
        for (float[] blob : blobs) {
            spawn(x + blob[0], y + blob[1], -direction * MathUtils.random(25f, 45f), MathUtils.random(14f, 30f),
                0.42f, blob[2], 0, 1f, 1f, 1f);
            particles.peek().puff = true;
        }
    }

    public void burst(float x, float y, int count, float speed, float r, float g, float b) {
        for (int i = 0; i < count; i++) {
            float angle = MathUtils.random(MathUtils.PI2);
            float v = MathUtils.random(speed * 0.3f, speed);
            spawn(x, y, MathUtils.cos(angle) * v, MathUtils.sin(angle) * v,
                MathUtils.random(0.3f, 0.75f), MathUtils.random(3f, 6f), 520, r, g, b);
        }
    }

    public void popup(String text, float x, float y, float scale, Color color) {
        Popup popup = popupPool.obtain();
        popup.text = text;
        popup.x = x;
        popup.y = y;
        popup.life = 1;
        popup.scale = scale;
        popup.color.set(color);
        popups.add(popup);
    }

    public void update(float dt) {
        for (int i = particles.size - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                particles.removeIndex(i);
                particlePool.free(p);
                continue;
            }
            p.vy -= p.gravity * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }
        for (int i = popups.size - 1; i >= 0; i--) {
            Popup popup = popups.get(i);
            popup.life -= dt;
            popup.y += 60 * dt;
            if (popup.life <= 0) {
                popups.removeIndex(i);
                popupPool.free(popup);
            }
        }
    }

    public void render(GameAssets assets) {
        for (Particle p : particles) {
            float t = p.life / p.maxLife;
            if (p.puff) {
                // 처음엔 작았다가 부풀며 사라집니다.
                float puffSize = p.size * (1.45f - 0.75f * t);
                assets.batch.setColor(p.color.r, p.color.g, p.color.b, t * 0.9f);
                assets.batch.draw(assets.circle, p.x - puffSize / 2, p.y - puffSize / 2, puffSize, puffSize);
                continue;
            }
            assets.batch.setColor(p.color.r, p.color.g, p.color.b, t);
            float size = p.size * (0.5f + 0.5f * t);
            assets.batch.draw(assets.white, p.x - size / 2, p.y - size / 2, size, size);
        }
        assets.batch.setColor(Color.WHITE);
        for (Popup popup : popups) {
            popup.color.a = Math.min(1, popup.life * 2.5f);
            assets.textCenter(popup.text, popup.x, popup.y, popup.scale, popup.color);
        }
    }

    public void clear() {
        particlePool.freeAll(particles);
        particles.clear();
        popupPool.freeAll(popups);
        popups.clear();
    }
}
