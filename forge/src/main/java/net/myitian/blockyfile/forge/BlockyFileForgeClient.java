package net.myitian.blockyfile.forge;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.myitian.blockyfile.BlockyFile;
import net.myitian.blockyfile.CommandBuilder;
import net.myitian.blockyfile.CommandFeedback;

@Mod(BlockyFile.MOD_ID)
@Mod.EventBusSubscriber(modid = BlockyFile.MOD_ID, value = Dist.CLIENT)
public final class BlockyFileForgeClient {
    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        BlockyFile.init();
    }

    @SubscribeEvent
    public static void onClientCommandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(new CommandBuilder<>(
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
            }).build());
    }
}