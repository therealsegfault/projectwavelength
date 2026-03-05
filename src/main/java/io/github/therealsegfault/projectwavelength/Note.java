package io.github.therealsegfault.projectwavelength.core;

public class Note {
    public long hitTimeMs;
    public long durationMs;     // 0 = tap; >0 = hold
    public int  lane;
    public boolean hit          = false;
    public boolean holdComplete = false;
    public long holdStartMs     = -1;
    public long spawnTimeMs;

    private static final long APPROACH_TIME_MS = RhythmEngine.APPROACH_TIME_MS;

    /** Tap note */
    public Note(long hitTimeMs, int lane) {
        this(hitTimeMs, 0, lane);
    }

    /** Hold or tap note */
    public Note(long hitTimeMs, long durationMs, int lane) {
        this.hitTimeMs   = hitTimeMs;
        this.durationMs  = durationMs;
        this.lane        = lane;
        this.spawnTimeMs = hitTimeMs - APPROACH_TIME_MS;
    }

    public boolean isHold() { return durationMs > 0; }
}
