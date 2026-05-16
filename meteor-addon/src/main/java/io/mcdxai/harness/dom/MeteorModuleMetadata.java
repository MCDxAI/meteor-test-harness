package io.mcdxai.harness.dom;

public final class MeteorModuleMetadata {
    public final String name;
    public final String title;
    public final String category;
    public final String uiLabel;
    public final boolean active;

    public MeteorModuleMetadata(String name, String title, String category, String uiLabel, boolean active) {
        this.name = name;
        this.title = title;
        this.category = category;
        this.uiLabel = uiLabel;
        this.active = active;
    }
}
