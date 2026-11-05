package io.jbnu.test.render;

import com.badlogic.gdx.graphics.Pixmap;

/** 이미지 파일 없이 코드로 도트 스프라이트를 그립니다. Pixmap 좌표의 y축은 아래 방향입니다. */
final class Sprites {
    private static final int TONGUE = 0xEE8E9E;

    private Sprites() {
    }

    private static Pixmap canvas(int w, int h) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        return p;
    }

    private static void color(Pixmap p, int rgb) {
        p.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1);
    }

    /** 눈이 제각각인 띨빵한 몬스터입니다. mood: 0, 1 달리기, 2 어지러움, 3 잠. */
    static Pixmap derpy(int mood) {
        Pixmap p = canvas(48, 44);
        color(p, 0x6F4B6A);
        int shift = mood == 1 ? 4 : 0;
        p.fillRectangle(8 + shift, 38, 10, 6);
        p.fillRectangle(30 - shift, 38, 10, 6);
        color(p, 0x8C6387);
        p.fillCircle(24, 23, 19);
        color(p, 0xB488AE);
        p.fillCircle(24, 25, 17);
        p.fillRectangle(5, 26, 38, 8);
        color(p, 0x8C6387);
        p.fillTriangle(6, 8, 14, 7, 7, 0);
        p.fillTriangle(42, 8, 34, 7, 42, 1);

        if (mood == 3) {
            color(p, 0x222222);
            p.fillRectangle(11, 17, 11, 2);
            p.fillRectangle(28, 15, 11, 2);
        } else if (mood == 2) {
            color(p, 0xFFFFFF);
            p.fillCircle(16, 17, 7);
            p.fillCircle(33, 15, 8);
            color(p, 0x222222);
            p.drawLine(12, 13, 20, 21);
            p.drawLine(12, 21, 20, 13);
            p.drawLine(29, 11, 37, 19);
            p.drawLine(29, 19, 37, 11);
        } else {
            color(p, 0xFFFFFF);
            p.fillCircle(16, 17, 7);
            p.fillCircle(33, 15, 8);
            color(p, 0x222222);
            p.fillCircle(14, 21, 2); // 왼쪽 눈은 아래로
            p.fillCircle(38, 11, 3); // 오른쪽 눈은 위로
        }
        color(p, 0x4A2B48);
        p.fillRectangle(13, 29, 24, 6);
        color(p, 0xFFFFFF);
        p.fillRectangle(20, 29, 5, 6); // 뻐드렁니
        color(p, TONGUE);
        p.fillRectangle(28, 33, 7, 8);
        return p;
    }

    static Pixmap tangerine(boolean golden) {
        Pixmap p = canvas(32, 32);
        color(p, golden ? 0xB8860B : 0xA6400A);
        p.fillCircle(16, 18, 13);
        color(p, golden ? 0xFFD633 : 0xFF850D);
        p.fillCircle(16, 17, 11);
        color(p, golden ? 0xFFF5A8 : 0xFFBA3B);
        p.fillCircle(12, 13, 4);
        color(p, 0x593514);
        p.fillRectangle(15, 3, 3, 6);
        color(p, 0x3D9721);
        p.fillTriangle(17, 6, 23, 1, 27, 5);
        return p;
    }

    /** 오른쪽을 보는 멧돼지입니다. frame에 따라 다리가 움직입니다. */
    static Pixmap boar(int frame) {
        Pixmap p = canvas(42, 30);
        color(p, 0x3A2413);
        int[] legs = frame == 0 ? new int[]{7, 15, 25, 33} : new int[]{10, 18, 22, 30};
        for (int leg : legs) p.fillRectangle(leg, 23, 4, 7);
        color(p, 0x6B4423);
        p.fillCircle(17, 15, 11);
        p.fillRectangle(8, 6, 18, 18);
        color(p, 0x855A30);
        p.fillCircle(30, 15, 9);
        color(p, 0x4A2E17);
        p.fillTriangle(24, 2, 30, 9, 22, 9);
        p.fillRectangle(9, 3, 14, 3);
        color(p, 0xE8A87C);
        p.fillRectangle(35, 13, 7, 7);
        color(p, 0x8C5A44);
        p.fillRectangle(38, 15, 2, 2);
        color(p, 0xFFF4DC);
        p.fillTriangle(34, 21, 38, 21, 37, 15);
        color(p, 0x111111);
        p.fillRectangle(30, 10, 3, 3);
        return p;
    }

    static Pixmap hedgehog(boolean curled) {
        Pixmap p = canvas(38, 28);
        if (curled) {
            color(p, 0x3B3030);
            p.fillCircle(19, 16, 11);
            drawSpikes(p, 19, 16, 11, 17, 12);
            return p;
        }
        color(p, 0xD9B48A);
        p.fillRectangle(6, 22, 5, 6);
        p.fillRectangle(26, 22, 5, 6);
        p.fillCircle(29, 19, 7);
        color(p, 0x3B3030);
        p.fillCircle(15, 17, 10);
        p.fillRectangle(6, 18, 18, 7);
        for (int i = 0; i < 6; i++) {
            int x = 3 + i * 4;
            p.fillTriangle(x, 13 + Math.abs(i - 2), x + 6, 13 + Math.abs(i - 2), x + 2, 2 + Math.abs(i - 2) * 2);
        }
        color(p, 0x111111);
        p.fillRectangle(31, 16, 2, 2);
        p.fillRectangle(35, 19, 3, 3);
        return p;
    }

    private static void drawSpikes(Pixmap p, int cx, int cy, int inner, int outer, int count) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            int x0 = cx + (int) Math.round(Math.cos(a) * inner);
            int y0 = cy + (int) Math.round(Math.sin(a) * inner);
            int x1 = cx + (int) Math.round(Math.cos(a) * outer);
            int y1 = cy + (int) Math.round(Math.sin(a) * outer);
            p.drawLine(x0, y0, x1, y1);
            p.drawLine(x0 + 1, y0, x1, y1);
        }
    }

    static Pixmap hawk(boolean wingsUp) {
        Pixmap p = canvas(40, 26);
        color(p, 0x5A3B20);
        if (wingsUp) {
            p.fillTriangle(10, 13, 26, 13, 12, 0);
        } else {
            p.fillTriangle(10, 13, 26, 13, 12, 25);
        }
        color(p, 0x7A5230);
        p.fillCircle(19, 14, 7);
        p.fillTriangle(1, 9, 1, 19, 14, 14);
        color(p, 0xF2E3C6);
        p.fillCircle(28, 11, 5);
        color(p, 0xF2B21B);
        p.fillTriangle(32, 8, 32, 14, 39, 12);
        color(p, 0x111111);
        p.fillRectangle(29, 9, 2, 2);
        return p;
    }
}
