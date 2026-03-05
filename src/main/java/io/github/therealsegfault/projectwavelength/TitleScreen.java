package io.github.therealsegfault.projectwavelength.desktop.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import io.github.therealsegfault.projectwavelength.core.*;
import io.github.therealsegfault.projectwavelength.desktop.DesktopLauncher;

public class TitleScreen implements Screen {

    private final DesktopLauncher game;
    private SpriteBatch batch;
    private BitmapFont  font;

    public TitleScreen(DesktopLauncher game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font  = new BitmapFont();
        font.setColor(Color.WHITE);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.016f, 0.016f, 0.031f, 1f);
        batch.begin();
        font.draw(batch, "PROJECT WAVELENGTH", 100, 500);
        font.draw(batch, "Press ENTER to play", 100, 440);
        font.draw(batch, "Press Q to quit",     100, 400);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            GameConfig config = new GameConfig.Builder()
                .audio("assets/songs/hasurvoicebeentrulylockedaway.wav")
                .midi("assets/midi/wornouttapes.mid")
                .bpm(149)
                .difficulty(Difficulty.NORMAL)
                .build();
            game.setScreen(new RhythmScreen(config));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            Gdx.app.exit();
        }
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
