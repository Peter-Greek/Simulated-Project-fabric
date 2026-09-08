"""One-time, exact-match continuation edits; never re-copy upstream sources."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'simulated/fabric/src/main/java/dev/simulated_team/simulated'

def edit(name, old, new):
    p = ROOT / name
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise RuntimeError('Missing edit anchor: ' + name + '\n' + old[:120])
    p.write_text(text.replace(old, new), encoding='utf-8')

def guard(name, condition, signature='public void handle(final ServerPacketContext context) {'):
    edit(name, signature, signature + '\n        if (' + condition + ') return;')
    edit(name, '\nimport ', '\nimport ', ) if False else None
    p = ROOT / name
    text = p.read_text(encoding='utf-8')
    if 'import dev.simulated_team.simulated.network.PacketValidation;' not in text:
        text = text.replace('\nimport ', '\nimport dev.simulated_team.simulated.network.PacketValidation;\nimport ', 1)
    p.write_text(text, encoding='utf-8')

P = 'network/packets/'
for name, pos in [('AssemblePacket', 'pos'), ('ThrottleLeverSignalPacket', 'pos'),
                  ('SteeringWheelPacket', 'pos'), ('linked_typewriter/TypewriterKeyInteractionPacket', 'interactionPos'),
                  ('linked_typewriter/TypewriterKeySavePacket', 'pos'),
                  ('name_plate/NameplateChangeNamePacket', 'controllerPos')]:
    guard(P + name + '.java', '!PacketValidation.canInteract(context.player(), this.' + pos + ')')
guard(P+'BlockEntityObservedPacket.java', '!context.level().isLoaded(this.pos)')
guard(P+'linked_typewriter/TypewriterDisconnectUser.java', '!context.level().isLoaded(this.pos)')
guard(P+'SteeringWheelPacket.java', '!Float.isFinite(this.targetAngle) || Math.abs(this.targetAngle) > 360')
guard(P+'ThrottleLeverSignalPacket.java', 'this.signal < 0 || this.signal > 15')
guard(P+'linked_typewriter/TypewriterKeyInteractionPacket.java', '!PacketValidation.validKey(this.key) || (this.action != 0 && this.action != 1)')
edit(P+'linked_typewriter/TypewriterKeyInteractionPacket.java', 'final boolean pressed =', 'if (!typeWriter.checkUser(context.player().getUUID())) return;\n            final boolean pressed =')
guard(P+'linked_typewriter/TypewriterKeySavePacket.java', 'this.changedKeys.size() > 317 || this.changedKeys.entrySet().stream().anyMatch(e -> !PacketValidation.validKey(e.getKey()) || e.getValue().glfwKeyCode != e.getKey())')
guard(P+'linked_typewriter/TypewriterSaveKeyToItemPacket.java', '!PacketValidation.validKey(this.entry.glfwKeyCode) || !context.player().getItemInHand(this.hand).is(SimBlocks.LINKED_TYPEWRITER.asItem()) || context.player().isSpectator()')
edit(P+'linked_typewriter/TypewriterMenuModifySlots.java', 'player.containerMenu instanceof final LinkedTypewriterMenuCommon menu)', 'player.containerMenu instanceof final LinkedTypewriterMenuCommon menu && menu.stillValid(player))')
edit(P+'linked_typewriter/TypewriterMenuModifySlots.java', 'menu.ghostInventory.setStackInSlot(0, this.first);\n            menu.ghostInventory.setStackInSlot(1, this.second);', 'final ItemStack firstCopy = this.first.copy();\n            final ItemStack secondCopy = this.second.copy();\n            firstCopy.setCount(1);\n            secondCopy.setCount(1);\n            menu.ghostInventory.setStackInSlot(0, firstCopy);\n            menu.ghostInventory.setStackInSlot(1, secondCopy);')
guard(P+'name_plate/NameplateChangeNamePacket.java', 'this.name != null && (this.name.length() > 64 || this.name.chars().anyMatch(Character::isISOControl))')
guard(P+'UpdatePlayerUsingHandlePacket.java', '!Float.isFinite(this.desiredRange) || this.desiredRange < -1 || this.desiredRange > 5', 'public void handle(final ServerPacketContext ctx) {')
edit(P+'UpdatePlayerUsingHandlePacket.java', 'final BlockEntity be = level.getBlockEntity(this.interactionPos);', 'if (this.remove) ServerHandleHoldingHandler.stopHolding(player);\n        if (!PacketValidation.canInteract(player, this.interactionPos)) return;\n        final BlockEntity be = level.getBlockEntity(this.interactionPos);')

helper = P+'helpers/SimBlockEntityConfigurationPacket.java'
edit(helper, 'private final BlockPos pos;', 'private final BlockPos pos;\n    private final Class<T> entityClass;')
edit(helper, 'public SimBlockEntityConfigurationPacket(final BlockPos pos) {\n        this.pos = pos;', 'public SimBlockEntityConfigurationPacket(final BlockPos pos, final Class<T> entityClass) {\n        this.pos = pos;\n        this.entityClass = entityClass;')
guard(helper, '!PacketValidation.canInteract(context.player(), this.pos)')
edit(helper, 'if (blockEntity instanceof SyncedBlockEntity) {\n                    this.applySettings(player, (T) blockEntity);', 'if (this.entityClass.isInstance(blockEntity)) {\n                    this.applySettings(player, this.entityClass.cast(blockEntity));')
for name, entity in [('ConfigureAltitudeSensorPacket', 'AltitudeSensorBlockEntity'), ('ConfigureModulatingLinkedRecieverPacket', 'ModulatingLinkedReceiverBlockEntity')]:
    edit(P+name+'.java', 'super(pos);', 'super(pos, '+entity+'.class);')
edit(P+'ConfigureAltitudeSensorPacket.java', 'final AltitudeSensorBlockEntity abe = be;', 'if (!Float.isFinite(this.highSignal) || !Float.isFinite(this.lowSignal)) return;\n            final AltitudeSensorBlockEntity abe = be;')
edit(P+'ConfigureModulatingLinkedRecieverPacket.java', 'final ModulatingLinkedReceiverBlockEntity abe = be;', 'if (this.minRange < 1 || this.maxRange > 256 || this.minRange > this.maxRange) return;\n            final ModulatingLinkedReceiverBlockEntity abe = be;')

# Draining removals matters when rebinding a key which is currently transmitting.
edit('content/blocks/redstone/linked_typewriter/LinkedTypewriterEntries.java', 'this.newlyDeactivatedKeyboardEntries.clear();\n        this.newlyActivatedKeyboardEntries.clear();', 'this.newlyActivatedKeyboardEntries.clear();')

# Check both attachment faces before placement, and restore original states on failure.
for name, method in [('PlaceSpringPacket', 'addSpring'), ('PlaceMergingGluePacket', 'addMergingGlue')]:
    file = P+name+'.java'
    guard(file, '!PacketValidation.canInteract(ctx.player(), this.parentPos, 40) || !PacketValidation.canInteract(ctx.player(), this.childPos)', 'public void handle(final ServerPacketContext ctx) {')
    anchor = 'final BlockPos childRelative = this.childPos().relative(this.childFacing);'
    edit(file, anchor, anchor + '''
        if (parentRelative.equals(childRelative)
                || !PacketValidation.canInteract(player, parentRelative, 40)
                || !PacketValidation.canInteract(player, childRelative)
                || !player.mayUseItemAt(parentRelative, this.parentFacing, player.getItemInHand(this.hand))
                || !player.mayUseItemAt(childRelative, this.childFacing, player.getItemInHand(this.hand))
                || !level.getBlockState(parentRelative).canBeReplaced()
                || !level.getBlockState(childRelative).canBeReplaced()
                || level.getBlockState(this.parentPos).isAir()
                || level.getBlockState(this.childPos).isAir()) return;
        final BlockState previousParent = level.getBlockState(parentRelative);
        final BlockState previousChild = level.getBlockState(childRelative);''')
    edit(file, 'level.setBlockAndUpdate(parentRelative, Blocks.AIR.defaultBlockState());\n            level.setBlockAndUpdate(childRelative, Blocks.AIR.defaultBlockState());', 'level.setBlockAndUpdate(parentRelative, previousParent);\n            level.setBlockAndUpdate(childRelative, previousChild);')
edit(P+'PlaceSpringPacket.java', 'parentSpring.setController(controller);', 'if (parentSpring == null) return null;\n            parentSpring.setController(controller);')

# Reject malformed allocation sizes before allocating any collection.
edit('backport/net/ByteBufCodecs.java', 'final int size = new FriendlyByteBuf(asByteBuf(buf)).readVarInt();', 'final int size = new FriendlyByteBuf(asByteBuf(buf)).readVarInt();\n                    if (size < 0 || size > 65536 || size > asByteBuf(buf).readableBytes()) {\n                        throw new DecoderException("Invalid collection size");\n                    }')
edit('backport/net/VeilPacketManager.java', 'final T packet = codec.decode(wrap(buf, server.registryAccess()));\n            server.execute(() -> handler.handle(packet, serverContext(player)));', '''if (buf.readableBytes() > 65536) return;
            final T packet;
            try {
                packet = codec.decode(wrap(buf, server.registryAccess()));
                if (buf.isReadable()) return;
            } catch (RuntimeException malformed) {
                return;
            }
            server.execute(() -> {
                if (!player.isRemoved() && player.connection == listener) {
                    handler.handle(packet, serverContext(player));
                }
            });''')
edit('backport/net/VeilPacketManager.java', 'client.execute(() -> handler.handle(packet, clientContext()));', 'client.execute(() -> {\n                            if (client.level != null && client.player != null && client.getConnection() == listener)\n                                handler.handle(packet, clientContext());\n                        });')

# V1 diagrams can be configured on ordinary blocks, with explicitly empty physics data.
edit(P+'contraption_diagram/DiagramSaveConfigPacket.java', 'entity.distanceToSqr(context.player()) < 64.0 * 64.0', 'dev.simulated_team.simulated.network.PacketValidation.canInteract(context.player(), entity)')
edit(P+'contraption_diagram/DiagramSaveConfigPacket.java', '            final SubLevel subLevel = Sable.HELPER.getContaining(diagram);\n            if (subLevel == null) return;\n', '')
edit('content/entities/diagram/DiagramItem.java', 'stack.shrink(1);', 'if (!world.isClientSide && (player == null || !player.getAbilities().instabuild)) stack.shrink(1);')

print('Applied v1 continuation edits.')

