package io.jbnu.test.level;

import com.badlogic.gdx.utils.Array;

/** JSON 레벨 파일의 내용을 담습니다. 레벨을 바꿀 때 코드를 고칠 필요가 없습니다. */
public final class LevelDef {
    public static final class MoverDef {
        public float x, y, w, min, max, speed;
        public boolean vertical;
    }

    public static final class TrapDef {
        public float x, y, w, period, active, phase;
    }

    public static final class CrumbleDef {
        public float x, y, w;
    }

    public static final class EnemyDef {
        public String type;
        public float x, y, min, max;
    }

    public static final class SignDef {
        public float x, y;
        public String text;
    }

    public String name, theme;
    public float worldWidth, worldHeight;
    public float[] start, flag;
    public float[] switchPad, door, golden, child; // 없을 수 있음
    public boolean autoTangerines;
    public int required; // 0이면 전부 모아야 함

    public boolean hasTide;
    public float tideStart, tideSpeed, tideAccel, tideMax;

    public final Array<float[]> blocks = new Array<>();
    public final Array<float[]> springs = new Array<>();
    public final Array<float[]> tangerines = new Array<>();
    public final Array<MoverDef> movers = new Array<>();
    public final Array<TrapDef> traps = new Array<>();
    public final Array<CrumbleDef> crumbles = new Array<>();
    public final Array<EnemyDef> enemies = new Array<>();
    public final Array<SignDef> signs = new Array<>();
}
