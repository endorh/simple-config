package endorh.simple_config.clothconfig2.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class EnumListEntry<T extends Enum<?>> extends SelectionListEntry<T> {
   public static final Function<Enum<?>, ITextComponent> DEFAULT_NAME_PROVIDER = (t) -> {
      return new TranslationTextComponent(t instanceof SelectionListEntry.Translatable ? ((SelectionListEntry.Translatable)t).getKey() : t.toString());
   };

   /** @deprecated */
   @Deprecated
   @Internal
   public EnumListEntry(ITextComponent fieldName, Class<T> clazz, T value, ITextComponent resetButtonKey, Supplier<T> defaultValue, Consumer<T> saveConsumer) {
      super(fieldName, clazz.getEnumConstants(), value, resetButtonKey, defaultValue, saveConsumer, Objects.requireNonNull(DEFAULT_NAME_PROVIDER)::apply);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public EnumListEntry(ITextComponent fieldName, Class<T> clazz, T value, ITextComponent resetButtonKey, Supplier<T> defaultValue, Consumer<T> saveConsumer, Function<Enum<?>, ITextComponent> enumNameProvider) {
      super(fieldName, clazz.getEnumConstants(), value, resetButtonKey, defaultValue, saveConsumer, Objects.requireNonNull(enumNameProvider)::apply, null);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public EnumListEntry(ITextComponent fieldName, Class<T> clazz, T value, ITextComponent resetButtonKey, Supplier<T> defaultValue, Consumer<T> saveConsumer, Function<Enum<?>, ITextComponent> enumNameProvider, Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      super(fieldName, clazz.getEnumConstants(), value, resetButtonKey, defaultValue, saveConsumer, Objects.requireNonNull(enumNameProvider)::apply, tooltipSupplier, false);
   }

   /** @deprecated */
   @Deprecated
   @Internal
   public EnumListEntry(ITextComponent fieldName, Class<T> clazz, T value, ITextComponent resetButtonKey, Supplier<T> defaultValue, Consumer<T> saveConsumer, Function<Enum<?>, ITextComponent> enumNameProvider, Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart) {
      super(fieldName, clazz.getEnumConstants(), value, resetButtonKey, defaultValue, saveConsumer, Objects.requireNonNull(enumNameProvider)::apply, tooltipSupplier, requiresRestart);
   }
}
