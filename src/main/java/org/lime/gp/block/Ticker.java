package org.lime.gp.block;

public class Ticker {
    /*
    public static class TickEvent extends Event {
        private static final HandlerList handlers = new HandlerList();

        private final WorldServer world;
        private final Chunk chunk;
        private final BlockPosition position;
        private final TileEntity entity;

        protected TickEvent(WorldServer world, Chunk chunk, BlockPosition position, TileEntity entity) {
            this.world = world;
            this.chunk = chunk;
            this.position = position;
            this.entity = entity;
        }

        @Override public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }

        public WorldServer getWorld() { return world; }
        public Chunk getChunk() { return chunk; }
        public BlockPosition getPosition() { return position; }
        public TileEntity getEntity() { return entity; }
    }
    public static CoreElement create() {
        return CoreElement.create(Ticker.class)
                .withInit(Ticker::init);
    }
    public static void init() {
        lime.repeatTicks(Ticker::tickServer, 1);
    }
    private final static World main = Bukkit.getWorlds().get(0);
    private final static WorldServer mainHandle = ((CraftWorld)main).getHandle();
    private final static MinecraftServer server = MinecraftServer.getServer();
    private final static IRegistryCustom.Dimension dimension = server.registryHolder;
    public static void tickServer() {
        server.getAllLevels().forEach(world -> {
            ChunkProviderServer chunkProviderServer = world.getChunkSource();
            ReflectionAccess.entityTickingChunks_ChunkProviderServer.get(chunkProviderServer).iterator().forEachRemaining(chunk -> {
                PlayerChunk holder = chunk.playerChunk;
                if (holder == null || !(boolean)ReflectionAccess.anyPlayerCloseEnoughForSpawning_PlayerChunkMap.call(chunkProviderServer.chunkMap, new Object[] { holder, chunk.getPos(), false })) return;
                tickChunk(world, chunk);
            });
        });
    }
    public static void tickChunk(WorldServer world, Chunk chunk) {
        new HashMap<>(chunk.getBlockEntities()).forEach((position, entity) -> Bukkit.getPluginManager().callEvent(new TickEvent(world, chunk, position, entity)));
    }
    */
}











