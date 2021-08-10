package endorh.simple_config.clothconfig2.api;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public interface ConfigCategory {
   ITextComponent getCategoryKey();

   /** @deprecated */
   @Deprecated
   List<Object> getEntries();

   ConfigCategory addEntry(AbstractConfigListEntry var1);

   ConfigCategory setCategoryBackground(ResourceLocation var1);

   void setBackground(@Nullable ResourceLocation var1);

   @Nullable
   ResourceLocation getBackground();

   @Nullable
   Supplier<Optional<ITextProperties[]>> getDescription();

   void setDescription(@Nullable Supplier<Optional<ITextProperties[]>> var1);

   default void setDescription(@Nullable ITextProperties[] description) {
      this.setDescription(() -> {
         return Optional.ofNullable(description);
      });
   }

   void removeCategory();
}
