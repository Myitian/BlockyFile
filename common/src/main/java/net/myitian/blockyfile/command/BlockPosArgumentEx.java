package net.myitian.blockyfile.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.myitian.blockyfile.PlatformUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BlockPosArgumentEx implements ArgumentType<Coordinates> {
    public static final Collection<String> EXAMPLES = List.of("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5");

    public static BlockPosArgumentEx blockPos() {
        return new BlockPosArgumentEx();
    }

    public static <S> BlockPos getInWorldBlockPos(CommandContext<S> context, Level level, String name) throws CommandSyntaxException {
        BlockPos blockPos = getBlockPos(context, name);
        if (!level.isInWorldBounds(blockPos)) {
            throw BlockPosArgument.ERROR_OUT_OF_WORLD.create();
        } else {
            return blockPos;
        }
    }

    public static <S> BlockPos getBlockPos(CommandContext<S> context, String name) {
        Coordinates coordinates = context.getArgument(name, Coordinates.class);
        S source = context.getSource();
        if (source instanceof CommandSourceStack s) {
            return coordinates.getBlockPos(s);
        } else {
            return PlatformUtil.getBlockPos(coordinates, source);
        }
    }

    public Coordinates parse(StringReader reader) throws CommandSyntaxException {
        return reader.canRead() && reader.peek() == '^' ? LocalCoordinates.parse(reader) : WorldCoordinates.parseInt(reader);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        if (commandContext.getSource() instanceof SharedSuggestionProvider source) {
            String string = suggestionsBuilder.getRemaining();
            Collection<SharedSuggestionProvider.TextCoordinates> collection;
            if (!string.isEmpty() && string.charAt(0) == '^') {
                collection = Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL);
            } else {
                collection = source.getRelevantCoordinates();
            }
            return SharedSuggestionProvider.suggestCoordinates(string, collection, suggestionsBuilder, Commands.createValidator(this::parse));
        }
        return Suggestions.empty();
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}