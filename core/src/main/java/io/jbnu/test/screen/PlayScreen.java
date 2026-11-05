package io.jbnu.test.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.jbnu.test.Main;
import io.jbnu.test.audio.AudioManager;
import io.jbnu.test.level.LevelDef;
import io.jbnu.test.level.LevelParser;
import io.jbnu.test.render.FamilyTrail;
import io.jbnu.test.render.GameAssets;
import io.jbnu.test.render.GameCamera;
import io.jbnu.test.render.Hud;
import io.jbnu.test.render.ParallaxBackground;
import io.jbnu.test.render.ParticleSystem;
import io.jbnu.test.render.Theme;
import io.jbnu.test.render.WorldRenderer;
import io.jbnu.test.ui.TouchControls;
import io.jbnu.test.ui.UiButton;
import io.jbnu.test.ui.UiMouse;
import io.jbnu.test.world.GameWorld;
import io.jbnu.test.world.HotSpring;
import io.jbnu.test.world.InputState;
import io.jbnu.test.world.Player;
import io.jbnu.test.world.RunState;
import io.jbnu.test.world.Tuning;
import io.jbnu.test.world.WorldListener;

/** 라운드 진행을 담당합니다: 인트로 → 플레이 → (문 열림 연출) → 클리어 연출 → 다음 라운드. */
public final class PlayScreen extends ScreenAdapter {
    private enum Phase { INTRO, PLAY, CUTSCENE, CLEAR }

    private static final float INTRO_SECONDS = 2.4f;
    private static final float CLEAR_SECONDS = 3f;
    private static final float CLEAR_CAMERA_RISE = 360f;
    private static final float CUTSCENE_SECONDS = 1.4f;
    private static final Color GOLD = new Color(1, 0.84f, 0.25f, 1);

    private final Main game;
    private final GameAssets assets;
    private final AudioManager audio;
    private final RunState run;
    private final boolean fullRun;

    private final GameCamera gameCamera = new GameCamera();
    private final FitViewport viewport = new FitViewport(Tuning.VIEW_W, Tuning.VIEW_H, gameCamera.camera);
    private final OrthographicCamera hudCamera = new OrthographicCamera();
    private final ParticleSystem particles = new ParticleSystem();
    private final InputState input = new InputState();
    private final FamilyTrail trail = new FamilyTrail();
    private final UiMouse mouse = new UiMouse();
    private final TouchControls touch = new TouchControls();
    // 오른쪽 위 톱니바퀴와, 누르면 그 아래로 펼쳐지는 아이콘들
    private final UiButton gearButton = UiButton.icon(Tuning.VIEW_W - 44, Tuning.VIEW_H - 41, 38);
    private final UiButton soundButton = UiButton.icon(Tuning.VIEW_W - 44, Tuning.VIEW_H - 87, 38);
    private final UiButton pauseButton = UiButton.icon(Tuning.VIEW_W - 44, Tuning.VIEW_H - 129, 38);
    private final UiButton homeButton = UiButton.icon(Tuning.VIEW_W - 44, Tuning.VIEW_H - 171, 38);
    private boolean settingsOpen;
    private final Hud hud;
    private final WorldEvents events = new WorldEvents();

    private int round;
    private RunState roundStart;
    private GameWorld world;
    private Theme theme;
    private WorldRenderer renderer;
    private ParallaxBackground background;

    private Phase phase = Phase.INTRO;
    private boolean paused;
    private float phaseTime, accumulator, hitStop, time, ambientTimer, steamTimer, soakTextTimer, puffTimer;
    private String message;
    private float messageTimer;
    private int clearBonus;

    public PlayScreen(Main game, int startRound, boolean fullRun) {
        this.game = game;
        this.assets = game.assets;
        this.audio = game.audio;
        this.round = startRound;
        this.fullRun = fullRun;
        this.run = new RunState();
        this.run.children = startRound; // 연습으로 중간부터 시작해도 앞 라운드의 자식은 함께 있습니다.
        this.hud = new Hud(assets);
        hudCamera.setToOrtho(false, Tuning.VIEW_W, Tuning.VIEW_H);
    }

    @Override
    public void show() {
        loadRound();
    }

    private void loadRound() {
        LevelDef def = LevelParser.parse(Gdx.files.internal("levels/round" + (round + 1) + ".json").readString("UTF-8"));
        roundStart = run.copy();
        world = new GameWorld(def, run, events);
        theme = Theme.get(def.theme);
        renderer = new WorldRenderer(assets, theme);
        renderer.setTrail(trail);
        trail.reset(world);
        background = new ParallaxBackground(assets, theme);
        particles.clear();
        audio.playMusic(theme.music);
        audio.setDuck(1f);
        gameCamera.snapTo(world);
        setPhase(Phase.INTRO);
        paused = false;
        accumulator = 0;
        message = null;
    }

