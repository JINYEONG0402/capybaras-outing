package io.jbnu.test.world;

/** 라운드를 넘어 유지되는 한 판의 기록입니다. */
public final class RunState {
    public int score, deaths, friends, goldens;
    /** 지금까지 찾은 자식 수입니다. */
    public int children;
    public float time, zen;

    public RunState copy() {
        RunState c = new RunState();
        c.set(this);
        return c;
    }

    public void set(RunState o) {
        score = o.score;
        deaths = o.deaths;
        friends = o.friends;
        goldens = o.goldens;
        children = o.children;
        time = o.time;
        zen = o.zen;
    }

    /** 사망 횟수와 시간으로 최종 등급을 계산합니다. */
    public String rank() {
        if (deaths <= 2 && time <= 420) return "S";
        if (deaths <= 6 && time <= 600) return "A";
        if (deaths <= 14) return "B";
        return "C";
    }
}
