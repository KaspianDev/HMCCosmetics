package io.github.fisher2911.hmccosmetics.user;

import com.comphenix.protocol.events.PacketContainer;
import io.github.fisher2911.hmccosmetics.HMCCosmetics;
import io.github.fisher2911.hmccosmetics.config.Settings;
import io.github.fisher2911.hmccosmetics.config.WardrobeSettings;
import io.github.fisher2911.hmccosmetics.gui.ArmorItem;
import io.github.fisher2911.hmccosmetics.inventory.PlayerArmor;
import io.github.fisher2911.hmccosmetics.packet.PacketManager;
import io.github.fisher2911.hmccosmetics.task.SupplierTask;
import io.github.fisher2911.hmccosmetics.task.Task;
import io.github.fisher2911.hmccosmetics.task.TaskChain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Wardrobe extends User {

    private final HMCCosmetics plugin;
    private final UUID ownerUUID;
    private final int viewerId;
    private boolean active;
    private boolean cameraLocked;

    private boolean spawned;

    private Location currentLocation;

    public Wardrobe(
            final HMCCosmetics plugin,
            final UUID uuid,
            final int entityId,
            final UUID ownerUUID,
            final PlayerArmor playerArmor,
            final int armorStandId,
            final int viewerId,
            final boolean active) {
        super(uuid, entityId, playerArmor, armorStandId);
        this.plugin = plugin;
        this.ownerUUID = ownerUUID;
        this.viewerId = viewerId;
        this.active = active;
        this.wardrobe = this;
    }

    public void spawnFakePlayer(final Player viewer) {
        final WardrobeSettings settings = this.plugin.getSettings().getWardrobeSettings();
        if (settings.inDistanceOfStatic(viewer.getLocation())) {
            this.currentLocation = settings.getWardrobeLocation();
            new TaskChain(this.plugin).chain(
                    () -> {
                        viewer.teleport(settings.getViewerLocation());
                        this.cameraLocked = true;
                        this.hidePlayer();
                    }
            ).execute();
            // for if we ever switch to packets
//            final Location viewerLocation = settings.getViewerLocation();
//            final UUID viewerUUID = UUID.randomUUID();
//            new TaskChain(this.plugin).chain(() -> {
//                viewer.setGameMode(GameMode.SPECTATOR);
//            }).chain(
//                    () -> {
//                        PacketManager.sendPacket(
//                                viewer,
//                                PacketManager.getEntitySpawnPacket(
//                                        viewerLocation,
//                                        this.viewerId,
//                                        EntityType.ZOMBIE,
//                                        viewerUUID
//                                ),
//                                PacketManager.getLookPacket(this.viewerId, viewerLocation),
//                                PacketManager.getRotationPacket(this.viewerId, viewerLocation),
//                                PacketManager.getSpectatePacket(this.viewerId)
//                                );
//                    },
//                    true
//            ).execute();


        } else if (this.currentLocation == null) {
            this.currentLocation = viewer.getLocation().clone();
            this.currentLocation.setPitch(0);
            this.currentLocation.setYaw(0);
        } else if (this.spawned) {
            return;
        }

        final PacketContainer playerSpawnPacket = PacketManager.getFakePlayerSpawnPacket(
                this.currentLocation,
                this.getUuid(),
                this.getEntityId()
        );
        final PacketContainer playerInfoPacket = PacketManager.getFakePlayerInfoPacket(
                viewer,
                this.getUuid()
        );

        PacketManager.sendPacket(viewer, playerInfoPacket, playerSpawnPacket);
        this.spawnArmorStand(viewer, this.currentLocation, this.plugin.getSettings().getCosmeticSettings());
        this.updateArmorStand(viewer, plugin.getSettings(), this.currentLocation);
        PacketManager.sendPacket(viewer, PacketManager.getLookPacket(this.getEntityId(), this.currentLocation));
        PacketManager.sendPacket(viewer, PacketManager.getRotationPacket(this.getEntityId(), this.currentLocation));

        this.spawned = true;
        this.startSpinTask(viewer);
    }

    @Override
    public void updateArmorStand(final Player player, final Settings settings) {
        this.updateArmorStand(player, settings, this.currentLocation);
    }

    public void despawnFakePlayer(final Player viewer) {
        final WardrobeSettings settings = this.plugin.getSettings().getWardrobeSettings();
        PacketManager.sendPacket(
                viewer,
                PacketManager.getEntityDestroyPacket(this.getEntityId())
                // for spectator packets
//                PacketManager.getEntityDestroyPacket(this.viewerId)
        );
        this.showPlayer(this.plugin.getUserManager());
        this.despawnAttached();
        this.active = false;
        this.spawned = false;
        this.cameraLocked = false;
        this.currentLocation = null;
        this.getPlayerArmor().clear();

        if (settings.isAlwaysDisplay()) {
            this.currentLocation = settings.getWardrobeLocation();
            if (this.currentLocation == null) return;
            this.spawnFakePlayer(viewer);
        }
    }

    private void startSpinTask(final Player player) {
        final AtomicInteger data = new AtomicInteger();
        final int rotationSpeed = this.plugin.getSettings().getWardrobeSettings().getRotationSpeed();
        final Task task = new SupplierTask(
                () -> {
                    if (this.currentLocation == null) return;
                    final Location location = this.currentLocation.clone();
                    final int yaw = data.get();
                    location.setYaw(yaw);
                    PacketManager.sendPacket(player, PacketManager.getLookPacket(this.getEntityId(), location));
                    this.updateArmorStand(player, this.plugin.getSettings(), location);
                    location.setYaw(this.getNextYaw(yaw - 30, rotationSpeed));
                    PacketManager.sendPacket(player, PacketManager.getRotationPacket(this.getEntityId(), location));
                    data.set(this.getNextYaw(yaw, rotationSpeed));
                },
                () -> !this.spawned || this.currentLocation == null
        );
        this.plugin.getTaskManager().submit(task);
    }

    private int getNextYaw(final int current, final int rotationSpeed) {
        if (current + rotationSpeed > 179) return -179;
        return current + rotationSpeed;
    }

    public boolean isCameraLocked() {
        return cameraLocked;
    }

    @Override
    public boolean hasPermissionToUse(final ArmorItem armorItem) {
        return true;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public void setCurrentLocation(final Location currentLocation) {
        this.currentLocation = currentLocation;
    }

    @Nullable
    public Location getCurrentLocation() {
        return currentLocation;
    }

    @Override
    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(this.ownerUUID);
    }

    private void hidePlayer() {
        final Player player = this.getPlayer();
        if (player == null) return;
        for (final Player p : Bukkit.getOnlinePlayers()) {
            p.hidePlayer(this.plugin, player);
            player.hidePlayer(this.plugin, p);
        }
    }

    private void showPlayer(final UserManager userManager) {
        final Player player = this.getPlayer();
        if (player == null) return;
        final Optional<User> optionalUser = userManager.get(player.getUniqueId());
        for (final Player p : Bukkit.getOnlinePlayers()) {
            final Optional<User> optional = userManager.get(p.getUniqueId());
            if (optional.isEmpty()) continue;
            if (optional.get().getWardrobe().isActive()) continue;
            player.showPlayer(this.plugin, p);
            p.showPlayer(this.plugin, player);
            Bukkit.getScheduler().runTaskLaterAsynchronously(
                    this.plugin,
                    () -> optional.ifPresent(user -> userManager.updateCosmetics(user, player)),
                    1
            );
            Bukkit.getScheduler().runTaskLaterAsynchronously(
                    this.plugin,
                    () -> optionalUser.ifPresent(userManager::updateCosmetics),
                    1
            );
        }
    }

}