    private void restartRound() {
        run.set(roundStart);
        loadRound();
        setPhase(Phase.PLAY); // 재시작은 인트로를 건너뜁니다.
        gameCamera.snapTo(world);
    }

    private void setPhase(Phase next) {
        phase = next;
        phaseTime = 0;
    }

    @Override
    public void render(float delta) {
        mouse.update(viewport);
        if (handleSettingsClick() || handleGlobalKeys()) return; // 타이틀로 나가면 이 화면은 끝납니다.
        if (!paused) update(Math.min(delta, 0.05f));
        draw();
    }

    /** 톱니바퀴와 그 아래 아이콘의 클릭을 처리합니다. 화면을 떠나면 true를 돌려줍니다. */
    private boolean handleSettingsClick() {
        if (!mouse.clicked) return false;
        if (gearButton.clicked(mouse)) {
            settingsOpen = !settingsOpen;
            audio.play("land", 0.5f, 1.4f);
            return false;
        }
        if (!settingsOpen) return false;
        if (soundButton.clicked(mouse)) {
            audio.toggleMute();
            audio.play("land", 0.5f, 1.4f);
        } else if (pauseButton.clicked(mouse)) {
            setPaused(!paused);
            audio.play("land", 0.5f, 1.4f);
        } else if (homeButton.clicked(mouse)) {
            audio.play("land", 0.5f, 1.4f);
            game.showTitle();
            return true;
        }
        return false;
    }

