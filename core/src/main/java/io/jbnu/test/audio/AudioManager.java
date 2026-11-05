package io.jbnu.test.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.ObjectMap;

/** 효과음과 배경음의 재생, 음소거를 관리합니다. */
public final class AudioManager {
    private static final String[] SFX = {
        "jump", "collect", "golden", "stomp", "hurt", "death", "switch", "checkpoint",
        "zen", "locked", "crumble", "land", "clear", "allclear"
    };
    private static final float MUSIC_VOLUME = 0.45f;

    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private Music music;
    private String musicName;
    private boolean muted;
    private float duck = 1f;

    public AudioManager() {
        for (String name : SFX) {
            sounds.put(name, Gdx.audio.newSound(Gdx.files.internal("audio/" + name + ".wav")));
        }
    }

    public void play(String name) {
        play(name, 0.7f, 1f);
    }

    public void play(String name, float volume, float pitch) {
        if (!muted) sounds.get(name).play(volume, pitch, 0);
    }

    public void playMusic(String name) {
        if (name.equals(musicName)) {
            resumeMusic();
            return;
        }
        stopMusic();
        musicName = name;
        music = Gdx.audio.newMusic(Gdx.files.internal("audio/bgm_" + name + ".wav"));
        music.setLooping(true);
        duck = 1f;
        applyVolume();
        music.play();
    }

    public void stopMusic() {
        if (music != null) {
            music.stop();
            music.dispose();
            music = null;
            musicName = null;
        }
    }

    public void pauseMusic() {
        if (music != null) music.pause();
    }

    public void resumeMusic() {
        if (music != null && !music.isPlaying()) music.play();
    }

    /** Zen 모드처럼 분위기를 바꿀 때 배경음을 줄입니다. */
    public void setDuck(float amount) {
        duck = amount;
        applyVolume();
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean toggleMute() {
        muted = !muted;
        applyVolume();
        return muted;
    }

    private void applyVolume() {
        if (music != null) music.setVolume(muted ? 0 : MUSIC_VOLUME * duck);
    }

    public void dispose() {
        stopMusic();
        for (Sound sound : sounds.values()) sound.dispose();
    }
}
