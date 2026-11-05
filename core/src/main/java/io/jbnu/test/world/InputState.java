package io.jbnu.test.world;

/** 키보드와 게임 로직을 분리하는 입력 상태입니다. 테스트 봇도 같은 형식으로 조작합니다. */
public final class InputState {
    public boolean left, right, jumpHeld;
    public boolean jumpPressed, zenPressed;

    /** 한 번만 처리해야 하는 입력을 물리 스텝 후 비웁니다. */
    public void clearPressed() {
        jumpPressed = false;
        zenPressed = false;
    }
}
