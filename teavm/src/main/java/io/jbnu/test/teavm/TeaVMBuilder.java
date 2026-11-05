package io.jbnu.test.teavm;

import com.github.xpenatan.gdx.backends.teavm.config.AssetFileHandle;
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuildConfiguration;
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.teavm.tooling.TeaVMTool;
import org.teavm.vm.TeaVMOptimizationLevel;

/** 게임을 자바스크립트로 변환해 웹사이트 폴더를 만듭니다. */
public final class TeaVMBuilder {
    private TeaVMBuilder() {
    }

    public static void main(String[] args) throws IOException {
        TeaBuildConfiguration configuration = new TeaBuildConfiguration();
        configuration.assetsPath.add(new AssetFileHandle("../assets"));
        configuration.webappPath = new File("build/dist").getCanonicalPath();
        configuration.htmlTitle = "Capybara's Outing";
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
        addMobileSettings(new File(configuration.webappPath, "webapp/index.html"));
    }

    /** 휴대폰에서 화면 확대, 스크롤, 길게 눌러 선택하기가 일어나지 않도록 페이지를 고칩니다. */
    private static void addMobileSettings(File index) throws IOException {
        String html = new String(Files.readAllBytes(index.toPath()), StandardCharsets.UTF_8);
        String settings = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, "
            + "maximum-scale=1, user-scalable=no, viewport-fit=cover\">\n"
            + "<meta name=\"mobile-web-app-capable\" content=\"yes\">\n"
            + "<meta name=\"apple-mobile-web-app-capable\" content=\"yes\">\n"
            + "<style>html, body, canvas { touch-action: none; -webkit-user-select: none; user-select: none; "
            + "-webkit-touch-callout: none; overscroll-behavior: none; } "
            + "#progress { width: 80vw !important; margin-left: -40vw !important; }</style>\n";
        html = html.replace("</head>", settings + "</head>");
        Files.write(index.toPath(), html.getBytes(StandardCharsets.UTF_8));
    }
}
