package org.lime.gp.module.mobs.spawn;

import org.lime.gp.module.mobs.IMobCreator;
import org.lime.gp.module.mobs.IPopulateSpawn;

import java.util.Optional;

public class EmptySpawn implements ISpawn {
    public static final EmptySpawn Instance = new EmptySpawn();
    private EmptySpawn() {}
    @Override public Optional<IMobCreator> generateMob(IPopulateSpawn populate) { return Optional.empty(); }
}
