package endorh.simpleconfig.api.entry;

import com.electronwill.nightconfig.core.CommentedConfig;
import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ui.icon.Icon;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Used internally to wrap config entries from some mods.
 */
public interface CommentedConfigEntryBuilder extends ConfigEntryBuilder<
   @NotNull CommentedConfig, CommentedConfig, CommentedConfig, CommentedConfigEntryBuilder
> {
   /**
    * Add an editable entry for this config object.
    */
   @Contract(pure=true) @NotNull CommentedConfigEntryBuilder add(String name, ConfigEntryBuilder<?, ?, ?, ?> entryBuilder);

   /**
    * Add an editable entry for this config object, as a caption for this entry.<br>
    * Only one property can be the caption. The last set is used.
    */
   @Contract(pure=true) @NotNull <CB extends ConfigEntryBuilder<?, ?, ?, ?> & AtomicEntryBuilder>
      CommentedConfigEntryBuilder caption(String name, CB entryBuilder);

   /**
    * Remove a previously set caption property.
    * Transforms it into a regular property.
    */
   @Contract(pure=true) @NotNull CommentedConfigEntryBuilder withoutCaption();

   /**
    * Set an icon to be displayed at the header of this entry.<br>
    * Max recommended icon size is 18×18, but you can try something larger and see
    * how it fits in the GUI.
    * Icons can depend on the GUI value of the bean.
    */
   @Contract(pure=true) @NotNull CommentedConfigEntryBuilder withIcon(Function<CommentedConfig, Icon> icon);

   /**
    * Set an icon to be displayed at the header of this entry.<br>
    * Max recommended icon size is 18×18, but you can try something larger and see
    * how it fits in the GUI.
    * Icons can depend on the GUI value of the bean.
    */
   @Contract(pure=true) @NotNull default CommentedConfigEntryBuilder withIcon(@Nullable Icon icon) {
      return withIcon(icon == null ? null : b -> icon);
   }
}
