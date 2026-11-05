package io.jbnu.test.teavm;

import com.github.xpenatan.gdx.backends.teavm.config.AssetFileHandle;
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuildConfiguration;
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.teavm.tooling.TeaVMTool;
import org.teavm.vm.TeaVMOptimizationLevel;

/** 게임을 자바스크립트로 변환해 웹사이트 폴더를 만듭니다. */
public final class TeaVMBuilder {
    /** 게임에서 쓰지 않아 웹 다운로드에서 빼는 파일입니다. 휴대폰 로딩이 빨라집니다. */
    private static final String[] UNUSED_ASSETS = {
        "block.png", "player.png", "libgdx.png", "audio/bgm_volcano.wav", "levels/round4.json"
    };

    private TeaVMBuilder() {
    }

    public static void main(String[] args) throws IOException {
        TeaBuildConfiguration configuration = new TeaBuildConfiguration();
        configuration.assetsPath.add(new AssetFileHandle("../assets"));
        configuration.assetFilter = (file, isDirectory, op) -> {
            String path = file.replace('\\', '/');
            for (String unused : UNUSED_ASSETS) {
                if (path.endsWith(unused)) return false;
            }
            return true;
        };
        configuration.webappPath = new File("build/dist").getCanonicalPath();
        configuration.htmlTitle = "Daddy Capybara's Journey";
        configuration.htmlWidth = 960;
        configuration.htmlHeight = 540;
        configuration.showLoadingLogo = false;
        TeaBuilder.config(configuration);

        TeaVMTool tool = new TeaVMTool();
        tool.setMainClass(TeaVMLauncher.class.getName());
        tool.setOptimizationLevel(TeaVMOptimizationLevel.ADVANCED);
        tool.setObfuscated(true);
        boolean ok = TeaBuilder.build(tool);
        if (!ok) throw new IllegalStateException("Web build failed");

        File webapp = new File(configuration.webappPath, "webapp");
        copyWebFiles(webapp);
        addMobileSettings(new File(webapp, "index.html"));
    }

    /** 아이콘, 앱 설정, 휴대폰 지원 코드를 웹사이트에 넣습니다. */
    private static void copyWebFiles(File webapp) throws IOException {
        File[] files = new File("web").listFiles();
        if (files == null) throw new IOException("teavm/web folder not found");
        for (File file : files) {
            Files.copy(file.toPath(), new File(webapp, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** 휴대폰에서 확대, 스크롤, 길게 눌러 선택하기를 막고, 홈 화면에 추가하면 전체화면으로 열리게 합니다. */
    private static void addMobileSettings(File index) throws IOException {
        String html = new String(Files.readAllBytes(index.toPath()), StandardCharsets.UTF_8);
        String settings = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, "
            + "maximum-scale=1, user-scalable=no, viewport-fit=cover\">\n"
            + "<meta name=\"mobile-web-app-capable\" content=\"yes\">\n"
            + "<meta name=\"apple-mobile-web-app-capable\" content=\"yes\">\n"
            + "<meta name=\"apple-mobile-web-app-status-bar-style\" content=\"black-translucent\">\n"
            + "<meta name=\"apple-mobile-web-app-title\" content=\"Daddy Capybara\">\n"
            + "<link rel=\"manifest\" href=\"manifest.webmanifest\">\n"
            + "<link rel=\"apple-touch-icon\" href=\"apple-touch-icon.png\">\n"
            + "<style>html, body { position: fixed !important; top: 0; left: 0; width: 100% !important; height: 100% !important; overflow: hidden; margin: 0; } "
            + "canvas { display: block; position: absolute; top: 0; left: 0; } "
            + "html, body, canvas { touch-action: none; -webkit-user-select: none; user-select: none; "
            + "-webkit-touch-callout: none; overscroll-behavior: none; background: #000; } "
            + "#progress { width: 80vw !important; margin-left: -40vw !important; }</style>\n"
            + "<script src=\"mobile.js\"></script>\n";
        html = html.replace("</head>", settings + "</head>");
        Files.write(index.toPath(), html.getBytes(StandardCharsets.UTF_8));
    }
}
