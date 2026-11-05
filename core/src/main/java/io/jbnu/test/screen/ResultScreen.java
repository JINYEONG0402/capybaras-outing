package io.jbnu.test.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.jbnu.test.Main;
import io.jbnu.test.render.GameAssets;
import io.jbnu.test.render.ParallaxBackground;
import io.jbnu.test.render.ParticleSystem;
import io.jbnu.test.render.Theme;
import io.jbnu.test.ui.UiButton;
import io.jbnu.test.ui.UiMouse;
import io.jbnu.test.world.RunState;
import io.jbnu.test.world.Tuning;

/** 모든 라운드를 클리어했음을 알리고, 처음부터 다시 시작할 수 있게 합니다. */
public final class ResultScreen extends ScreenAdapter {
    private static final Color GOLD = new Color(1, 0.84f, 0.25f, 1);
    private static final Color BUTTON_MAIN = new Color(0.9f, 0.45f, 0.08f, 1);
    private static final Color BUTTON_SUB = new Color(0.25f, 0.5f, 0.42f, 1);

    private final Main game;
    private final GameAssets assets;
    private final RunState run;
    private final boolean fullRun, newBest;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(Tuning.VIEW_W, Tuning.VIEW_H, camera);
    private final ParallaxBackground background;
    private final ParticleSystem particles = new ParticleSystem();
    private final UiMouse mouse = new UiMouse();
    private final UiButton againButton = UiButton.text(250, 72, 270, 46, "PLAY AGAIN FROM ROUND 1", 1.2f);
    private final UiButton homeButton = UiButton.text(540, 72, 170, 46, "HOME", 1.4f);
    private float time, confetti;

    public ResultScreen(Main game, RunState run, boolean fullRun, boolean newBest) {
        this.game = game;
        this.assets = game.assets;
        this.run = run;
        this.fullRun = fullRun;
        this.newBest = newBest;
        camera.setToOrtho(false, Tuning.VIEW_W, Tuning.VIEW_H);
        background = new ParallaxBackground(assets, Theme.get("sunset"));
    }

    @Override
    public void show() {
        game.audio.playMusic("title");
    }

    @Override
    public void render(float delta) {
        time += delta;
        mouse.update(viewport);
        if (againButton.clicked(mouse) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.startRun(0, true);
            return;
        }
        if (homeButton.clicked(mouse) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showTitle();
            return;
        }
        confetti -= delta;
        if (confetti <= 0) {
            confetti = 0.04f;
            particles.spawn(MathUtils.random(0f, Tuning.VIEW_W), Tuning.VIEW_H + 10, MathUtils.random(-30f, 30f),
                MathUtils.random(-170f, -80f), 3.5f, 7, 20,
                MathUtils.random(0.5f, 1f), MathUtils.random(0.4f, 1f), MathUtils.random(0.1f, 1f));
        }
        particles.update(delta);

        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        assets.batch.setProjectionMatrix(camera.combined);
        assets.batch.begin();
        background.render(time * 15, Tuning.VIEW_H / 2, time);
        particles.render(assets);
        drawContent();
        assets.batch.end();
    }

    private void drawContent() {
        float cx = Tuning.VIEW_W / 2;
        assets.rect(cx - 330, 60, 660, 430, 0, 0, 0, 0.55f);
        assets.textCenter("ALL ROUNDS CLEAR!", cx, 462, 3.4f, GOLD);
        assets.textCenter("The capybara family is together again.", cx, 410, 1.3f, Color.WHITE);

        assets.textCenter(run.rank(), cx - 190, 310, 9f, GOLD);
        assets.textCenter("RANK", cx - 190, 195, 1.4f, Color.WHITE);

        int s = (int) run.time;
        float x = cx + 80, y = 340;
        row("Score", String.valueOf(run.score), x, y);
        row("Time", String.format("%d:%02d", s / 60, s % 60), x, y - 38);
        row("Falls", String.valueOf(run.deaths), x, y - 76);
        row("Kids found", run.children + " / " + Main.ROUND_COUNT, x, y - 114);
        row("Golden Yuzu", String.valueOf(run.goldens), x, y - 152);

        if (!fullRun) assets.textCenter("(Practice run - not saved)", cx, 146, 1.1f, Color.WHITE);
        else if (newBest) assets.textCenter("NEW BEST SCORE!", cx, 150, 1.7f, GOLD);

        againButton.draw(assets, mouse, BUTTON_MAIN);
        homeButton.draw(assets, mouse, BUTTON_SUB);
    }

    private void row(String label, String value, float x, float y) {
        assets.text(label, x - 110, y, 1.4f, Color.WHITE, com.badlogic.gdx.utils.Align.left);
        assets.text(value, x + 170, y, 1.4f, GOLD, com.badlogic.gdx.utils.Align.right);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
