package io.jbnu.test.teavm;

import com.github.xpenatan.gdx.backends.teavm.TeaApplication;
import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration;
import io.jbnu.test.Main;
import org.teavm.jso.JSBody;

/** 웹 진입점입니다. 터치 화면 기기에서는 화면 버튼을 보여 줍니다. */
public final class TeaVMLauncher {
    private TeaVMLauncher() {
    }

    // 주소에 ?touch=1 을 붙이면 PC에서도 화면 버튼을 켜서 시험할 수 있습니다.
    @JSBody(script = "return (navigator.maxTouchPoints || 0) > 0 || 'ontouchstart' in window"
        + " || window.location.search.indexOf('touch=1') >= 0;")
    private static native boolean hasTouchScreen();

    // 아이폰 Safari는 전체화면을 지원하지 않아, 홈 화면에 추가한 경우에만 전체화면으로 열립니다.
    @JSBody(script = "return /iPhone|iPod/.test(navigator.userAgent) && !navigator.standalone"
        + " || window.location.search.indexOf('tip=1') >= 0;")
    private static native boolean isIphoneBrowser();

    public static void main(String[] args) {
        TeaApplicationConfiguration configuration = new TeaApplicationConfiguration("canvas");
        configuration.width = 0; // 브라우저 창 크기에 맞춥니다.
        configuration.height = 0;
        boolean touch = hasTouchScreen();
        configuration.usePhysicalPixels = touch; // 휴대폰 화면의 실제 픽셀로 선명하게 그립니다.
        String tip = isIphoneBrowser() ? "Full screen: tap Share, then Add to Home Screen" : null;
        new TeaApplication(new Main(touch, tip), configuration);
    }
}
