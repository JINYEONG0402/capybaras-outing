package io.jbnu.test;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import io.jbnu.test.audio.AudioManager;
import io.jbnu.test.render.GameAssets;
import io.jbnu.test.screen.EndingScreen;
import io.jbnu.test.screen.PlayScreen;
import io.jbnu.test.screen.ResultScreen;
import io.jbnu.test.screen.TitleScreen;
import io.jbnu.test.world.RunState;
import io.jbnu.test.world.Tuning;

/** 공용 자원을 만들고 화면(타이틀, 플레이, 결과) 사이를 전환합니다. */
public class Main extends Game {
    public static final int ROUND_COUNT = 3;

    /** 휴대폰처럼 키보드가 없는 기기에서 화면 버튼을 보여 줄지 정합니다. */
    public final boolean touchUi;
    /** 홈 화면 아래에 보여 줄 안내 문구입니다. 없으면 null입니다. */
    public final String homeTip;
    public GameAssets assets;
    public AudioManager audio;
    private Preferences save;
    private final OrthographicCamera rotateCamera = new OrthographicCamera();

    public Main() {
        this(false);
    }

    public Main(boolean touchUi) {
        this(touchUi, null);
    }

    public Main(boolean touchUi, String homeTip) {
        this.touchUi = touchUi;
        this.homeTip = homeTip;
    }

    @Override
    public void create() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true); // 안드로이드 뒤로 가기는 일시정지로 씁니다.
        rotateCamera.setToOrtho(false, Tuning.VIEW_W, Tuning.VIEW_H);
        assets = new GameAssets();
        audio = new AudioManager();
        save = Gdx.app.getPreferences("capybara-tangerine-trip");
        showTitle();
    }

    public void showTitle() {
        switchTo(new TitleScreen(this));
    }

    public void startRun(int round, boolean fullRun) {
        switchTo(new PlayScreen(this, round, fullRun));
    }

    /** 마지막 라운드 뒤의 가족 엔딩입니다. 끝나면 결과 화면으로 넘어갑니다. */
    public void showEnding(RunState run, boolean fullRun) {
        switchTo(new EndingScreen(this, run, fullRun));
    }

    public void showResult(RunState run, boolean fullRun) {
        boolean newBest = false;
        if (fullRun && run.score > bestScore()) {
            newBest = true;
            save.putInteger("bestScore", run.score);
            save.putString("bestRank", run.rank());
            save.flush();
        }
        switchTo(new ResultScreen(this, run, fullRun, newBest));
    }

    public int bestScore() {
        return save.getInteger("bestScore", 0);
    }

    public String bestRank() {
        return save.getString("bestRank", "-");
    }

    @Override
    public void render() {
        super.render();
        if (touchUi && Gdx.graphics.getHeight() > Gdx.graphics.getWidth()) drawRotateHint();
    }

    /** 휴대폰을 세로로 들면 화면이 너무 작아지므로 가로로 돌리도록 안내합니다. */
    private void drawRotateHint() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        assets.batch.setProjectionMatrix(rotateCamera.combined);
        assets.batch.begin();
        assets.rect(0, 0, Tuning.VIEW_W, Tuning.VIEW_H, 0, 0, 0, 0.9f);
        assets.textCenter("Please rotate your phone", Tuning.VIEW_W / 2, 300, 5f, Color.WHITE);
        assets.batch.end();
    }

    private void switchTo(Screen next) {
        Screen previous = getScreen();
        setScreen(next);
        if (previous != null) previous.dispose();
    }

    @Override
    public void dispose() {
        super.dispose();
        audio.dispose();
        assets.dispose();
    }
}
