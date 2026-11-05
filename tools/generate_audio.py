"""게임에 쓰는 효과음과 배경음(WAV)을 코드로 합성합니다. 외부 라이브러리가 필요 없습니다.

실행: python tools/generate_audio.py  (결과는 assets/audio 에 저장)
"""
import math
import os
import random
import struct
import wave

RATE = 22050
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets", "audio")


def save(name, samples):
    peak = max(1e-6, max(abs(s) for s in samples))
    gain = min(1.0, 0.85 / peak)
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(s * gain * 32767)) for s in samples))


def osc(kind, phase):
    p = phase % 1.0
    if kind == "sine":
        return math.sin(2 * math.pi * p)
    if kind == "square":
        return 1.0 if p < 0.5 else -1.0
    if kind == "pulse":
        return 1.0 if p < 0.25 else -1.0
    if kind == "tri":
        return 4 * abs(p - 0.5) - 1
    return random.uniform(-1, 1)  # noise


def tone(f0, f1, dur, kind="square", vol=0.6, attack=0.005, curve=2.0):
    """f0에서 f1로 음높이가 변하고 지수적으로 줄어드는 소리 한 개."""
    n = int(dur * RATE)
    out, phase = [], 0.0
    for i in range(n):
        t = i / max(1, n - 1)
        phase += (f0 + (f1 - f0) * t) / RATE
        env = min(1.0, (i / RATE) / attack) * (1 - t) ** curve
        out.append(osc(kind, phase) * env * vol)
    return out


def chain(*parts):
    out = []
    for part in parts:
        out.extend(part)
    return out


def mix(*parts):
    out = [0.0] * max(len(p) for p in parts)
    for part in parts:
        for i, s in enumerate(part):
            out[i] += s
    return out


def note_freq(semitone):
    return 440.0 * 2 ** ((semitone - 69) / 12)


def make_sfx():
    save("jump", tone(320, 720, 0.14, "square", 0.45))
    save("collect", chain(tone(990, 990, 0.05, "pulse", 0.5, curve=1), tone(1480, 1480, 0.13, "pulse", 0.5)))
    save("golden", chain(*[tone(note_freq(n), note_freq(n), 0.09, "pulse", 0.5, curve=1) for n in (76, 79, 83, 88, 91)]))
    save("stomp", mix(tone(420, 110, 0.14, "square", 0.6), tone(200, 80, 0.1, "noise", 0.4)))
    save("hurt", chain(tone(620, 300, 0.1, "square", 0.5), tone(520, 180, 0.15, "square", 0.5)))
    save("death", chain(tone(660, 500, 0.12, "tri", 0.7, curve=1), tone(500, 380, 0.12, "tri", 0.7, curve=1), tone(380, 120, 0.3, "tri", 0.7)))
    save("switch", chain(tone(300, 300, 0.05, "square", 0.5, curve=1), tone(600, 600, 0.05, "square", 0.5, curve=1), mix(tone(120, 60, 0.45, "noise", 0.4), tone(180, 90, 0.45, "square", 0.25))))
    save("checkpoint", mix(tone(392, 392, 0.45, "sine", 0.45), chain([0.0] * 2200, tone(523, 523, 0.45, "sine", 0.45)), chain([0.0] * 4400, tone(659, 659, 0.5, "sine", 0.45))))
    save("zen", mix(tone(180, 900, 0.7, "sine", 0.5, attack=0.15, curve=1), chain([0.0] * 3000, tone(660, 1320, 0.55, "tri", 0.3, attack=0.1, curve=1))))
    save("locked", chain(tone(190, 170, 0.1, "square", 0.45, curve=1), [0.0] * 500, tone(170, 150, 0.14, "square", 0.45)))
    save("crumble", tone(240, 90, 0.3, "noise", 0.5, curve=1.2))
    save("land", tone(170, 70, 0.07, "tri", 0.5))
    save("clear", chain(*[tone(note_freq(n), note_freq(n), d, "pulse", 0.5, curve=0.8) for n, d in ((72, .1), (76, .1), (79, .1), (84, .16), (79, .08), (84, .4))]))
    save("allclear", chain(*[tone(note_freq(n), note_freq(n), d, "pulse", 0.5, curve=0.7) for n, d in ((72, .12), (72, .12), (72, .12), (76, .2), (79, .2), (84, .2), (88, .2), (91, .6))]))


