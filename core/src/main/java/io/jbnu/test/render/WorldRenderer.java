package io.jbnu.test.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import io.jbnu.test.level.LevelDef;
import io.jbnu.test.world.CrumblePlatform;
import io.jbnu.test.world.Derpy;
import io.jbnu.test.world.Enemy;
import io.jbnu.test.world.GameWorld;
import io.jbnu.test.world.Hawk;
import io.jbnu.test.world.Hedgehog;
import io.jbnu.test.world.HotSpring;
import io.jbnu.test.world.MovingPlatform;
import io.jbnu.test.world.Player;
import io.jbnu.test.world.Trap;
import io.jbnu.test.world.Tuning;

/** 게임 로직의 상태를 읽어 그리기만 합니다. 규칙은 바꾸지 않습니다. */
public final class WorldRenderer {
    private static final Color GOLD = new Color(1, 0.84f, 0.2f, 1);
    private static final Color SIGN = new Color();

    private final GameAssets assets;
    private final SpriteBatch batch;
    private final Theme theme;
    private float viewLeft, viewRight;
    private FamilyTrail trail;

    public WorldRenderer(GameAssets assets, Theme theme) {
        this.assets = assets;
        this.batch = assets.batch;
        this.theme = theme;
    }

    public void render(GameWorld world, OrthographicCamera camera, float time) {
        float halfW = camera.viewportWidth / 2 * camera.zoom;
        viewLeft = camera.position.x - halfW - 40;
        viewRight = camera.position.x + halfW + 40;

        drawSigns(world);
        for (Rectangle block : world.statics) {
            if (visible(block)) drawGround(block);
        }
        drawDoorAndSwitch(world);
        for (MovingPlatform mover : world.movers) drawMover(mover);
        for (CrumblePlatform c : world.crumbles) drawCrumble(c);
        for (Trap trap : world.traps) drawTrap(trap, time);
        for (HotSpring spring : world.springs) drawSpringBack(spring, time);
        drawPickups(world, time);
        drawFlag(world, time);
        for (Enemy enemy : world.enemies) {
            if (enemy.alive) drawEnemy(enemy, time);
        }
        drawChild(world, time);
        if (trail != null) trail.draw(assets, world, time);
        drawPlayer(world, time);
        for (HotSpring spring : world.springs) drawSpringFront(spring, time);
    }

    public void setTrail(FamilyTrail trail) {
        this.trail = trail;
    }

    /** 깃발 앞에서 아빠를 기다리는 자식입니다. 만나면 기뻐서 높이 뜁니다. */
    private void drawChild(GameWorld world, float time) {
        Rectangle c = world.child;
        if (c == null || !visible(c)) return;
        boolean found = world.phase == GameWorld.Phase.CLEARED;
        float hop = found ? Math.abs(MathUtils.sin(time * 8)) * 26 : Math.abs(MathUtils.sin(time * 3)) * 4;
        Texture t = assets.baby;
        batch.setColor(Color.WHITE);
        batch.draw(t, c.x, c.y + hop, c.width, c.height, 0, 0, t.getWidth(), t.getHeight(), true, false);
        float cx = c.x + c.width / 2;
        if (found) {
            float size = 18 + 4 * MathUtils.sin(time * 6);
            batch.setColor(1f, 0.4f, 0.55f, 1);
            batch.draw(assets.heart, cx - size / 2, c.y + hop + c.height + 6, size, size);
            batch.setColor(Color.WHITE);
        } else if (Math.abs(world.player.bounds.x - c.x) < 420) {
            assets.textCenter("Dad!", cx, c.y + c.height + 26 + hop, 1.1f, theme.text);
        }
    }

    private boolean visible(Rectangle r) {
        return r.x + r.width >= viewLeft && r.x <= viewRight;
    }

    private void drawSigns(GameWorld world) {
        SIGN.set(theme.text);
        for (LevelDef.SignDef sign : world.def.signs) {
            assets.textCenter(sign.text, sign.x, sign.y, 1.05f, SIGN);
        }
    }

