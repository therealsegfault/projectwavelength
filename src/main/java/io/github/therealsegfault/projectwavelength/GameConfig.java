package io.github.therealsegfault.projectwavelength.core;

public class GameConfig {
    public final String audioPath;      // path to WAV/MP3
    public final String midiPath;       // null if autochart
    public final String videoPath;      // null if no background video
    public final int    bpm;
    public final Difficulty difficulty;
    public final boolean useAutochart;
    public final boolean showMode;      // cinematic show mode (no lane UI)

    private GameConfig(Builder b) {
        this.audioPath    = b.audioPath;
        this.midiPath     = b.midiPath;
        this.videoPath    = b.videoPath;
        this.bpm          = b.bpm;
        this.difficulty   = b.difficulty;
        this.useAutochart = b.useAutochart;
        this.showMode     = b.showMode;
    }

    public static class Builder {
        private String audioPath;
        private String midiPath     = null;
        private String videoPath    = null;
        private int    bpm          = 120;
        private Difficulty difficulty = Difficulty.NORMAL;
        private boolean useAutochart = false;
        private boolean showMode     = false;

        public Builder audio(String path)           { this.audioPath    = path;  return this; }
        public Builder midi(String path)            { this.midiPath     = path;  return this; }
        public Builder video(String path)           { this.videoPath    = path;  return this; }
        public Builder bpm(int bpm)                 { this.bpm         = bpm;   return this; }
        public Builder difficulty(Difficulty d)     { this.difficulty  = d;     return this; }
        public Builder autochart(boolean v)         { this.useAutochart = v;    return this; }
        public Builder showMode(boolean v)          { this.showMode    = v;     return this; }
        public GameConfig build()                   { return new GameConfig(this); }
    }
}
