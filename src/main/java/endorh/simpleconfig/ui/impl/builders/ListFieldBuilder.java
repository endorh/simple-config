package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public abstract class ListFieldBuilder<V, Entry extends AbstractListListEntry<V, ?, Entry>,
  Self extends ListFieldBuilder<V, Entry, Self>> extends FieldBuilder<List<V>, Entry, Self> {
	
	@NotNull protected BiFunction<Integer, V, Optional<Component>> cellErrorSupplier = (i, v) -> Optional.empty();
	@NotNull protected Function<List<V>, @Nullable List<Optional<Component>>> multiCellErrorSupplier = l -> null;
	protected Component[] addTooltip = new Component[] {
	  Component.translatable("simpleconfig.help.list.insert"),
	  Component.translatable("simpleconfig.help.list.insert:key")
	};
	protected Component[] removeTooltip = new Component[] {
	  Component.translatable("simpleconfig.help.list.remove"),
	  Component.translatable("simpleconfig.help.list.remove:key")
	};
	protected boolean expanded = false;
	protected boolean insertInFront = false;
	protected boolean deleteButtonEnabled = true;
	protected boolean captionControlsEnabled = false;
	// protected @Nullable AbstractConfigListEntry<?> heldEntry = null;
	
	protected ListFieldBuilder(
	  Class<?> entryClass, ConfigFieldBuilder builder, Component name, List<V> value
	) {
		super(entryClass, builder, name, value);
	}
	
	public Self setCellErrorSupplier(@NotNull BiFunction<Integer, V, Optional<Component>> cellError) {
		cellErrorSupplier = cellError;
		return self();
	}
	
	public Self setMultiCellErrorSupplier(@NotNull Function<List<V>, @Nullable List<Optional<Component>>> multiCellError) {
		multiCellErrorSupplier = multiCellError;
		return self();
	}
	
	public Self setAddButtonTooltip(Component[] addTooltip) {
		this.addTooltip = addTooltip;
		return self();
	}
	
	public Self setRemoveButtonTooltip(Component[] removeTooltip) {
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
	
	@Override public @NotNull Entry build() {
		final Entry entry = super.build();
		entry.setCellErrorSupplier(cellErrorSupplier);
		entry.setMultiCellErrorSupplier(multiCellErrorSupplier);
		entry.setExpanded(expanded);
		entry.setAddTooltip(addTooltip);
		entry.setRemoveTooltip(removeTooltip);
		entry.setDeleteButtonEnabled(deleteButtonEnabled);
		entry.setInsertInFront(insertInFront);
		entry.setCaptionControlsEnabled(captionControlsEnabled);
		return entry;
	}
}
