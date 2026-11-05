package io.jbnu.test.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.jbnu.test.Main;
import io.jbnu.test.level.LevelDef;
import io.jbnu.test.render.GameAssets;
import io.jbnu.test.render.ParallaxBackground;
import io.jbnu.test.render.ParticleSystem;
import io.jbnu.test.render.Theme;
import io.jbnu.test.render.WorldRenderer;
import io.jbnu.test.world.GameWorld;
import io.jbnu.test.world.RunState;
import io.jbnu.test.world.Tuning;
import io.jbnu.test.world.WorldListener;

/**
 * 엔딩: 1라운드의 낮 초원을 아빠가 걸어가고 아기들이 뒤에서 통통 따라가다가,
 * 화면이 원형으로 닫히며 THE END가 나옵니다.
 * 그 뒤 결과 화면으로 넘어갑니다. 클릭이나 Enter로 건너뛸 수 있습니다.
 */
public final class EndingScreen extends ScreenAdapter {
    private static final float HAPPY_SECONDS = 7f, CLOSE_SECONDS = 2.2f, END_SECONDS = 2.6f;
    private static final float CAMERA_ZOOM = 0.62f;
    private static final float CAMERA_X = 470;
    private static final float FAMILY_Y = 90; // 원이 닫히는 중심 높이
    private static final float GROUND_Y = 60;
    private static final float DAD_START_X = 130, WALK_SPEED = 62;
    private static final float KID_SPACING = 48;
    private static final int KID_COUNT = 3;
    private static final float START_RADIUS = 620;
    private static final float SPRING_X = 195;
    private static final Color GOLD = new Color(1, 0.84f, 0.25f, 1);
    private static final Color INK = new Color(0.1f, 0.2f, 0.28f, 1);

    private final Main game;
    private final GameAssets assets;
    private final RunState run;
    private final boolean fullRun;
    private final OrthographicCamera worldCamera = new OrthographicCamera();
    private final OrthographicCamera hudCamera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(Tuning.VIEW_W, Tuning.VIEW_H, worldCamera);
    private final ParallaxBackground background;
    private final WorldRenderer renderer;
    private final GameWorld scene;
    private final ParticleSystem particles = new ParticleSystem();
    private final Array<float[]> hearts = new Array<>(); // x, y, 수명
    private final Color fade = new Color();
    private float time, heartTimer, steamTimer, puffTimer;
    private boolean finished;

    public EndingScreen(Main game, RunState run, boolean fullRun) {
        this.game = game;
        this.assets = game.assets;
        this.run = run;
        this.fullRun = fullRun;
        Theme theme = Theme.get("meadow");
        background = new ParallaxBackground(assets, theme);
        renderer = new WorldRenderer(assets, theme);
        scene = new GameWorld(buildScene(), new RunState(), new WorldListener() { });
        hudCamera.setToOrtho(false, Tuning.VIEW_W, Tuning.VIEW_H);
        worldCamera.setToOrtho(false, Tuning.VIEW_W, Tuning.VIEW_H);
        worldCamera.zoom = CAMERA_ZOOM;
        worldCamera.position.set(CAMERA_X, Tuning.VIEW_H / 2 * CAMERA_ZOOM, 0);
        worldCamera.update();
    }

    /** 온천과 귤이 있는 초원 장면입니다. 깃발과 플레이어는 화면 밖에 둡니다. */
    private static LevelDef buildScene() {
        LevelDef def = new LevelDef();
        def.name = "Ending";
        def.theme = "meadow";
        def.worldWidth = Tuning.VIEW_W;
        def.worldHeight = Tuning.VIEW_H;
        def.start = new float[]{-500, GROUND_Y};
        def.flag = new float[]{5000, GROUND_Y};
        def.blocks.add(new float[]{0, 0, Tuning.VIEW_W, GROUND_Y});
        def.springs.add(new float[]{SPRING_X, GROUND_Y});
        return def;
    }

    /** 아빠의 현재 위치입니다. 아기들은 그 뒤를 일정 간격으로 따라갑니다. */
    private float dadX() {
        return DAD_START_X + WALK_SPEED * Math.min(time, HAPPY_SECONDS + CLOSE_SECONDS);
    }

    @Override
    public void show() {
        game.audio.stopMusic();
        game.audio.play("allclear");
        game.audio.playMusic("meadow");
    }

    @Override
    public void render(float delta) {
        time += delta;
        boolean skip = Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        if (!finished && (skip || time > HAPPY_SECONDS + CLOSE_SECONDS + END_SECONDS)) {
            finished = true;
            game.showResult(run, fullRun);
            return;
        }
        updateEffects(delta);

        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        assets.batch.setProjectionMatrix(hudCamera.combined);
        assets.batch.begin();
        background.render(CAMERA_X, Tuning.VIEW_H / 2, time);
        assets.batch.setProjectionMatrix(worldCamera.combined);
        renderer.render(scene, worldCamera, time);
        drawFamily();
        particles.render(assets);
        assets.batch.setProjectionMatrix(hudCamera.combined);
        drawOverlay();
        assets.batch.end();
    }

