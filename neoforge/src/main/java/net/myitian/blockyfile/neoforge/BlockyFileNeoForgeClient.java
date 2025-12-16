package net.myitian.blockyfile.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.myitian.blockyfile.BlockyFile;
import net.myitian.blockyfile.CommandFeedback;
import net.myitian.blockyfile.integration.clothconfig.ConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = BlockyFile.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = BlockyFile.MOD_ID, value = Dist.CLIENT)
public final class BlockyFileNeoForgeClient {
    public BlockyFileNeoForgeClient(ModContainer modContainer) {
        if (BlockyFile.CLOTH_CONFIG_EXISTED) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (minecraft, screen) -> ConfigScreen.buildConfigScreen(screen));
        }
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        BlockyFile.init();
    }

    @SubscribeEvent
    public static void onClientCommandRegister(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(BlockyFile.buildCommandArguments(
            Commands::literal,
            Commands::argument,
            CommandSourceStack::getUnsidedLevel,
            source -> new CommandFeedback() {
                @Override
                public void sendFeedback(Component msg) {
                    source.sendSystemMessage(msg);
                }

                @Override
                public void sendError(Component msg) {
                    source.sendFailure(msg);
                }
            }));
    }
}