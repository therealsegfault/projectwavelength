package io.github.therealsegfault.projectwavelength.desktop.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;

import io.github.therealsegfault.projectwavelength.core.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RhythmScreen implements Screen {

    // ── Layout ────────────────────────────────────────────────────
    private static final int WIDTH       = 1280;
    private static final int HEIGHT      = 720;
    private static final int LANES       = 4;
    private static final int LANE_HEIGHT = 100;
    private static final int LANE_TOP    = (HEIGHT - LANES * LANE_HEIGHT) / 2;
    private static final int HIT_LINE_X  = 220;
    private static final int NOTE_R      = 27; // note radius

    // ── Colors ────────────────────────────────────────────────────
    private static final Color[] LANE_COLORS = {
            new Color(0f, 1f, 0.71f, 1f),       // neon teal
            new Color(1f, 0.12f, 0.47f, 1f),    // hot magenta
            new Color(0.16f, 0.63f, 1f, 1f),    // electric blue
            new Color(1f, 0.78f, 0f, 1f),       // hard yellow
    };
    private static final Color[] LANE_BG = {
            new Color(0f, 0.055f, 0.039f, 1f),
            new Color(0.055f, 0f, 0.031f, 1f),
            new Color(0f, 0.024f, 0.071f, 1f),
            new Color(0.055f, 0.039f, 0f, 1f),
    };
    private static final Color BG_COLOR    = new Color(0.016f, 0.016f, 0.031f, 1f);
    private static final Color PANEL_COLOR = new Color(0f, 0f, 0f, 0.63f);

    // ── LibGDX rendering ─────────────────────────────────────────
    private SpriteBatch  batch;
    private ShapeRenderer shapes;
    private BitmapFont   font;
    private BitmapFont   bigFont;
    private BitmapFont   countInFont;
    private Texture      spriteTexture;
    private Music        music;

    // ── Game state ────────────────────────────────────────────────
    private final GameConfig    config;
    private final RhythmEngine  engine;
    private final List<Note>    notes;
    private float songTime = 0f;           // seconds since song start
    private boolean started = false;

    // ── Count-in ──────────────────────────────────────────────────
    private static final String[] BEAT_TEXT = {"ONE","TWO","ONE","TWO","THREE","FOUR"};
    private float msPerBeat;
    private boolean countingIn = true;
    private float countInTimer = 0f;
    private float countInDuration;
    private float lastBeatTime = 0f;
    private int   beatIndex   = 0;
    private String countInLabel = "";

    // ── Particles ─────────────────────────────────────────────────
    private static class Particle {
        float x, y, vx, vy, life;
        Color color;
        Particle(float x, float y, Color c) {
            this.x = x; this.y = y; this.color = new Color(c);
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float speed = MathUtils.random(2f, 6f);
            vx = MathUtils.cos(angle) * speed;
            vy = MathUtils.sin(angle) * speed + 2f; // upward bias
            life = 1f;
        }
        void update(float delta) {
            x += vx; y += vy;
            vy -= 0.18f * 60f * delta; // gravity, frame-independent
            life -= 0.045f * 60f * delta;
        }
        boolean dead() { return life <= 0; }
    }
    private final List<Particle> particles = new ArrayList<>();

    // ── Scroll speed ──────────────────────────────────────────────
    // pixels per ms
    private final float scrollSpeed = (float)(WIDTH - HIT_LINE_X + NOTE_R * 2) / RhythmEngine.APPROACH_TIME_MS;

    // ── Input key map ─────────────────────────────────────────────
    private static final int[] LANE_KEYS = {
            Input.Keys.A, Input.Keys.S, Input.Keys.D, Input.Keys.F
    };
    private final boolean[] laneHeld = new boolean[LANES];

    public RhythmScreen(GameConfig config) {
        this.config = config;

        // Load chart
        List<Note> loaded;
        if (config.useAutochart || config.midiPath == null) {
            loaded = Chart.autoChart(config.audioPath, config.bpm, config.difficulty);
        } else {
            loaded = Chart.loadMidi(config.midiPath, config.difficulty);
        }
        Chart.shiftToApproach(loaded);
        loaded.sort((a, b) -> Long.compare(a.hitTimeMs, b.hitTimeMs));
        this.notes  = loaded;
        this.engine = new RhythmEngine(notes);

        msPerBeat       = 60000f / config.bpm;
        countInDuration = msPerBeat * 6f;
    }

    @Override
    public void show() {
        batch  = new SpriteBatch();
        shapes = new ShapeRenderer();

        // Fonts via FreeType
        try {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
                    Gdx.files.internal("assets/fonts/xa6gecebrahvythq.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = 24; font = gen.generateFont(p);
            p.size = 40; bigFont = gen.generateFont(p);
            gen.dispose();

            FreeTypeFontGenerator gen2 = new FreeTypeFontGenerator(
                    Gdx.files.internal("assets/fonts/PermanentMarker-Regular.ttf"));
            p.size = 96; countInFont = gen2.generateFont(p);
            gen2.dispose();
        } catch (Exception e) {
            font = new BitmapFont();
            bigFont = new BitmapFont();
            countInFont = new BitmapFont();
        }

        // Sprite
        try {
            spriteTexture = new Texture(Gdx.files.internal("assets/sprites/sprite_hit.png"));
        } catch (Exception e) { /* no sprite, fallback drawn */ }

        // Audio
        try {
            music = Gdx.audio.newMusic(Gdx.files.internal(config.audioPath));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void render(float delta) {
        // Update timers (in ms)
        float deltaMs = delta * 1000f;

        if (countingIn) {
            countInTimer += deltaMs;
            if (countInTimer - lastBeatTime >= msPerBeat) {
                lastBeatTime  = countInTimer;
                countInLabel  = beatIndex < BEAT_TEXT.length ? BEAT_TEXT[beatIndex] : "";
                beatIndex++;
            }
            if (countInTimer >= countInDuration) {
                countingIn = false;
                if (music != null) music.play();
                songTime = 0;
            }
        } else {
            songTime += deltaMs;
        }

        long nowMs = (long) songTime;

        // Update engine
        engine.update(nowMs);

        // Update particles
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) { Particle p = it.next(); p.update(delta); if (p.dead()) it.remove(); }

        // Input
        handleInput(nowMs);

        // ── Render ────────────────────────────────────────────────
        ScreenUtils.clear(BG_COLOR);

        // ── PASS 1: All shape rendering ───────────────────────────
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Scanlines
        for (int y = 0; y < HEIGHT; y += 4) {
            shapes.setColor(0f, 0f, 0f, y % 8 == 0 ? 0.16f : 0.06f);
            shapes.rect(0, y, WIDTH, 2);
        }

        // Lane backgrounds
        for (int lane = 0; lane < LANES; lane++) {
            int ly = laneY(lane);
            shapes.setColor(LANE_BG[lane]);
            shapes.rect(HIT_LINE_X, ly, WIDTH - HIT_LINE_X, LANE_HEIGHT);
            Color lc = LANE_COLORS[lane];
            shapes.setColor(lc.r, lc.g, lc.b, 0.8f);
            shapes.rect(HIT_LINE_X, ly + LANE_HEIGHT - 2, WIDTH - HIT_LINE_X, 2);
            for (int gx = 1; gx <= 5; gx++) {
                shapes.setColor(lc.r, lc.g, lc.b, 0.06f / gx);
                shapes.rect(HIT_LINE_X, ly + LANE_HEIGHT - 2 - gx, WIDTH - HIT_LINE_X, 1);
            }
            shapes.setColor(lc.r, lc.g, lc.b, 0.5f);
            shapes.rect(HIT_LINE_X, ly, WIDTH - HIT_LINE_X, 2);
        }

        // Hit line bloom
        float[] hitWidths = {28f, 18f, 10f, 4f, 2f};
        float[] hitAlphas = {0.03f, 0.06f, 0.12f, 0.5f, 1f};
        for (int p = 0; p < hitWidths.length; p++) {
            shapes.setColor(1f, 1f, 1f, hitAlphas[p]);
            shapes.rect(HIT_LINE_X - hitWidths[p] / 2, 0, hitWidths[p], HEIGHT);
        }

        // Hold tails
        for (Note n : notes) {
            if (!n.isHold() || (n.hit && n.holdComplete)) continue;
            if (n.spawnTimeMs > nowMs || nowMs > n.hitTimeMs + n.durationMs) continue;
            int nx    = noteX(n, nowMs);
            int tailX = noteX(n.hitTimeMs + n.durationMs, nowMs);
            int cy    = laneY(n.lane) + LANE_HEIGHT / 2;
            Color lc  = LANE_COLORS[n.lane];
            shapes.setColor(lc.r, lc.g, lc.b, 0.16f);
            shapes.rect(nx, cy - 9, tailX - nx, 18);
            shapes.setColor(lc.r, lc.g, lc.b, 0.7f);
            shapes.rect(nx, cy - 5, tailX - nx, 10);
        }

        // Notes (circles via shapes)
        for (Note n : notes) {
            if (n.hit && (!n.isHold() || n.holdComplete)) continue;
            if (n.spawnTimeMs > nowMs) continue;
            int nx = noteX(n, nowMs);
            if (nx < -NOTE_R * 2 || nx > WIDTH + NOTE_R) continue;
            int cy = laneY(n.lane) + LANE_HEIGHT / 2;
            Color lc = LANE_COLORS[n.lane];
            shapes.setColor(lc.r, lc.g, lc.b, 0.14f);
            shapes.circle(nx, cy, NOTE_R + 10);
            shapes.setColor(lc.r, lc.g, lc.b, 0.27f);
            shapes.circle(nx, cy, NOTE_R + 4);
            shapes.setColor(BG_COLOR.r, BG_COLOR.g, BG_COLOR.b, 1f);
            shapes.circle(nx, cy, NOTE_R);
            shapes.setColor(lc.r, lc.g, lc.b, 1f);
            shapes.circle(nx, cy, NOTE_R - 2);
            shapes.setColor(1f, 1f, 1f, 0.63f);
            shapes.circle(nx, cy, 4);
        }

        // Receptors
        for (int lane = 0; lane < LANES; lane++) {
            Color lc = LANE_COLORS[lane];
            int cy   = laneY(lane) + LANE_HEIGHT / 2;
            boolean held = laneHeld[lane];
            drawDiamond(shapes, HIT_LINE_X, cy, NOTE_R + (held ? 4 : 0),
                    lc, held ? 0.24f : 0.08f, held ? 1f : 0.8f);
        }

        // Character panel background + separator
        shapes.setColor(PANEL_COLOR);
        shapes.rect(0, 0, HIT_LINE_X, HEIGHT);
        Color sep = LANE_COLORS[0];
        for (int gx = 4; gx >= 0; gx--) {
            shapes.setColor(sep.r, sep.g, sep.b, gx == 0 ? 0.78f : 0.06f * gx);
            shapes.rect(HIT_LINE_X - 1 - gx, 0, gx == 0 ? 2 : gx * 2, HEIGHT);
        }

        // HUD panel background
        int bw = 210, bh = 86;
        int rx = WIDTH - bw - 10, ry = HEIGHT - 10 - bh;
        shapes.setColor(0.008f, 0.016f, 0.031f, 0.82f);
        shapes.rect(rx, ry, bw, bh);

        // Particles
        for (Particle p : particles) {
            float a = Math.max(0, p.life);
            shapes.setColor(p.color.r, p.color.g, p.color.b, a * 0.2f);
            shapes.circle(p.x, p.y, 8);
            shapes.setColor(p.color.r, p.color.g, p.color.b, a);
            shapes.circle(p.x, p.y, 3);
        }

        shapes.end();

        // HUD border (Line type)
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(LANE_COLORS[0]);
        shapes.rect(rx, ry, bw, bh);
        shapes.end();

        // ── PASS 2: Single batch pass for all text + sprite ───────
        batch.begin();

        // Sprite
        if (spriteTexture != null) {
            int sw = spriteTexture.getWidth();
            int sh = spriteTexture.getHeight();
            batch.draw(spriteTexture, HIT_LINE_X / 2f - sw / 2f, HEIGHT / 2f - sh / 2f);
        }

        // HUD text
        String scoreStr = String.format("%08d", engine.getScore());
        bigFont.setColor(LANE_COLORS[0]);
        bigFont.draw(batch, scoreStr, rx + 16, ry + bh - 12);
        if (engine.combo > 0) {
            font.setColor(LANE_COLORS[1]);
            font.draw(batch, engine.combo + "  COMBO", rx + 16, ry + 30);
        }

        // Judgement text
        long sysNow = System.currentTimeMillis();
        if (!engine.judgementText.isEmpty() &&
                sysNow - engine.judgementTimer < RhythmEngine.JUDGEMENT_DISPLAY_MS) {
            font.setColor(judgementColor(engine.judgementText));
            font.draw(batch, engine.judgementText,
                    HIT_LINE_X + 40,
                    laneY(LANES / 2) + LANE_HEIGHT / 2f + 30);
        }

        // Count-in
        if (countingIn && !countInLabel.isEmpty()) {
            countInFont.setColor(0f, 1f, 0.71f, 0.86f);
            countInFont.draw(batch, countInLabel,
                    WIDTH / 2f - countInLabel.length() * 28f,
                    HEIGHT / 2f + 60f);
        }

        batch.end();
    }

    // ── Drawing helpers ───────────────────────────────────────────

    private void drawDiamond(ShapeRenderer sr, float cx, float cy, float r,
                             Color color, float fillAlpha, float edgeAlpha) {
        sr.setColor(color.r, color.g, color.b, fillAlpha);
        sr.triangle(cx, cy + r, cx + r, cy, cx, cy - r);
        sr.triangle(cx, cy + r, cx - r, cy, cx, cy - r);
        sr.setColor(color.r, color.g, color.b, edgeAlpha);
        sr.rectLine(cx, cy + r, cx + r, cy, 2f);
        sr.rectLine(cx + r, cy, cx, cy - r, 2f);
        sr.rectLine(cx, cy - r, cx - r, cy, 2f);
        sr.rectLine(cx - r, cy, cx, cy + r, 2f);
    }

    private Color judgementColor(String text) {
        if (text.contains("PERFECT")) return LANE_COLORS[0];
        if (text.contains("GOOD"))    return LANE_COLORS[2];
        return LANE_COLORS[1]; // MISS, DROPPED
    }

    // ── Coordinate helpers ────────────────────────────────────────

    private int laneY(int lane) {
        // LibGDX Y is bottom-up; convert from top-down layout
        return HEIGHT - LANE_TOP - (lane + 1) * LANE_HEIGHT;
    }

    private int noteX(Note n, long nowMs) {
        return (int)(HIT_LINE_X + (n.hitTimeMs - nowMs) * scrollSpeed);
    }

    private int noteX(long timeMs, long nowMs) {
        return (int)(HIT_LINE_X + (timeMs - nowMs) * scrollSpeed);
    }

    // ── Input ─────────────────────────────────────────────────────

    private void handleInput(long nowMs) {
        for (int lane = 0; lane < LANES; lane++) {
            boolean pressed = Gdx.input.isKeyPressed(LANE_KEYS[lane]);
            if (pressed && !laneHeld[lane]) {
                String result = engine.press(lane, nowMs);
                spawnSparkle(lane, result.equals("MISS") ? 4 : 16);
            }
            if (!pressed && laneHeld[lane]) {
                engine.release(lane);
            }
            laneHeld[lane] = pressed;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // TODO: return to menu
            Gdx.app.exit();
        }
    }

    // ── Particles ─────────────────────────────────────────────────

    private void spawnSparkle(int lane, int count) {
        int cy = laneY(lane) + LANE_HEIGHT / 2;
        Color base = LANE_COLORS[lane];
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(HIT_LINE_X, cy, i % 2 == 0 ? base : Color.WHITE));
        }
    }

    // ── Screen lifecycle ──────────────────────────────────────────

    @Override public void resize(int w, int h) {}
    @Override public void pause()  { if (music != null) music.pause(); }
    @Override public void resume() { if (music != null && !countingIn) music.play(); }

    @Override
    public void hide() {
        if (music != null) { music.stop(); music.dispose(); }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
        bigFont.dispose();
        countInFont.dispose();
        if (spriteTexture != null) spriteTexture.dispose();
        if (music != null) music.dispose();
    }
}