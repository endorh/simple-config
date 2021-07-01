package dnj.simple_config.core;

import dnj.simple_config.core.entry.TextEntry;
import dnj.simple_config.core.SimpleConfig.IAbstractGUIEntry;
import dnj.simple_config.core.SimpleConfigBuilder.GroupBuilder;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

abstract class AbstractSimpleConfigEntryHolderBuilder<Builder extends AbstractSimpleConfigEntryHolderBuilder<Builder>> {
	
	protected final Map<String, GroupBuilder> groups = new LinkedHashMap<>();
	protected final Map<String, AbstractConfigEntry<?, ?, ?, ?>> entries = new LinkedHashMap<>();
	protected final List<IAbstractGUIEntry> guiOrder = new ArrayList<>();
	protected AbstractConfigEntry<?, ?, ?, ?> last = null;
	protected boolean requireRestart = false;
	
	protected abstract void addEntry(AbstractConfigEntry<?, ?, ?, ?> entry);
	protected abstract AbstractConfigEntry<?, ?, ?, ?> getEntry(String name);
	protected abstract boolean hasEntry(String name);
	
	public Builder restart() {
		requireRestart = true;
		groups.values().forEach(GroupBuilder::restart);
		entries.values().forEach(AbstractConfigEntry::restart);
		return self();
	}
	
	@SuppressWarnings("unchecked")
	protected Builder self() {
		return (Builder) this;
	}
	
	public Builder add(String name, AbstractConfigEntry<?, ?, ?, ?> entry) {
		addEntry(entry.name(name));
		return self();
	}
	
	public Builder text(String name) {
		add(name, new TextEntry());
		return self();
	}
	public Builder text(ITextComponent text) {
		add(SimpleConfig.nextTextID(), new TextEntry(() -> text));
		return self();
	}
	public Builder text(Supplier<ITextComponent> textSupplier) {
		add(SimpleConfig.nextTextID(), new TextEntry(textSupplier));
		return self();
	}
	
	protected abstract void translate(AbstractConfigEntry<?, ?, ?, ?> entry);
	public abstract Builder n(GroupBuilder group);
}
