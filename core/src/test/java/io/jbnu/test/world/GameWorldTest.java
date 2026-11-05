package io.jbnu.test.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import io.jbnu.test.level.LevelDef;
import io.jbnu.test.level.LevelParser;
import org.junit.Test;

/** 핵심 규칙이 의도대로 동작하는지 작은 레벨로 확인합니다. */
public class GameWorldTest {
    private static final String BASE = "\"name\":\"t\",\"theme\":\"meadow\",\"worldWidth\":2000,\"worldHeight\":600,"
        + "\"start\":[100,60],\"blocks\":[[0,0,2000,60]],\"flag\":[1800,60],";

    private final InputState in = new InputState();

    private GameWorld world(String extra) {
        LevelDef def = LevelParser.parse("{" + BASE + extra + "}");
        return new GameWorld(def, new RunState(), new WorldListener() { });
    }

    private void run(GameWorld w, float seconds) {
        for (int i = 0; i < seconds / Tuning.FIXED_STEP; i++) {
            w.step(Tuning.FIXED_STEP, in);
            in.clearPressed();
        }
    }

    private GameWorld withBoar() {
        return world("\"enemies\":[{\"type\":\"boar\",\"x\":100,\"y\":60,\"min\":90,\"max\":150}]");
    }

    @Test
    public void stompingBoarGivesLifeAndBounces() {
        GameWorld w = withBoar();
        w.player.place(105, 130);
        run(w, 0.3f);
        assertEquals(Tuning.START_LIVES + 1, w.lives);
        assertTrue(w.player.vy > 0);
        assertTrue(!w.enemies.first().alive);
    }

    @Test
    public void hitsCostLivesUntilGameOver() {
        GameWorld w = withBoar();
        for (int life = Tuning.START_LIVES - 1; life >= 0; life--) {
            w.player.invuln = 0;
            w.player.place(w.enemies.first().bounds.x, 60);
            w.step(Tuning.FIXED_STEP, in);
            assertEquals(GameWorld.Phase.DYING, w.phase);
            assertEquals(life, w.lives);
            run(w, Tuning.DEATH_TIME + 0.05f);
        }
        assertEquals(GameWorld.Phase.GAME_OVER, w.phase);
        assertEquals(Tuning.START_LIVES, w.run.deaths);
    }

    @Test
    public void stompedBoarStaysGoneAfterRespawn() {
        GameWorld w = withBoar();
        w.player.place(105, 130);
        run(w, 0.3f);
        w.player.place(1000, -200);
        run(w, Tuning.DEATH_TIME + 0.1f);
        assertEquals(GameWorld.Phase.PLAYING, w.phase);
        assertTrue(!w.enemies.first().alive);
    }

    @Test
    public void hedgehogCannotBeStomped() {
        GameWorld w = world("\"enemies\":[{\"type\":\"hedgehog\",\"x\":100,\"y\":60,\"min\":90,\"max\":150}]");
        w.player.place(105, 110);
        run(w, 0.2f);
        assertEquals(Tuning.START_LIVES - 1, w.lives);
        assertEquals(GameWorld.Phase.DYING, w.phase);
    }

    @Test
    public void zenSlowsWorldButNotPlayerAndProtects() {
        GameWorld w = world("\"movers\":[{\"x\":400,\"y\":200,\"w\":100,\"min\":0,\"max\":2000,\"speed\":100,\"vertical\":false}],"
            + "\"enemies\":[{\"type\":\"hedgehog\",\"x\":100,\"y\":60,\"min\":90,\"max\":150}]");
        w.run.zen = 1;
        in.zenPressed = true;
        run(w, 1f);
        assertTrue(w.zenActive());
        assertEquals(GameWorld.Phase.PLAYING, w.phase); // 고슴도치와 겹쳐도 다치지 않음
        assertEquals(400 + 100 * Tuning.ZEN_WORLD_SCALE, w.movers.first().bounds.x, 1f);
    }

    @Test
    public void flagNeedsAllTangerines() {
        GameWorld w = world("\"tangerines\":[[1000,60]]");
        w.player.place(1800, 60);
        run(w, 0.1f);
        assertEquals(GameWorld.Phase.PLAYING, w.phase);
        w.player.place(1000, 60);
        run(w, 0.05f);
        assertEquals(1, w.collected);
        w.player.place(1800, 60);
        run(w, 0.05f);
        assertEquals(GameWorld.Phase.CLEARED, w.phase);
        assertEquals(1, w.run.children); // 클리어하면 자식 한 명을 찾습니다.
    }

