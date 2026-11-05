package io.jbnu.test.render;

import com.badlogic.gdx.graphics.Color;

/** 라운드별 색, 배경, 배경음, 분위기 입자를 정의합니다. */
public final class Theme {
    public enum Ambient { PETALS, LEAVES, FIREFLIES, EMBERS }

    public final String music;
    public final Ambient ambient;
    public final Color skyTop, skyBottom, sun, hillFar, hillNear;
    public final Color earth, speck, grassDark, grass, grassTip, text;
    public final boolean stars, clouds, peaks;

    private Theme(String music, Ambient ambient, int skyTop, int skyBottom, int sun, int hillFar, int hillNear,
                  int earth, int speck, int grassDark, int grass, int grassTip, int text,
                  boolean stars, boolean clouds, boolean peaks) {
        this.music = music;
        this.ambient = ambient;
        this.skyTop = rgb(skyTop);
        this.skyBottom = rgb(skyBottom);
        this.sun = rgb(sun);
        this.hillFar = rgb(hillFar);
        this.hillNear = rgb(hillNear);
        this.earth = rgb(earth);
        this.speck = rgb(speck);
        this.grassDark = rgb(grassDark);
        this.grass = rgb(grass);
        this.grassTip = rgb(grassTip);
        this.text = rgb(text);
        this.stars = stars;
        this.clouds = clouds;
        this.peaks = peaks;
    }

    private static Color rgb(int value) {
        return new Color(((value >> 16) & 255) / 255f, ((value >> 8) & 255) / 255f, (value & 255) / 255f, 1);
    }

    public static Theme get(String name) {
        switch (name) {
            case "sunset":
                return new Theme("sunset", Ambient.LEAVES, 0x5B4B8A, 0xFFB56B, 0xFFE28A, 0xA9668F, 0x6F4A73,
                    0x5C3B3A, 0x7A5148, 0x7A5A2E, 0xB88A3C, 0xE0B65A, 0xFFF4DC, false, true, false);
            case "night":
                return new Theme("night", Ambient.FIREFLIES, 0x0E1433, 0x2F4A7A, 0xF4F0D0, 0x24335F, 0x18234A,
                    0x2B2F49, 0x3D4263, 0x1E5A4E, 0x2F8A6F, 0x5CC49A, 0xDDE8FF, true, false, false);
            case "volcano":
                return new Theme("volcano", Ambient.EMBERS, 0x23121F, 0x8A3324, 0xFF7A3D, 0x4B2230, 0x311823,
                    0x3A2A2E, 0x523C3E, 0x6E3B24, 0xB5562B, 0xF0893A, 0xFFE9D0, true, false, true);
            default:
                return new Theme("meadow", Ambient.PETALS, 0x7CC6F2, 0xDDF2FB, 0xFFF3B0, 0xA8D8B0, 0x7FC08A,
                    0x7A4A26, 0x9A6336, 0x33751F, 0x61B833, 0x94D447, 0x24404F, false, true, false);
        }
    }
}
