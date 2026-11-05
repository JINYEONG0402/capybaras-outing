package io.jbnu.test.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import io.jbnu.test.render.GameAssets;

/** 글자 또는 아이콘을 표시하고, 마우스를 올리면 밝아지는 버튼입니다. */
public final class UiButton {
    public final Rectangle bounds;
    private final String label;
    private final float labelScale;

    private UiButton(float x, float y, float w, float h, String label, float labelScale) {
        bounds = new Rectangle(x, y, w, h);
        this.label = label;
        this.labelScale = labelScale;
    }

    public static UiButton text(float x, float y, float w, float h, String label, float scale) {
        return new UiButton(x, y, w, h, label, scale);
    }

    public static UiButton icon(float x, float y, float size) {
        return new UiButton(x, y, size, size, null, 0);
    }

    public boolean hovered(UiMouse mouse) {
        return bounds.contains(mouse.x, mouse.y);
    }

    public boolean clicked(UiMouse mouse) {
        return mouse.clicked && hovered(mouse);
    }

    /** 글자 버튼을 그립니다. */
    public void draw(GameAssets assets, UiMouse mouse, Color base) {
        float light = hovered(mouse) ? 1.18f : 1f;
        assets.rect(bounds.x + 3, bounds.y - 4, bounds.width, bounds.height, 0, 0, 0, 0.3f);
        assets.rect(bounds.x, bounds.y, bounds.width, bounds.height,
            Math.min(1, base.r * light), Math.min(1, base.g * light), Math.min(1, base.b * light), 1);
        assets.textCenter(label, bounds.x + bounds.width / 2,
            bounds.y + bounds.height / 2 + 6.5f * labelScale, labelScale, Color.WHITE);
    }

    /** 아이콘 버튼을 그립니다. */
    public void drawIcon(GameAssets assets, UiMouse mouse, Texture icon) {
        float shade = hovered(mouse) ? 0.42f : 0.2f;
        assets.rect(bounds.x, bounds.y, bounds.width, bounds.height, shade, shade, shade, 0.88f);
        float pad = bounds.width * 0.16f;
        assets.batch.setColor(Color.WHITE);
        assets.batch.draw(icon, bounds.x + pad, bounds.y + pad, bounds.width - pad * 2, bounds.height - pad * 2);
    }
}
