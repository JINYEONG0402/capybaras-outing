package io.jbnu.test.level;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** JSON 문자열을 {@link LevelDef}로 변환합니다. */
public final class LevelParser {
    private LevelParser() {
    }

    public static LevelDef parse(String json) {
        JsonValue root = new JsonReader().parse(json);
        LevelDef def = new LevelDef();
        def.name = root.getString("name");
        def.theme = root.getString("theme");
        def.worldWidth = root.getFloat("worldWidth");
        def.worldHeight = root.getFloat("worldHeight");
        def.start = root.get("start").asFloatArray();
        def.flag = root.get("flag").asFloatArray();
        def.switchPad = optionalArray(root, "switch");
        def.door = optionalArray(root, "door");
        def.golden = optionalArray(root, "golden");
        def.child = optionalArray(root, "child");
        def.autoTangerines = root.getBoolean("autoTangerines", false);
        def.required = root.getInt("required", 0);
        readArrays(root, "blocks", def.blocks);
        readArrays(root, "springs", def.springs);
        readArrays(root, "tangerines", def.tangerines);

        JsonValue tide = root.get("tide");
        if (tide != null) {
            def.hasTide = true;
            def.tideStart = tide.getFloat("start");
            def.tideSpeed = tide.getFloat("speed");
            def.tideAccel = tide.getFloat("accel");
            def.tideMax = tide.getFloat("max");
        }
        for (JsonValue v : children(root, "movers")) {
            LevelDef.MoverDef m = new LevelDef.MoverDef();
            m.x = v.getFloat("x");
            m.y = v.getFloat("y");
            m.w = v.getFloat("w");
            m.min = v.getFloat("min");
            m.max = v.getFloat("max");
            m.speed = v.getFloat("speed");
            m.vertical = v.getBoolean("vertical");
            def.movers.add(m);
        }
        for (JsonValue v : children(root, "traps")) {
            LevelDef.TrapDef t = new LevelDef.TrapDef();
            t.x = v.getFloat("x");
            t.y = v.getFloat("y");
            t.w = v.getFloat("w");
            t.period = v.getFloat("period");
            t.active = v.getFloat("active");
            t.phase = v.getFloat("phase");
            def.traps.add(t);
        }
        for (JsonValue v : children(root, "crumbles")) {
            LevelDef.CrumbleDef c = new LevelDef.CrumbleDef();
            c.x = v.getFloat("x");
            c.y = v.getFloat("y");
            c.w = v.getFloat("w");
            def.crumbles.add(c);
        }
        for (JsonValue v : children(root, "enemies")) {
            LevelDef.EnemyDef e = new LevelDef.EnemyDef();
            e.type = v.getString("type");
            e.x = v.getFloat("x");
            e.y = v.getFloat("y");
            e.min = v.getFloat("min", 0);
            e.max = v.getFloat("max", 0);
            def.enemies.add(e);
        }
        for (JsonValue v : children(root, "signs")) {
            LevelDef.SignDef s = new LevelDef.SignDef();
            s.x = v.getFloat("x");
            s.y = v.getFloat("y");
            s.text = v.getString("text");
            def.signs.add(s);
        }
        return def;
    }

    private static float[] optionalArray(JsonValue root, String name) {
        JsonValue v = root.get(name);
        return v == null ? null : v.asFloatArray();
    }

    private static void readArrays(JsonValue root, String name, Array<float[]> out) {
        for (JsonValue v : children(root, name)) out.add(v.asFloatArray());
    }

    private static Array<JsonValue> children(JsonValue root, String name) {
        Array<JsonValue> list = new Array<>();
        JsonValue parent = root.get(name);
        if (parent == null) return list;
        for (JsonValue v = parent.child; v != null; v = v.next) list.add(v);
        return list;
    }
}
