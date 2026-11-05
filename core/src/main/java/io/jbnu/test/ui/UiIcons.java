package io.jbnu.test.ui;

import com.badlogic.gdx.graphics.Pixmap;

/** 설정 메뉴에 쓰는 흰색 아이콘을 코드로 그립니다. Pixmap의 y축은 아래 방향입니다. */
public final class UiIcons {
    private static final int SIZE = 64;

    private UiIcons() {
    }

    private static Pixmap canvas() {
        Pixmap p = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        p.setColor(1, 1, 1, 1);
        return p;
    }

    /** 이미 그린 부분에 투명한 구멍을 냅니다. */
    private static void beginErase(Pixmap p) {
        p.setBlending(Pixmap.Blending.None);
        p.setColor(0, 0, 0, 0);
    }

    /** Zen 모드를 나타내는 방패입니다. 무적이 되는 기능을 모양으로 알려 줍니다. */
    public static Pixmap shield() {
        Pixmap p = canvas();
        shieldShape(p, 0);
        beginErase(p);
        shieldShape(p, 6);
        p.setBlending(Pixmap.Blending.SourceOver);
        p.setColor(1, 1, 1, 1);
        shieldShape(p, 11);
        return p;
    }

    /** inset만큼 안쪽으로 줄인 방패 모양을 현재 색으로 채웁니다. */
    private static void shieldShape(Pixmap p, int inset) {
        int left = 8 + inset, right = 56 - inset, top = 6 + inset;
        int shoulder = 34;
        int tip = 62 - inset * 3 / 2;
        p.fillRectangle(left, top, right - left + 1, shoulder - top);
        p.fillTriangle(left, shoulder, right, shoulder, 32, tip);
    }

    public static Pixmap heart() {
        Pixmap p = canvas();
        p.fillCircle(20, 22, 15);
        p.fillCircle(44, 22, 15);
        p.fillTriangle(6, 29, 58, 29, 32, 58);
        return p;
    }

    /** 가운데만 뚫린 검은 판입니다. 엔딩에서 화면이 원형으로 닫히는 연출에 씁니다. */
    public static Pixmap irisHole() {
        Pixmap p = new Pixmap(512, 512, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 1);
        p.fill();
        beginErase(p);
        p.fillCircle(256, 256, 250);
        return p;
    }

    public static Pixmap gear() {
        Pixmap p = canvas();
        p.fillCircle(32, 32, 21);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            double dx = Math.cos(a), dy = Math.sin(a);
            double nx = -dy * 6, ny = dx * 6;
            int x0 = (int) (32 + dx * 16 + nx), y0 = (int) (32 + dy * 16 + ny);
            int x1 = (int) (32 + dx * 16 - nx), y1 = (int) (32 + dy * 16 - ny);
            int x2 = (int) (32 + dx * 30 + nx * 0.7), y2 = (int) (32 + dy * 30 + ny * 0.7);
            int x3 = (int) (32 + dx * 30 - nx * 0.7), y3 = (int) (32 + dy * 30 - ny * 0.7);
            p.fillTriangle(x0, y0, x1, y1, x2, y2);
            p.fillTriangle(x1, y1, x2, y2, x3, y3);
        }
        beginErase(p);
        p.fillCircle(32, 32, 10);
        return p;
    }

    private static void speaker(Pixmap p) {
        p.fillRectangle(6, 24, 12, 16);
        p.fillTriangle(16, 24, 32, 9, 32, 55);
        p.fillTriangle(16, 24, 16, 40, 32, 55);
    }

    public static Pixmap soundOn() {
        Pixmap p = canvas();
        speaker(p);
        arc(p, 12);
        arc(p, 23);
        return p;
    }

    private static void arc(Pixmap p, int radius) {
        for (int degree = -45; degree <= 45; degree += 3) {
            double a = Math.toRadians(degree);
            p.fillCircle((int) (32 + Math.cos(a) * radius), (int) (32 + Math.sin(a) * radius), 2);
        }
    }

    public static Pixmap soundOff() {
        Pixmap p = canvas();
        speaker(p);
        for (int i = 0; i <= 20; i++) {
            p.fillCircle(38 + i, 22 + i, 2);
            p.fillCircle(58 - i, 22 + i, 2);
        }
        return p;
    }

    public static Pixmap pause() {
        Pixmap p = canvas();
        p.fillRectangle(15, 10, 13, 44);
        p.fillRectangle(36, 10, 13, 44);
        return p;
    }

    public static Pixmap play() {
        Pixmap p = canvas();
        p.fillTriangle(16, 8, 16, 56, 54, 32);
        return p;
    }

    public static Pixmap home() {
        Pixmap p = canvas();
        p.fillTriangle(32, 6, 4, 32, 60, 32);
        p.fillRectangle(13, 32, 38, 25);
        beginErase(p);
        p.fillRectangle(26, 40, 12, 17);
        return p;
    }
}
