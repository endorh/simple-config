package endorh.simple_config.clothconfig2.api;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.AbstractConfigScreen;
import endorh.simple_config.clothconfig2.gui.ClothConfigScreen;
import endorh.simple_config.clothconfig2.gui.widget.DynamicElementListWidget;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractConfigEntry<T> extends DynamicElementListWidget.ElementEntry<AbstractConfigEntry<T>> implements ReferenceProvider<T> {
   private AbstractConfigScreen screen;
   private Supplier<Optional<ITextComponent>> errorSupplier;
   @Nullable
   private List<ReferenceProvider<?>> referencableEntries = null;

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public final void setReferencableEntries(@Nullable List<AbstractConfigEntry<?>> referencableEntries) {
      this.setReferenceProviderEntries(
        referencableEntries == null? null :
        referencableEntries.stream().map(AbstractConfigEntry::provideReferenceEntry).collect(Collectors.toList()));
   }

   public final void setReferenceProviderEntries(@Nullable List<ReferenceProvider<?>> referencableEntries) {
      this.referencableEntries = referencableEntries;
   }

   public void requestReferenceRebuilding() {
      AbstractConfigScreen configScreen = this.getConfigScreen();
      if (configScreen instanceof ReferenceBuildingConfigScreen) {
         ((ReferenceBuildingConfigScreen)configScreen).requestReferenceRebuilding();
      }

   }

   @NotNull
   public AbstractConfigEntry<T> provideReferenceEntry() {
      return this;
   }

   /** @deprecated */
   @Deprecated
   @Nullable
   @Internal
   @ScheduledForRemoval
   public final List<AbstractConfigEntry<?>> getReferencableEntries() {
      return this.referencableEntries == null? null :
             this.referencableEntries.stream().map(ReferenceProvider::provideReferenceEntry)
               .collect(Collectors.toList());
   }

   @Nullable
   @Internal
   public final List<ReferenceProvider<?>> getReferenceProviderEntries() {
      return this.referencableEntries;
   }

   public abstract boolean isRequiresRestart();

   public abstract void setRequiresRestart(boolean var1);

   public abstract ITextComponent getFieldName();

   public ITextComponent getDisplayedFieldName() {
      IFormattableTextComponent text = this.getFieldName().deepCopy();
      boolean hasError = this.getConfigError().isPresent();
      boolean isEdited = this.isEdited();
      if (hasError) {
         text = text.mergeStyle(TextFormatting.RED);
      }

      if (isEdited) {
         text = text.mergeStyle(TextFormatting.ITALIC);
      }

      if (!hasError && !isEdited) {
         text = text.mergeStyle(TextFormatting.GRAY);
      }

      return text;
   }

   public abstract T getValue();

   public final Optional<ITextComponent> getConfigError() {
      return this.errorSupplier != null && this.errorSupplier.get().isPresent() ? this.errorSupplier.get()
                                                                                : this.getError();
   }

   public void lateRender(MatrixStack matrices, int mouseX, int mouseY, float delta) {
   }

   public void setErrorSupplier(Supplier<Optional<ITextComponent>> errorSupplier) {
      this.errorSupplier = errorSupplier;
   }

   public Optional<ITextComponent> getError() {
      return Optional.empty();
   }

   public abstract Optional<T> getDefaultValue();

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   @Nullable
   public final ClothConfigScreen getScreen() {
      return this.screen instanceof ClothConfigScreen ? (ClothConfigScreen)this.screen : null;
   }

   @Nullable
   public final AbstractConfigScreen getConfigScreen() {
      return this.screen;
   }

   public final void addTooltip(@NotNull Tooltip tooltip) {
      this.screen.addTooltip(tooltip);
   }

   public void updateSelected(boolean isSelected) {
   }

   @Internal
   public final void setScreen(AbstractConfigScreen screen) {
      this.screen = screen;
   }

   public abstract void save();

   public boolean isEdited() {
      return this.getConfigError().isPresent();
   }

   public int getItemHeight() {
      return 24;
   }

   public int getInitialReferenceOffset() {
      return 0;
   }
}
