package io.jbnu.test.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import io.jbnu.test.level.LevelDef;

/**
 * 한 라운드의 규칙과 상태를 담는 순수 로직입니다.
 * 그래픽, 소리, 키보드에 의존하지 않으므로 창 없이도 실행하고 테스트할 수 있습니다.
 */
public final class GameWorld {
    public enum Phase { PLAYING, DYING, GAME_OVER, CLEARED }

    public final LevelDef def;
    public final RunState run;
    public final Player player = new Player();
    public final Array<Rectangle> solids = new Array<>();
    public final Array<Rectangle> statics = new Array<>();
    public final Array<MovingPlatform> movers = new Array<>();
    public final Array<CrumblePlatform> crumbles = new Array<>();
    public final Array<Trap> traps = new Array<>();
    public final Array<Enemy> enemies = new Array<>();
    public final Array<HotSpring> springs = new Array<>();
    public final Array<Rectangle> tangerines = new Array<>();
    public final Rectangle flag = new Rectangle();
    public Rectangle golden, switchPad, door;
    /** 이 라운드에서 찾을 자식입니다. 깃발 앞에 서 있습니다. */
    public Rectangle child;

    public Phase phase = Phase.PLAYING;
    public boolean switchOn, soaking;
    public int collected, required, combo;
    /** 이번 레벨에 남은 목숨입니다. 멧돼지를 밟으면 늘어납니다. */
    public int lives = Tuning.START_LIVES;
    public float comboTimer, zenTimer, doorAnim, roundTime, tideY, tideSpeed, deathTimer;
    public HotSpring lastSpring;

    /** 테스트용: 켜면 적과 함정이 플레이어를 해치지 않습니다. */
    public boolean hazardsDisabled;

    private final WorldListener events;
    private float checkpointX, checkpointY, soakDelay, lockedCooldown;

    public GameWorld(LevelDef def, RunState run, WorldListener events) {
        this.def = def;
        this.run = run;
        this.events = events;
        build();
    }

    private void build() {
        for (float[] b : def.blocks) {
            Rectangle block = new Rectangle(b[0], b[1], b[2], b[3]);
            solids.add(block);
            statics.add(block);
        }
        for (LevelDef.MoverDef m : def.movers) {
            MovingPlatform platform = new MovingPlatform(m.x, m.y, m.w, m.min, m.max, m.speed, m.vertical);
            movers.add(platform);
            solids.add(platform.bounds);
        }
        for (LevelDef.CrumbleDef c : def.crumbles) {
            CrumblePlatform platform = new CrumblePlatform(c.x, c.y, c.w);
            crumbles.add(platform);
            solids.add(platform.bounds);
        }
        for (LevelDef.TrapDef t : def.traps) {
            traps.add(new Trap(t.x, t.y, t.w, t.period, t.active, t.phase));
        }
        for (float[] s : def.springs) springs.add(new HotSpring(s[0], s[1]));
        for (LevelDef.EnemyDef e : def.enemies) enemies.add(createEnemy(e));

        flag.set(def.flag[0], def.flag[1], 20, 100);
        if (def.switchPad != null) switchPad = new Rectangle(def.switchPad[0], def.switchPad[1], 50, 12);
        if (def.door != null) {
            door = new Rectangle(def.door[0], def.door[1], 28, def.door[2]);
            solids.add(door);
        } else {
            switchOn = true;
        }
        if (def.golden != null) golden = new Rectangle(def.golden[0], def.golden[1], 30, 30);
        if (def.child != null) child = new Rectangle(def.child[0], def.child[1], 40, 36);

        if (def.autoTangerines) placeTangerines();
        for (float[] t : def.tangerines) tangerines.add(new Rectangle(t[0], t[1], 24, 24));
        required = def.required > 0 ? def.required : tangerines.size;

        if (def.hasTide) {
            tideY = def.tideStart;
            tideSpeed = def.tideSpeed;
        }
        checkpointX = def.start[0];
        checkpointY = def.start[1];
        player.place(checkpointX, checkpointY);
        // 시작 지점의 온천은 이미 도착한 곳으로 치고, 체크포인트 알림을 띄우지 않습니다.
        for (HotSpring spring : springs) {
            if (player.bounds.overlaps(spring.bounds)) {
                spring.activated = true;
                lastSpring = spring;
            }
        }
    }

    private static Enemy createEnemy(LevelDef.EnemyDef e) {
        if ("boar".equals(e.type)) return new Boar(e.x, e.y, e.min, e.max);
        if ("hedgehog".equals(e.type)) return new Hedgehog(e.x, e.y, e.min, e.max);
        if ("hawk".equals(e.type)) return new Hawk(e.x, e.y);
        if ("derpy".equals(e.type)) return new Derpy(e.x, e.y);
        throw new IllegalArgumentException("Unknown enemy type: " + e.type);
    }

