package dev.stardust.gui.widgets.meteorites;

import net.minecraft.util.StringIdentifiable;

public enum FieldSize implements StringIdentifiable {
    Small(new int[]{640, 480, 3, 32, 160}), Medium(new int[]{1280, 720, 6, 72, 200}),
    Large(new int[]{1600, 900, 12, 136, 240}), Huge(new int[]{1920, 1080, 24, 272, 280});

    private final int[] values;

    FieldSize(int[] values) {
        this.values = values;
    }

    public int getWidth() {
        return values[0];
    }
    public int getHeight() {
        return values[1];
    }
    public int getMinAsteroids() {
        return values[2];
    }
    public int getMaxAsteroids() {
        return values[3];
    }
    public int getShipThrustAmount() {
        return values[4];
    }

    @Override
    public String asString() {
        return switch (this) {
            case Small -> "Small";
            case Medium -> "Medium";
            case Large -> "Large";
            case Huge -> "Huge";
        };
    }
}
