package io.github.therealsegfault.projectwavelength.core;

import java.util.ArrayList;
import java.util.List;

public class RhythmEngine {
    private List<Note> notes;
    private int laneCount;
    private double songLength;
    private JudgementWindow judgementWindow;

    private int score;

    public RhythmEngine(List<Note> notes, int laneCount, double songLength, JudgementWindow judgementWindow) {
        this.notes = new ArrayList<>(notes);
        this.laneCount = laneCount;
        this.songLength = songLength;
        this.judgementWindow = judgementWindow;
        this.score = 0;
    }

    public void update(double currentTime) {
        // Update logic for notes, scoring, etc.
    }

    public String press(int lane, double time) {
        // Logic for pressing a lane at a given time
        // Return judgement string
        return "Hit";
    }

    public String release(int lane, double time) {
        // Logic for releasing a lane at a given time
        // Return judgement string
        return "Released";
    }

    public String getScoreSummary() {
        return Integer.toString(score);
    }

    public static class Note {
        public int lane;
        public double startTime;
        public double endTime;
        public int id;

        public Note(int lane, double startTime, double endTime, int id) {
            this.lane = lane;
            this.startTime = startTime;
            this.endTime = endTime;
            this.id = id;
        }
    }

    public static class JudgementWindow {
        public double perfect;
        public double great;
        public double good;
        public double bad;
        public double miss;

        public JudgementWindow(double perfect, double great, double good, double bad, double miss) {
            this.perfect = perfect;
            this.great = great;
            this.good = good;
            this.bad = bad;
            this.miss = miss;
        }
    }
}