package io.jbnu.test.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.badlogic.gdx.math.Rectangle;
import io.jbnu.test.level.LevelParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * 봇이 실제 물리와 키 입력만으로 라운드를 끝까지 깨 봅니다. 레벨을 수정해 클리어가 불가능해지면 실패합니다.
 * 경로 검증이 목적이므로 적과 함정의 피해는 끕니다. 구덩이와 끓는 물에 빠지면 실패 처리합니다.
 */
public class LevelPlaythroughTest {
    private static final float DT = Tuning.FIXED_STEP;

    private final InputState in = new InputState();
    private GameWorld w;

    private void load(int round) throws IOException {
        String path = "../assets/levels/round" + round + ".json";
        String json = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        w = new GameWorld(LevelParser.parse(json), new RunState(), new WorldListener() { });
        w.hazardsDisabled = true;
    }

    private void tick() {
        w.step(DT, in);
        in.clearPressed();
        if (w.phase == GameWorld.Phase.DYING) {
            Rectangle p = w.player.bounds;
            fail("Bot fell at x=" + p.x + " y=" + p.y + " tide=" + w.tideY + " t=" + w.roundTime);
        }
    }

    private void stopKeys() {
        in.left = in.right = in.jumpHeld = false;
    }

    private void steerTo(float x) {
        float center = w.player.bounds.x + Player.W / 2;
        in.left = center > x + 5;
        in.right = center < x - 5;
    }

    /** 움직이는 발판에 착지할 때 쓰는 값으로, 착지 높이를 검사하지 않습니다. */
    private static final float ANY_HEIGHT = -1;

    /** 사람처럼 발판 끝까지 걸어간 다음, 점프해서 x 지점의 높이 y 발판에 착지합니다. */
    private void hop(float x, float y) {
        int guard = 0;
        // 낙하 중이면 먼저 땅에 닿을 때까지 기다립니다.
        while (w.player.ground == null) {
            tick();
            if (++guard > 1000) fail("never landed");
        }
        walkToTakeoff(x);
        // 반대 방향으로 달리던 관성이 멈춘 뒤에 뜁니다.
        float direction = Math.signum(x - (w.player.bounds.x + Player.W / 2));
        while (w.player.vx * direction < 0 && w.player.ground != null) tick();
        in.jumpPressed = true;
        in.jumpHeld = true;
        guard = 0;
        do {
            steerTo(x);
            tick();
            if (w.phase == GameWorld.Phase.CLEARED) return;
            if (++guard > 480) fail("hop timeout to " + x + "," + y + " at " + w.player.bounds);
        } while (w.player.ground == null || guard < 5);
        stopKeys();
        if (y != ANY_HEIGHT && Math.abs(w.player.bounds.y - y) > 25) {
            fail("hop to " + x + "," + y + " landed at " + w.player.bounds);
        }
    }

    /** 목표가 멀면 발판 위에서 도약 지점까지 먼저 걸어갑니다. 움직이는 발판도 위치를 다시 계산합니다. */
    private void walkToTakeoff(float x) {
        int guard = 0;
        while (guard++ < 1200) {
            Rectangle ground = w.player.ground;
            if (ground == null) break;
            float center = w.player.bounds.x + Player.W / 2;
            if (Math.abs(x - center) <= 110) break;
            float takeoff = x - Math.signum(x - center) * 110;
            float minX = ground.x + Player.W / 2 + 2;
            float maxX = ground.x + ground.width - Player.W / 2 - 2;
            takeoff = Math.max(minX, Math.min(maxX, takeoff));
            if (Math.abs(center - takeoff) <= 6) break;
            steerTo(takeoff);
            tick();
        }
        stopKeys();
    }

    /** 땅 위를 걸어서 이동합니다. 벽이나 구덩이를 만나면 점프합니다. */
    private void walk(float x) {
        int guard = 0;
        while (Math.abs(w.player.bounds.x + Player.W / 2 - x) > 8) {
            steerTo(x);
            boolean blocked = w.player.ground != null && Math.abs(w.player.vx) < 1 && guard > 15;
            if (blocked) {
                in.jumpPressed = true;
                in.jumpHeld = true;
            } else if (w.player.ground != null) {
                in.jumpHeld = false;
            }
            tick();
            if (w.phase == GameWorld.Phase.CLEARED) return;
            if (++guard > 6000) fail("walk timeout to " + x + " at " + w.player.bounds);
        }
        stopKeys();
        settle();
    }

    private void settle() {
        int guard = 0;
        while (w.player.ground == null && guard++ < 600) tick();
    }

    private void waitMover(int index, boolean vertical, float at, float tolerance) {
        Rectangle box = w.movers.get(index).bounds;
        int guard = 0;
        while (Math.abs((vertical ? box.y : box.x) - at) > tolerance) {
            tick();
            if (++guard > 6000) fail("mover " + index + " never reached " + at);
        }
    }

    private void wait(float seconds) {
        for (int i = 0; i < seconds / DT; i++) tick();
    }

