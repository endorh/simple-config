package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.api.IChildListEntry;
import endorh.simple_config.clothconfig2.gui.entries.AbstractListListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class ListFieldBuilder<V, Entry extends AbstractListListEntry<V, ?, Entry>,
  Self extends ListFieldBuilder<V, Entry, Self>> extends FieldBuilder<List<V>, Entry, Self> {
	
	@NotNull protected Function<V, Optional<ITextComponent>> cellErrorSupplier = v -> Optional.empty();
	protected ITextComponent addTooltip = new TranslationTextComponent("text.cloth-config.list.add");
	protected ITextComponent removeTooltip = new TranslationTextComponent("text.cloth-config.list.remove");
	protected boolean expanded = false;
	protected boolean insertInFront = false;
	protected boolean deleteButtonEnabled = true;
	protected boolean captionControlsEnabled = false;
	protected @Nullable AbstractConfigListEntry<?> heldEntry = null;
	
	protected ListFieldBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, List<V> value
	) {
		super(builder, name, value);
	}
	
	public Self setCellErrorSupplier(@NotNull Function<V, Optional<ITextComponent>> cellError) {
		cellErrorSupplier = cellError;
		return self();
	}
	
	public Self setAddButtonTooltip(ITextComponent addTooltip) {
		this.addTooltip = addTooltip;
		return self();
	}
	
	public Self setRemoveButtonTooltip(ITextComponent removeTooltip) {
		this.removeTooltip = removeTooltip;
		return self();
	}
	
	public Self setExpanded(boolean expanded) {
		this.expanded = expanded;
		return self();
	}
	
	public Self setInsertInFront(boolean insertInFront) {
		this.insertInFront = insertInFront;
		return self();
	}
	
	public Self setDeleteButtonEnabled(boolean enabled) {
		this.deleteButtonEnabled = enabled;
		return self();
	}
	
	public Self setCaptionControlsEnabled(boolean enabled) {
		this.captionControlsEnabled = enabled;
		return self();
	}
	
	public <E extends AbstractConfigListEntry<?> & IChildListEntry> Self setHeldEntry(E entry) {
		heldEntry = entry;
		return self();
	}
	
	protected <E extends AbstractConfigListEntry<?> & IChildListEntry> E getHeldEntry() {
		//noinspection unchecked
		return (E) heldEntry;
	}
	
	@Override public @NotNull Entry build() {
		final Entry entry = super.build();
		entry.setCellErrorSupplier(cellErrorSupplier);
		entry.setExpanded(expanded);
		entry.setAddTooltip(addTooltip);
		entry.setRemoveTooltip(removeTooltip);
		entry.setDeleteButtonEnabled(deleteButtonEnabled);
		entry.setInsertInFront(insertInFront);
		entry.setCaptionControlsEnabled(captionControlsEnabled);
		if (heldEntry != null)
			entry.setHeldEntry(getHeldEntry());
		return entry;
	}
}