    @Test
    public void springSetsCheckpointAndSoakingChargesZen() {
        GameWorld w = world("\"springs\":[[500,60]]");
        w.player.place(520, 60);
        run(w, 3f);
        assertTrue(w.soaking);
        assertTrue(w.run.zen > Tuning.ZEN_PER_CHECKPOINT);
        w.player.place(1000, -200);
        run(w, Tuning.DEATH_TIME + 0.2f);
        Rectangle p = w.player.bounds;
        assertEquals(500 + (HotSpring.W - Player.W) / 2, p.x, 1f);
    }

    @Test
    public void crumbleFallsAndReturns() {
        GameWorld w = world("\"crumbles\":[{\"x\":500,\"y\":100,\"w\":100}]");
        w.player.place(520, 120);
        run(w, Tuning.CRUMBLE_SHAKE + 0.1f);
        assertEquals(CrumblePlatform.State.FALLEN, w.crumbles.first().state);
        w.player.place(100, 60);
        run(w, Tuning.CRUMBLE_RESPAWN + 0.1f);
        assertEquals(CrumblePlatform.State.IDLE, w.crumbles.first().state);
    }

    @Test
    public void tideKillsAndRecedesOnRespawn() {
        GameWorld w = world("\"tide\":{\"start\":200,\"speed\":10,\"accel\":0,\"max\":10}");
        run(w, 0.1f);
        assertEquals(GameWorld.Phase.DYING, w.phase);
        run(w, Tuning.DEATH_TIME + 0.1f);
        assertEquals(GameWorld.Phase.PLAYING, w.phase);
        assertTrue(w.tideY < w.player.bounds.y);
    }

    private GameWorld withDerpy() {
        return world("\"enemies\":[{\"type\":\"derpy\",\"x\":600,\"y\":60}]");
    }

    @Test
    public void derpySleepsThenChasesThePlayer() {
        GameWorld w = withDerpy();
        Derpy derpy = (Derpy) w.enemies.first();
        run(w, 1f);
        assertEquals(Derpy.State.ASLEEP, derpy.state);
        assertEquals(600, derpy.bounds.x, 0.01f);
        run(w, 4f);
        assertTrue("moved toward player: " + derpy.bounds.x, derpy.bounds.x < 450);
    }

    @Test
    public void stompingDerpyStunsHimAndGivesNoLife() {
        GameWorld w = withDerpy();
        Derpy derpy = (Derpy) w.enemies.first();
        derpy.state = Derpy.State.CHASE;
        w.player.place(605, 130);
        run(w, 0.3f);
        assertEquals(Derpy.State.DIZZY, derpy.state);
        assertTrue(derpy.alive);
        assertEquals(Tuning.START_LIVES, w.lives);
    }

    @Test
    public void derpyCatchingPlayerCostsLife() {
        GameWorld w = withDerpy();
        Derpy derpy = (Derpy) w.enemies.first();
        derpy.state = Derpy.State.CHASE;
        w.player.place(605, 60);
        run(w, 0.05f);
        assertEquals(Tuning.START_LIVES - 1, w.lives);
        assertEquals(Derpy.State.DANCE, derpy.state);
    }

    @Test
    public void derpyFallingInPitComesBack() {
        GameWorld w = withDerpy();
        Derpy derpy = (Derpy) w.enemies.first();
        derpy.state = Derpy.State.CHASE;
        derpy.bounds.setPosition(600, -119);
        run(w, 0.1f);
        assertTrue("dropped back from above: " + derpy.bounds.y, derpy.bounds.y > 300);
    }

    @Test
    public void standingUnderDescendingLiftPushesPlayerOut() {
        GameWorld w = world("\"movers\":[{\"x\":300,\"y\":120,\"w\":100,\"min\":60,\"max\":121,\"speed\":200,\"vertical\":true}]");
        w.player.place(340, 60);
        run(w, 1f);
        assertEquals(GameWorld.Phase.PLAYING, w.phase);
    }
}
