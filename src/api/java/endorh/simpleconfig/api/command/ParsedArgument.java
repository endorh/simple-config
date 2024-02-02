package endorh.simpleconfig.api.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public record ParsedArgument<A>(A value, String text) {
   @Override public String toString() {
      return text;
   }

   public static <A> @Nullable ParsedArgument<A> tryParse(ArgumentType<A> type, String text) {
      StringReader reader = new StringReader(text);
      try {
         A parsed = type.parse(reader);
         if (!reader.canRead()) return new ParsedArgument<>(parsed, text);
      } catch (CommandSyntaxException ignored) {}
      return null;
   }

   public static <A> @NotNull ParsedArgument<A> parse(ArgumentType<A> type, String text) throws CommandSyntaxException {
      StringReader reader = new StringReader(text);
      A parsed = type.parse(reader);
      if (reader.canRead()) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
         .dispatcherUnknownArgument()
         .createWithContext(reader);
      return new ParsedArgument<>(parsed, text);
   }

   public static <A> @NotNull List<ParsedArgument<A>> getExamples(ArgumentType<A> type) {
      return type.getExamples().stream()
         .map(s -> tryParse(type, s))
         .filter(Objects::nonNull)
         .toList();
   }
}
