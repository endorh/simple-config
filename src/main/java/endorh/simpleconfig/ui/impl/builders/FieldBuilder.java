package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.TooltipListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public abstract class FieldBuilder<V, Entry extends AbstractConfigListEntry<V>, Self extends FieldBuilder<V, Entry, Self>> {
	private final Class<?> entryClass;
	private final ConfigFieldBuilder builder;
	@NotNull protected final ITextComponent fieldNameKey;
	protected Consumer<Entry> onBuildListener;
	protected boolean requireRestart = false;
	protected V value;
	protected V original;
	protected String name;
	@NotNull protected Supplier<V> defaultValue;
	@NotNull protected Consumer<V> saveConsumer = t -> {};
	@NotNull protected Function<V, Optional<ITextComponent>> errorSupplier = t -> Optional.empty();
	@NotNull protected Function<V, Optional<ITextComponent[]>> tooltipSupplier = t -> Optional.empty();
	@Nullable protected Supplier<Boolean> editableSupplier = null;
	protected List<EntryTag> entryTags = new ArrayList<>();
	protected boolean ignoreEdits = false;
	
	@Internal protected FieldBuilder(
	  Class<?> entryClass, ConfigFieldBuilder builder, ITextComponent name, V value
	) {
		this.entryClass = entryClass;
		this.builder = builder;
		this.value = original = value;
		defaultValue = () -> value;
		fieldNameKey = Objects.requireNonNull(name);
	}
	
	@Internal public Class<?> getEntryClass() {
		return entryClass;
	}
	
	protected ConfigFieldBuilder getEntryBuilder() {
		return builder;
	}
	
	protected Self self() {
		//noinspection unchecked
		return (Self) this;
	}
	
	public Self setOriginal(V original) {
		this.original = original;
		return self();
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
	
	public Self withTags(Collection<? extends EntryTag> flags) {
		entryTags.addAll(flags);
		return self();
	}
	
	public Self withTags(EntryTag... flags) {
		entryTags.addAll(Arrays.asList(flags));
		return self();
	}
	
	public Self withoutTags(Collection<? extends EntryTag> flags) {
		entryTags.removeAll(flags);
		return self();
	}
	
	public Self withoutTags(EntryTag... flags) {
		entryTags.removeAll(Arrays.asList(flags));
		return self();
	}
	
	public Self requireRestart(boolean requireRestart) {
		this.requireRestart = requireRestart;
		if (requireRestart) withTags(EntryTag.REQUIRES_RESTART);
		else withoutTags(EntryTag.REQUIRES_RESTART);
		return self();
	}
	
	public Self nonPersistent(boolean nonPersistent) {
		return nonPersistent
		       ? withTags(EntryTag.NON_PERSISTENT)
		       : withoutTags(EntryTag.NON_PERSISTENT);
	}
	
	public Self setEditableSupplier(@Nullable Supplier<Boolean> editableSupplier) {
		this.editableSupplier = editableSupplier;
		return self();
	}
	
	public Self withSaveConsumer(Consumer<V> saveConsumer) {
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
	
	public Self withBuildListener(Consumer<Entry> listener) {
		onBuildListener = listener;
		return self();
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
		entry.getEntryTags().addAll(entryTags);
		entry.setEditableSupplier(editableSupplier);
		entry.setIgnoreEdits(ignoreEdits);
		if (onBuildListener != null) onBuildListener.accept(entry);
		return entry;
	}
	
	protected abstract Entry buildEntry();
}

