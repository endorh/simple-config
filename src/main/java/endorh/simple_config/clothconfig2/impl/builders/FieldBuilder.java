package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public abstract class FieldBuilder<T, A extends AbstractConfigListEntry> {
   @NotNull
   private final ITextComponent fieldNameKey;
   @NotNull
   private final ITextComponent resetButtonKey;
   protected boolean requireRestart = false;
   @Nullable
   protected Supplier<T> defaultValue = null;
   @Nullable
   protected Function<T, Optional<ITextComponent>> errorSupplier;

   protected FieldBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey) {
      this.resetButtonKey = Objects.requireNonNull(resetButtonKey);
      this.fieldNameKey = Objects.requireNonNull(fieldNameKey);
   }

   @Nullable
   public final Supplier<T> getDefaultValue() {
      return this.defaultValue;
   }

   /** @deprecated */
   @Deprecated
   public final AbstractConfigListEntry buildEntry() {
      return this.build();
   }

   @NotNull
   public abstract A build();

   @NotNull
   public final ITextComponent getFieldNameKey() {
      return this.fieldNameKey;
   }

   @NotNull
   public final ITextComponent getResetButtonKey() {
      return this.resetButtonKey;
   }

   public boolean isRequireRestart() {
      return this.requireRestart;
   }

   public void requireRestart(boolean requireRestart) {
      this.requireRestart = requireRestart;
   }
}
