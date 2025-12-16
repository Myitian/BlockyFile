package net.myitian.blockyfile.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.myitian.blockyfile.AxisOrder;
import net.myitian.blockyfile.BlockyFile;
import net.myitian.blockyfile.CommandFeedback;
import net.myitian.blockyfile.OperationMode;
import net.myitian.blockyfile.command.BlockPosArgumentEx;
import net.myitian.blockyfile.command.EnumArgument;
import net.myitian.blockyfile.integration.clothconfig.ConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.Objects;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod(value = BlockyFile.MOD_ID, dist = Dist.CLIENT)
public class BlockyFileNeoForgeClient {
    public BlockyFileNeoForgeClient(ModContainer modContainer) {
        if (BlockyFile.CLOTH_CONFIG_EXISTED) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (minecraft, screen) -> ConfigScreen.buildConfigScreen(screen));
        }
        IEventBus eventBus = Objects.requireNonNull(modContainer.getEventBus());
        eventBus.addListener(BlockyFileNeoForgeClient::commonSetup);
        eventBus.addListener(BlockyFileNeoForgeClient::onClientCommandRegister);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(BlockyFile::init);
    }

    public static void onClientCommandRegister(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(literal("blockyfile")
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
                                    CommandSourceStack source = context.getSource();
                                    LocalPlayer player = Minecraft.getInstance().player;
                                    return BlockyFile.executeCommand(mode, pos1, pos2, axisOrder, file, new CommandFeedback() {
                                        @Override
                                        public void sendFeedback(Component msg) {
                                            source.sendSystemMessage(msg);
                                        }

                                        @Override
                                        public void sendError(Component msg) {
                                            source.sendFailure(msg);
                                        }
                                    }, player);
                                })))))));
    }
}