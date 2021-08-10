package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.StringListListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class StringListBuilder extends FieldBuilder<List<String>, StringListListEntry> {
   private Function<String, Optional<ITextComponent>> cellErrorSupplier;
   private Consumer<List<String>> saveConsumer = null;
   private Function<List<String>, Optional<ITextComponent[]>> tooltipSupplier = (list) -> {
      return Optional.empty();
   };
   private final List<String> value;
   private boolean expanded = false;
   private Function<StringListListEntry, StringListListEntry.StringListCell> createNewInstance;
   private ITextComponent addTooltip = new TranslationTextComponent("text.cloth-config.list.add");
   private ITextComponent removeTooltip = new TranslationTextComponent("text.cloth-config.list.remove");
   private boolean deleteButtonEnabled = true;
   private boolean insertInFront = true;

   public StringListBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey, List<String> value) {
      super(resetButtonKey, fieldNameKey);
      this.value = value;
   }

   public Function<String, Optional<ITextComponent>> getCellErrorSupplier() {
      return this.cellErrorSupplier;
   }

   public StringListBuilder setCellErrorSupplier(Function<String, Optional<ITextComponent>> cellErrorSupplier) {
      this.cellErrorSupplier = cellErrorSupplier;
      return this;
   }

   public StringListBuilder setErrorSupplier(Function<List<String>, Optional<ITextComponent>> errorSupplier) {
      this.errorSupplier = errorSupplier;
      return this;
   }

   public StringListBuilder setDeleteButtonEnabled(boolean deleteButtonEnabled) {
      this.deleteButtonEnabled = deleteButtonEnabled;
      return this;
   }

   public StringListBuilder setInsertInFront(boolean insertInFront) {
      this.insertInFront = insertInFront;
      return this;
   }

   public StringListBuilder setAddButtonTooltip(ITextComponent addTooltip) {
      this.addTooltip = addTooltip;
      return this;
   }

   public StringListBuilder setRemoveButtonTooltip(ITextComponent removeTooltip) {
      this.removeTooltip = removeTooltip;
      return this;
   }

   public StringListBuilder requireRestart() {
      this.requireRestart(true);
      return this;
   }

   public StringListBuilder setCreateNewInstance(Function<StringListListEntry, StringListListEntry.StringListCell> createNewInstance) {
      this.createNewInstance = createNewInstance;
      return this;
   }

   public StringListBuilder setExpanded(boolean expanded) {
      this.expanded = expanded;
      return this;
   }

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public StringListBuilder setExpended(boolean expanded) {
      return this.setExpanded(expanded);
   }

   public StringListBuilder setSaveConsumer(Consumer<List<String>> saveConsumer) {
      this.saveConsumer = saveConsumer;
      return this;
   }

   public StringListBuilder setDefaultValue(Supplier<List<String>> defaultValue) {
      this.defaultValue = defaultValue;
      return this;
   }

   public StringListBuilder setDefaultValue(List<String> defaultValue) {
      this.defaultValue = () -> {
         return defaultValue;
      };
      return this;
   }

   public StringListBuilder setTooltipSupplier(Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = (list) -> {
         return (Optional)tooltipSupplier.get();
      };
      return this;
   }

   public StringListBuilder setTooltipSupplier(Function<List<String>, Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = tooltipSupplier;
      return this;
   }

   public StringListBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
      this.tooltipSupplier = (list) -> {
         return tooltip;
      };
      return this;
   }

   public StringListBuilder setTooltip(ITextComponent... tooltip) {
      this.tooltipSupplier = (list) -> {
         return Optional.ofNullable(tooltip);
      };
      return this;
   }

   @NotNull
   public StringListListEntry build() {
      StringListListEntry entry = new StringListListEntry(this.getFieldNameKey(), this.value, this.expanded,
                                                          null, this.saveConsumer, this.defaultValue, this.getResetButtonKey(), this.isRequireRestart(), this.deleteButtonEnabled, this.insertInFront);
      if (this.createNewInstance != null) {
         entry.setCreateNewInstance(this.createNewInstance);
      }

      entry.setCellErrorSupplier(this.cellErrorSupplier);
      entry.setTooltipSupplier(() -> {
         return this.tooltipSupplier.apply(entry.getValue());
      });
      entry.setAddTooltip(this.addTooltip);
      entry.setRemoveTooltip(this.removeTooltip);
      if (this.errorSupplier != null) {
         entry.setErrorSupplier(() -> {
            return this.errorSupplier.apply(entry.getValue());
         });
      }

      return entry;
   }
}
