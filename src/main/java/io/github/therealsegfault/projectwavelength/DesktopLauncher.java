package io.github.therealsegfault.projectwavelength.desktop;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import io.github.therealsegfault.projectwavelength.desktop.screens.TitleScreen;

public class DesktopLauncher extends Game {

    @Override
    public void create() {
        setScreen(new TitleScreen(this));
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Project Wavelength");
        config.setWindowedMode(1280, 720);
        config.setResizable(false);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new DesktopLauncher(), config);
    }
}
