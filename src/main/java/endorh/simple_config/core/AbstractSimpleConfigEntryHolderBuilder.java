package endorh.simple_config.core;

import endorh.simple_config.core.SimpleConfigBuilder.GroupBuilder;
import endorh.simple_config.core.entry.TextEntry;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;

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
	
	// /**
	//  * Add a config entry with a held entry.<br>
	//  * Held entries are rendered as children in the config GUI, but
	//  * have their own entries in the file as normal.<br>
	//  * <br>
	//  * Only the entries that implement {@link IEntryHoldingEntry}
	//  * can hold entries, and only the entries that implement {@link IKeyEntry}
	//  * can be held by other entries. Attempting to pass wrong entry types will
	//  * result in a compile-time error.<br>
	//  * <br>
	//  * By default, the name of the held entry is taken from the holding entry
	//  * by adding the '{@code $root}' suffix, but their name can be specified
	//  * as well.
	//  */
	// public <
	//   T, G, E extends AbstractConfigEntry<T, ?, G, E> & IEntryHoldingEntry,
	//   EB extends AbstractConfigEntryBuilder<T, ?, G, E, EB> & IEntryHoldingEntryBuilder,
	//   H, HE extends AbstractConfigEntry<H, ?, ?, HE> & IKeyEntry<?, ?>
	//   > Builder add(
	//   String name, EB entryBuilder, AbstractConfigEntryBuilder<H, ?, ?, HE, ?> heldEntryBuilder
	// ) {
	// 	return add(name, entryBuilder, name + "$root", heldEntryBuilder);
	// }
	//
	//
	// /**
	//  * Add a config entry with a held entry.<br>
	//  * Held entries are rendered as children in the config GUI, but
	//  * have their own entries in the file as normal.<br>
	//  * <br>
	//  * Only the entries that implement {@link IEntryHoldingEntry}
	//  * can hold entries, and only the entries that implement {@link IKeyEntry}
	//  * can be held by other entries. Attempting to pass wrong entry types will
	//  * result in a compile-time error.<br>
	//  * <br>
	//  * By default, the name of the held entry is taken from the holding entry
	//  * by adding the '{@code $root}' suffix, but their name can be specified
	//  * as well.
	//  */
	// public <
	//   T, G, E extends AbstractConfigEntry<T, ?, G, E> & IEntryHoldingEntry,
	//   EB extends AbstractConfigEntryBuilder<T, ?, G, E, EB> & IEntryHoldingEntryBuilder,
	//   H, HE extends AbstractConfigEntry<H, ?, ?, HE> & IKeyEntry<?, ?>
	// > Builder add(
	//   String name, EB entryBuilder,
	//   String heldEntryName, AbstractConfigEntryBuilder<H, ?, ?, HE, ?> heldEntryBuilder
	// ) {
	// 	addEntry(0, heldEntryName, heldEntryBuilder);
	// 	guiOrder.remove(heldEntryName);
	// 	addEntry(0, name, entryBuilder);
	// 	try {
	// 		entryBuilder.setHeldEntryBuilder(heldEntryName, heldEntryBuilder);
	// 	} catch (UnsupportedOperationException e) {
	// 		throw new IllegalArgumentException(
	// 		  "Entry builder of type " + entryBuilder.getClass().getSimpleName() +
	// 		  " violates the implementation contract of IEntryHoldingEntryBuilder." +
	// 		  "\nMust override AbstractConfigEntryBuilder#setHeldEntryBuilder(...)");
	// 	}
	// 	return self();
	// }
	
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
