package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.command.ParsedArgument;
import endorh.simpleconfig.api.entry.CommandArgumentEntryBuilder;
import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.TypeWrapper;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandArgumentEntry<
   A, T extends ArgumentType<A>, TM extends ArgumentTypeInfo.Template<T>,
   TI extends ArgumentTypeInfo<T, TM>
> extends AbstractConfigEntry<ParsedArgument<A>, String, ParsedArgument<A>> {
   private final T aType;
   private final ParsedArgumentTypeWrapper<A> wrapper;
   public CommandArgumentEntry(ConfigEntryHolder parent, String name, ParsedArgument<A> defValue, T aType) {
      super(parent, name, defValue);
      this.aType = aType;
      wrapper = new ParsedArgumentTypeWrapper<>(aType);
   }

   public static class Builder<
      A, T extends ArgumentType<A>, TM extends ArgumentTypeInfo.Template<T>,
      TI extends ArgumentTypeInfo<T, TM>
   > extends AbstractConfigEntryBuilder<
      ParsedArgument<A>, String, ParsedArgument<A>, CommandArgumentEntry<A, T, TM, TI>,
      CommandArgumentEntryBuilder<A, T>, Builder<A, T, TM, TI>
   > implements CommandArgumentEntryBuilder<A, T> {
      private T aType;

      public Builder(ParsedArgument<A> value, T type) {
         super(value, EntryType.of(value.getClass(), EntryType.uncheckedSubClasses(value.value().getClass())));
         aType = type;
      }

      @Override public CommandArgumentEntryBuilder<A, T> withType(T type) {
         Builder<A, T, TM, TI> copy = copy();
         copy.aType = type;
         return copy;
      }

      @Override public CommandArgumentEntryBuilder<A, T> withType(Function<T, T> type) {
         Builder<A, T, TM, TI> copy = copy();
         copy.aType = type.apply(copy.aType);
         return copy;
      }

      @Override protected CommandArgumentEntry<A, T, TM, TI> buildEntry(
         ConfigEntryHolder parent, String name
      ) {
         return new CommandArgumentEntry<>(parent, name, value, aType);
      }

      @Override protected Builder<A, T, TM, TI> createCopy(ParsedArgument<A> value) {
         return new Builder<>(value, aType);
      }
   }

   @Override public String getConfigCommentTooltip() {
      return "Command Arg";
   }

   @Override public List<String> getConfigCommentTooltips() {
      return List.of(
         "Suggestions: " + aType.getExamples().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ")));
   }

   @Override public String forConfig(ParsedArgument<A> value) {
      return value.text();
   }

   @Override public @Nullable ParsedArgument<A> fromConfig(@Nullable String value) {
      return ParsedArgument.tryParse(aType, value);
   }

   @Override public @Nullable String forCommand(ParsedArgument<A> value) {
      return value.text();
   }

   @Override public ParsedArgument<A> fromCommand(String value) {
      return ParsedArgument.tryParse(aType, value);
   }

   @Override public Optional<FieldBuilder<ParsedArgument<A>, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
      ComboBoxFieldBuilder<ParsedArgument<A>> fieldBuilder = builder.startComboBox(getDisplayName(), wrapper, forGui(get()));
      fieldBuilder.setSuggestionProvider(new ComboBoxModel<>(aType));
      return Optional.of(decorate(fieldBuilder));
   }

   public static class ComboBoxModel<A> extends SimpleComboBoxModel<ParsedArgument<A>> {
      private final ArgumentType<A> aType;

      public ComboBoxModel(ArgumentType<A> aType) {
         super(ParsedArgument.getExamples(aType));
         this.aType = aType;
      }

      @Override public Optional<List<ParsedArgument<A>>> updateSuggestions(
         TypeWrapper<ParsedArgument<A>> typeWrapper, String query
      ) {
         CompletableFuture<Suggestions> suggestions = aType.listSuggestions(new CommandContext<Object>(
            Minecraft.getInstance(), query, Collections.emptyMap(),
            null, null, Collections.emptyList(), new StringRange(0, query.length()),
            null, null, false
         ), new SuggestionsBuilder(query, 0));
         try {
            List<Suggestion> list = suggestions.get().getList();
            return Optional.of(list.stream()
               .map(s -> ParsedArgument.tryParse(aType, s.getText()))
               .filter(Objects::nonNull).toList());
         } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
         }
      }
   }

   public static class ParsedArgumentTypeWrapper<A> implements TypeWrapper<ParsedArgument<A>> {
      private final ArgumentType<A> aType;

      public ParsedArgumentTypeWrapper(ArgumentType<A> aType) {
         this.aType = aType;
      }

      @Override public Pair<Optional<ParsedArgument<A>>, Optional<Component>> parseElement(@NotNull String text) {
         try {
            return Pair.of(Optional.of(ParsedArgument.parse(aType, text)), Optional.empty());
         } catch (CommandSyntaxException e) {
            return Pair.of(Optional.empty(), Optional.of(Component.literal(
               e.getMessage()).withStyle(s -> s.withColor(ChatFormatting.RED))));
         }
      }

      @Override public Component getDisplayName(@NotNull ParsedArgument<A> element) {
         return Component.literal(element.text());
      }

      @Override public @Nullable TextFormatter getTextFormatter() {
         return TypeWrapper.super.getTextFormatter();
      }
   }
}
