package endorh.simple_config.clothconfig2.impl;

import com.google.common.collect.Lists;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigBuilder;
import endorh.simple_config.clothconfig2.api.ConfigCategory;
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
public class ConfigCategoryImpl implements ConfigCategory {
   private final ConfigBuilder builder;
   private final List<Object> data;
   @Nullable
   private ResourceLocation background;
   private final ITextComponent categoryKey;
   @Nullable
   private Supplier<Optional<ITextProperties[]>> description = Optional::empty;

   ConfigCategoryImpl(ConfigBuilder builder, ITextComponent categoryKey) {
      this.builder = builder;
      this.data = Lists.newArrayList();
      this.categoryKey = categoryKey;
   }

   public ITextComponent getCategoryKey() {
      return this.categoryKey;
   }

   public List<Object> getEntries() {
      return this.data;
   }

   public ConfigCategory addEntry(AbstractConfigListEntry entry) {
      this.data.add(entry);
      return this;
   }

   public ConfigCategory setCategoryBackground(ResourceLocation identifier) {
      if (this.builder.hasTransparentBackground()) {
         throw new IllegalStateException("Cannot set category background if screen is using transparent background.");
      } else {
         this.background = identifier;
         return this;
      }
   }

   public void removeCategory() {
      this.builder.removeCategory(this.categoryKey);
   }

   public void setBackground(@Nullable ResourceLocation background) {
      this.background = background;
   }

   @Nullable
   public ResourceLocation getBackground() {
      return this.background;
   }

   @Nullable
   public Supplier<Optional<ITextProperties[]>> getDescription() {
      return this.description;
   }

   public void setDescription(@Nullable Supplier<Optional<ITextProperties[]>> description) {
      this.description = description;
   }
}
