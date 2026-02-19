package io.github.therealsegfault.projectwavelength.desktop;

import io.github.therealsegfault.projectwavelength.core.RhythmEngine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class DesktopLauncherLite extends ApplicationAdapter {

    enum ScreenState {
        TITLE,
        RHYTHM,
        WORLD,
        PARTY,
        SOUND_CHECK
    }

    private ScreenState currentScreen = ScreenState.TITLE;

    private SpriteBatch batch;
    private BitmapFont font;

    private RhythmEngine engine;
    private double songTime = 0.0;
    private final int laneCount = 4;
    private final boolean[] laneHeld = new boolean[laneCount];
    private String lastJudgement = "";

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();

        // Test chart for RhythmEngine
        java.util.List<RhythmEngine.Note> testNotes = java.util.List.of(
                new RhythmEngine.Note(0, 1.0, 1.0, 0),   // tap
                new RhythmEngine.Note(1, 2.0, 3.5, 1),   // hold
                new RhythmEngine.Note(2, 4.0, 4.0, 2),   // tap
                new RhythmEngine.Note(3, 5.0, 6.0, 3)    // hold
        );

        engine = new RhythmEngine(
                testNotes,
                laneCount,
                2.0,
                new RhythmEngine.JudgementWindow(0.05, 0.10, 0.15, 0.20, 0.25)
        );
    }

    @Override
    public void render() {
        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float delta = Gdx.graphics.getDeltaTime();
        if (currentScreen == ScreenState.RHYTHM && engine != null) {
            songTime += delta;
            engine.update(songTime);
        }

        batch.begin();
        switch (currentScreen) {
            case TITLE -> renderTitleScreen();
            case RHYTHM -> renderRhythmScreen();
            case WORLD -> renderWorldScreen();
            case PARTY -> renderPartyScreen();
            case SOUND_CHECK -> renderSoundCheckScreen();
        }
        batch.end();

        handleInput();
    }

    private void renderTitleScreen() {
        font.draw(batch, "TITLE SCREEN", 100, 400);
        font.draw(batch, "1. Rhythm", 100, 350);
        font.draw(batch, "2. World", 100, 300);
        font.draw(batch, "3. Options", 100, 250);
        font.draw(batch, "4. Quit", 100, 200);
    }

    private void renderRhythmScreen() {
        font.draw(batch, "RHYTHM MODE (ASDF)", 100, 450);
        font.draw(batch, "Time: " + String.format("%.2f", songTime), 100, 420);
        font.draw(batch, "Lane 0: A", 100, 380);
        font.draw(batch, "Lane 1: S", 100, 350);
        font.draw(batch, "Lane 2: D", 100, 320);
        font.draw(batch, "Lane 3: F", 100, 290);
        font.draw(batch, "Score: " + engine.getScoreSummary(), 100, 250);
        font.draw(batch, "Last: " + lastJudgement, 100, 220);
    }

    private void renderWorldScreen() {
        font.draw(batch, "WORLD / Open World Placeholder", 100, 400);
        font.draw(batch, "Character moving in test world", 100, 350);
    }

    private void renderPartyScreen() {
        font.draw(batch, "PARTY / INVENTORY Placeholder", 100, 400);
        font.draw(batch, "Manage instruments, characters, etc.", 100, 350);
    }

    private void renderSoundCheckScreen() {
        font.draw(batch, "SOUND CHECK", 100, 400);
        font.draw(batch, "Band on stage - test sounds", 100, 350);
        font.draw(batch, "Edit feel / minimal test map", 100, 300);
    }

    private void handleInput() {
        if (currentScreen == ScreenState.TITLE) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) currentScreen = ScreenState.RHYTHM;
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) currentScreen = ScreenState.WORLD;
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) currentScreen = ScreenState.PARTY;
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) Gdx.app.exit();
        }

        // Press ESC to go back to title from any screen
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (currentScreen != ScreenState.TITLE) currentScreen = ScreenState.TITLE;
        }

        if (currentScreen == ScreenState.RHYTHM && engine != null) {
            int[] keys = {Input.Keys.A, Input.Keys.S, Input.Keys.D, Input.Keys.F};

            for (int lane = 0; lane < laneCount; lane++) {
                boolean currentlyPressed = Gdx.input.isKeyPressed(keys[lane]);

                // Key just pressed
                if (currentlyPressed && !laneHeld[lane]) {
                    lastJudgement = engine.press(lane, songTime);
                }

                // Key just released
                if (!currentlyPressed && laneHeld[lane]) {
                    lastJudgement = engine.release(lane, songTime);
                }

                laneHeld[lane] = currentlyPressed;
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }

    public static void main(String[] args) {
        com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration config =
                new com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration();
        config.setTitle("Wavelength Lite");
        config.setWindowedMode(800, 600);
        // config.setHdpiMode(com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.HdpiMode.Logical);
        new com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application(new DesktopLauncherLite(), config);
    }
}