    /** 고정 발판 위에 귤을 놓되, 캐릭터가 지나갈 공간이 없거나 위험한 곳은 제외합니다. */
    private void placeTangerines() {
        for (Rectangle block : statics) {
            for (float x = block.x + 28; x + 24 <= block.x + block.width - 12; x += 120) {
                Rectangle fruit = new Rectangle(x, block.y + block.height, 24, 24);
                if (!isObstructed(fruit, block)) tangerines.add(fruit);
            }
        }
    }

    private boolean isObstructed(Rectangle fruit, Rectangle block) {
        Rectangle space = new Rectangle(fruit.x + fruit.width / 2f - Player.W / 2f - 4, fruit.y,
            Player.W + 8, Player.H + 4);
        if (space.overlaps(flag)) return true;
        if (switchPad != null && fruit.overlaps(switchPad)) return true;
        for (Rectangle other : solids) {
            if (other == block) continue;
            if (space.overlaps(other)) return true;
            // 발판 아래에 가려진 하단 경로에는 놓지 않습니다.
            boolean overhead = other.y >= fruit.y && space.x < other.x + other.width
                && space.x + space.width > other.x;
            if (overhead) return true;
        }
        for (Trap trap : traps) {
            if (space.overlaps(trap.bounds)) return true;
        }
        for (HotSpring spring : springs) {
            if (space.overlaps(spring.bounds)) return true;
        }
        return false;
    }

    public boolean flagUnlocked() {
        return collected >= required;
    }

    public boolean zenActive() {
        return zenTimer > 0;
    }

    /** 고정 시간 간격으로 호출되는 한 번의 시뮬레이션입니다. */
    public void step(float dt, InputState in) {
        if (phase == Phase.CLEARED || phase == Phase.GAME_OVER) return;
        run.time += dt;
        roundTime += dt;
        if (phase == Phase.DYING) {
            deathTimer -= dt;
            if (deathTimer <= 0) {
                if (lives > 0) {
                    respawn();
                } else {
                    phase = Phase.GAME_OVER;
                    events.onGameOver();
                }
            }
            return;
        }
        updateZen(dt, in);
        // Zen 모드는 세상만 느려지고 플레이어의 속도는 그대로입니다.
        float worldDt = zenActive() ? dt * Tuning.ZEN_WORLD_SCALE : dt;

        updateMovers(worldDt);
        if (phase != Phase.PLAYING) return;
        updateCrumbles(worldDt);
        for (Trap trap : traps) trap.update(worldDt);
        updateTide(worldDt);

        player.update(dt, in, solids, def.worldWidth, events);
        for (Enemy enemy : enemies) {
            if (enemy.alive) enemy.update(worldDt, this);
        }

        comboTimer = Math.max(0, comboTimer - dt);
        if (comboTimer == 0) combo = 0;
        lockedCooldown = Math.max(0, lockedCooldown - dt);

        checkHazards(in);
        if (phase != Phase.PLAYING) return;
        checkPickups();
        checkSprings(dt);
        checkSwitch();
        checkFlag();
    }

    /** 카메라 연출처럼 시뮬레이션이 멈춘 동안에도 진행되는 시각 효과입니다. */
    public void tickVisuals(float dt) {
        if (switchOn && doorAnim < 1) doorAnim = Math.min(1, doorAnim + dt);
    }

    private void updateZen(float dt, InputState in) {
        if (in.zenPressed && !zenActive() && run.zen >= 1) {
            run.zen = 0;
            zenTimer = Tuning.ZEN_DURATION;
            events.onZenStart();
        }
        if (zenActive()) {
            zenTimer -= dt;
            if (zenTimer <= 0) {
                zenTimer = 0;
                // Zen이 끝난 직후 적과 겹쳐 억울하게 맞는 일을 막습니다.
                player.invuln = Math.max(player.invuln, 0.5f);
                events.onZenEnd();
            }
        }
    }

    private void addZen(float amount) {
        run.zen = Math.min(1, run.zen + amount);
    }

    private void updateMovers(float dt) {
        Rectangle p = player.bounds;
        for (MovingPlatform mover : movers) {
            Rectangle box = mover.bounds;
            boolean riding = player.ground == box;
            float delta = mover.advance(dt);
            if (riding) {
                // 수직 발판은 위치를 직접 맞춰 실수 오차로 발판과 겹치는 일을 막습니다.
                if (mover.vertical) p.y = box.y + box.height;
                else p.x = MathUtils.clamp(p.x + delta, 0, def.worldWidth - Player.W);
            } else if (p.overlaps(box)) {
                // 타고 있지 않을 때 발판이 다가오면 진행 방향으로 밀어냅니다.
                if (mover.vertical) {
                    if (delta > 0) {
                        p.y = box.y + box.height;
                        player.vy = Math.max(player.vy, 0);
                    } else {
                        p.y = box.y - Player.H;
                        player.vy = Math.min(player.vy, 0);
                    }
                } else {
                    p.x = delta > 0 ? box.x + box.width : box.x - Player.W;
                }
            } else {
                continue;
            }
            resolveSqueeze(box, mover.vertical, delta);
            if (phase != Phase.PLAYING) return;
        }
    }

