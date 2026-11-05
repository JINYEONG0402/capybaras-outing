package io.jbnu.test.world;

/**
 * 옵저버 패턴: 게임 로직은 사건만 알리고, 소리·입자·화면 흔들림은 듣는 쪽이 처리합니다.
 * 덕분에 로직은 그래픽과 오디오 없이도 실행되고 테스트할 수 있습니다.
 */
public interface WorldListener {
    default void onJump() { }
    default void onLand(float x, float y, float impact) { }
    default void onTangerine(float x, float y, int combo, int points) { }
    default void onGolden(float x, float y, int points) { }
    default void onStomp(float x, float y, int combo, int points) { }
    default void onStun(float x, float y, int points) { }
    default void onFriendLost(float x, float y) { }
    default void onDeath(float x, float y) { }
    default void onRespawn() { }
    default void onSwitch() { }
    default void onCheckpoint(float x, float y) { }
    default void onZenStart() { }
    default void onZenEnd() { }
    default void onFlagLocked() { }
    default void onCrumble(float x, float y) { }
    default void onClear(int bonus) { }
}
