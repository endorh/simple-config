package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.gui.entries.LongListListEntry;
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
public class LongListBuilder extends FieldBuilder<List<Long>, LongListListEntry> {
   protected Function<Long, Optional<ITextComponent>> cellErrorSupplier;
   private Consumer<List<Long>> saveConsumer = null;
   private Function<List<Long>, Optional<ITextComponent[]>> tooltipSupplier = (list) -> {
      return Optional.empty();
   };
   private final List<Long> value;
   private boolean expanded = false;
   private Long min = null;
   private Long max = null;
   private Function<LongListListEntry, LongListListEntry.LongListCell> createNewInstance;
   private ITextComponent addTooltip = new TranslationTextComponent("text.cloth-config.list.add");
   private ITextComponent removeTooltip = new TranslationTextComponent("text.cloth-config.list.remove");
   private boolean deleteButtonEnabled = true;
   private boolean insertInFront = true;

   public LongListBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey, List<Long> value) {
      super(resetButtonKey, fieldNameKey);
      this.value = value;
   }

   public Function<Long, Optional<ITextComponent>> getCellErrorSupplier() {
      return this.cellErrorSupplier;
   }

   public LongListBuilder setCellErrorSupplier(Function<Long, Optional<ITextComponent>> cellErrorSupplier) {
      this.cellErrorSupplier = cellErrorSupplier;
      return this;
   }

   public LongListBuilder setErrorSupplier(Function<List<Long>, Optional<ITextComponent>> errorSupplier) {
      this.errorSupplier = errorSupplier;
      return this;
   }

   public LongListBuilder setDeleteButtonEnabled(boolean deleteButtonEnabled) {
      this.deleteButtonEnabled = deleteButtonEnabled;
      return this;
   }

   public LongListBuilder setInsertInFront(boolean insertInFront) {
      this.insertInFront = insertInFront;
      return this;
   }

   public LongListBuilder setAddButtonTooltip(ITextComponent addTooltip) {
      this.addTooltip = addTooltip;
      return this;
   }

   public LongListBuilder setRemoveButtonTooltip(ITextComponent removeTooltip) {
      this.removeTooltip = removeTooltip;
      return this;
   }

   public LongListBuilder requireRestart() {
      this.requireRestart(true);
      return this;
   }

   public LongListBuilder setCreateNewInstance(Function<LongListListEntry, LongListListEntry.LongListCell> createNewInstance) {
      this.createNewInstance = createNewInstance;
      return this;
   }

   public LongListBuilder setExpanded(boolean expanded) {
      this.expanded = expanded;
      return this;
   }

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public LongListBuilder setExpended(boolean expanded) {
      return this.setExpanded(expanded);
   }

   public LongListBuilder setSaveConsumer(Consumer<List<Long>> saveConsumer) {
      this.saveConsumer = saveConsumer;
      return this;
   }

   public LongListBuilder setDefaultValue(Supplier<List<Long>> defaultValue) {
      this.defaultValue = defaultValue;
      return this;
   }

   public LongListBuilder setMin(long min) {
      this.min = min;
      return this;
   }

   public LongListBuilder setMax(long max) {
      this.max = max;
      return this;
   }

   public LongListBuilder removeMin() {
      this.min = null;
      return this;
   }

   public LongListBuilder removeMax() {
      this.max = null;
      return this;
   }

   public LongListBuilder setDefaultValue(List<Long> defaultValue) {
      this.defaultValue = () -> {
         return defaultValue;
      };
      return this;
   }

   public LongListBuilder setTooltipSupplier(Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = (list) -> {
         return (Optional)tooltipSupplier.get();
      };
      return this;
   }

   public LongListBuilder setTooltipSupplier(Function<List<Long>, Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = tooltipSupplier;
      return this;
   }

   public LongListBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
      this.tooltipSupplier = (list) -> {
         return tooltip;
      };
      return this;
   }

   public LongListBuilder setTooltip(ITextComponent... tooltip) {
      this.tooltipSupplier = (list) -> {
         return Optional.ofNullable(tooltip);
      };
      return this;
   }

   @NotNull
   public LongListListEntry build() {
      LongListListEntry entry = new LongListListEntry(this.getFieldNameKey(), this.value, this.expanded,
                                                      null, this.saveConsumer, this.defaultValue, this.getResetButtonKey(), this.isRequireRestart(), this.deleteButtonEnabled, this.insertInFront);
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
