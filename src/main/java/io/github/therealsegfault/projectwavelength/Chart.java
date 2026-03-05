package io.github.therealsegfault.projectwavelength.core;

import javax.sound.midi.*;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.*;

/**
 * Loads charts from MIDI files or generates them from audio via envelope detection.
 */
public class Chart {

    private static final int LANES = 4;

    // ── MIDI loader ───────────────────────────────────────────────
    public static List<Note> loadMidi(String filename, Difficulty difficulty) {
        List<Note> loadedNotes = new ArrayList<>();
        try {
            Sequence sequence = MidiSystem.getSequence(new File(filename));
            int minPitch = Integer.MAX_VALUE, maxPitch = Integer.MIN_VALUE;
            int resolution = sequence.getResolution();

            // First pass: find pitch range
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    if (!(event.getMessage() instanceof ShortMessage sm)) continue;
                    if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                        int pitch = sm.getData1();
                        minPitch = Math.min(minPitch, pitch);
                        maxPitch = Math.max(maxPitch, pitch);
                    }
                }
            }
            if (minPitch > maxPitch) return loadedNotes;

            int laneSize = Math.max(1, (maxPitch - minPitch + 1) / LANES);

            // Second pass: pair NOTE_ON / NOTE_OFF into notes with durations
            Map<Integer, Long> noteOnMs = new HashMap<>();
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    if (!(event.getMessage() instanceof ShortMessage sm)) continue;
                    int pitch = sm.getData1();
                    long ms   = (long)((event.getTick() * 60000.0) / (resolution * 120));
                    boolean isOn  = sm.getCommand() == ShortMessage.NOTE_ON  && sm.getData2() > 0;
                    boolean isOff = sm.getCommand() == ShortMessage.NOTE_OFF
                                 || (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0);
                    if (isOn) {
                        noteOnMs.put(pitch, ms);
                    } else if (isOff && noteOnMs.containsKey(pitch)) {
                        long onMs = noteOnMs.remove(pitch);
                        long dur  = ms - onMs;
                        int lane  = Math.min((pitch - minPitch) / laneSize, LANES - 1);
                        // Only make it a hold if duration > 80ms
                        loadedNotes.add(new Note(onMs, dur > 80 ? dur : 0, lane));
                    }
                }
            }

            loadedNotes.sort(Comparator.comparingLong(n -> n.hitTimeMs));
            loadedNotes = applyDifficulty(loadedNotes, difficulty);

        } catch (Exception e) { e.printStackTrace(); }
        return loadedNotes;
    }

    // ── Autochart ─────────────────────────────────────────────────
    public static List<Note> autoChart(String audioFile, int bpm, Difficulty difficulty) {
        List<Note> notes = new ArrayList<>();
        try {
            var ais    = AudioSystem.getAudioInputStream(new File(audioFile));
            var format = ais.getFormat();
            boolean bigEndian   = format.isBigEndian();
            int bytesPerSample  = format.getSampleSizeInBits() / 8;
            float sampleRate    = format.getSampleRate();
            int frameSize       = format.getFrameSize();
            byte[] audioBytes   = ais.readAllBytes();
            int totalFrames     = audioBytes.length / frameSize;
            int windowSize      = (int)(sampleRate * 0.02);

            // Build RMS envelope
            double[] envelope = new double[totalFrames / windowSize + 1];
            for (int w = 0; w < envelope.length; w++) {
                double sum = 0; int count = 0;
                int fStart = w * windowSize;
                int fEnd   = Math.min((w + 1) * windowSize, totalFrames);
                for (int f = fStart; f < fEnd; f++) {
                    int idx = f * frameSize;
                    int sample = 0;
                    if (bytesPerSample == 2) {
                        int lo = audioBytes[idx] & 0xFF, hi = audioBytes[idx + 1] & 0xFF;
                        sample = bigEndian ? (hi << 8) | lo : (lo << 8) | hi;
                        if (sample > 32767) sample -= 65536;
                    } else if (bytesPerSample == 1) {
                        sample = (audioBytes[idx] & 0xFF) - 128;
                    }
                    sum += sample * (double) sample; count++;
                }
                envelope[w] = count > 0 ? Math.sqrt(sum / count) : 0;
            }
            double maxEnv = Arrays.stream(envelope).max().orElse(1.0);
            if (maxEnv > 0) for (int i = 0; i < envelope.length; i++) envelope[i] /= maxEnv;

            // Difficulty controls density
            double threshold;
            long   minSep;
            int    maxChordSize;
            switch (difficulty) {
                case EASY  -> { threshold = 0.15; minSep = 300; maxChordSize = 1; }
                case HARD  -> { threshold = 0.02; minSep =  60; maxChordSize = 2; }
                default    -> { threshold = 0.06; minSep = 120; maxChordSize = 1; }
            }

            // Peak detection
            List<Integer> peaks = new ArrayList<>();
            for (int i = 1; i < envelope.length - 1; i++) {
                if (envelope[i] > threshold) { peaks.add(i); i += 4; }
            }

            // Snap to 16th notes
            double msPerSixteenth = (60000.0 / bpm) / 4.0;
            Set<Long> snapped = new HashSet<>();
            for (int idx : peaks) {
                double timeMs  = idx * windowSize * 1000.0 / sampleRate;
                long snapIdx   = Math.round(timeMs / msPerSixteenth);
                snapped.add((long) Math.round(snapIdx * msPerSixteenth));
            }

            // Filter by minimum separation
            List<Long> sorted = new ArrayList<>(snapped);
            Collections.sort(sorted);
            List<Long> filtered = new ArrayList<>();
            long last = -minSep - 1;
            for (long t : sorted) {
                if (t - last >= minSep) { filtered.add(t); last = t; }
            }

            // Round-robin lane assignment
            Random rng = new Random(audioFile.hashCode());
            int[] laneOrder = {0, 1, 2, 3};
            for (int i = 3; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int tmp = laneOrder[i]; laneOrder[i] = laneOrder[j]; laneOrder[j] = tmp;
            }
            int laneIdx = 0;
            for (long t : filtered) {
                int chordsAtTime = 1 + (maxChordSize > 1 && rng.nextFloat() < 0.2f ? 1 : 0);
                Set<Integer> usedLanes = new HashSet<>();
                for (int c = 0; c < chordsAtTime; c++) {
                    int tries = 0;
                    while (usedLanes.contains(laneOrder[laneIdx % LANES]) && tries < LANES) {
                        laneIdx++; tries++;
                    }
                    int lane = laneOrder[laneIdx++ % LANES];
                    usedLanes.add(lane);
                    notes.add(new Note(t, lane));
                }
            }
            System.out.println("Autochart [" + difficulty + "]: " + notes.size() + " notes");

        } catch (Exception e) { e.printStackTrace(); }
        return notes;
    }

    // ── Shared difficulty filter ──────────────────────────────────
    private static List<Note> applyDifficulty(List<Note> input, Difficulty difficulty) {
        long minSpacing; int maxChordSize;
        switch (difficulty) {
            case EASY   -> { minSpacing = 250; maxChordSize = 1; }
            case HARD   -> { minSpacing = 80;  maxChordSize = LANES; }
            default     -> { minSpacing = 150; maxChordSize = 2; }
        }

        // Spacing filter
        List<Note> spaced = new ArrayList<>();
        long lastTime = -9999;
        for (Note n : input) {
            if (n.hitTimeMs - lastTime >= minSpacing) {
                spaced.add(n);
                lastTime = n.hitTimeMs;
            }
        }

        // Chord size cap
        Map<Long, List<Note>> byTime = new LinkedHashMap<>();
        for (Note n : spaced)
            byTime.computeIfAbsent(n.hitTimeMs, k -> new ArrayList<>()).add(n);

        List<Note> result = new ArrayList<>();
        for (List<Note> group : byTime.values()) {
            group.sort(Comparator.comparingInt(n -> n.lane));
            for (int i = 0; i < Math.min(maxChordSize, group.size()); i++)
                result.add(group.get(i));
        }
        result.sort(Comparator.comparingLong(n -> n.hitTimeMs));

        // Hold proximity enforcement: after a hold ends, next note within window must be tap
        long lastHoldEndMs = -9999;
        for (Note n : result) {
            if (n.hitTimeMs - lastHoldEndMs < RhythmEngine.MAX_HIT_WINDOW_MS)
                n.durationMs = 0;
            if (n.isHold())
                lastHoldEndMs = n.hitTimeMs + n.durationMs;
        }

        return result;
    }

    // ── Approach time shift ───────────────────────────────────────
    /** Shifts notes so the first note reaches the hit line after APPROACH_TIME_MS. */
    public static void shiftToApproach(List<Note> notes) {
        if (notes.isEmpty()) return;
        long firstHit = notes.stream().mapToLong(n -> n.hitTimeMs).min().orElse(0);
        long shift    = RhythmEngine.APPROACH_TIME_MS - firstHit;
        for (Note n : notes) {
            n.hitTimeMs  += shift;
            n.spawnTimeMs = n.hitTimeMs - RhythmEngine.APPROACH_TIME_MS;
        }
    }
}
