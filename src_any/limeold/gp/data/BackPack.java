package org.limeold.gp.data;


public class BackPack implements Listener {
    private static final BackPack Instance = new BackPack();
    private BackPack() {}

    private static final BackPackManager BACK_PACK_MANAGER = new BackPackManager();
    private static final HashMap<Material, String> backPacks = new HashMap<>();
    public static void Init() {
        DebugHandlerManager.registerEvents(Instance, lime._plugin);
        if (!lime.ExistConfig("backpack")) lime.WriteAllConfig("backpack", "{}");
        InitFromJson(lime.ReadAllConfig("backpack"));
        Displays.InitDisplay(BACK_PACK_MANAGER);
        lime.RepeatTicks(BACK_PACK_MANAGER::updateRotation, 1);
        lime.Repeat(BACK_PACK_MANAGER::updateHead, 1);
    }
    public static void InitFromJson(String json) {
        HashMap<Material, String> backPacks = new HashMap<>();
        for (Map.Entry<String, JsonElement> kv : new JsonParser().parse(json).getAsJsonObject().entrySet()) {
            String item = kv.getValue().getAsString();
            if (!ItemManager.HasItem(item)) throw new IllegalArgumentException("BACKPACK." + item + " ITEM NOT FOUNDED!");
            backPacks.put(Material.valueOf(kv.getKey()), item);
        }
        BackPack.backPacks.clear();
        BackPack.backPacks.putAll(backPacks);
    }

    private static class BackPackDisplay extends Displays.ObjectDisplay<Player, EntityArmorStand> {
        private final int ownerID;
        private final Player owner;
        private static final net.minecraft.server.v1_16_R3.ItemStack AIR = CraftItemStack.asNMSCopy(new ItemStack(Material.AIR));
        private net.minecraft.server.v1_16_R3.ItemStack head = AIR;
        protected BackPackDisplay(Player data) {
            super(data::getLocation);
            owner = data;
            ownerID = data.getEntityId();
            postInit();
            Displays.addPassengerID(ownerID, entityID);
        }

        @Override protected void Show(Player player) {
            super.Show(player);
            WrapperPlayServerMount mount = new WrapperPlayServerMount();
            mount.setEntityID(ownerID);
            mount.setPassengerIds(Displays.getPassengerIDs(ownerID));
            mount.sendPacket(player);
        }

        public void setHead(ItemStack head) {
            this.head = head == null ? AIR : CraftItemStack.asNMSCopy(head);
        }

        private static byte getAngle(float dat) {
            return (byte)(int)(dat * 256.0F / 360.0F);
        }

        @Override
        public boolean IsFilter(Player player) {
            return Settings.getSettings(player).showBackpack && player.canSee(owner);
        }

        @Override protected void SendData(Player player) {
            Location location = getLocation.invoke();
            PacketPlayOutEntityTeleport ppoet = new PacketPlayOutEntityTeleport(entity);
            PacketPlayOutEntityHeadRotation ppoehr = new PacketPlayOutEntityHeadRotation(entity, getAngle(location.getYaw()));
            PacketPlayOutEntity.PacketPlayOutEntityLook ppoel = new PacketPlayOutEntity.PacketPlayOutEntityLook(entityID, getAngle(location.getYaw()), getAngle(location.getPitch()), false);
            PacketPlayOutEntityEquipment ppoee = new PacketPlayOutEntityEquipment(entityID, Collections.singletonList(new Pair<>(EnumItemSlot.HEAD, head)));
            PacketManager.SendPackets(player, ppoet, ppoee, ppoehr, ppoel);
            SendDataWatcher(player);
        }
        @Override protected EntityArmorStand createEntity(Location location) {
            EntityArmorStand stand = new EntityArmorStand(
                    ((CraftWorld)location.getWorld()).getHandle(),
                    location.getBlockX(), location.getBlockY(), location.getBlockZ());
            stand.setBasePlate(false);
            stand.setSmall(true);
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            return stand;
        }
        public void updateRotation() {
            Location location = getLocation.invoke();
            entity.setPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            if (owner.isSneaking()) entity.setHeadPose(new Vector3f(30, 0, 0));
            else entity.setHeadPose(new Vector3f(0, 0, 0));
            InvokeAll(this::SendData);
        }
        public void updateHead() {
            ItemStack chest = owner.getInventory().getItem(EquipmentSlot.CHEST);
            String item = chest == null ? null : backPacks.getOrDefault(chest.getType(), null);
            setHead(item == null ? null : ItemManager.CreateItem(item));
        }

        public static BackPackDisplay create(UUID uuid, Player player) {
            return new BackPackDisplay(player);
        }
    }
    private static class BackPackManager extends Displays.DisplayManager<UUID, Player, BackPackDisplay> {
        @Override public Map<UUID, Player> getData() { return Bukkit.getOnlinePlayers().stream().filter(p -> p.getGameMode() != GameMode.SPECTATOR).filter(p -> !p.isDead()).filter(p -> !MainSirPlugin.isLay(p)).filter(p -> !p.hasPotionEffect(PotionEffectType.INVISIBILITY)).collect(Collectors.toMap(Entity::getUniqueId, v -> v)); }
        public void updateRotation() {
            this.getDisplays().values().forEach(BackPackDisplay::updateRotation);
        }
        public void updateHead() {
            this.getDisplays().values().forEach(BackPackDisplay::updateHead);
        }
        @Override public BackPackDisplay create(UUID uuid, Player player) {
            return BackPackDisplay.create(uuid, player);
        }
    }

    @EventHandler
    public static void OnLeave(PlayerQuitEvent e) {
        BACK_PACK_MANAGER.remove(e.getPlayer().getUniqueId());
    }
}