    private void updateEffects(float delta) {
        particles.update(delta);
        heartTimer -= delta;
        if (heartTimer <= 0) {
            heartTimer = 0.32f;
            float x = dadX() - MathUtils.random(-10f, KID_SPACING * KID_COUNT);
            hearts.add(new float[]{x, GROUND_Y + MathUtils.random(45f, 70f), 1.8f});
        }
        puffTimer -= delta;
        if (puffTimer <= 0 && time < HAPPY_SECONDS + CLOSE_SECONDS) {
            puffTimer = 0.22f;
            particles.cloudPuff(dadX() - 2, GROUND_Y + 6, 1);
        }
        for (int i = hearts.size - 1; i >= 0; i--) {
            float[] h = hearts.get(i);
            h[1] += 42 * delta;
            h[2] -= delta;
            if (h[2] <= 0) hearts.removeIndex(i);
        }
        steamTimer -= delta;
        if (steamTimer <= 0) {
            steamTimer = 0.14f;
            particles.spawn(MathUtils.random(SPRING_X, SPRING_X + 70), GROUND_Y + 16, MathUtils.random(-6f, 6f),
                MathUtils.random(25f, 50f), 1.4f, 7, 0, 0.95f, 0.97f, 1f);
        }
    }

    private void drawFamily() {
        // 아기들은 아빠 뒤에서 한 명씩 번갈아 통통 뛰며 따라갑니다.
        Texture baby = assets.baby;
        float dadX = dadX();
        assets.batch.setColor(Color.WHITE);
        for (int i = 0; i < KID_COUNT; i++) {
            float hop = Math.abs(MathUtils.sin(time * 7 + i * 1.1f)) * 22;
            float x = dadX - KID_SPACING * (i + 1) - 4;
            assets.batch.draw(baby, x, GROUND_Y + hop, 38, 34);
        }

        // 아빠는 앞장서서 뒤뚱뒤뚱 걸어갑니다.
        Texture dad = assets.capybara;
        float step = MathUtils.sin(time * 9);
        assets.batch.draw(dad, dadX, GROUND_Y + Math.abs(step) * 4, 20, 0, 40, 55, 1, 1, step * 4,
            0, 0, dad.getWidth(), dad.getHeight(), false, false);
        for (float[] h : hearts) {
            float alpha = Math.min(1, h[2]);
            float size = 15 + 3 * MathUtils.sin(time * 5 + h[0]);
            assets.batch.setColor(1f, 0.38f, 0.52f, alpha);
            assets.batch.draw(assets.heart, h[0] + MathUtils.sin(time * 3 + h[0]) * 6, h[1], size, size);
        }
        assets.batch.setColor(Color.WHITE);
    }

    private void drawOverlay() {
        float cx = Tuning.VIEW_W / 2;
        fade.set(GOLD);
        float textAlpha = MathUtils.clamp((time - 0.6f) * 1.5f, 0, 1);
        fade.a = textAlpha;
        assets.textCenter("TOGETHER AGAIN!", cx, 495, 3.4f, fade);
        fade.set(INK);
        fade.a = textAlpha;
        assets.textCenter("Dad found all three kids. Let's go home!", cx, 430, 1.4f, fade);

        float closing = MathUtils.clamp((time - HAPPY_SECONDS) / CLOSE_SECONDS, 0, 1);
        if (closing > 0) drawIris(closing);
        if (closing >= 1) {
            fade.set(Color.WHITE);
            fade.a = MathUtils.clamp((time - HAPPY_SECONDS - CLOSE_SECONDS) * 1.8f, 0, 1);
            assets.textCenter("THE END", cx, 285, 4f, fade);
        }
    }

    /** 가족을 중심으로 검은 원이 조여 드는 마무리 연출입니다. */
    private void drawIris(float progress) {
        float eased = progress * progress;
        float radius = START_RADIUS * (1 - eased);
        float w = Tuning.VIEW_W, h = Tuning.VIEW_H;
        if (radius < 2) {
            assets.rect(0, 0, w, h, 0, 0, 0, 1);
            return;
        }
        float familyX = dadX() - KID_SPACING; // 움직이는 가족의 가운데로 원이 좁혀집니다.
        float cx = (familyX - worldCamera.position.x) / CAMERA_ZOOM + w / 2;
        float cy = (FAMILY_Y - worldCamera.position.y) / CAMERA_ZOOM + h / 2;
        float half = radius * 256f / 250f; // 텍스처의 구멍 크기에 맞춘 배율
        assets.batch.setColor(Color.WHITE);
        assets.batch.draw(assets.iris, cx - half, cy - half, half * 2, half * 2);
        assets.rect(0, cy + half, w, Math.max(0, h - cy - half), 0, 0, 0, 1);
        assets.rect(0, 0, w, Math.max(0, cy - half), 0, 0, 0, 1);
        assets.rect(0, cy - half, Math.max(0, cx - half), half * 2, 0, 0, 0, 1);
        assets.rect(cx + half, cy - half, Math.max(0, w - cx - half), half * 2, 0, 0, 0, 1);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }
}
