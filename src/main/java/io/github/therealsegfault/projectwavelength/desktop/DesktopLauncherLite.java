package io.github.therealsegfault.projectwavelength.desktop;

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

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override
    public void render() {
        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
        font.draw(batch, "Rhythm Screen Placeholder", 100, 400);
        font.draw(batch, "Engine goes here", 100, 350);
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
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}