    private void assertAllCollectedAndClear(float flagX) {
        if (w.tangerines.size > 0 && !w.flagUnlocked()) {
            StringBuilder left = new StringBuilder();
            for (Rectangle r : w.tangerines) left.append(" (").append(r.x).append(',').append(r.y).append(')');
            fail("uncollected:" + left);
        }
        walk(flagX);
        assertEquals("round time " + w.roundTime, GameWorld.Phase.CLEARED, w.phase);
    }

    @Test
    public void meadowRound3IsClearable() throws IOException {
        load(3);
        walk(40);
        walk(290);
        hop(340, 124);
        hop(415, 60);
        walk(560);
        waitMover(0, false, 590, 12);
        hop(650, ANY_HEIGHT);
        waitMover(0, false, 780, 10);
        hop(925, 60);
        hop(1000, 140);
        hop(1090, 60);
        walk(1280);
        hop(1500, 60);
        walk(1630);
        hop(1695, 130);
        hop(1850, 200);
        hop(1990, 270);
        // 오른쪽 아래 지면의 귤을 모은 뒤 엘리베이터로 다시 올라갑니다.
        walk(2085);
        walk(2570);
        walk(2190);
        waitMover(1, true, 62, 4);
        hop(2245, ANY_HEIGHT);
        waitMover(1, true, 240, 5);
        hop(2150, 294);
        waitMover(1, true, 280, 6);
        hop(2245, ANY_HEIGHT);
        waitMover(1, true, 296, 5);
        hop(2320, 344);
        assertAllCollectedAndClear(2430);
        System.out.println("Round 3 bot time: " + w.roundTime);
    }

    @Test
    public void sunsetRound1IsClearable() throws IOException {
        load(1);
        walk(40);
        walk(500);
        waitMover(0, true, 62, 6);
        hop(590, ANY_HEIGHT);
        waitMover(0, true, 225, 10);
        hop(760, 260);
        walk(805);
        wait(1.0f);
        walk(900);
        in.right = true;
        wait(1.0f);
        stopKeys();
        settle();
        walk(1000);
        walk(1160);
        hop(1250, 154);
        walk(1530);
        wait(0.1f);
        hop(1620, 60);
        walk(1730);
        hop(1800, 130);
        hop(1950, 154);
        assertAllCollectedAndClear(2050);
        System.out.println("Round 1 bot time: " + w.roundTime);
    }

    @Test
    public void ferryRound2IsClearable() throws IOException {
        load(2);
        walk(40);
        walk(500);
        waitMover(0, false, 525, 10);
        hop(590, ANY_HEIGHT);
        waitMover(0, false, 790, 10);
        hop(950, 60);
        hop(1040, 130);
        walk(1045);
        wait(0.5f);
        in.right = true;
        wait(0.6f);
        stopKeys();
        settle();
        walk(1520);
        waitMover(1, false, 1545, 8);
        hop(1610, ANY_HEIGHT);
        waitMover(1, false, 1830, 8);
        hop(1980, 60);
        walk(2260);
        hop(2330, 130);
        hop(2480, 154);
        // 가장 오른쪽 아래 귤을 먹고, 낮은 발판으로 되돌아옵니다.
        walk(2580);
        hop(2480, 154);
        hop(2650, 224);
        assertAllCollectedAndClear(2730);
        System.out.println("Round 2 bot time: " + w.roundTime);
    }

    @Test
    public void round4IsClearableBeforeTheTide() throws IOException {
        load(4);
        hop(260, 144);
        hop(500, 234);
        hop(770, 324);
        // 첫 번째 움직이는 발판: 오른쪽 끝(560)에 왔을 때 탑니다.
        // 발판 바로 밑에서 뛰면 머리를 박으므로, 옆에서 기다렸다가 뛰어오릅니다.
        walk(710);
        waitMover(0, false, 500, 12);
        hop(570, 412);
        waitMover(0, false, 395, 12);
        hop(225, 504);
        hop(430, 594);
        hop(610, 684);
        hop(830, 774);
        walk(895);
        walk(762);
        waitMover(1, true, 775, 6);
        hop(600, ANY_HEIGHT);
        waitMover(1, true, 930, 8);
        hop(385, 1034);
        hop(170, 1134);
        hop(390, 1224);
        hop(690, 1314);
        hop(415, 1409);
        hop(205, 1499);
        walk(280);
        waitMover(2, false, 345, 10);
        hop(405, ANY_HEIGHT);
        waitMover(2, false, 630, 10);
        hop(875, 1674);
        walk(815);
        hop(675, 1769);
        hop(485, 1864);
        hop(265, 1954);
        hop(535, 2044);
        hop(810, 2134);
        assertTrue("required tangerines, collected " + w.collected, w.flagUnlocked());
        assertAllCollectedAndClear(835);
        System.out.println("Round 4 bot time: " + w.roundTime + " tide " + w.tideY);
    }
}
