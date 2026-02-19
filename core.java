package io.github.therealsegfault.projectbeatsgdx.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal time-based rhythm engine core.
 * Absolute time (seconds), earliest-hittable-per-lane, difficulty-controlled approach time.
 */
public class RhythmEngine {

    /** Single lane note */
    public static class Note {
        public final int lane;
        public final double hitTime; // seconds
        public final int seq; // stable ordering

        public Note(int lane, double hitTime, int seq) {
            this.lane = lane;
            this.hitTime = hitTime;
            this.seq = seq;
        }
    }

    /** Judgement windows (seconds relative to hitTime) */
    public static class JudgementWindow {
        public final double perfect;
        public final double good;
        public final double safe;
        public final double sad;
        public final double miss;

        public JudgementWindow(double perfect, double good, double safe, double sad, double miss) {
            this.perfect = perfect;
            this.good = good;
            this.safe = safe;
            this.sad = sad;
            this.miss = miss;
        }
    }

    /** Active note with progress tracking */
    private static class ActiveNote {
        final Note note;
        boolean judged = false;

        ActiveNote(Note note) {
            this.note = note;
        }
    }

    private final int laneCount;
    private final List<Note> notes; // sorted ascending by hitTime, seq
    private final JudgementWindow judgementWindow;
    private final double approachTime; // difficulty controlled
    private final List<ActiveNote> activeNotes = new ArrayList<>();

    private int nextNoteIndex = 0;

    public RhythmEngine(int laneCount, List<Note> notes, JudgementWindow judgementWindow, double approachTime) {
        this.laneCount = laneCount;
        this.notes = notes;
        this.judgementWindow = judgementWindow;
        this.approachTime = approachTime;
    }

    /** Call every frame with current absolute time */
    public void update(double currentTime) {
        // Spawn new notes
        while (nextNoteIndex < notes.size() &&
                notes.get(nextNoteIndex).hitTime - approachTime <= currentTime) {
            activeNotes.add(new ActiveNote(notes.get(nextNoteIndex)));
            nextNoteIndex++;
        }

        // Remove notes that expired beyond miss window
        activeNotes.removeIf(an -> !an.judged && currentTime - an.note.hitTime > judgementWindow.miss);
    }

    /**
     * Call when player hits a lane
     * Returns the judgement string
     */
    public String hit(int lane, double currentTime) {
        ActiveNote earliest = null;

        // Find earliest hittable note for the lane
        for (ActiveNote an : activeNotes) {
            if (!an.judged && an.note.lane == lane && an.note.hitTime - judgementWindow.miss <= currentTime) {
                if (earliest == null || an.note.hitTime < earliest.note.hitTime ||
                        (an.note.hitTime == earliest.note.hitTime && an.note.seq < earliest.note.seq)) {
                    earliest = an;
                }
            }
        }

        if (earliest == null) return "none";

        double delta = Math.abs(currentTime - earliest.note.hitTime);
        earliest.judged = true;

        if (delta <= judgementWindow.perfect) return "perfect";
        if (delta <= judgementWindow.good) return "good";
        if (delta <= judgementWindow.safe) return "safe";
        if (delta <= judgementWindow.sad) return "sad";
        return "miss";
    }

    /** Returns progress 0..1 for rendering approach effects */
    public double getNoteProgress(Note note, double currentTime) {
        double progress = (currentTime - (note.hitTime - approachTime)) / approachTime;
        return Math.min(Math.max(progress, 0.0), 1.0);
    }

    public List<ActiveNote> getActiveNotes() {
        return new ArrayList<>(activeNotes);
    }

    /** Quick test main */
    public static void main(String[] args) {
        List<Note> testNotes = List.of(
                new Note(0, 1.0, 0),
                new Note(1, 2.0, 1),
                new Note(0, 3.0, 2)
        );

        RhythmEngine engine = new RhythmEngine(
                2,
                testNotes,
                new JudgementWindow(0.1, 0.25, 0.34, 0.42, 0.5),
                2.0
        );

        double t = 0.0;
        double dt = 0.1;
        while (t < 5.0) {
            engine.update(t);
            if (Math.abs(t - 1.0) < 1e-6) engine.hit(0, t);
            if (Math.abs(t - 2.0) < 1e-6) engine.hit(1, t);
            t += dt;
        }

        System.out.println("Active notes remaining: " + engine.getActiveNotes().size());
    }
}
