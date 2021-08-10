package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.DoubleListListEntry;
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
public class DoubleListBuilder extends FieldBuilder<List<Double>, DoubleListListEntry> {
   protected Function<Double, Optional<ITextComponent>> cellErrorSupplier;
   private Consumer<List<Double>> saveConsumer = null;
   private Function<List<Double>, Optional<ITextComponent[]>> tooltipSupplier = (list) -> {
      return Optional.empty();
   };
   private final List<Double> value;
   private boolean expanded = false;
   private Double min = null;
   private Double max = null;
   private Function<DoubleListListEntry, DoubleListListEntry.DoubleListCell> createNewInstance;
   private ITextComponent addTooltip = new TranslationTextComponent("text.cloth-config.list.add");
   private ITextComponent removeTooltip = new TranslationTextComponent("text.cloth-config.list.remove");
   private boolean deleteButtonEnabled = true;
   private boolean insertInFront = true;

   public DoubleListBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey, List<Double> value) {
      super(resetButtonKey, fieldNameKey);
      this.value = value;
   }

   public Function<Double, Optional<ITextComponent>> getCellErrorSupplier() {
      return this.cellErrorSupplier;
   }

   public DoubleListBuilder setCellErrorSupplier(Function<Double, Optional<ITextComponent>> cellErrorSupplier) {
      this.cellErrorSupplier = cellErrorSupplier;
      return this;
   }

   public DoubleListBuilder setErrorSupplier(Function<List<Double>, Optional<ITextComponent>> errorSupplier) {
      this.errorSupplier = errorSupplier;
      return this;
   }

   public DoubleListBuilder setDeleteButtonEnabled(boolean deleteButtonEnabled) {
      this.deleteButtonEnabled = deleteButtonEnabled;
      return this;
   }

   public DoubleListBuilder setInsertInFront(boolean insertInFront) {
      this.insertInFront = insertInFront;
      return this;
   }

   public DoubleListBuilder setAddButtonTooltip(ITextComponent addTooltip) {
      this.addTooltip = addTooltip;
      return this;
   }

   public DoubleListBuilder setRemoveButtonTooltip(ITextComponent removeTooltip) {
      this.removeTooltip = removeTooltip;
      return this;
   }

   public DoubleListBuilder requireRestart() {
      this.requireRestart(true);
      return this;
   }

   public DoubleListBuilder setCreateNewInstance(Function<DoubleListListEntry, DoubleListListEntry.DoubleListCell> createNewInstance) {
      this.createNewInstance = createNewInstance;
      return this;
   }

   public DoubleListBuilder setExpanded(boolean expanded) {
      this.expanded = expanded;
      return this;
   }

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public DoubleListBuilder setExpended(boolean expanded) {
      return this.setExpanded(expanded);
   }

   public DoubleListBuilder setSaveConsumer(Consumer<List<Double>> saveConsumer) {
      this.saveConsumer = saveConsumer;
      return this;
   }

   public DoubleListBuilder setDefaultValue(Supplier<List<Double>> defaultValue) {
      this.defaultValue = defaultValue;
      return this;
   }

   public DoubleListBuilder setMin(double min) {
      this.min = min;
      return this;
   }

   public DoubleListBuilder setMax(double max) {
      this.max = max;
      return this;
   }

   public DoubleListBuilder removeMin() {
      this.min = null;
      return this;
   }

   public DoubleListBuilder removeMax() {
      this.max = null;
      return this;
   }

   public DoubleListBuilder setDefaultValue(List<Double> defaultValue) {
      this.defaultValue = () -> {
         return defaultValue;
      };
      return this;
   }

   public DoubleListBuilder setTooltipSupplier(Function<List<Double>, Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = tooltipSupplier;
      return this;
   }

   public DoubleListBuilder setTooltipSupplier(Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = (list) -> {
         return (Optional)tooltipSupplier.get();
      };
      return this;
   }

   public DoubleListBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
      this.tooltipSupplier = (list) -> {
         return tooltip;
      };
      return this;
   }

   public DoubleListBuilder setTooltip(ITextComponent... tooltip) {
      this.tooltipSupplier = (list) -> {
         return Optional.ofNullable(tooltip);
      };
      return this;
   }

   @NotNull
   public DoubleListListEntry build() {
      DoubleListListEntry entry = new DoubleListListEntry(this.getFieldNameKey(), this.value, this.expanded,
                                                          null, this.saveConsumer, this.defaultValue, this.getResetButtonKey(), this.requireRestart, this.deleteButtonEnabled, this.insertInFront);
      if (this.min != null) {
         entry.setMinimum(this.min);
      }

      if (this.max != null) {
         entry.setMaximum(this.max);
      }

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
