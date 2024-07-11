package io.github.sakurawald.module.initializer.resource_world;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.sakurawald.Fuji;
import io.github.sakurawald.config.Configs;
import io.github.sakurawald.config.model.ConfigModel;
import io.github.sakurawald.module.initializer.ModuleInitializer;
import io.github.sakurawald.module.initializer.newbie_welcome.random_teleport.RandomTeleport;
import io.github.sakurawald.module.initializer.resource_world.interfaces.DimensionOptionsMixinInterface;
import io.github.sakurawald.module.initializer.resource_world.interfaces.SimpleRegistryMixinInterface;
import io.github.sakurawald.util.CommandUtil;
import io.github.sakurawald.util.MessageUtil;
import io.github.sakurawald.util.ScheduleUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.EndPlatformFeature;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.util.Optional;

import static net.minecraft.server.command.CommandManager.literal;

public class ResourceWorldInitializer extends ModuleInitializer {

    private final String DEFAULT_RESOURCE_WORLD_NAMESPACE = "resource_world";
    private final String DEFAULT_THE_NETHER_PATH = "the_nether";
    private final String DEFAULT_THE_END_PATH = "the_end";
    private final String DEFAULT_OVERWORLD_PATH = "overworld";

    @Override
    public void onInitialize() {
        ServerWorldEvents.UNLOAD.register(this::onWorldUnload);
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            this.loadWorlds(server);
            this.registerScheduleTask(server);
        });
    }

    public void registerScheduleTask(MinecraftServer server) {
        ScheduleUtil.addJob(ResourceWorldAutoResetJob.class, null, null, Configs.configHandler.model().modules.resource_world.auto_reset_cron, new JobDataMap() {
            {
                this.put(MinecraftServer.class.getName(), server);
                this.put(ResourceWorldInitializer.class.getName(), ResourceWorldInitializer.this);
            }
        });
    }

    @SuppressWarnings("unused")
    @Override
    public void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                CommandManager.literal("rw")
                        .then(literal("reset").requires(source -> source.hasPermissionLevel(4)).executes(this::$reset))
                        .then(literal("delete").requires(source -> source.hasPermissionLevel(4)).then(literal(DEFAULT_OVERWORLD_PATH).executes(this::$delete))
                                .then(literal(DEFAULT_THE_NETHER_PATH).executes(this::$delete))
                                .then(literal(DEFAULT_THE_END_PATH).executes(this::$delete)))
                        .then(literal("tp").then(literal(DEFAULT_OVERWORLD_PATH).executes(this::$tp))
                                .then(literal(DEFAULT_THE_NETHER_PATH).executes(this::$tp))
                                .then(literal(DEFAULT_THE_END_PATH).executes(this::$tp))
                        )
        );
    }

    private int $reset(CommandContext<ServerCommandSource> ctx) {
        resetWorlds(ctx.getSource().getServer());
        return Command.SINGLE_SUCCESS;
    }

    private void resetWorlds(MinecraftServer server) {
        MessageUtil.sendBroadcast("resource_world.world.reset");
        Configs.configHandler.model().modules.resource_world.seed = RandomSeed.getSeed();
        Configs.configHandler.saveToDisk();
        deleteWorld(server, DEFAULT_OVERWORLD_PATH);
        deleteWorld(server, DEFAULT_THE_NETHER_PATH);
        deleteWorld(server, DEFAULT_THE_END_PATH);
    }

    public void loadWorlds(MinecraftServer server) {
        long seed = Configs.configHandler.model().modules.resource_world.seed;

        ConfigModel.Modules.ResourceWorld.ResourceWorlds resourceWorlds = Configs.configHandler.model().modules.resource_world.resource_worlds;
        if (resourceWorlds.enable_overworld) {
            createWorld(server, DimensionTypes.OVERWORLD, DEFAULT_OVERWORLD_PATH, seed);
        }
        if (resourceWorlds.enable_the_nether) {
            createWorld(server, DimensionTypes.THE_NETHER, DEFAULT_THE_NETHER_PATH, seed);
        }
        if (resourceWorlds.enable_the_end) {
            createWorld(server, DimensionTypes.THE_END, DEFAULT_THE_END_PATH, seed);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private ChunkGenerator getChunkGenerator(MinecraftServer server, RegistryKey<DimensionType> dimensionTypeRegistryKey) {
        if (dimensionTypeRegistryKey == DimensionTypes.OVERWORLD) {
            return server.getWorld(World.OVERWORLD).getChunkManager().getChunkGenerator();
        }
        if (dimensionTypeRegistryKey == DimensionTypes.THE_NETHER) {
            return server.getWorld(World.NETHER).getChunkManager().getChunkGenerator();
        }
        if (dimensionTypeRegistryKey == DimensionTypes.THE_END) {
            return server.getWorld(World.END).getChunkManager().getChunkGenerator();
        }
        return null;
    }

    private DimensionOptions createDimensionOptions(MinecraftServer server, RegistryKey<DimensionType> dimensionTypeRegistryKey) {
        RegistryEntry<DimensionType> dimensionTypeRegistryEntry = getDimensionTypeRegistryEntry(server, dimensionTypeRegistryKey);
        ChunkGenerator chunkGenerator = getChunkGenerator(server, dimensionTypeRegistryKey);
        //noinspection DataFlowIssue
        return new DimensionOptions(dimensionTypeRegistryEntry, chunkGenerator);
    }

    private RegistryEntry<DimensionType> getDimensionTypeRegistryEntry(MinecraftServer server, RegistryKey<DimensionType> dimensionTypeRegistryKey) {
        return server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).getEntry(dimensionTypeRegistryKey).orElse(null);
    }

    private RegistryKey<DimensionType> getDimensionTypeRegistryKeyByPath(String path) {
        if (path.equals(DEFAULT_OVERWORLD_PATH)) return DimensionTypes.OVERWORLD;
        if (path.equals(DEFAULT_THE_NETHER_PATH)) return DimensionTypes.THE_NETHER;
        if (path.equals(DEFAULT_THE_END_PATH)) return DimensionTypes.THE_END;
        return null;
    }


    private SimpleRegistry<DimensionOptions> getDimensionOptionsRegistry(MinecraftServer server) {
        DynamicRegistryManager registryManager = server.getCombinedDynamicRegistries().getCombinedRegistryManager();
        return (SimpleRegistry<DimensionOptions>) registryManager.get(RegistryKeys.DIMENSION);
    }

    @SuppressWarnings("deprecation")
    private void createWorld(MinecraftServer server, RegistryKey<DimensionType> dimensionTypeRegistryKey, String path, long seed) {
        /* create the world */
        // note: we use the same WorldData from OVERWORLD
        ResourceWorldProperties resourceWorldProperties = new ResourceWorldProperties(server.getSaveProperties(), seed);
        RegistryKey<World> worldRegistryKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(DEFAULT_RESOURCE_WORLD_NAMESPACE, path));
        DimensionOptions dimensionOptions = createDimensionOptions(server, dimensionTypeRegistryKey);
        ServerWorld world = new ResourceWorld(server,
                Util.getMainWorkerExecutor(),
                server.session,
                resourceWorldProperties,
                worldRegistryKey,
                dimensionOptions,
                VoidWorldGenerationProgressListener.INSTANCE,
                false,
                BiomeAccess.hashSeed(seed),
                ImmutableList.of(),
                true,
                null);

        if (dimensionTypeRegistryKey == DimensionTypes.THE_END) {
            world.setEnderDragonFight(new EnderDragonFight(world, world.getSeed(), EnderDragonFight.Data.DEFAULT));
        }

        /* register the world */
        ((DimensionOptionsMixinInterface) (Object) dimensionOptions).fuji$setSaveProperties(false);

        SimpleRegistry<DimensionOptions> dimensionsRegistry = getDimensionOptionsRegistry(server);
        boolean isFrozen = ((SimpleRegistryMixinInterface<?>) dimensionsRegistry).fuji$isFrozen();
        ((SimpleRegistryMixinInterface<?>) dimensionsRegistry).fuji$setFrozen(false);
        var dimensionOptionsRegistryKey = RegistryKey.of(RegistryKeys.DIMENSION, worldRegistryKey.getValue());
        if (!dimensionsRegistry.contains(dimensionOptionsRegistryKey)) {
            dimensionsRegistry.add(dimensionOptionsRegistryKey, dimensionOptions, RegistryEntryInfo.DEFAULT);
        }
        ((SimpleRegistryMixinInterface<?>) dimensionsRegistry).fuji$setFrozen(isFrozen);

        server.worlds.put(world.getRegistryKey(), world);
        ServerWorldEvents.LOAD.invoker().onWorldLoad(server, world);
        world.tick(() -> true);
        MessageUtil.sendBroadcast("resource_world.world.created", path);
    }

    private ServerWorld getResourceWorldByPath(MinecraftServer server, String path) {
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(DEFAULT_RESOURCE_WORLD_NAMESPACE, path));
        return server.getWorld(worldKey);
    }

    private int $tp(CommandContext<ServerCommandSource> ctx) {
        return CommandUtil.playerOnlyCommand(ctx, player -> {
            String path = ctx.getNodes().get(2).getNode().getName();
            ServerWorld world = getResourceWorldByPath(ctx.getSource().getServer(), path);
            if (world == null) {
                MessageUtil.sendMessage(ctx.getSource(), "resource_world.world.not_found", path);
                return 0;
            }

            Optional<RegistryKey<DimensionType>> type = world.getDimensionEntry().getKey();
            if (type.isPresent() && type.get() == DimensionTypes.THE_END) {
                this.createEndSpawnPlatform(world);

                BlockPos endSpawnPos = ServerWorld.END_SPAWN_POS;
                player.teleport(world, endSpawnPos.getX() + 0.5, endSpawnPos.getY(), endSpawnPos.getZ() + 0.5, 90, 0);
            } else {
                MessageUtil.sendActionBar(player, "resource_world.world.tp.tip");
                RandomTeleport.randomTeleport(player, world, false);
            }

            return Command.SINGLE_SUCCESS;
        });
    }

    private void createEndSpawnPlatform(ServerWorld world) {
        BlockPos endSpawnPos = ServerWorld.END_SPAWN_POS;
        Vec3d vec3d = endSpawnPos.toBottomCenterPos();
        EndPlatformFeature.generate(world, BlockPos.ofFloored(vec3d).down(), true);
    }

    private void deleteWorld(MinecraftServer server, String path) {
        ServerWorld world = getResourceWorldByPath(server, path);
        if (world == null) return;

        ResourceWorldManager.enqueueWorldDeletion(world);
        MessageUtil.sendBroadcast("resource_world.world.deleted", path);
    }

    private int $delete(CommandContext<ServerCommandSource> ctx) {
        String path = ctx.getNodes().get(2).getNode().getName();
        deleteWorld(ctx.getSource().getServer(), path);
        return Command.SINGLE_SUCCESS;
    }

    public void onWorldUnload(MinecraftServer server, ServerWorld world) {
        if (server.isRunning()) {
            String namespace = world.getRegistryKey().getValue().getNamespace();
            String path = world.getRegistryKey().getValue().getPath();
            // IMPORTANT: only delete the world if it's a resource world
            if (!namespace.equals(DEFAULT_RESOURCE_WORLD_NAMESPACE)) return;

            Fuji.LOGGER.info("onWorldUnload() -> Creating world {} ...", path);
            long seed = Configs.configHandler.model().modules.resource_world.seed;
            this.createWorld(server, this.getDimensionTypeRegistryKeyByPath(path), path, seed);
        }
    }

    public static class ResourceWorldAutoResetJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            Fuji.LOGGER.info("Start to reset resource worlds.");
            MinecraftServer server = (MinecraftServer) context.getJobDetail().getJobDataMap().get(MinecraftServer.class.getName());
            ResourceWorldInitializer module = (ResourceWorldInitializer) context.getJobDetail().getJobDataMap().get(ResourceWorldInitializer.class.getName());
            server.execute(() -> module.resetWorlds(server));
        }
    }
}