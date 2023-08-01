package org.lime.gp.item.projectile;

import org.lime.display.DisplayManager;

import java.util.Map;

public class ProjectileDisplayManager extends DisplayManager<Integer, ProjectileFrame, ProjectileDisplay> {
    @Override public boolean isAsync() { return true; }
    @Override public boolean isFast() { return true; }

    private final Map<Integer, ProjectileFrame> data;
    public ProjectileDisplayManager(Map<Integer, ProjectileFrame> data) { this.data = data; }
    @Override public Map<Integer, ProjectileFrame> getData() { return this.data; }
    @Override public ProjectileDisplay create(Integer integer, ProjectileFrame projectileFrame) {
        return new ProjectileDisplay(projectileFrame);
    }
}
