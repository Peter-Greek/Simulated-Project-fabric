package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.foundation.utility.ControlsUtil;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.mixin.accessor.KeyMappingsAccessor;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterDisconnectUser;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterKeyInteractionPacket;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterKeySavePacket;
import dev.simulated_team.simulated.backport.net.VeilPacketManager;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.lwjgl.glfw.GLFW;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Vector;

public class LinkedTypewriterInteractionHandler {

    private static final Vector<Integer> renderPressed = new Vector<>();
    private static final Int2IntMap presetKeys = new Int2IntOpenHashMap();
    private static WeakReference<LinkedTypewriterBlockEntity> TYPEWRITER = new WeakReference<>(null);
    private static Mode MODE = Mode.IDLE;

    static {
        // Hardcoded key locations for renderer
        presetKeys.put(GLFW.GLFW_KEY_Q, 0);
        presetKeys.put(GLFW.GLFW_KEY_W, 1);
        presetKeys.put(GLFW.GLFW_KEY_E, 2);
        presetKeys.put(GLFW.GLFW_KEY_A, 6);
        presetKeys.put(GLFW.GLFW_KEY_S, 7);
        presetKeys.put(GLFW.GLFW_KEY_D, 8);
        presetKeys.put(GLFW.GLFW_KEY_UP, 4);
        presetKeys.put(GLFW.GLFW_KEY_LEFT, 10);
        presetKeys.put(GLFW.GLFW_KEY_DOWN, 11);
        presetKeys.put(GLFW.GLFW_KEY_RIGHT, 12);
        presetKeys.put(GLFW.GLFW_KEY_SPACE, 13);
        presetKeys.put(GLFW.GLFW_KEY_0, 12);
        presetKeys.put(GLFW.GLFW_KEY_KP_0, 12);

        for (int i = 0; i < 9; i++) {
            presetKeys.put(GLFW.GLFW_KEY_1 + i, i);
            presetKeys.put(GLFW.GLFW_KEY_KP_1 + i, i);
        }
    }

    public static void associateTypewriter(final LinkedTypewriterBlockEntity be) {
        if (be == null) {
            MODE = Mode.IDLE;
            stopInteraction();
        } else {
            MODE = Mode.ACTIVE;
        }

        TYPEWRITER = new WeakReference<>(be);
    }

    public static void tick() {
        LinkedTypewriterRenderer.tick();

        if (getMode() == Mode.BINDING_FROM_ITEM) {
            LinkedTypewriterItemBindHandler.tick();
        }

        final LinkedTypewriterBlockEntity be = TYPEWRITER.get();
        if (be == null) {
            return;
        }

        if (getMode() == Mode.ACTIVE &&
                !LinkedTypewriterBlockEntity.playerInRange(Minecraft.getInstance().player, be.getLevel(), be.getBlockPos())) {
            VeilPacketManager.server().sendPacket(new TypewriterDisconnectUser(be.getBlockPos()));
            associateTypewriter(null);
        }

        if (getMode() != Mode.SCREEN_BINDING && Minecraft.getInstance().screen != null && !be.isRemoved()) {
            VeilPacketManager.server().sendPacket(new TypewriterDisconnectUser(be.getBlockPos()));
            associateTypewriter(null);
        }
    }

    private static void stopInteraction() {
        LinkedTypewriterRenderer.resetKeys();
        renderPressed.clear();
    }

