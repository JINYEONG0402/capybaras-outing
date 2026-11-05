package io.jbnu.test.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.jbnu.test.Main;
import io.jbnu.test.render.GameAssets;
import io.jbnu.test.render.ParallaxBackground;
import io.jbnu.test.render.Theme;
import io.jbnu.test.ui.UiButton;
import io.jbnu.test.ui.UiMouse;
import io.jbnu.test.world.Tuning;

/** 홈 화면입니다. 마우스로 게임을 시작하거나 게임 방법을 볼 수 있습니다. */
public final class TitleScreen extends ScreenAdapter {
    private static final Color INK = new Color(0.1f, 0.2f, 0.28f, 1);
    private static final Color ORANGE = new Color(1f, 0.5f, 0.05f, 1);
    private static final Color BUTTON_MAIN = new Color(0.9f, 0.45f, 0.08f, 1);
    private static final Color BUTTON_SUB = new Color(0.25f, 0.5f, 0.42f, 1);
    private static final Color GOLD = new Color(1, 0.84f, 0.25f, 1);

    private final Main game;
    private final GameAssets assets;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(Tuning.VIEW_W, Tuning.VIEW_H, camera);
    private final ParallaxBackground background;
    private final UiMouse mouse = new UiMouse();
    private final UiButton startButton = UiButton.text(465, 235, 300, 66, "GAME START", 2f);
    private final UiButton howToButton = UiButton.text(465, 150, 300, 58, "HOW TO PLAY", 1.7f);
    private final UiButton backButton = UiButton.text(390, 42, 180, 46, "BACK", 1.5f);
    private boolean showHelp;
    private float time;

    public TitleScreen(Main game) {
        this.game = game;
        this.assets = game.assets;
        camera.setToOrtho(false, Tuning.VIEW_W, Tuning.VIEW_H);
        background = new ParallaxBackground(assets, Theme.get("meadow"));
    }

    @Override
    public void show() {
        game.audio.playMusic("title");
    }

    @Override
    public void render(float delta) {
        time += delta;
        mouse.update(viewport);
        if (handleInput()) return;

        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        assets.batch.setProjectionMatrix(camera.combined);
        assets.batch.begin();
        background.render(time * 25, Tuning.VIEW_H / 2, time);
        if (showHelp) drawHelp();
        else drawHome();
        assets.batch.end();
    }

