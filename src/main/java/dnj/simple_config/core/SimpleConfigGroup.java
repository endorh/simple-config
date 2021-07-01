package dnj.simple_config.core;

import dnj.simple_config.core.SimpleConfig.IGUIEntry;
import dnj.simple_config.core.SimpleConfig.NoSuchConfigGroupError;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.util.*;

public class SimpleConfigGroup extends AbstractSimpleConfigEntryHolder implements IGUIEntry {
	public final SimpleConfigCategory category;
	public final @Nullable
	SimpleConfigGroup parentGroup;
	public final String name;
	public boolean expanded;
	protected final String title;
	protected final String tooltip;
	protected Map<String, SimpleConfigGroup> groups;
	protected List<IGUIEntry> order;
	
	protected SimpleConfigGroup(
	  SimpleConfigGroup parent, String name, String title,
	  String tooltip, boolean expanded
	) {
		this.category = parent.category;
		this.parentGroup = parent;
		this.name = name;
		this.title = title;
		this.tooltip = tooltip;
		this.expanded = expanded;
		root = category.root;
	}
	
	protected SimpleConfigGroup(
	  SimpleConfigCategory parent, String name, String title,
	  String tooltip, boolean expanded
	) {
		this.category = parent;
		this.parentGroup = null;
		this.name = name;
		this.title = title;
		this.tooltip = tooltip;
		this.expanded = expanded;
		root = category.root;
	}
	
	protected void build(
	  Map<String, AbstractConfigEntry<?, ?, ?, ?>> entries, Map<String, ConfigValue<?>> specValues,
	  Map<String, SimpleConfigGroup> groups, List<IGUIEntry> guiOrder
	) {
		if (this.entries != null)
			throw new IllegalStateException("Called build() twice");
		this.entries = entries;
		this.specValues = specValues;
		this.groups = groups;
		children = groups;
		this.order = guiOrder;
	}
	
	@SuppressWarnings("unused")
	public SimpleConfigCategory getCategory() {
		return category;
	}
	
	@Override public void markDirty(boolean dirty) {
		super.markDirty(dirty);
		if (dirty) (parentGroup != null ? parentGroup : category).markDirty(true);
	}
	
	protected ITextComponent getTitle() {
		if (root.debugTranslations)
			return new StringTextComponent(title);
		if (!I18n.hasKey(title)) {
			final String[] split = title.split("\\.");
			return new StringTextComponent(split[split.length - 1]);
		}
		return new TranslationTextComponent(title);
	}
	
	@OnlyIn(Dist.CLIENT)
	protected Optional<ITextComponent[]> getDebugTooltip() {
		List<ITextComponent> lines = new ArrayList<>();
		lines.add(new StringTextComponent("Group Translation key:").mergeStyle(TextFormatting.GRAY));
		if (title != null) {
			final IFormattableTextComponent status =
			  I18n.hasKey(title)
			  ? new StringTextComponent("(✔ present)").mergeStyle(TextFormatting.DARK_GREEN)
			  : new StringTextComponent("(✘ missing)").mergeStyle(TextFormatting.RED);
			lines.add(new StringTextComponent("   " + title + " ")
			            .mergeStyle(TextFormatting.DARK_AQUA).append(status));
		} else lines.add(new StringTextComponent("   Error: couldn't map translation key").mergeStyle(TextFormatting.RED));
		lines.add(new StringTextComponent("Tooltip key:").mergeStyle(TextFormatting.GRAY));
		if (tooltip != null) {
			final IFormattableTextComponent status =
			  I18n.hasKey(tooltip)
			  ? new StringTextComponent("(✔ present)").mergeStyle(TextFormatting.DARK_GREEN)
			  : new StringTextComponent("(not present)").mergeStyle(TextFormatting.GOLD);
			lines.add(new StringTextComponent("   " + tooltip + " ")
			            .mergeStyle(TextFormatting.DARK_AQUA).append(status));
		} else lines.add(new StringTextComponent("   Error: couldn't map tooltip translation key").mergeStyle(TextFormatting.RED));
		lines.add(new StringTextComponent(" "));
		lines.add(new StringTextComponent(" ⚠ Translation debug mode active").mergeStyle(TextFormatting.GOLD));
		lines.add(new StringTextComponent("     Remember to remove the call to .debugTranslations()").mergeStyle(TextFormatting.GOLD));
		return Optional.of(lines.toArray(new ITextComponent[0]));
	}
	
	@OnlyIn(Dist.CLIENT)
	protected Optional<ITextComponent[]> getTooltip() {
		if (root.debugTranslations)
			return getDebugTooltip();
		if (tooltip != null && I18n.hasKey(tooltip))
			return Optional.of(
			  Arrays.stream(I18n.format(tooltip).split("\n"))
				 .map(StringTextComponent::new).toArray(ITextComponent[]::new));
		return Optional.empty();
	}
	
	@OnlyIn(Dist.CLIENT)
	protected SubCategoryListEntry buildGUI(ConfigEntryBuilder entryBuilder) {
		final SubCategoryBuilder group = entryBuilder
		  .startSubCategory(getTitle())
		  .setExpanded(expanded)
		  .setTooltip(getTooltip());
		if (!order.isEmpty()) {
			for (IGUIEntry entry : order) {
				if (entry instanceof AbstractConfigEntry) {
					((AbstractConfigEntry<?, ?, ?, ?>) entry).buildGUIEntry(entryBuilder, this).ifPresent(group::add);
				} else if (entry instanceof SimpleConfigGroup) {
					group.add(((SimpleConfigGroup) entry).buildGUI(entryBuilder));
				}
			}
		}
		return group.build();
	}
	
	@Override public void buildGUI(
	  me.shedaniel.clothconfig2.api.ConfigCategory category, ConfigEntryBuilder entryBuilder, ISimpleConfigEntryHolder config
	) {
		category.addEntry(buildGUI(entryBuilder));
	}
	
	protected void bakeFields() throws IllegalAccessException {
		for (SimpleConfigGroup group : groups.values())
			group.bakeFields();
		for (AbstractConfigEntry<?, ?, ?, ?> entry : entries.values())
			if (entry.backingField != null)
				entry.backingField.set(null, get(entry.name));
	}
	
	/**
	 * Get a config subgroup
	 *
	 * @param path Name or dot-separated path to the group
	 * @throws NoSuchConfigGroupError if the group is not found
	 */
	public SimpleConfigGroup getGroup(String path) {
		if (path.contains(".")) {
			final String[] split = path.split("\\.", 2);
			if (groups.containsKey(split[0]))
				return groups.get(split[0]).getGroup(split[1]);
		} else if (groups.containsKey(path))
			return groups.get(path);
		throw new NoSuchConfigGroupError(path);
	}
}
