package endorh.simpleconfig.api.entry;

import com.mojang.brigadier.arguments.ArgumentType;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.command.ParsedArgument;

import java.util.function.Function;

public interface CommandArgumentEntryBuilder<A, T extends ArgumentType<A>>
   extends ConfigEntryBuilder<ParsedArgument<A>, String, ParsedArgument<A>, CommandArgumentEntryBuilder<A, T>> {
   /**
    * Modify this entry's argument type.<br>
    * <br>
    * Cannot change the type's type, only the type's properties.
    */
   CommandArgumentEntryBuilder<A, T> withType(T type);

   /**
    * Modify this entry's argument type.<br>
    * <br>
    * Cannot change the type's type, only the type's properties.
    */
   CommandArgumentEntryBuilder<A, T> withType(Function<T, T> type);
}