    /** 발판에 밀려 다른 지형과 겹치면 빠져나갈 곳으로 옮기고, 없으면 끼임으로 사망 처리합니다. */
    private void resolveSqueeze(Rectangle box, boolean vertical, float delta) {
        Rectangle p = player.bounds;
        for (Rectangle other : solids) {
            if (other == box || !p.overlaps(other)) continue;
            if (vertical) {
                // 내려오는 엘리베이터 밑에 있으면 가까운 옆쪽으로 밀어냅니다.
                float center = box.x + box.width / 2;
                p.x = p.x + Player.W / 2 < center ? box.x - Player.W : box.x + box.width;
                p.y = Math.max(p.y, other.y + other.height);
            } else {
                p.x = delta > 0 ? other.x - Player.W : other.x + other.width;
            }
            if (p.overlaps(box) || overlapsSolid()) die();
            return;
        }
    }

    private boolean overlapsSolid() {
        for (Rectangle solid : solids) {
            if (player.bounds.overlaps(solid)) return true;
        }
        return false;
    }

    private void updateCrumbles(float dt) {
        for (CrumblePlatform c : crumbles) {
            switch (c.state) {
                case IDLE:
                    if (player.ground == c.bounds) {
                        c.state = CrumblePlatform.State.SHAKING;
                        c.timer = Tuning.CRUMBLE_SHAKE;
                    }
                    break;
                case SHAKING:
                    c.timer -= dt;
                    if (c.timer <= 0) {
                        c.state = CrumblePlatform.State.FALLEN;
                        c.timer = Tuning.CRUMBLE_RESPAWN;
                        solids.removeValue(c.bounds, true);
                        if (player.ground == c.bounds) player.ground = null;
                        events.onCrumble(c.bounds.x + c.bounds.width / 2, c.bounds.y);
                    }
                    break;
                default:
                    c.timer -= dt;
                    if (c.timer <= 0 && !player.bounds.overlaps(c.bounds)) {
                        c.restore();
                        solids.add(c.bounds);
                    }
                    break;
            }
        }
    }

    private void updateTide(float dt) {
        if (!def.hasTide) return;
        tideSpeed = Math.min(def.tideMax, tideSpeed + def.tideAccel * dt);
        tideY += tideSpeed * dt;
    }

    private void checkHazards(InputState in) {
        Rectangle p = player.bounds;
        if (p.y < -100 || (def.hasTide && p.y < tideY - 12)) {
            die();
            return;
        }
        if (hazardsDisabled) return;
        for (Enemy enemy : enemies) {
            if (enemy.alive && !enemy.harmless() && p.overlaps(enemy.bounds)) {
                touchEnemy(enemy, in);
                if (phase != Phase.PLAYING) return;
            }
        }
        for (Trap trap : traps) {
            if (trap.active() && p.overlaps(trap.bounds)) {
                hurt();
                if (phase != Phase.PLAYING) return;
            }
        }
    }

    private void touchEnemy(Enemy enemy, InputState in) {
        Rectangle p = player.bounds;
        Rectangle e = enemy.bounds;
        boolean fromAbove = player.vy < 60 && p.y >= e.y + e.height * 0.4f;
        if (enemy.stompable() && (fromAbove || zenActive())) {
            if (enemy.givesLife()) stompForLife(enemy, in, !zenActive() || fromAbove);
            else stunEnemy(enemy, in, fromAbove);
        } else if (!zenActive() && player.invuln <= 0) {
            hurt();
            enemy.onHitPlayer();
        }
    }

    private void stunEnemy(Enemy enemy, InputState in, boolean bounce) {
        enemy.stun();
        int gained = addCombo(Tuning.POINTS_STOMP);
        addZen(Tuning.ZEN_PER_STOMP);
        if (bounce) player.stompBounce(in.jumpHeld);
        events.onStun(enemy.bounds.x + enemy.bounds.width / 2, enemy.bounds.y + enemy.bounds.height, gained);
    }

    /** 밟은 동물은 사라지고 목숨이 하나 늘어납니다. */
    private void stompForLife(Enemy enemy, InputState in, boolean bounce) {
        enemy.alive = false;
        lives++;
        int gained = addCombo(Tuning.POINTS_STOMP);
        addZen(Tuning.ZEN_PER_STOMP);
        if (bounce) player.stompBounce(in.jumpHeld);
        events.onStomp(enemy.bounds.x + enemy.bounds.width / 2, enemy.bounds.y + enemy.bounds.height,
            combo, gained);
    }

