package io.github.therealsegfault.projectwavelength.core;

import java.util.*;

/**
 * Pure game-logic engine. No rendering, no audio, no LibGDX deps.
 * RhythmScreen feeds it time and input; it returns judgements and state.
 */
public class RhythmEngine {

    // ── Timing constants ──────────────────────────────────────────
    public  static final long APPROACH_TIME_MS  = (long)((60000.0 / 149.0) * 6);
    public  static final long MAX_HIT_WINDOW_MS = 250;
    public  static final long PERFECT_WINDOW_MS = 110;
    public  static final long JUDGEMENT_DISPLAY_MS = 500;
    public  static final long HIT_POSE_DURATION_MS = 150;

    // ── Scoring ───────────────────────────────────────────────────
    public int combo     = 0;
    public int maxCombo  = 0;
    public int perfectCount = 0;
    public int goodCount    = 0;
    public int missCount    = 0;

    public int getScore() {
        return perfectCount * 300 + goodCount * 100;
    }

    public double getAccuracy() {
        int total = perfectCount + goodCount + missCount;
        if (total == 0) return 100.0;
        return ((perfectCount * 1.0) + (goodCount * 0.7)) / total * 100.0;
    }

    // ── Judgement state (read by renderer) ───────────────────────
    public String  judgementText  = "";
    public long    judgementTimer = 0;  // System.currentTimeMillis() of last judgement
    public boolean hitPose        = false;
    public long    hitPoseTimer   = 0;
    public boolean waveHitPose    = false;
    public long    waveHitPoseTimer = 0;

    // ── Notes & input state ───────────────────────────────────────
    private final List<Note> notes;
    private final Set<Integer> heldLanes = new HashSet<>();

    public RhythmEngine(List<Note> notes) {
        this.notes = new ArrayList<>(notes);
    }

    public List<Note> getNotes() { return notes; }

    // ── Called every frame by RhythmScreen ───────────────────────
    public void update(long nowMs) {
        for (Note n : notes) {
            if (!n.hit) continue;
            if (!n.isHold() || n.holdStartMs < 0 || n.holdComplete) continue;

            boolean stillHeld = heldLanes.contains(n.lane);
            long holdEnd = n.hitTimeMs + n.durationMs;

            if (nowMs >= holdEnd) {
                n.holdComplete = true;
                combo++;
                if (combo > maxCombo) maxCombo = combo;
                perfectCount++;
                judgementText  = "PERFECT HOLD";
                judgementTimer = System.currentTimeMillis();

            } else if (!stillHeld) {
                n.holdComplete = true;
                long held = nowMs - n.holdStartMs;
                float frac = (float) held / n.durationMs;
                if (frac >= 0.5f) {
                    combo++;
                    if (combo > maxCombo) maxCombo = combo;
                    goodCount++;
                    judgementText = "GOOD HOLD";
                } else {
                    combo = 0;
                    missCount++;
                    judgementText = "DROPPED";
                }
                judgementTimer = System.currentTimeMillis();
            }
        }

        // Auto-miss notes that have scrolled past the window
        for (Note n : notes) {
            if (!n.hit && !n.holdComplete && nowMs > n.hitTimeMs + MAX_HIT_WINDOW_MS) {
                n.hit = true; // mark as handled
                combo = 0;
                missCount++;
                judgementText  = "MISS";
                judgementTimer = System.currentTimeMillis();
            }
        }
    }

    // ── Input ─────────────────────────────────────────────────────
    /** Returns judgement string for this press. */
    public String press(int lane, long nowMs) {
        heldLanes.add(lane);

        Note candidate = null;
        long bestOffset = Long.MAX_VALUE;
        for (Note n : notes) {
            if (n.hit || n.lane != lane) continue;
            long offset = Math.abs(n.hitTimeMs - nowMs);
            if (offset <= MAX_HIT_WINDOW_MS && offset < bestOffset) {
                candidate   = n;
                bestOffset  = offset;
            }
        }

        if (candidate != null) {
            long offset = Math.abs(candidate.hitTimeMs - nowMs);
            candidate.hit = true;

            if (candidate.isHold()) {
                candidate.holdStartMs = nowMs;
                judgementText  = "HOLD!";
                judgementTimer = System.currentTimeMillis();
                return "HOLD";
            } else {
                combo++;
                if (combo > maxCombo) maxCombo = combo;
                boolean perfect = offset <= PERFECT_WINDOW_MS;
                if (perfect) { perfectCount++; judgementText = "PERFECT"; }
                else         { goodCount++;    judgementText = "GOOD";    }
                judgementTimer    = System.currentTimeMillis();
                hitPose           = true;
                hitPoseTimer      = System.currentTimeMillis();
                if (lane == 0) {
                    waveHitPose      = true;
                    waveHitPoseTimer = System.currentTimeMillis();
                }
                printStats();
                return perfect ? "PERFECT" : "GOOD";
            }
        } else {
            combo          = 0;
            missCount++;
            judgementText  = "MISS";
            judgementTimer = System.currentTimeMillis();
            hitPose        = true;
            hitPoseTimer   = System.currentTimeMillis();
            printStats();
            return "MISS";
        }
    }

    public void release(int lane) {
        heldLanes.remove(lane);
    }

    // ── Helpers ───────────────────────────────────────────────────
    private void printStats() {
        int total = perfectCount + goodCount + missCount;
        if (total == 0) return;
        System.out.printf("Combo: %d | Max: %d | P: %d | G: %d | M: %d | Acc: %.2f%%%n",
            combo, maxCombo, perfectCount, goodCount, missCount, getAccuracy());
    }
}
