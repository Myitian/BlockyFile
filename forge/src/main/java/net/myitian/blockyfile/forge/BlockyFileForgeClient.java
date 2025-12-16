package net.myitian.blockyfile.forge;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.myitian.blockyfile.AxisOrder;
import net.myitian.blockyfile.BlockyFile;
import net.myitian.blockyfile.CommandFeedback;
import net.myitian.blockyfile.OperationMode;
import net.myitian.blockyfile.command.BlockPosArgumentEx;
import net.myitian.blockyfile.command.EnumArgument;
import net.myitian.blockyfile.integration.clothconfig.ConfigScreen;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod(BlockyFile.MOD_ID)
public class BlockyFileForgeClient {
    public BlockyFileForgeClient(FMLJavaModLoadingContext context) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        if (BlockyFile.CLOTH_CONFIG_EXISTED) {
            context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ConfigScreen.buildConfigScreen(parent))
            );
        }
        IEventBus eventBus = context.getModEventBus();
        eventBus.addListener(BlockyFileForgeClient::commonSetup);
        eventBus.addListener(BlockyFileForgeClient::onClientCommandRegister);
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