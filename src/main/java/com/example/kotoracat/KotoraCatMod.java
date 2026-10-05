package com.example.kotoracat;

import com.example.kotoracat.entity.KotoraCatEntity;
import com.example.kotoracat.registry.KotoraCatEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KotoraCatMod.MODID)
public class KotoraCatMod {
    public static final String MODID = "kotoracat";

    private static long spawnClock = 0L;

    public KotoraCatMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        KotoraCatEntities.ENTITY_TYPES.register(bus);
        KotoraCatEntities.ITEMS.register(bus);
        bus.addListener(KotoraCatMod::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(KotoraCatEntities.KOTORA_CAT.get(), KotoraCatEntity.createAttributes().build());
    }

    /**
     * Rare, server-side world spawning. It intentionally does not depend on
     * vanilla Cat spawning tables, so Kotora can appear independently in worlds.
     */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("kotoracat")
                    .then(Commands.literal("tame").executes(context -> tameNearest(context.getSource())))
                    .then(Commands.literal("tameall").executes(context -> tameAll(context.getSource()))));
        }

        private static int tameNearest(net.minecraft.commands.CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
            Player player = source.getPlayerOrException();
            KotoraCatEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (KotoraCatEntity cat : player.serverLevel().getEntitiesOfClass(
                    KotoraCatEntity.class, player.getBoundingBox().inflate(32.0D), e -> !e.isTame())) {
                double d = player.distanceToSqr(cat);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = cat;
                }
            }
            if (best == null) {
                source.sendFailure(Component.literal("Рядом нет дикого Kotora."));
                return 0;
            }
            tameCat(best, player);
            source.sendSuccess(() -> Component.literal("Kotora приручён!"), true);
            return 1;
        }

        private static int tameAll(net.minecraft.commands.CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
            Player player = source.getPlayerOrException();
            int count = 0;
            for (KotoraCatEntity cat : player.serverLevel().getEntitiesOfClass(
                    KotoraCatEntity.class, player.getBoundingBox().inflate(64.0D), e -> !e.isTame())) {
                tameCat(cat, player);
                count++;
            }
            source.sendSuccess(() -> Component.literal("Приручено Kotora: " + count), true);
            return count;
        }

        private static void tameCat(KotoraCatEntity cat, Player player) {
            cat.tame(player);
            cat.setOrderedToSit(false);
            cat.setInSittingPose(false);
            cat.heal(8.0F);
            cat.setCustomName(Component.literal("Kotora • " + cat.getPersonalityName()));
            cat.level().broadcastEntityEvent(cat, (byte) 61);
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (++spawnClock % 40L != 0L) return; // check twice per second

            var server = event.getServer();
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            if (server.overworld().random.nextInt(180) != 0) return; // rare random moment

            var player = server.getPlayerList().getPlayers().get(server.overworld().random.nextInt(
                    server.getPlayerList().getPlayers().size()));
            ServerLevel level = player.serverLevel();
            if (level.dimension() != LevelKeyHolder.overworldKey(level)) {
                // Kotora may spawn in other dimensions too, but at a lower rate.
                if (level.random.nextInt(3) != 0) return;
            }

            int x = player.blockPosition().getX() + level.random.nextInt(49) - 24;
            int z = player.blockPosition().getZ() + level.random.nextInt(49) - 24;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);

            if (!level.isLoaded(pos) || !level.getBlockState(pos.below()).isSolid()) return;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) return;
            if (level.getBrightness(LightLayer.SKY, pos) < 5 && level.getBrightness(LightLayer.BLOCK, pos) < 5) return;
            if (level.getEntitiesOfClass(KotoraCatEntity.class, player.getBoundingBox().inflate(48), e -> true).size() >= 3) return;

            KotoraCatEntity cat = KotoraCatEntities.KOTORA_CAT.get().create(level);
            if (cat != null) {
                cat.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(cat);
                cat.setCustomName(ComponentNames.name(cat));
            }
        }
    }

    /** Small helper keeps the event class free of registry/version-specific key construction. */
    private static final class LevelKeyHolder {
        private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> overworldKey(ServerLevel level) {
            return net.minecraft.world.level.Level.OVERWORLD;
        }
    }

    private static final class ComponentNames {
        private static net.minecraft.network.chat.Component name(KotoraCatEntity cat) {
            return net.minecraft.network.chat.Component.literal("Kotora • " + cat.getPersonalityName());
        }
    }
}