    /** 키보드 단축키입니다. 화면을 떠나면 true를 돌려줍니다. */
    private boolean handleGlobalKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) audio.toggleMute();
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            setPaused(!paused);
        }
        if (paused && Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            game.showTitle();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && (paused || phase == Phase.PLAY)) restartRound();
        return false;
    }

    private void setPaused(boolean value) {
        paused = value;
        if (paused) audio.pauseMusic();
        else audio.resumeMusic();
    }

    @Override
    public void pause() {
        setPaused(true);
    }

    private void update(float delta) {
        time += delta;
        particles.update(delta);
        spawnAmbient(delta);
        messageTimer = Math.max(0, messageTimer - delta);
        phaseTime += delta;
        world.tickVisuals(delta);

        switch (phase) {
            case INTRO:
                updateIntro();
                break;
            case PLAY:
                updatePlay(delta);
                break;
            case CUTSCENE:
                updateCutscene(delta);
                break;
            default:
                updateClear(delta);
                break;
        }
    }

    /** 깃발에서 플레이어까지 카메라로 훑어 라운드의 목표를 먼저 보여 줍니다. */
    private void updateIntro() {
        float progress = MathUtils.clamp(phaseTime / INTRO_SECONDS, 0, 1);
        float eased = progress * progress * (3 - 2 * progress);
        Rectangle flag = world.flag;
        Rectangle p = world.player.bounds;
        float x = MathUtils.lerp(flag.x, p.x + Player.W / 2, eased);
        float y = MathUtils.lerp(flag.y + 60, p.y + Tuning.VIEW_H * 0.28f, eased);
        gameCamera.setPosition(x, y, world);
        boolean skip = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || mouse.clicked;
        if (progress >= 1 || skip) {
            gameCamera.snapTo(world);
            setPhase(Phase.PLAY);
        }
    }

    private void updatePlay(float delta) {
        readInput();
        if (hitStop > 0) {
            hitStop -= delta; // 타격 순간을 잠깐 멈춰 손맛을 줍니다.
        } else {
            accumulator += delta;
            // 고정 시간 간격으로 물리를 계산해 프레임 속도와 무관하게 같은 결과를 냅니다.
            while (accumulator >= Tuning.FIXED_STEP && phase == Phase.PLAY) {
                world.step(Tuning.FIXED_STEP, input);
                input.clearPressed();
                accumulator -= Tuning.FIXED_STEP;
                trail.record(world);
            }
        }
        spawnRunPuffs(delta);
        if (phase == Phase.PLAY || phase == Phase.CUTSCENE) gameCamera.update(delta, world);
    }

    /** 땅에서 달리는 동안 발 뒤에 구름 먼지를 일정 간격으로 남깁니다. */
    private void spawnRunPuffs(float delta) {
        puffTimer -= delta;
        Player player = world.player;
        if (puffTimer > 0 || player.ground == null || Math.abs(player.vx) < 150) return;
        if (world.phase != GameWorld.Phase.PLAYING) return;
        puffTimer = 0.15f;
        int direction = player.vx > 0 ? 1 : -1;
        particles.cloudPuff(player.bounds.x + Player.W / 2 - direction * 22, player.bounds.y + 6, direction);
    }

    private void readInput() {
        input.left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
        input.right = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);
        input.jumpHeld = Gdx.input.isKeyPressed(Input.Keys.SPACE);
        // 물리 스텝이 없는 프레임에도 입력이 사라지지 않도록 누적합니다.
        input.jumpPressed |= Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        input.zenPressed |= Gdx.input.isKeyJustPressed(Input.Keys.X)
            || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT);
        if (game.touchUi) touch.apply(viewport, input);
    }

    private void updateCutscene(float delta) {
        gameCamera.update(delta, world);
        if (phaseTime >= CUTSCENE_SECONDS) {
            gameCamera.clearFocus();
            setPhase(Phase.PLAY);
        }
    }

    private void updateClear(float delta) {
        float progress = MathUtils.clamp(phaseTime / CLEAR_SECONDS, 0, 1);
        float eased = progress * progress * (3 - 2 * progress);
        gameCamera.setRise(CLEAR_CAMERA_RISE * eased);
        gameCamera.update(delta, world);
        if (MathUtils.randomBoolean(0.35f)) {
            OrthographicCamera cam = gameCamera.camera;
            particles.spawn(cam.position.x + MathUtils.random(-480f, 480f), cam.position.y + 280, MathUtils.random(-40f, 40f),
                MathUtils.random(-160f, -60f), 2.5f, 6, 40, MathUtils.random(0.6f, 1f), MathUtils.random(0.4f, 1f), MathUtils.random(0.1f, 0.6f));
        }
        if (progress >= 1) {
            round++;
            if (round < Main.ROUND_COUNT) {
                loadRound();
            } else {
                game.showEnding(run, fullRun);
            }
        }
    }

    private void spawnAmbient(float delta) {
        ambientTimer -= delta;
        OrthographicCamera cam = gameCamera.camera;
        float cx = cam.position.x, cy = cam.position.y;
        if (ambientTimer <= 0) {
            ambientTimer = 0.14f;
            float x = cx + MathUtils.random(-500f, 500f);
            switch (theme.ambient) {
                case PETALS:
                    particles.spawn(x, cy + 280, MathUtils.random(-30f, 10f), MathUtils.random(-55f, -30f), 6, 4, 0, 1f, 0.8f, 0.86f);
                    break;
                case LEAVES:
                    particles.spawn(x, cy + 280, MathUtils.random(-70f, -30f), MathUtils.random(-70f, -40f), 6, 5, 0, 0.9f, 0.5f, 0.2f);
                    break;
                case FIREFLIES:
                    particles.spawn(x, cy + MathUtils.random(-250f, 100f), MathUtils.random(-15f, 15f), MathUtils.random(-8f, 18f), 3.5f, 4, 0, 0.8f, 1f, 0.45f);
                    break;
                default:
                    particles.spawn(x, cy - 280, MathUtils.random(-25f, 25f), MathUtils.random(70f, 130f), 5, 4, 0, 1f, 0.5f, 0.15f);
                    break;
            }
        }
        soakTextTimer -= delta;
        if (world.soaking && soakTextTimer <= 0) {
            soakTextTimer = 0.8f;
            Rectangle p = world.player.bounds;
            particles.popup("z z z", p.x + 44, p.y + 40, 1.1f, Color.WHITE);
        }
        steamTimer -= delta;
        if (steamTimer <= 0) {
            steamTimer = 0.12f;
            for (HotSpring spring : world.springs) {
                Rectangle b = spring.bounds;
                if (Math.abs(b.x - cx) < 560) {
                    particles.spawn(b.x + MathUtils.random(0f, b.width), b.y + b.height, MathUtils.random(-6f, 6f), MathUtils.random(25f, 50f), 1.4f, 7, 0, 0.95f, 0.97f, 1f);
                }
            }
            if (world.def.hasTide) {
                particles.spawn(cx + MathUtils.random(-480f, 480f), world.tideY, 0, MathUtils.random(40f, 110f), 0.7f, 5, 0, 1f, 0.75f, 0.3f);
            }
        }
    }

    private void draw() {
        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        assets.batch.setProjectionMatrix(hudCamera.combined);
        assets.batch.begin();
        background.render(gameCamera.x(), gameCamera.y(), time);
        assets.batch.setProjectionMatrix(gameCamera.camera.combined);
        renderer.render(world, gameCamera.camera, time);
        particles.render(assets);
        renderer.renderTide(world, gameCamera.camera, time);
        assets.batch.setProjectionMatrix(hudCamera.combined);
        drawOverlays();
        assets.batch.end();
    }

    private void drawOverlays() {
        if (world.zenActive()) {
            float end = world.zenTimer < 1 ? 0.5f + 0.5f * MathUtils.sin(time * 18) : 1;
            assets.rect(0, 0, Tuning.VIEW_W, Tuning.VIEW_H, 0.35f, 0.9f, 0.65f, 0.12f * end);
        }
        hud.render(world, round, Main.ROUND_COUNT, gameCamera.camera, time, message, messageTimer * 2);
        float cx = Tuning.VIEW_W / 2;
        if (phase == Phase.INTRO) {
            assets.rect(0, 200, Tuning.VIEW_W, 130, 0, 0, 0, 0.4f);
            assets.textCenter("ROUND " + (round + 1), cx, 305, 3.6f, Color.WHITE);
            assets.textCenter(world.def.name + "  -  Find kid #" + (round + 1), cx, 240, 1.7f, GOLD);
        } else if (phase == Phase.CLEAR) {
            assets.rect(0, 190, Tuning.VIEW_W, 150, 0, 0, 0, 0.45f);
            assets.textCenter("YOU FOUND YOUR KID!", cx, 315, 3f, GOLD);
            int s = (int) world.roundTime;
            assets.textCenter(String.format("Time %d:%02d    Bonus +%d", s / 60, s % 60, clearBonus), cx, 245, 1.5f, Color.WHITE);
        }
        if (game.touchUi && phase == Phase.PLAY && !paused) touch.draw(assets, world.run.zen >= 1 && !world.zenActive());
        if (paused) drawPause(cx);
        drawSettings();
    }

    private void drawPause(float cx) {
        assets.rect(0, 0, Tuning.VIEW_W, Tuning.VIEW_H, 0, 0, 0, 0.78f);
        assets.batch.setColor(Color.WHITE);
        assets.batch.draw(assets.pause, cx - 60, 320, 120, 120);
        assets.textCenter("PAUSED", cx, 300, 2.6f, Color.WHITE);
        if (game.touchUi) {
            assets.textCenter("Use the icons at the top right", cx, 235, 1.4f, Color.WHITE);
            return;
        }
        assets.textCenter("P / ESC : Resume", cx, 235, 1.4f, Color.WHITE);
        assets.textCenter("R : Restart round", cx, 205, 1.4f, Color.WHITE);
        assets.textCenter("Q : Home", cx, 175, 1.4f, Color.WHITE);
        assets.textCenter("M : Mute / unmute", cx, 145, 1.4f, Color.WHITE);
    }

    private void drawSettings() {
        gearButton.drawIcon(assets, mouse, assets.iconGear);
        if (!settingsOpen) return;
        boolean muted = audio.isMuted();
        soundButton.drawIcon(assets, mouse, muted ? assets.iconSoundOff : assets.iconSoundOn);
        pauseButton.drawIcon(assets, mouse, paused ? assets.iconPlay : assets.iconPause);
        homeButton.drawIcon(assets, mouse, assets.iconHome);
        // 마우스를 올린 아이콘의 뜻을 왼쪽에 글자로 보여 줍니다.
        if (soundButton.hovered(mouse)) drawTooltip(soundButton, muted ? "Sound on" : "Mute");
        if (pauseButton.hovered(mouse)) drawTooltip(pauseButton, paused ? "Resume" : "Pause");
        if (homeButton.hovered(mouse)) drawTooltip(homeButton, "Home");
    }

    private void drawTooltip(UiButton button, String text) {
        Rectangle b = button.bounds;
        assets.text(text, b.x - 10, b.y + b.height / 2 + 8, 1.2f, Color.WHITE, Align.right);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    private void showMessage(String text, float seconds) {
        message = text;
        messageTimer = seconds;
    }

    /** 게임 로직의 사건을 소리, 입자, 카메라 효과로 바꿉니다. */
    private final class WorldEvents implements WorldListener {
        @Override
        public void onJump() {
            audio.play("jump", 0.4f, MathUtils.random(0.96f, 1.06f));
            Rectangle p = world.player.bounds;
            dust(p.x + Player.W / 2, p.y, 5);
        }

        @Override
        public void onLand(float x, float y, float impact) {
            audio.play("land", 0.35f, 1f);
            dust(x, y, impact > 600 ? 9 : 4);
        }

        private void dust(float x, float y, int count) {
            for (int i = 0; i < count; i++) {
                particles.spawn(x, y + 2, MathUtils.random(-90f, 90f), MathUtils.random(10f, 70f), 0.35f, 5, 120, 0.85f, 0.8f, 0.7f);
            }
        }

        @Override
        public void onTangerine(float x, float y, int combo, int points) {
            // 콤보가 오를수록 효과음이 높아져 계속 모으고 싶게 만듭니다.
            audio.play("collect", 0.55f, Math.min(2f, 1f + (combo - 1) * 0.09f));
            particles.burst(x, y, 10 + combo * 2, 190, 1f, 0.6f, 0.1f);
            particles.popup("+" + points, x, y + 18, 1.1f + Math.min(combo, 9) * 0.06f, GOLD);
        }

        @Override
        public void onGolden(float x, float y, int points) {
            audio.play("golden");
            particles.burst(x, y, 40, 300, 1f, 0.9f, 0.25f);
            particles.popup("GOLDEN YUZU! +" + points + "  ZEN FULL", x, y + 26, 1.4f, GOLD);
            gameCamera.shake(4, 0.25f);
        }

        @Override
        public void onStomp(float x, float y, int combo, int points) {
            audio.play("stomp", 0.75f, Math.min(1.6f, 1f + (combo - 1) * 0.06f));
            particles.burst(x, y, 16, 240, 1f, 0.85f, 0.5f);
            particles.popup("FRIEND! +" + points, x, y + 26, 1.25f, Color.WHITE);
            gameCamera.shake(5, 0.15f);
            hitStop = 0.05f;
        }

        @Override
        public void onStun(float x, float y, int points) {
            audio.play("stomp", 0.8f, 0.7f);
            particles.burst(x, y, 14, 220, 1f, 0.84f, 0.25f);
            particles.popup("BONK! +" + points, x, y + 26, 1.25f, Color.WHITE);
            gameCamera.shake(6, 0.18f);
            hitStop = 0.06f;
        }

        @Override
        public void onFriendLost(float x, float y) {
            audio.play("hurt");
            particles.burst(x, y, 18, 260, 0.75f, 0.5f, 0.3f);
            particles.popup("Your friend took the hit!", x, y + 34, 1.1f, Color.WHITE);
            gameCamera.shake(8, 0.25f);
            hitStop = 0.08f;
        }

        @Override
        public void onDeath(float x, float y) {
            audio.play("death");
            particles.burst(x, y, 26, 300, 0.85f, 0.55f, 0.3f);
            gameCamera.shake(12, 0.35f);
            hitStop = 0.1f;
        }

        @Override
        public void onRespawn() {
            gameCamera.clearFocus();
            trail.reset(world);
        }

        @Override
        public void onSwitch() {
            audio.play("switch");
            showMessage("A door has opened!", 2f);
            // 문이 열리는 모습을 카메라로 보여 줍니다.
            Rectangle door = world.door;
            gameCamera.setFocus(door.x, door.y + 200);
            gameCamera.shake(4, 0.8f);
            setPhase(Phase.CUTSCENE);
        }

        @Override
        public void onCheckpoint(float x, float y) {
            audio.play("checkpoint");
            particles.burst(x, y + 14, 22, 200, 0.5f, 0.9f, 0.95f);
            particles.popup("CHECKPOINT - stay still to soak", x, y + 110, 1.1f, Color.WHITE);
        }

        @Override
        public void onZenStart() {
            audio.play("zen");
            audio.setDuck(0.4f);
            gameCamera.setZoomTarget(0.88f);
            Rectangle p = world.player.bounds;
            particles.burst(p.x + Player.W / 2, p.y + Player.H / 2, 34, 340, 0.6f, 1f, 0.7f);
            showMessage("ZEN MODE", 1.2f);
        }

        @Override
        public void onZenEnd() {
            audio.setDuck(1f);
            gameCamera.setZoomTarget(1f);
        }

        @Override
        public void onFlagLocked() {
            audio.play("locked");
            showMessage("Locked! " + Math.max(0, world.required - world.collected) + " more tangerines needed", 2f);
        }

        @Override
        public void onCrumble(float x, float y) {
            audio.play("crumble", 0.5f, 1f);
            particles.burst(x, y, 12, 130, 0.6f, 0.5f, 0.42f);
        }

        @Override
        public void onClear(int bonus) {
            clearBonus = bonus;
            audio.stopMusic();
            audio.play("clear");
            gameCamera.setZoomTarget(1f);
            Rectangle flag = world.flag;
            particles.burst(flag.x + 20, flag.y + 60, 60, 420, 1f, 0.8f, 0.2f);
            setPhase(Phase.CLEAR);
        }
    }
}