    private void hurt() {
        if (player.invuln > 0 || zenActive()) return;
        die();
    }

    private void die() {
        if (phase != Phase.PLAYING) return;
        phase = Phase.DYING;
        deathTimer = Tuning.DEATH_TIME;
        run.deaths++;
        lives--;
        combo = 0;
        comboTimer = 0;
        zenTimer = 0;
        events.onDeath(player.bounds.x + Player.W / 2, player.bounds.y + Player.H / 2);
    }

    /** 마지막 체크포인트에서 부활합니다. 모은 귤, 열린 문, 이미 밟은 동물은 그대로 둡니다. */
    private void respawn() {
        phase = Phase.PLAYING;
        player.place(checkpointX, checkpointY);
        player.invuln = Tuning.INVULN_TIME;
        for (Enemy enemy : enemies) {
            if (enemy.alive) enemy.reset();
        }
        for (CrumblePlatform c : crumbles) {
            if (c.state == CrumblePlatform.State.FALLEN) solids.add(c.bounds);
            c.restore();
        }
        if (def.hasTide) {
            tideY = Math.min(tideY, checkpointY - Tuning.TIDE_RESPAWN_GAP);
            tideSpeed = def.tideSpeed;
        }
        events.onRespawn();
    }

    private void checkPickups() {
        Rectangle p = player.bounds;
        for (int i = tangerines.size - 1; i >= 0; i--) {
            Rectangle fruit = tangerines.get(i);
            if (p.overlaps(fruit)) {
                tangerines.removeIndex(i);
                collected++;
                int gained = addCombo(Tuning.POINTS_TANGERINE);
                addZen(Tuning.ZEN_PER_TANGERINE);
                events.onTangerine(fruit.x + fruit.width / 2, fruit.y + fruit.height / 2, combo, gained);
            }
        }
        if (golden != null && p.overlaps(golden)) {
            float x = golden.x + golden.width / 2;
            float y = golden.y + golden.height / 2;
            golden = null;
            run.goldens++;
            run.score += Tuning.POINTS_GOLDEN;
            run.zen = 1;
            events.onGolden(x, y, Tuning.POINTS_GOLDEN);
        }
    }

    /** 연속 획득할수록 점수 배수가 커집니다. */
    private int addCombo(int basePoints) {
        combo = Math.min(combo + 1, Tuning.COMBO_CAP);
        comboTimer = Tuning.COMBO_WINDOW;
        int gained = basePoints * combo;
        run.score += gained;
        return gained;
    }

    private void checkSprings(float dt) {
        soaking = false;
        Rectangle p = player.bounds;
        boolean inSpring = false;
        for (HotSpring spring : springs) {
            if (!player.grounded() || !p.overlaps(spring.bounds)) continue;
            inSpring = true;
            if (lastSpring != spring) {
                lastSpring = spring;
                checkpointX = spring.bounds.x + (spring.bounds.width - Player.W) / 2;
                checkpointY = spring.bounds.y;
                if (!spring.activated) {
                    spring.activated = true;
                    addZen(Tuning.ZEN_PER_CHECKPOINT);
                }
                events.onCheckpoint(spring.bounds.x + spring.bounds.width / 2, spring.bounds.y + spring.bounds.height);
            }
            if (Math.abs(player.vx) < 5) {
                soakDelay += dt;
                if (soakDelay >= Tuning.SOAK_DELAY) {
                    soaking = true;
                    addZen(Tuning.SOAK_RATE * dt);
                }
            } else {
                soakDelay = 0;
            }
        }
        if (!inSpring) soakDelay = 0;
    }

    private void checkSwitch() {
        if (switchOn || switchPad == null) return;
        if (player.grounded() && player.bounds.overlaps(switchPad)) {
            switchOn = true;
            solids.removeValue(door, true);
            run.score += 200;
            events.onSwitch();
        }
    }

    private void checkFlag() {
        if (!player.bounds.overlaps(flag)) return;
        if (flagUnlocked()) {
            phase = Phase.CLEARED;
            run.children++;
            int bonus = Tuning.POINTS_CLEAR + lives * Tuning.POINTS_LIFE_BONUS;
            run.score += bonus;
            events.onClear(bonus);
        } else if (lockedCooldown == 0) {
            lockedCooldown = 1.5f;
            events.onFlagLocked();
        }
    }

    /** 화면 밖 안내 화살표가 가리킬 다음 목표입니다. */
    public Rectangle nextObjective() {
        if (flagUnlocked()) return flag;
        Rectangle best = null;
        float bestDistance = Float.MAX_VALUE;
        for (Rectangle fruit : tangerines) {
            float dx = fruit.x - player.bounds.x;
            float dy = fruit.y - player.bounds.y;
            float distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = fruit;
            }
        }
        return best;
    }
}
