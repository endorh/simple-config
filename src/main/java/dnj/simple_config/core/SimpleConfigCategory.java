package dnj.simple_config.core;

import dnj.simple_config.core.SimpleConfig.IGUIEntry;
import dnj.simple_config.core.SimpleConfig.NoSuchConfigGroupError;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A Config category<br>
 * Has its own tab in the GUI, and it's no different to a top-level group in the config<br>
 * Each type of config (Client/Server (and Common)) has its default category too,
 * containing all the root entries/groups<br>
 * You may access any config entry under a category with {@link SimpleConfigCategory#get(String)},
 * which accepts dot-separated paths to entries, taking into account their group structure
 */
public class SimpleConfigCategory extends AbstractSimpleConfigEntryHolder {
	public final SimpleConfig parent;
	public final String name;
	protected final String title;
	protected final @Nullable Consumer<SimpleConfigCategory> baker;
	protected Map<String, SimpleConfigGroup> groups;
	protected List<IGUIEntry> order;
	
	protected SimpleConfigCategory(
	  SimpleConfig parent, String name, String title, @Nullable Consumer<SimpleConfigCategory> baker
	) {
		this.parent = parent;
		this.name = name;
		this.title = title;
		this.baker = baker;
		root = parent;
	}
	
	protected void build(
	  Map<String, AbstractConfigEntry<?, ?, ?, ?>> entries, Map<String, SimpleConfigGroup> groups,
	  Map<String, ConfigValue<?>> specValues,
	  List<IGUIEntry> order
	) {
		if (this.entries != null)
			throw new IllegalStateException("Called build() twice");
		this.entries = entries;
		this.groups = groups;
		children = groups;
		this.specValues = specValues;
		this.order = order;
	}
	
	@Override public void markDirty(boolean dirty) {
		super.markDirty(dirty);
		if (dirty) parent.markDirty(true);
	}
	
	@OnlyIn(Dist.CLIENT)
	public void buildGUI(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
		me.shedaniel.clothconfig2.api.ConfigCategory category = builder.getOrCreateCategory(getTitle());
		if (!order.isEmpty()) {
			for (IGUIEntry entry : order)
				entry.buildGUI(category, entryBuilder, this);
		}
	}
	
	protected void bakeFields() throws IllegalAccessException {
		for (SimpleConfigGroup group : groups.values())
			group.bakeFields();
		for (AbstractConfigEntry<?, ?, ?, ?> entry : entries.values())
			if (entry.backingField != null)
				entry.backingField.set(null, get(entry.name));
	}
	
	/**
	 * Get a config group
	 *
	 * @param path Name or dot-separated path to the group
	 * @throws NoSuchConfigGroupError if the group is not found
	 */
	@SuppressWarnings("unused")
	public SimpleConfigGroup getGroup(String path) {
		if (path.contains(".")) {
			final String[] split = path.split("\\.", 2);
			if (groups.containsKey(split[0]))
				return groups.get(split[0]).getGroup(split[1]);
		} else if (groups.containsKey(path))
			return groups.get(path);
		throw new NoSuchConfigGroupError(path);
	}
	
	protected ITextComponent getTitle() {
		if (parent.debugTranslations)
			return new StringTextComponent(title);
		return new TranslationTextComponent(title);
	}
}
