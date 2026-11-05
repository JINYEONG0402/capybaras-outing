package io.jbnu.test.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import io.jbnu.test.ui.UiIcons;

/** 공용 텍스처와 글꼴을 보관합니다. 적과 귤 스프라이트는 Pixmap으로 직접 생성합니다. */
public final class GameAssets {
    public final SpriteBatch batch = new SpriteBatch();
    public final BitmapFont font = new BitmapFont();
    public final Texture white, capybara, flag, pause, circle, arrow, tangerine, golden;
    public final Texture baby, heart, iris;
    public final Texture iconShield;
    public final Texture iconGear, iconSoundOn, iconSoundOff, iconPause, iconPlay, iconHome;
    public final Texture[] boar, hedgehog, hawk, derpy;

    private final Array<Texture> owned = new Array<>();
    private final GlyphLayout layout = new GlyphLayout();
    private final Color shadow = new Color();

    public GameAssets() {
        font.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
        font.setUseIntegerPositions(false);

        white = own(pixel());
        capybara = own(smoothTexture("capybara.png", 512, 1024));
        derpy = new Texture[]{own(fromPixmap(Sprites.derpy(0))), own(fromPixmap(Sprites.derpy(1))),
            own(fromPixmap(Sprites.derpy(2))), own(fromPixmap(Sprites.derpy(3)))};
        baby = own(smoothTexture("baby.png", 256, 256));
        heart = own(soft(fromPixmap(UiIcons.heart())));
        iris = own(soft(fromPixmap(UiIcons.irisHole())));
        iconShield = own(soft(fromPixmap(UiIcons.shield())));
        iconGear = own(soft(fromPixmap(UiIcons.gear())));
        iconSoundOn = own(soft(fromPixmap(UiIcons.soundOn())));
        iconSoundOff = own(soft(fromPixmap(UiIcons.soundOff())));
        iconPause = own(soft(fromPixmap(UiIcons.pause())));
        iconPlay = own(soft(fromPixmap(UiIcons.play())));
        iconHome = own(soft(fromPixmap(UiIcons.home())));
        pause = own(new Texture(Gdx.files.internal("pause.png"), true));
        pause.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear);
        flag = own(whiteSilhouette("flag.png"));
        circle = own(soft(circlePixmap()));
        arrow = own(soft(arrowPixmap()));
        tangerine = own(fromPixmap(Sprites.tangerine(false)));
        golden = own(fromPixmap(Sprites.tangerine(true)));
        boar = new Texture[]{own(fromPixmap(Sprites.boar(0))), own(fromPixmap(Sprites.boar(1)))};
        hedgehog = new Texture[]{own(fromPixmap(Sprites.hedgehog(false))), own(fromPixmap(Sprites.hedgehog(true)))};
        hawk = new Texture[]{own(fromPixmap(Sprites.hawk(true))), own(fromPixmap(Sprites.hawk(false)))};
    }

    /**
     * 크게 줄여 그리는 그림을 매끄럽게 표시하도록 밉맵을 만듭니다.
     * 휴대폰 WebGL은 2의 거듭제곱 크기에만 밉맵을 허용하므로 그 크기로 늘려 담습니다.
     * 그릴 때 가로세로 크기를 직접 지정하므로 화면의 비율은 변하지 않습니다.
     */
    private static Texture smoothTexture(String path, int width, int height) {
        Pixmap source = new Pixmap(Gdx.files.internal(path));
        Pixmap scaled = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        scaled.setBlending(Pixmap.Blending.None);
        scaled.setFilter(Pixmap.Filter.BiLinear);
        scaled.drawPixmap(source, 0, 0, source.getWidth(), source.getHeight(), 0, 0, width, height);
        source.dispose();
        Texture texture = new Texture(scaled, true);
        scaled.dispose();
        texture.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear);
        return texture;
    }

    private Texture own(Texture texture) {
        owned.add(texture);
        return texture;
    }

    private static Texture pixel() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        return fromPixmap(p);
    }

    private static Texture fromPixmap(Pixmap pixmap) {
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private static Texture soft(Texture texture) {
        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        return texture;
    }

    /** 검은 깃발 이미지를 흰색으로 바꾸어, 잠김과 열림 상태를 색으로 표시합니다. */
    private static Texture whiteSilhouette(String path) {
        Pixmap source = new Pixmap(Gdx.files.internal(path));
        Pixmap target = new Pixmap(source.getWidth(), source.getHeight(), Pixmap.Format.RGBA8888);
        target.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int alpha = source.getPixel(x, y) & 0xFF;
                target.drawPixel(x, y, 0xFFFFFF00 | alpha);
            }
        }
        source.dispose();
        return soft(fromPixmap(target));
    }

    private static Texture circlePixmap() {
        Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fillCircle(32, 32, 31);
        return fromPixmap(p);
    }

    private static Texture arrowPixmap() {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fillTriangle(30, 16, 4, 3, 4, 29);
        return fromPixmap(p);
    }

    /** 그림자를 넣어 어떤 배경에서도 읽히는 글자를 그립니다. */
    public void text(String text, float x, float y, float scale, Color color, int align) {
        font.getData().setScale(scale);
        layout.setText(font, text, color, 0, align, false);
        float offset = Math.max(1, scale * 0.6f);
        // 어두운 글자에는 밝은 그림자를 넣어 번져 보이지 않게 합니다.
        boolean darkText = color.r + color.g + color.b < 1.2f;
        if (darkText) shadow.set(1, 1, 1, 0.4f * color.a);
        else shadow.set(0, 0, 0, 0.45f * color.a);
        layout.setText(font, text, shadow, 0, align, false);
        font.draw(batch, layout, x + offset, y - offset);
        layout.setText(font, text, color, 0, align, false);
        font.draw(batch, layout, x, y);
        font.getData().setScale(1);
    }

    public void textCenter(String text, float x, float y, float scale, Color color) {
        text(text, x, y, scale, color, Align.center);
    }

    public void rect(float x, float y, float w, float h, float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(white, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
        for (Texture texture : owned) texture.dispose();
    }
}
