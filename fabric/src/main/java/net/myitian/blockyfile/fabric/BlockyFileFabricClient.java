package net.myitian.blockyfile.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import net.myitian.blockyfile.BlockyFile;
import net.myitian.blockyfile.CommandBuilder;
import net.myitian.blockyfile.CommandFeedback;

public final class BlockyFileFabricClient implements ClientModInitializer {
    private static void commonSetup(Minecraft client) {
        BlockyFile.init();
    }

    private static void onClientCommandRegister(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(new CommandBuilder<>(
            ClientCommandManager::literal,
            ClientCommandManager::argument,
            FabricClientCommandSource::getWorld,
            source -> new CommandFeedback() {
                @Override
                public void sendFeedback(Component msg) {
                    source.sendFeedback(msg);
                }

                @Override
                public void sendError(Component msg) {
                    source.sendError(msg);
                }
            }).build());
    }

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(BlockyFileFabricClient::commonSetup);
        ClientCommandRegistrationCallback.EVENT.register(BlockyFileFabricClient::onClientCommandRegister);
    }
}