    /** 지면과 공중 발판을 잔디와 흙으로 그립니다. 라운드 테마에 따라 색이 달라집니다. */
    private void drawGround(Rectangle ground) {
        batch.setColor(theme.earth);
        batch.draw(assets.white, ground.x, ground.y, ground.width, ground.height);
        batch.setColor(theme.speck);
        for (float y = ground.y + 5; y + 3 < ground.y + ground.height - 12; y += 20) {
            for (float x = ground.x + 6; x + 5 <= ground.x + ground.width; x += 19) {
                if (x >= viewLeft && x <= viewRight) batch.draw(assets.white, x, y, 5, 3);
            }
        }
        float top = ground.y + ground.height;
        batch.setColor(theme.grassDark);
        batch.draw(assets.white, ground.x, top - 12, ground.width, 12);
        batch.setColor(theme.grass);
        batch.draw(assets.white, ground.x, top - 6, ground.width, 6);
        batch.setColor(theme.grassTip);
        for (float x = ground.x + 2; x + 3 <= ground.x + ground.width; x += 9) {
            if (x >= viewLeft && x <= viewRight) batch.draw(assets.white, x, top - 4, 3, 4);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawDoorAndSwitch(GameWorld world) {
        if (world.switchPad != null) {
            Rectangle pad = world.switchPad;
            float press = world.switchOn ? 6 : 0;
            assets.rect(pad.x - 4, pad.y, pad.width + 8, 4, 0.2f, 0.2f, 0.25f, 1);
            if (world.switchOn) assets.rect(pad.x, pad.y, pad.width, pad.height - press, 0.2f, 0.85f, 0.3f, 1);
            else assets.rect(pad.x, pad.y, pad.width, pad.height, 1f, 0.65f, 0.15f, 1);
            assets.textCenter(world.switchOn ? "OPEN" : "STEP ON", pad.x + pad.width / 2, pad.y + 34, 1f,
                theme.text);
        }
        if (world.door != null && world.doorAnim < 1) {
            // 스위치를 밟으면 문이 땅속으로 내려갑니다.
            Rectangle door = world.door;
            float height = door.height * (1 - world.doorAnim);
            assets.rect(door.x, door.y, door.width, height, 0.55f, 0.25f, 0.8f, 1);
            for (float y = door.y + 18; y < door.y + height - 4; y += 36) {
                assets.rect(door.x + 4, y, door.width - 8, 6, 0.75f, 0.45f, 0.95f, 1);
            }
            assets.textCenter("LOCK", door.x + door.width / 2, door.y + height + 20, 1f, theme.text);
        }
    }

    private void drawMover(MovingPlatform mover) {
        Rectangle box = mover.bounds;
        if (!visible(box)) return;
        assets.rect(box.x, box.y, box.width, box.height, 0.42f, 0.27f, 0.13f, 1);
        assets.rect(box.x, box.y + box.height - 6, box.width, 6, 0.72f, 0.5f, 0.26f, 1);
        for (float x = box.x + 16; x < box.x + box.width - 4; x += 28) {
            assets.rect(x, box.y, 3, box.height - 6, 0.3f, 0.18f, 0.08f, 1);
        }
        float cx = box.x + box.width / 2;
        assets.textCenter(mover.vertical ? "^  LIFT  v" : "<  FERRY  >", cx, box.y + 14, 0.8f, Color.WHITE);
    }

    private void drawCrumble(CrumblePlatform c) {
        Rectangle box = c.bounds;
        float ox = 0, oy = 0, alpha = 1;
        if (c.state == CrumblePlatform.State.SHAKING) {
            ox = MathUtils.random(-2f, 2f);
            oy = MathUtils.random(-1f, 1f);
        } else if (c.state == CrumblePlatform.State.FALLEN) {
            float fallen = Tuning.CRUMBLE_RESPAWN - c.timer;
            oy = -fallen * fallen * 500;
            alpha = Math.max(0, 1 - fallen * 2.5f);
            // 돌아오기 직전에는 희미하게 보여 다시 밟을 시점을 알려 줍니다.
            if (c.timer < 0.6f) {
                alpha = 0.35f;
                oy = 0;
            }
        }
        if (alpha <= 0) return;
        assets.rect(box.x + ox, box.y + oy, box.width, box.height, 0.55f, 0.47f, 0.4f, alpha);
        assets.rect(box.x + ox, box.y + oy + box.height - 5, box.width, 5, 0.72f, 0.64f, 0.54f, alpha);
        for (float x = box.x + 10; x < box.x + box.width - 6; x += 22) {
            assets.rect(x + ox, box.y + oy + 2, 2, box.height - 8, 0.2f, 0.15f, 0.13f, alpha);
            assets.rect(x + ox + 2, box.y + oy + 8, 8, 2, 0.2f, 0.15f, 0.13f, alpha);
        }
    }

    private void drawTrap(Trap trap, float time) {
        Rectangle b = trap.bounds;
        if (!visible(b)) return;
        boolean active = trap.active();
        // 켜지기 직전에는 깜빡여 미리 경고합니다.
        boolean warning = !active && trap.remaining() < 0.3f && MathUtils.sin(time * 30) > 0;
        if (active) {
            assets.rect(b.x, b.y, b.width, b.height, 0.95f, 0.2f, 0.2f, 1);
            for (float x = b.x; x < b.x + b.width; x += 10) {
                float h = 8 + 6 * MathUtils.sin(time * 18 + x);
                assets.rect(x, b.y + b.height, 8, h, 1f, 0.6f, 0.15f, 0.95f);
            }
        } else {
            float g = warning ? 0.75f : 0.45f;
            assets.rect(b.x, b.y, b.width, 8, warning ? 1f : 0.3f, g, 0.25f, 1);
        }
        assets.rect(b.x, b.y + 34, b.width * trap.remaining(), 4,
            active ? 1f : 0.4f, active ? 0.4f : 0.9f, 0.4f, 0.9f);
        assets.textCenter(active ? "HOT" : "SAFE", b.x + b.width / 2, b.y + 56, 0.9f, theme.text);
    }

    private void drawSpringBack(HotSpring spring, float time) {
        Rectangle b = spring.bounds;
        if (!visible(b)) return;
        // 노천탕: 나무 표지판과 바닥을 먼저 그리고, 돌과 물은 플레이어 앞에 그립니다.
        assets.rect(b.x - 28, b.y, 4, 44, 0.42f, 0.27f, 0.13f, 1);
        assets.rect(b.x - 48, b.y + 34, 44, 18, 0.62f, 0.42f, 0.22f, 1);
        assets.textCenter("ONSEN", b.x - 26, b.y + 48, 0.72f, Color.WHITE);
        assets.rect(b.x - 8, b.y, b.width + 16, 8, 0.32f, 0.32f, 0.36f, 1);
        if (spring.activated) {
            float glow = 0.5f + 0.5f * MathUtils.sin(time * 4);
            assets.textCenter("CHECK", b.x + b.width / 2, b.y + 145 + glow * 3, 0.9f, GOLD);
        }
    }

    private void drawSpringFront(HotSpring spring, float time) {
        Rectangle b = spring.bounds;
        if (!visible(b)) return;
        // 플레이어 앞에 물을 그려 온천에 몸을 담근 것처럼 보이게 합니다.
        for (float x = b.x; x < b.x + b.width; x += 7) {
            float h = b.height + 1.5f * MathUtils.sin(time * 3 + x * 0.15f);
            assets.rect(x, b.y + 2, 7, h, 0.45f, 0.82f, 0.88f, 0.72f);
        }
        // 물에 둥둥 뜬 귤과 둘레의 돌
        float bob = MathUtils.sin(time * 2.2f + b.x) * 1.5f;
        batch.setColor(Color.WHITE);
        batch.draw(assets.tangerine, b.x + 6, b.y + b.height - 7 + bob, 13, 13);
        batch.draw(assets.tangerine, b.x + b.width - 18, b.y + b.height - 7 - bob, 13, 13);
        float[][] stones = {{-14, 0, 15}, {-8, 7, 12}, {b.width - 2, 0, 16}, {b.width - 4, 8, 11}};
        for (int i = 0; i < stones.length; i++) {
            float shade = i % 2 == 0 ? 0.56f : 0.68f;
            batch.setColor(shade, shade, shade + 0.03f, 1);
            batch.draw(assets.circle, b.x + stones[i][0], b.y + stones[i][1], stones[i][2], stones[i][2]);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawPickups(GameWorld world, float time) {
        batch.setColor(Color.WHITE);
        for (Rectangle fruit : world.tangerines) {
            if (!visible(fruit)) continue;
            float bob = MathUtils.sin(time * 3 + fruit.x * 0.05f) * 3;
            batch.draw(assets.tangerine, fruit.x, fruit.y + bob + 3, fruit.width, fruit.height);
        }
        Rectangle golden = world.golden;
        if (golden != null && visible(golden)) {
            float pulse = 1 + 0.15f * MathUtils.sin(time * 5);
            batch.setColor(1, 0.9f, 0.3f, 0.3f);
            float glow = 54 * pulse;
            batch.draw(assets.circle, golden.x + 15 - glow / 2, golden.y + 15 - glow / 2, glow, glow);
            batch.setColor(Color.WHITE);
            batch.draw(assets.golden, golden.x, golden.y, golden.width, golden.height);
        }
    }

    private void drawFlag(GameWorld world, float time) {
        Rectangle flag = world.flag;
        if (!visible(flag)) return;
        boolean open = world.flagUnlocked();
        float wave = open ? MathUtils.sin(time * 6) * 3 : 0;
        if (open) batch.setColor(1f, 0.55f, 0.1f, 1);
        else batch.setColor(0.55f, 0.55f, 0.6f, 1);
        batch.draw(assets.flag, flag.x - 6, flag.y, 62 + wave, flag.height);
        batch.setColor(Color.WHITE);
        assets.textCenter(open ? "GOAL!" : "LOCKED", flag.x + 22, flag.y + flag.height + 20, 1f,
            open ? GOLD : theme.text);
    }

    private void drawEnemy(Enemy enemy, float time) {
        Rectangle b = enemy.bounds;
        if (!visible(b)) return;
        Texture texture;
        switch (enemy.kind()) {
            case BOAR:
                texture = assets.boar[(int) (enemy.animTime * 8) % 2];
                break;
            case HEDGEHOG:
                texture = assets.hedgehog[((Hedgehog) enemy).curled() ? 1 : 0];
                break;
            case DERPY:
                texture = derpyTexture((Derpy) enemy);
                break;
            default:
                texture = assets.hawk[(int) (enemy.animTime * 9) % 2];
                break;
        }
        batch.setColor(Color.WHITE);
        batch.draw(texture, b.x, b.y, b.width, b.height, 0, 0, texture.getWidth(), texture.getHeight(),
            enemy.facing < 0, false);
        if (enemy instanceof Derpy) drawDerpyMood((Derpy) enemy, time);
        if (enemy instanceof Hawk && ((Hawk) enemy).state == Hawk.State.AIM) {
            float shake = MathUtils.sin(time * 40) * 2;
            assets.textCenter("!", b.x + b.width / 2 + shake, b.y + b.height + 28, 2.2f, Color.RED);
        }
    }

    private Texture derpyTexture(Derpy derpy) {
        switch (derpy.state) {
            case ASLEEP:
                return assets.derpy[3];
            case DIZZY:
                return assets.derpy[2];
            default:
                return assets.derpy[(int) (derpy.animTime * 8) % 2];
        }
    }

    /** 머리 위에 지금 무슨 생각을 하는지 보여 줍니다. */
    private void drawDerpyMood(Derpy derpy, float time) {
        Rectangle b = derpy.bounds;
        float cx = b.x + b.width / 2, top = b.y + b.height;
        String bubble = null;
        switch (derpy.state) {
            case ASLEEP:
                bubble = "z z Z";
                break;
            case CONFUSED:
                bubble = "?";
                break;
            case DANCE:
                bubble = "YAY!";
                break;
            case DIZZY:
                for (int i = 0; i < 3; i++) {
                    float a = time * 6 + i * MathUtils.PI2 / 3;
                    batch.setColor(GOLD);
                    batch.draw(assets.circle, cx + MathUtils.cos(a) * 20 - 3, top + 6 + MathUtils.sin(a) * 5, 6, 6);
                }
                batch.setColor(Color.WHITE);
                break;
            default:
                if (derpy.falling) bubble = "WAAAA";
                break;
        }
        if (bubble != null) assets.textCenter(bubble, cx, top + 24, 1.3f, theme.text);
    }

    private void drawPlayer(GameWorld world, float time) {
        Player player = world.player;
        Rectangle b = player.bounds;
        boolean dying = world.phase == GameWorld.Phase.DYING;
        boolean moving = Math.abs(player.vx) > 20;
        Texture t = assets.capybara;
        float w = 40, h = 55; // capybara.png 비율을 유지하되 충돌 박스보다 약간 크게 그립니다.
        float scaleX = 1, scaleY = 1, rotation = 0, alpha = 1, y = b.y;

        if (world.phase == GameWorld.Phase.DYING) {
            float progress = 1 - world.deathTimer / Tuning.DEATH_TIME;
            y += progress * 90;
            rotation = progress * 540;
            alpha = 1 - progress;
        } else {
            if (player.ground == null) {
                float stretch = MathUtils.clamp(Math.abs(player.vy) / 4000f, 0, 0.12f);
                scaleX -= stretch;
                scaleY += stretch;
            } else if (moving) {
                y += Math.abs(MathUtils.sin(player.walkTime * 13)) * 2;
                rotation = MathUtils.sin(player.walkTime * 13) * 3 * player.facing;
            }
            if (player.landSquash > 0) {
                scaleX += player.landSquash * 0.28f;
                scaleY -= player.landSquash * 0.28f;
            }
            if (world.soaking) {
                // 앞쪽에 그리는 물이 몸 아래를 가려 온천에 앉은 것처럼 보입니다.
                y -= 2;
                rotation = MathUtils.sin(time * 1.5f) * 3;
            }
            if (player.invuln > 0 && MathUtils.sin(time * 35) > 0) alpha = 0.35f;
        }

        if (world.zenActive()) {
            float pulse = 72 + 10 * MathUtils.sin(time * 6);
            batch.setColor(1, 0.9f, 0.4f, 0.32f);
            batch.draw(assets.circle, b.x + Player.W / 2 - pulse / 2, b.y + Player.H / 2 - pulse / 2, pulse, pulse);
        }
        if (world.zenActive()) batch.setColor(1f, 0.95f, 0.65f, alpha);
        else batch.setColor(1, 1, 1, alpha);
        float cx = b.x + Player.W / 2;
        batch.draw(t, cx - w / 2, y - 2, w / 2, 0, w, h, scaleX, scaleY, rotation, 0, 0, t.getWidth(), t.getHeight(),
            player.facing < 0, false);
        batch.setColor(Color.WHITE);

        if (!dying) drawFriends(world, cx, y + h * scaleY - 8, time);
    }

    /** 밟아서 사귄 친구들이 카피바라 머리 위에 층층이 올라앉습니다. */
    private void drawFriends(GameWorld world, float cx, float headY, float time) {
        Player player = world.player;
        float y = headY;
        float sway = player.ground != null && Math.abs(player.vx) > 20 ? 3 : 1;
        for (int i = 0; i < world.run.friends; i++) {
            Texture t = assets.boar[0];
            float w = 30, h = 21;
            float x = cx - w / 2 + MathUtils.sin(time * 5 + i * 1.3f) * sway * (i + 1);
            batch.draw(t, x, y, w, h, 0, 0, t.getWidth(), t.getHeight(), player.facing < 0, false);
            y += h - 3;
        }
    }

    /** 끓는 물은 세계에서 가장 앞쪽에 그립니다. */
    public void renderTide(GameWorld world, OrthographicCamera camera, float time) {
        if (!world.def.hasTide) return;
        float halfH = camera.viewportHeight / 2 * camera.zoom;
        float bottom = camera.position.y - halfH - 20;
        if (world.tideY < bottom) return;
        for (float x = 0; x < world.def.worldWidth; x += 12) {
            float wave = MathUtils.sin(time * 3 + x * 0.04f) * 5 + MathUtils.sin(time * 5.3f + x * 0.09f) * 3;
            float top = world.tideY + wave;
            assets.rect(x, bottom, 12, top - bottom, 0.86f, 0.22f, 0.1f, 0.9f);
            assets.rect(x, top - 7, 12, 7, 1f, 0.6f, 0.18f, 1);
        }
    }
}