def make_song(name, bpm, scale, chords, lead_pattern, lead_kind="pulse", swing=0.0, drums=True):
    """8분음표 칸 단위 패턴으로 루프 음악을 합성합니다. 마디마다 코드 하나를 사용합니다."""
    eighth = 60.0 / bpm / 2
    bars = len(chords)
    length = int(bars * 8 * eighth * RATE)
    buf = [0.0] * length

    def add(start, dur, f, kind, vol, curve=1.5):
        i0 = int(start * RATE)
        n = int(dur * RATE)
        phase = 0.0
        for i in range(n):
            j = i0 + i
            if j >= length:
                break
            t = i / n
            phase += f / RATE
            env = min(1.0, i / 90) * (1 - t) ** curve
            buf[j] += osc(kind, phase) * env * vol

    random.seed(len(name) * 97)
    for bar, degree in enumerate(chords):
        root = scale[degree % len(scale)] + 12 * (degree // len(scale))
        third = scale[(degree + 2) % len(scale)] + 12 * ((degree + 2) // len(scale))
        fifth = scale[(degree + 4) % len(scale)] + 12 * ((degree + 4) // len(scale))
        for step in range(8):
            start = (bar * 8 + step) * eighth + (swing * eighth if step % 2 else 0)
            # 베이스: 근음과 5도를 번갈아 연주
            if step % 2 == 0:
                add(start, eighth * 1.7, note_freq(36 + (root if step % 4 == 0 else fifth)), "tri", 0.34, 0.7)
            # 아르페지오
            arp = (root, third, fifth, third)[step % 4]
            add(start, eighth * 0.9, note_freq(60 + arp), "square", 0.055, 2.0)
            # 멜로디: -1은 쉼표, 값은 코드 근음 기준의 스케일 도수
            pattern = lead_pattern[(bar * 8 + step) % len(lead_pattern)]
            if pattern >= 0:
                d = degree + pattern
                pitch = 72 + scale[d % len(scale)] + 12 * (d // len(scale))
                add(start, eighth * 1.6, note_freq(pitch), lead_kind, 0.15, 1.2)
            if drums:
                if step % 4 == 2:
                    add(start, 0.09, 0, "noise", 0.16, 2.5)
                elif step % 2 == 1:
                    add(start, 0.03, 0, "noise", 0.05, 3)
                if step % 4 == 0:
                    add(start, 0.1, 75, "sine", 0.4, 2)
    save("bgm_" + name, buf)


MAJOR = [0, 2, 4, 5, 7, 9, 11]
MINOR = [0, 2, 3, 5, 7, 8, 10]
PENTA_MINOR = [0, 3, 5, 7, 10, 12, 15]


def make_music():
    make_song("meadow", 124, MAJOR, [0, 3, 4, 0, 5, 3, 1, 4],
              [0, -1, 2, 4, -1, 2, 4, 5, 4, -1, 2, -1, 0, 1, 2, -1], "pulse", 0.1)
    make_song("sunset", 104, MAJOR, [5, 3, 0, 4, 5, 3, 4, 4],
              [4, -1, -1, 2, 4, -1, 5, 4, 2, -1, 0, -1, -1, 1, 2, -1], "tri", 0.2)
    make_song("night", 92, MINOR, [0, 5, 2, 6, 0, 5, 3, 4],
              [-1, 4, -1, 2, -1, 4, 7, -1, 6, -1, 4, -1, 2, -1, -1, -1], "sine", 0.0, False)
    make_song("volcano", 158, PENTA_MINOR, [0, 0, 2, 1, 0, 0, 3, 2],
              [0, 0, 2, -1, 3, -1, 2, 0, 4, -1, 3, 2, -1, 2, 3, -1], "square", 0.0)
    make_song("title", 112, MAJOR, [0, 4, 5, 3],
              [2, 4, 7, 4, 5, -1, 4, -1, 2, 4, 5, 7, 4, -1, -1, -1], "pulse", 0.15)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    make_sfx()
    make_music()
    print("generated", len(os.listdir(OUT)), "files in", os.path.abspath(OUT))
