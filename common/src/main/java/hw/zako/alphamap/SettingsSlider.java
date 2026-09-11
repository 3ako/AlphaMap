package hw.zako.alphamap;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

abstract class SettingsSlider extends AbstractSliderButton {

    SettingsSlider(int x, int y, int width, int height, double value) {
        super(x, y, width, height, Component.empty(), value);
        updateMessage();
    }
}
