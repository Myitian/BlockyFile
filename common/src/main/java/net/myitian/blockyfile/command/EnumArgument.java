package net.myitian.blockyfile.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class EnumArgument<E extends Enum<E>> implements ArgumentType<E> {
    public static final DynamicCommandExceptionType INVALID_VALUE_EXCEPTION = new DynamicCommandExceptionType(obj -> Component.translatableEscape("argument.blockyfile.enum.invalid", obj));
    private final Class<E> clazz;
    private final Collection<String> values;

    public EnumArgument(Class<E> clazz) {
        this.clazz = clazz;
        values = Arrays.stream(clazz.getEnumConstants()).map(Enum::name).toList();
    }

    public static <E extends Enum<E>> EnumArgument<E> enumArg(Class<E> clazz) {
        return new EnumArgument<>(clazz);
    }

    public static <E extends Enum<E>, S> E getEnum(CommandContext<S> context, String name, Class<E> clazz) {
        return context.getArgument(name, clazz);
    }

    @Override
    public E parse(StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.readUnquotedString();
        try {
            return Enum.valueOf(clazz, string);
        } catch (IllegalArgumentException e) {
            throw INVALID_VALUE_EXCEPTION.createWithContext(stringReader, string);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof SharedSuggestionProvider) {
            return SharedSuggestionProvider.suggest(values.stream(), builder);
        }
        return Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
        return values;
    }
}

