package endorh.simple_config.core;

import endorh.simple_config.SimpleConfigMod.Config;
import endorh.simple_config.core.SimpleConfig.ConfigReflectiveOperationException;
import endorh.simple_config.core.SimpleConfig.IGUIEntry;
import endorh.simple_config.core.SimpleConfig.NoSuchConfigGroupError;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class SimpleConfigGroup extends AbstractSimpleConfigEntryHolder implements IGUIEntry {
	public final SimpleConfigCategory category;
	public final @Nullable SimpleConfigGroup parentGroup;
	public final String name;
	public boolean expanded;
	protected final String title;
	protected final String tooltip;
	protected Map<String, SimpleConfigGroup> groups;
	protected List<IGUIEntry> order;
	protected @Nullable Consumer<SimpleConfigGroup> baker;
	
	@Internal protected SimpleConfigGroup(
	  SimpleConfigGroup parent, String name, String title,
	  String tooltip, boolean expanded, @Nullable Consumer<SimpleConfigGroup> baker
	) {
		this.category = parent.category;
		this.parentGroup = parent;
		this.name = name;
		this.title = title;
		this.tooltip = tooltip;
		this.expanded = expanded;
		this.baker = baker;
		root = category.root;
	}
	
	@Internal protected SimpleConfigGroup(
	  SimpleConfigCategory parent, String name, String title,
	  String tooltip, boolean expanded, @Nullable Consumer<SimpleConfigGroup> baker
	) {
		this.category = parent;
		this.parentGroup = null;
		this.name = name;
		this.title = title;
		this.tooltip = tooltip;
		this.expanded = expanded;
		this.baker = baker;
		root = category.root;
	}
	
	@Internal protected void build(
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
	
	@Override
	protected String getPath() {
		return parentGroup != null
		       ? parentGroup.getPath() + "." + name
		       : category != null ? category.getPath() + "." + name : name;
	}
	
	/**
	 * Get the parent category of this group
	 */
	public SimpleConfigCategory getCategory() {
		return category;
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
	
	@Override public void markDirty(boolean dirty) {
		super.markDirty(dirty);
		if (dirty) (parentGroup != null ? parentGroup : category).markDirty(true);
	}
	
	@OnlyIn(Dist.CLIENT)
	protected ITextComponent getTitle() {
		if (Config.advanced.translation_debug_mode)
			return getDebugTitle();
		if (!I18n.hasKey(title)) {
			final String[] split = title.split("\\.");
			return new StringTextComponent(split[split.length - 1]);
		}
		return new TranslationTextComponent(title);
	}
	
	@OnlyIn(Dist.CLIENT)
	protected ITextComponent getDebugTitle() {
		if (title != null) {
			IFormattableTextComponent status =
			  I18n.hasKey(title) ? new StringTextComponent("✔ ") : new StringTextComponent("✘ ");
			if (tooltip != null) {
				status = status.append(
				  I18n.hasKey(tooltip)
				  ? new StringTextComponent("✔ ").mergeStyle(TextFormatting.DARK_AQUA)
				  : new StringTextComponent("_ ").mergeStyle(TextFormatting.DARK_AQUA));
			}
			TextFormatting format =
			  I18n.hasKey(title)? TextFormatting.DARK_GREEN : TextFormatting.RED;
			return new StringTextComponent("").append(status.append(new StringTextComponent(title)).mergeStyle(format));
		} else return new StringTextComponent("").append(new StringTextComponent("⚠ " + name).mergeStyle(TextFormatting.DARK_RED));
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
		AbstractConfigEntry.addTranslationsDebugSuffix(lines);
		return Optional.of(lines.toArray(new ITextComponent[0]));
	}
	
	@OnlyIn(Dist.CLIENT)
	protected Optional<ITextComponent[]> getTooltip() {
		if (Config.advanced.translation_debug_mode)
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
	
	@OnlyIn(Dist.CLIENT)
	@Override @Internal public void buildGUI(
	  ConfigCategory category, ConfigEntryBuilder entryBuilder, ISimpleConfigEntryHolder config
	) {
		category.addEntry(buildGUI(entryBuilder));
	}
	
	@Override
	protected void bake() {
		for (SimpleConfigGroup group : groups.values())
			group.bake();
		if (baker != null)
			baker.accept(this);
	}
	
	/**
	 * Bakes all the backing fields<br>
	 */
	protected void bakeFields() {
		try {
			for (SimpleConfigGroup group : groups.values())
				group.bakeFields();
			for (AbstractConfigEntry<?, ?, ?, ?> entry : entries.values())
				entry.bakeField(this);
		} catch (IllegalAccessException e) {
			throw new ConfigReflectiveOperationException(
			  "Could not access mod config field during config bake\n  Details: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Commits any changes in the backing fields to the actual config file<br>
	 * You may also call this method on the root {@link SimpleConfig}
	 * or on the parent {@link SimpleConfigCategory} of this group
	 */
	public void commitFields() throws IllegalAccessException {
		try {
			for (SimpleConfigGroup group : groups.values())
				group.commitFields();
			for (AbstractConfigEntry<?, ?, ?, ?> entry : entries.values())
				entry.commitField(this);
		} catch (IllegalAccessException e) {
			throw new ConfigReflectiveOperationException(
			  "Could not access mod config field during config commit\n  Details: " + e.getMessage(), e);
		}
	}
}