    public static void onKeyPress(final int key, final int scanCode, final int action, final int modifiers) {
        final Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        final LinkedTypewriterBlockEntity be = TYPEWRITER.get();

        if (getMode() == Mode.BINDING_FROM_ITEM) {
            LinkedTypewriterItemBindHandler.keyPress(key, scanCode, action, modifiers);
            return;
        }

        if (getMode() != Mode.SCREEN_BINDING) {
            if (be != null && !be.isRemoved()) {
                final LinkedTypewriterEntries.KeyboardEntry frequency = be.getTypewriterEntries().getEntry(key);

                if (key == GLFW.GLFW_KEY_ESCAPE) {
                    be.disconnectUser();
                    VeilPacketManager.server().sendPacket(new TypewriterDisconnectUser(be.getBlockPos()));

                    minecraft.setScreen(null);
                }

                if (frequency != null) {
                    // TODO: cache these that way we don't iterate every keypress and repeat
                    preventPress(key, scanCode);

                    if (action != GLFW.GLFW_REPEAT) {
                        VeilPacketManager.server().sendPacket(new TypewriterKeyInteractionPacket(be.getBlockPos(), key, scanCode, action));
                    }

                    final LocalPlayer player = minecraft.player;
                    if (action == GLFW.GLFW_PRESS) {
                        SimSoundEvents.LINKED_TYPEWRITER_TAP.playAt(player.level(), player.blockPosition(), 1.0F, 1.0F, true);
                        checkKeyCodeAndSetPressed(key, true);
                    } else if (action == GLFW.GLFW_RELEASE) {
                        SimSoundEvents.LINKED_TYPEWRITER_UNTAP.playAt(player.level(), player.blockPosition(), 1.0F, 1.0F, true);
                        checkKeyCodeAndSetPressed(key, false);
                    }
                }

                for (final KeyMapping control : ControlsUtil.getControls()) {
                    if (control.matches(key, scanCode)) {
                        control.consumeClick();
                        control.setDown(false);
                        break;
                    }
                }

            } else {
                MODE = Mode.IDLE;
                stopInteraction();
            }
        }
    }

    public static void preventPress(final int key, final int scanCode) {
        for (final KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (mapping.matches(key, scanCode)) {
                // TODO: there might be a better, more robust way to prevent keystrokes from propagating
                mapping.consumeClick();
                mapping.setDown(false);

                break;
            }
        }
    }

    private static void checkKeyCodeAndSetPressed(final int keycode, final boolean pressed) {
        final int indexPressed;
        if (presetKeys.containsKey(keycode)) {
            indexPressed = presetKeys.get(keycode);
        } else {
            final RandomSource random = RandomSource.create(keycode);
            indexPressed = random.nextInt(13);
        }

        if (pressed) {
            renderPressed.addElement(indexPressed);
        } else {
            renderPressed.removeElement(indexPressed);
        }
    }

    public static Mode getMode() {
        return MODE;
    }

    public static void setMode(final Mode newMode) {
        MODE = newMode;
    }

    public static Vector<Integer> getPressedKeys() {
        return renderPressed;
    }

    /**
     * Saves the keys in a linked controller item to a typewriter
     *
     * @param level    the level the typewriter is in
     * @param blockPos the block position of the typewriter
     * @param item     the linked controller item
     */
    //TODO: refactor this to be significantly more player friendly
    public static void sendLinkedControllerData(final Level level, final BlockPos blockPos, final ItemStack item) {
        final BlockEntity blockEntity = level.getBlockEntity(blockPos);

        if (!(blockEntity instanceof LinkedTypewriterBlockEntity)) {
            return;
        }

        // Create keeps the Linked Controller's twelve frequency slots in a
        // component from 1.20.5 on; on 1.20.1 they are an item handler read off
        // the stack's NBT, which is what its own code uses here.
        final ItemStackHandler linkedControllerData =
                com.simibubi.create.content.redstone.link.controller.LinkedControllerItem.getFrequencyItems(item);

        final ObjectArrayList<ItemStack> linkedControllerItems = new ObjectArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            linkedControllerItems.add(i < linkedControllerData.getSlotCount()
                    ? linkedControllerData.getStackInSlot(i)
                    : ItemStack.EMPTY);
        }

        final Int2ObjectMap<LinkedTypewriterEntries.KeyboardEntry> newKeyBindings = new Int2ObjectOpenHashMap<>();

        int controlIndex = 0;
        for (final KeyMapping mapping : ControlsUtil.getControls()) {
            final int control = ((KeyMappingsAccessor) mapping).getKey().getValue();
            final ItemStack first = linkedControllerItems.get(2 * controlIndex);
            final ItemStack second = linkedControllerItems.get(2 * controlIndex + 1);
            newKeyBindings.put(control, new LinkedTypewriterEntries.KeyboardEntry(RedstoneLinkNetworkHandler.Frequency.of(first),
                    RedstoneLinkNetworkHandler.Frequency.of(second),
                    control,
                    blockPos));
            controlIndex++;
        }

        VeilPacketManager.server().sendPacket(new TypewriterKeySavePacket(newKeyBindings, blockPos, false));
    }

    public enum Mode {
        IDLE, ACTIVE, BIND, SCREEN_BINDING, BINDING_FROM_ITEM
    }
}
