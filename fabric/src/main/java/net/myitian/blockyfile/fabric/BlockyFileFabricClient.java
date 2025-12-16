package net.myitian.blockyfile.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.myitian.blockyfile.AxisOrder;
import net.myitian.blockyfile.BlockyFile;
import net.myitian.blockyfile.CommandFeedback;
import net.myitian.blockyfile.OperationMode;
import net.myitian.blockyfile.command.BlockPosArgumentEx;
import net.myitian.blockyfile.command.EnumArgument;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class BlockyFileFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            literal("blockyfile")
                .then(argument("mode", EnumArgument.enumArg(OperationMode.class))
                    .then(argument("pos1", BlockPosArgumentEx.blockPos())
                        .then(argument("pos2", BlockPosArgumentEx.blockPos())
                            .then(argument("axisOrder", EnumArgument.enumArg(AxisOrder.class))
                                .then(argument("file", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        OperationMode mode = EnumArgument.getEnum(context, "mode", OperationMode.class);
                                        BlockPos pos1 = BlockPosArgumentEx.getBlockPos(context, "pos1");
                                        BlockPos pos2 = BlockPosArgumentEx.getBlockPos(context, "pos2");
                                        AxisOrder axisOrder = EnumArgument.getEnum(context, "axisOrder", AxisOrder.class);
                                        String file = StringArgumentType.getString(context, "file");
                                        FabricClientCommandSource source = context.getSource();
                                        LocalPlayer player = source.getPlayer();
                                        return BlockyFile.executeCommand(mode, pos1, pos2, axisOrder, file, new CommandFeedback() {
                                            @Override
                                            public void sendFeedback(Component msg) {
                                                source.sendFeedback(msg);
                                            }

                                            @Override
                                            public void sendError(Component msg) {
                                                source.sendError(msg);
                                            }
                                        }, player);
                                    }))))))));
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            BlockyFile.init();
        });
    }
}