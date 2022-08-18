package endorh.simpleconfig.core;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolderBuilder;
import endorh.simpleconfig.api.ConfigGroupBuilder;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.GroupBuilder;
import endorh.simpleconfig.core.entry.TextEntry;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

public abstract class AbstractSimpleConfigEntryHolderBuilder<Builder extends ConfigEntryHolderBuilder<Builder>>
  implements ConfigEntryHolderBuilder<Builder> {
	protected final Map<String, GroupBuilder> groups = new LinkedHashMap<>();
	protected final Map<String, AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>> entries = new LinkedHashMap<>();
	protected final Map<String, Integer> guiOrder = new LinkedHashMap<>();
	protected boolean requireRestart = false;
	protected final Map<String, BackingField<?, ?>> backingFields = new HashMap<>();
	protected final Map<String, List<BackingField<?, ?>>> secondaryBackingFields = new HashMap<>();
	
	protected abstract void addEntry(int order, String name, AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> entry);
	protected abstract AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> getEntry(String name);
	protected abstract boolean hasEntry(String name);
	
	@Internal protected void setBackingField(String name, BackingField<?, ?> field) {
		backingFields.put(name, field);
	}
	
	@Internal protected List<? extends BackingFieldBinding<?, ?>> getSecondaryBackingFieldBindings(String name) {
		return getEntry(name).backingFieldBindings;
	}
	
	@Internal protected void setSecondaryBackingFields(String name, List<BackingField<?, ?>> fields) {
		if (secondaryBackingFields.containsKey(name))
			throw new IllegalStateException("Secondary backing fields already set for entry: " + name);
		secondaryBackingFields.put(name, fields);
	}
	
	@SuppressWarnings("unchecked") protected <T> BackingField<T, ?> getBackingField(String name) {
		return (BackingField<T, ?>) backingFields.get(name);
	}
	
	@SuppressWarnings("unchecked") protected <T> List<BackingField<T, ?>> getSecondaryBackingFields(String name) {
		return (List<BackingField<T, ?>>) (List<?>) secondaryBackingFields.get(name);
	}
	
	protected void checkName(String name) {
		if (name.contains("."))
			throw new IllegalArgumentException("Config entry names cannot contain dots: " + name);
		if (name.isEmpty())
			throw new IllegalArgumentException("Config entry names cannot be empty");
		if (entries.containsKey(name) || groups.containsKey(name))
			throw new IllegalArgumentException("Duplicate config entry name: " + name);
	}
	
	@Override @Contract("-> this") public Builder restart() {
		requireRestart = true;
		groups.values().forEach(GroupBuilder::restart);
		for (Entry<String, AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>> entry: entries.entrySet())
			entry.setValue((AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entry.getValue().restart());
		return self();
	}
	
	@SuppressWarnings("unchecked")
	@Contract("-> this") protected Builder self() {
		return (Builder) this;
	}
	
	@Override @Contract("_, _ -> this") public Builder add(
	  String name, ConfigEntryBuilder<?, ?, ?, ?> entryBuilder
	) {
		if (!(entryBuilder instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
		  "Entry builder not instance of AbstractConfigEntryBuilder");
		return add(0, name, (AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entryBuilder);
	}
	
	/**
	 * Add an entry with order argument.<br>
	 * Used by the class parser to specify the order with annotations.
	 */
	@Contract("_, _, _ -> this") protected Builder add(
	  int order, String name, ConfigEntryBuilder<?, ?, ?, ?> entryBuilder
	) {
		if (!(entryBuilder instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
		  "Entry builder not instance of AbstractConfigEntryBuilder");
		addEntry(order, name, (AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entryBuilder);
		return self();
	}
	
	@Override @Contract("_, _ -> this") public Builder text(String name, Object... args) {
		add(name, new TextEntry.Builder().args(args));
		return self();
	}
	
	@Override @Contract("_ -> this") public Builder text(ITextComponent text) {
		add(SimpleConfigImpl.nextTextID(), new TextEntry.Builder(() -> text));
		return self();
	}
	@Override @Contract("_ -> this") public Builder text(Supplier<ITextComponent> textSupplier) {
		add(SimpleConfigImpl.nextTextID(), new TextEntry.Builder(textSupplier));
		return self();
	}
	
	protected abstract void buildTranslations(AbstractConfigEntry<?, ?, ?> entry);
	
	/**
	 * Add a config group
	 */
	@Contract("_, _ -> this") protected abstract Builder n(ConfigGroupBuilder group, int index);
	
	@Override @Contract("_ -> this") public Builder n(ConfigGroupBuilder group) {
		return n(group, 0);
	}
}
