package net.myitian.blockyfile.integration.clothconfig;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.myitian.blockyfile.BlockyFile;
import net.myitian.blockyfile.config.Config;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public final class ConfigScreen {
    public static Screen buildConfigScreen(Screen parent) {
        File configFile = BlockyFile.CONFIG_PATH.toFile();
        Config.load(configFile);
        BlockyFile.loadPalette();
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("title.blockyfile.config"))
            .setSavingRunnable(() -> {
                Config.save(configFile);
                BlockyFile.loadPalette();
            });
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        createCommandCategory(builder, entryBuilder);
        createPaletteCategory(builder, entryBuilder);
        return builder.build();
    }

    private static void createCommandCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("category.blockyfile.command"));
        category.addEntry(entryBuilder.startStrField(
                Component.translatable("option.blockyfile.command"),
                Config.getCommand())
            .setDefaultValue(Config::defaultCommand)
            .setTooltip(Component.translatable("option.blockyfile.command.tooltip"))
            .setSaveConsumer(Config::setCommand)
            .build());
        category.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("option.blockyfile.forceCommand"),
                Config.isForceCommand())
            .setDefaultValue(Config::defaultForceCommand)
            .setTooltip(Component.translatable("option.blockyfile.forceCommand.tooltip"))
            .setSaveConsumer(Config::setForceCommand)
            .build());
        category.addEntry(entryBuilder.startLongField(
                Component.translatable("option.blockyfile.commandInterval"),
                Config.getCommandInterval())
            .setDefaultValue(Config::defaultCommandInterval)
            .setTooltip(Component.translatable("option.blockyfile.commandInterval.tooltip"))
            .setSaveConsumer(Config::setCommandInterval)
            .build());
    }

    private static void createPaletteCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("category.blockyfile.palette"));
        category.addEntry(entryBuilder.startStrList(
                Component.translatable("option.blockyfile.palette"),
                Config.getPalette())
            .setDefaultValue(Config::defaultPalette)
            .setTooltip(Component.translatable("option.blockyfile.palette.tooltip"))
            .setSaveConsumer(list -> {
                List<String> palette = Config.getPalette();
                palette.clear();
                palette.addAll(list);
            })
            .setErrorSupplier(list -> {
                int size = list.size();
                if (!BlockyFile.validatePaletteSize(size)) {
                    return Optional.of(BlockyFile.translatable_INVALID_PALETTE_SIZE(size));
                }
                HashSet<ResourceLocation> ids = new HashSet<>(size);
                for (String s : list) {
                    ResourceLocation id = ResourceLocation.tryParse(s);
                    if (id == null) {
                        return Optional.of(BlockyFile.translatable_INVALID_IDENTIFIER(s));
                    } else if (!ids.add(id)) {
                        return Optional.of(BlockyFile.translatable_DUPLICATE_IDENTIFIER(id));
                    } else if (BuiltInRegistries.BLOCK.getValue(id) == Blocks.AIR) {
                        return Optional.of(BlockyFile.translatable_BLOCK_IS_AIR_OR_NOT_EXIST(id));
                    }
                }
                return Optional.empty();
            })
            .build());
    }
}