    /** 게임 화면으로 넘어가면 true를 돌려줍니다. */
    private boolean handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) game.audio.toggleMute();
        if (showHelp) {
            if (backButton.clicked(mouse) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                showHelp = false;
                click();
            }
            return false;
        }
        if (howToButton.clicked(mouse)) {
            showHelp = true;
            click();
            return false;
        }
        if (startButton.clicked(mouse) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            click();
            game.startRun(0, true);
            return true;
        }
        for (int i = 0; i < Main.ROUND_COUNT; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                game.startRun(i, false);
                return true;
            }
        }
        return false;
    }

    private void click() {
        game.audio.play("land", 0.5f, 1.4f);
    }

    private void drawHome() {
        float cx = startButton.bounds.x + startButton.bounds.width / 2;
        assets.rect(0, 0, Tuning.VIEW_W, 62, 0.38f, 0.72f, 0.2f, 1);
        assets.rect(0, 62, Tuning.VIEW_W, 6, 0.58f, 0.83f, 0.28f, 1);

        Texture capy = assets.capybara;
        float bob = Math.abs(MathUtils.sin(time * 3)) * 10;
        assets.batch.draw(capy, 135, 66 + bob, 150, 206);
        for (int i = 0; i < 3; i++) {
            float y = 300 + MathUtils.sin(time * 2 + i) * 8;
            assets.batch.draw(assets.tangerine, 95 + i * 80, y, 44, 44);
        }

        assets.textCenter("CAPYBARA'S", cx, 465, 4f, ORANGE);
        assets.textCenter("OUTING", cx, 395, 3.4f, INK);
        assets.textCenter("Dad is on his way to find his 3 kids!", cx, 340, 1.3f, INK);

        startButton.draw(assets, mouse, BUTTON_MAIN);
        howToButton.draw(assets, mouse, BUTTON_SUB);
        if (game.bestScore() > 0) {
            assets.textCenter("BEST  " + game.bestScore() + "  (rank " + game.bestRank() + ")", cx, 56, 1.2f, Color.WHITE);
        }
        if (game.homeTip != null) {
            assets.textCenter(game.homeTip, Tuning.VIEW_W / 2, 26, 1.15f, Color.WHITE);
        }
    }

    private void drawHelp() {
        assets.rect(36, 24, Tuning.VIEW_W - 72, Tuning.VIEW_H - 48, 0.05f, 0.09f, 0.12f, 0.93f);
        assets.textCenter("HOW TO PLAY", Tuning.VIEW_W / 2, 502, 2.5f, GOLD);
        assets.textCenter("Find your kid in each round: collect ALL tangerines, then touch the flag!", Tuning.VIEW_W / 2, 448, 1.35f, Color.WHITE);

        float left = 72, y = 402;
        assets.text("CONTROLS", left, y, 1.4f, GOLD, Align.left);
        String[] controls = game.touchUi ? new String[]{
            "Left / right buttons : Move",
            "Up button : Jump (hold = higher)",
            "Shield button : ZEN mode (when the gauge is full)",
            "Hold the phone sideways"
        } : new String[]{
            "Arrows / A D : Move",
            "Space : Jump (hold = higher)",
            "X : ZEN mode (when the gauge is full)",
            "R : Restart the round"
        };
        for (String line : controls) {
            y -= 30;
            assets.text(line, left, y, 1.15f, Color.WHITE, Align.left);
        }
        y -= 44;
        assets.batch.setColor(Color.WHITE);
        assets.batch.draw(assets.iconGear, left, y - 22, 28, 28);
        assets.text("Top right: settings", left + 38, y, 1.15f, Color.WHITE, Align.left);
        y -= 38;
        float iconX = left;
        Texture[] icons = {assets.iconSoundOn, assets.iconPause, assets.iconPlay, assets.iconHome};
        for (Texture icon : icons) {
            assets.batch.draw(icon, iconX, y - 22, 26, 26);
            iconX += 36;
        }
        assets.text("Mute, pause, resume, home", iconX + 4, y, 1.05f, Color.WHITE, Align.left);

        float right = 490;
        y = 402;
        assets.text("FRIENDS & FOES", right, y, 1.4f, GOLD, Align.left);
        y = helpRow(assets.tangerine, right, y - 54, 28, 28, "Eat in a row for a combo. Fills ZEN.");
        y = helpRow(assets.boar[0], right, y, 34, 24, "Boar: stomp it to make a friend.");
        assets.text("A friend blocks one hit. Fall and all are gone.", right + 44, y + 16, 1f, Color.LIGHT_GRAY, Align.left);
        y -= 28;
        y = helpRow(assets.hedgehog[0], right, y, 32, 24, "Hedgehog: spiky! Jump over it.");
        y = helpRow(assets.derpy[0], right, y, 30, 28, "Derpy: he chases you. Stomp = dizzy.");
        y = helpRow(assets.golden, right, y, 28, 28, "Golden yuzu: fills ZEN at once.");
        assets.text("Hot spring: respawn point. Stay still to charge ZEN.", right, y + 10, 1f, Color.WHITE, Align.left);
        assets.text("ZEN: the world slows and you can't be hurt.", right, y - 12, 1f, Color.WHITE, Align.left);

        backButton.draw(assets, mouse, BUTTON_SUB);
    }

    private float helpRow(Texture icon, float x, float y, float w, float h, String text) {
        assets.batch.setColor(Color.WHITE);
        assets.batch.draw(icon, x, y, w, h);
        assets.text(text, x + 44, y + 22, 1.1f, Color.WHITE, Align.left);
        return y - 34;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
