package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.api.EntryFlag;
import endorh.simpleconfig.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public abstract class FieldBuilder<V, Entry extends AbstractConfigListEntry<V>, Self extends FieldBuilder<V, Entry, Self>> {
	private final WeakReference<ConfigEntryBuilder> builder;
	@NotNull protected final ITextComponent fieldNameKey;
	protected boolean requireRestart = false;
	protected V value;
	protected String name;
	@NotNull protected Supplier<V> defaultValue;
	@NotNull protected Consumer<V> saveConsumer = t -> {};
	@NotNull protected Function<V, Optional<ITextComponent>> errorSupplier = t -> Optional.empty();
	@NotNull protected Function<V, Optional<ITextComponent[]>> tooltipSupplier = t -> Optional.empty();
	@Nullable protected Supplier<Boolean> editableSupplier = null;
	protected List<EntryFlag> entryFlags = new ArrayList<>();
	protected boolean ignoreEdits = false;
	
	@Internal protected FieldBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, V value
	) {
		this.builder = new WeakReference<>(builder);
		this.value = value;
		this.defaultValue = () -> value;
		this.fieldNameKey = Objects.requireNonNull(name);
	}
	
	protected ConfigEntryBuilder getEntryBuilder() {
		return builder.get();
	}
	
	protected Self self() {
		//noinspection unchecked
		return (Self) this;
	}
	
	public Self setName(String name) {
		this.name = name;
		return self();
	}
	
	public Self setErrorSupplier(Function<V, Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
		return self();
	}
	
	public Self setTooltipSupplier(Function<V, Optional<ITextComponent[]>> tooltipSupplier) {
		this.tooltipSupplier = tooltipSupplier;
		return self();
	}
	
	public Self setTooltipSupplier(Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
		return setTooltipSupplier(v -> tooltipSupplier.get());
	}
	
	public Self setTooltip(ITextComponent... tooltip) {
		return setTooltipSupplier(() -> Optional.ofNullable(tooltip));
	}
	
	public Self withFlags(EntryFlag... flags) {
		entryFlags.addAll(Arrays.asList(flags));
		return self();
	}
	
	public Self withoutFlags(EntryFlag... flags) {
		entryFlags.removeAll(Arrays.asList(flags));
		return self();
	}
	
	public Self requireRestart(boolean requireRestart) {
		this.requireRestart = requireRestart;
		if (requireRestart) withFlags(EntryFlag.REQUIRES_RESTART);
		else withoutFlags(EntryFlag.REQUIRES_RESTART);
		return self();
	}
	
	public Self nonPersistent(boolean nonPersistent) {
		return nonPersistent
		       ? withFlags(EntryFlag.NON_PERSISTENT)
		       : withoutFlags(EntryFlag.NON_PERSISTENT);
	}
	
	public Self setEditableSupplier(@Nullable Supplier<Boolean> editableSupplier) {
		this.editableSupplier = editableSupplier;
		return self();
	}
	
	public Self setSaveConsumer(Consumer<V> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return self();
	}
	
	public Self setIgnoreEdits(boolean ignoreEdits) {
		this.ignoreEdits = ignoreEdits;
		return self();
	}
	
	public Self setDefaultValue(Supplier<V> defaultValue) {
		this.defaultValue = defaultValue;
		return self();
	}
	
	public Self setDefaultValue(V defaultValue) {
		return setDefaultValue(() -> defaultValue);
	}
	
	@NotNull public Entry build() {
		final Entry entry = buildEntry();
		entry.setRequiresRestart(requireRestart);
		entry.setErrorSupplier(() -> errorSupplier.apply(entry.getValue()));
		entry.setDefaultValue(defaultValue);
		entry.setSaveConsumer(saveConsumer);
		if (entry instanceof TooltipListEntry)
			((TooltipListEntry<?>) entry).setTooltipSupplier(() -> tooltipSupplier.apply(entry.getValue()));
		entry.setOriginal(value);
		if (name != null) entry.setName(name);
		entry.getEntryFlags().addAll(entryFlags);
		entry.setEditableSupplier(editableSupplier);
		entry.setIgnoreEdits(ignoreEdits);
		return entry;
	}
	
	protected abstract Entry buildEntry();
}

