package org.lime.gp.module.worlds;

public interface IWorldBorder {
    void reset();

    double get();

    void set(double size);
    void set(double size, double sec);
}
