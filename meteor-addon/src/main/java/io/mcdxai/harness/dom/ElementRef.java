package io.mcdxai.harness.dom;

public final class ElementRef {
    public final String id;
    public final Object backing;

    public double x = Double.NaN;
    public double y = Double.NaN;
    public double width = Double.NaN;
    public double height = Double.NaN;

    public String role;
    public String moduleName;
    public String moduleTitle;

    public ElementRef(String id, Object backing) {
        this.id = id;
        this.backing = backing;
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
