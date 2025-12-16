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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class CustomArgument<T> implements ArgumentType<T> {
    public static final DynamicCommandExceptionType INVALID_VALUE_EXCEPTION = new DynamicCommandExceptionType(obj -> Component.translatableEscape("argument.blockyfile.invalid", obj));
    private final Function<String, T> parser;
    private final Collection<String> values;

    public CustomArgument(Function<String, T> parser, Collection<String> values) {
        this.parser = parser;
        this.values = values;
    }

    @Override
    public T parse(StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.readUnquotedString();
        try {
            return parser.apply(string);
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

