package io.mcdxai.harness.universal.dom;

import io.mcdxai.harness.universal.adapter.ScreenEngine;

public final class ElementRef {
    public final String id;
    public final Object backing;
    public final ScreenEngine engine;

    public double x = Double.NaN;
    public double y = Double.NaN;
    public double width = Double.NaN;
    public double height = Double.NaN;

    public String role;

    public ElementRef(String id, Object backing, ScreenEngine engine) {
        this.id = id;
        this.backing = backing;
        this.engine = engine;
    }

    public boolean clickableCoordinates() {
        return !Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(width) && !Double.isNaN(height) && width > 0 && height > 0;
    }

    public double centerX() {
        return x + width / 2D;
    }

    public double centerY() {
        return y + height / 2D;
    }
}
