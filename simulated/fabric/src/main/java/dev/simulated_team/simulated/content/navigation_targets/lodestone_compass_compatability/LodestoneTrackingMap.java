package dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability;

import dev.simulated_team.simulated.backport.net.VeilPacketManager;
import dev.simulated_team.simulated.network.packets.lodestone_compass.UpdateClientLodestonePositionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Where each tracked lodestone sits, so a lodestone compass keeps pointing at it.
 *
 * <p><b>Reduced from upstream.</b> Upstream's whole reason for this map is that a
 * lodestone can be <em>inside a sub-level</em>: it stores a Sable tracking point
 * rather than a block position, follows the body as it moves, and keeps
 * following it while the body is unloaded, by way of the holding chunk map and
 * the saved sub-level pointer. None of that exists on this stack.
 *
 * <p>What is kept is the part that works without physics: a lodestone gets an id
 * the compass stores, the position behind that id is persisted with the level,
 * and it is pushed to clients so the compass needle points correctly. A
 * lodestone in the world therefore behaves exactly as upstream; a lodestone on a
 * moving contraption does not exist yet to behave differently. The sub-level
 * half comes back in V2 — it is recorded in FABRIC_PORT_PLAN.md.
 */
public class LodestoneTrackingMap extends SavedData {

    public static final String FILE_ID = "simulated_lodestone_tracker";

    private final Map<UUID, BlockPos> tracked = new HashMap<>();

    public LodestoneTrackingMap() {
    }

    public static LodestoneTrackingMap getOrLoad(final Level level) {
        if (!(level instanceof final ServerLevel serverLevel)) {
            return new LodestoneTrackingMap();
        }
        return serverLevel.getDataStorage().computeIfAbsent(
                LodestoneTrackingMap::load, LodestoneTrackingMap::new, FILE_ID);
    }

    public static LodestoneTrackingMap load(final CompoundTag tag) {
        final LodestoneTrackingMap map = new LodestoneTrackingMap();
        final ListTag list = tag.getList("Tracked", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            map.tracked.put(entry.getUUID("Id"), NbtUtils.readBlockPos(entry.getCompound("Pos")));
        }
        return map;
    }

    @Override
    public @NotNull CompoundTag save(final @NotNull CompoundTag tag) {
        final ListTag list = new ListTag();
        this.tracked.forEach((id, pos) -> {
            final CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            entry.put("Pos", NbtUtils.writeBlockPos(pos));
            list.add(entry);
        });
        tag.put("Tracked", list);
        return tag;
    }

    /**
     * The id for a lodestone at this position, creating one if it is the first
     * compass to bind to it, so two compasses on the same lodestone share an id.
     */
    public UUID addOrGetLodestoneTrackingPoint(final BlockPos pos) {
        for (final Map.Entry<UUID, BlockPos> entry : this.tracked.entrySet()) {
            if (entry.getValue().equals(pos)) {
                return entry.getKey();
            }
        }

        final UUID id = UUID.randomUUID();
        this.tracked.put(id, pos.immutable());
        this.setDirty();
        return id;
    }

    @Nullable
    public LodestoneInformation getInformation(@Nullable final UUID id) {
        if (id == null) {
            return null;
        }
        final BlockPos pos = this.tracked.get(id);
        if (pos == null) {
            return null;
        }
        return new LodestoneInformation(id, new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    public void sendUpdateForPlayer(final UUID id, final ServerPlayer player) {
        final LodestoneInformation information = this.getInformation(id);
        if (information == null) {
            return;
        }
        VeilPacketManager.player(player).sendPacket(
                new UpdateClientLodestonePositionPacket(information.id(), information.projectedPos()));
    }

    /**
     * Upstream re-reads each tracked point's position every tick, because the
     * body under it may have moved. Nothing moves here, so a tick has nothing to
     * do; the hook stays for V2.
     */
    public void tick() {
    }

    public void remove(final UUID id) {
        if (this.tracked.remove(id) != null) {
            this.setDirty();
        }
    }
}
