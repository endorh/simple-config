package endorh.simpleconfig.core;

import endorh.simpleconfig.core.SimpleConfigBuilder.GroupBuilder;
import endorh.simpleconfig.core.entry.TextEntry;
import net.minecraft.util.text.ITextComponent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractSimpleConfigEntryHolderBuilder<Builder extends AbstractSimpleConfigEntryHolderBuilder<Builder>> {
	
	protected final Map<String, GroupBuilder> groups = new LinkedHashMap<>();
	protected final Map<String, AbstractConfigEntryBuilder<?, ?, ?, ?, ?>> entries = new LinkedHashMap<>();
	protected final Map<String, Integer> guiOrder = new LinkedHashMap<>();
	protected boolean requireRestart = false;
	protected final Map<String, Field> backingFields = new HashMap<>();
	
	protected abstract void addEntry(int order, String name, AbstractConfigEntryBuilder<?, ?, ?, ?, ?> entry);
	protected abstract AbstractConfigEntryBuilder<?, ?, ?, ?, ?> getEntry(String name);
	protected abstract boolean hasEntry(String name);
	
	protected void setBackingField(String name, Field field) {
		backingFields.put(name, field);
	}
	
	protected void checkName(String name) {
		if (name.contains("."))
			throw new IllegalArgumentException("Config entry names cannot contain dots: " + name);
		if (entries.containsKey(name) || groups.containsKey(name))
			throw new IllegalArgumentException("Duplicate config entry name: " + name);
	}
	
	/**
	 * Flag this config section as requiring a restart to be effective
	 */
	public Builder restart() {
		requireRestart = true;
		groups.values().forEach(GroupBuilder::restart);
		entries.values().forEach(AbstractConfigEntryBuilder::restart);
		return self();
	}
	
	@SuppressWarnings("unchecked")
	protected Builder self() {
		return (Builder) this;
	}
	
	/**
	 * Add an entry to the config
	 */
	public Builder add(String name, AbstractConfigEntryBuilder<?, ?, ?, ?, ?> entryBuilder) {
		return add(0, name, entryBuilder);
	}
	
	/**
	 * Add an entry with order argument.<br>
	 * Used by the class parser to specify the order with annotations.
	 */
	protected Builder add(
	  int order, String name, AbstractConfigEntryBuilder<?, ?, ?, ?, ?> entryBuilder
	) {
		addEntry(order, name, entryBuilder);
		return self();
	}
	
	/**
	 * Create a text entry in the config<br>
	 * @param name Name of the entry
	 * @param args Args to be passed to the translation<br>
	 *             As a special case, {@code Supplier} args will be
	 *             called before being filled in
	 */
	public Builder text(String name, Object... args) {
		add(name, new TextEntry.Builder().args(args));
		return self();
	}
	/**
	 * Create a text entry in the config
	 */
	public Builder text(ITextComponent text) {
		add(SimpleConfig.nextTextID(), new TextEntry.Builder(() -> text));
		return self();
	}
	/**
	 * Create a text entry in the config
	 */
	public Builder text(Supplier<ITextComponent> textSupplier) {
		add(SimpleConfig.nextTextID(), new TextEntry.Builder(textSupplier));
		return self();
	}
	
	protected abstract void translate(AbstractConfigEntry<?, ?, ?, ?> entry);
	
	/**
	 * Add a config group
	 */
	protected abstract Builder n(GroupBuilder group, int index);
	
	/**
	 * Add a config group
	 */
	public Builder n(GroupBuilder group) {
		return n(group, 0);
	}
}