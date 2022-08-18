package endorh.simpleconfig.core;

import endorh.simpleconfig.api.SimpleConfig.ConfigReflectiveOperationException;
import endorh.simpleconfig.api.SimpleConfig.InvalidConfigValueException;
import endorh.simpleconfig.api.SimpleConfig.NoSuchConfigGroupError;
import endorh.simpleconfig.api.SimpleConfigGroup;
import endorh.simpleconfig.config.ClientConfig;
import endorh.simpleconfig.core.SimpleConfigImpl.IGUIEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigCategoryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.impl.builders.CaptionedSubCategoryBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.stripFormattingCodes;

public class SimpleConfigGroupImpl extends AbstractSimpleConfigEntryHolder
  implements SimpleConfigGroup, IGUIEntry {
	public final SimpleConfigCategoryImpl category;
	public final @Nullable SimpleConfigGroupImpl parentGroup;
	public final String name;
	public boolean expanded;
	protected final String title;
	protected final String tooltip;
	protected Map<String, SimpleConfigGroupImpl> groups;
	protected List<IGUIEntry> order;
	protected @Nullable Consumer<SimpleConfigGroup> baker;
	protected AbstractConfigEntry<?, ?, ?> heldEntry;
	
	@Internal protected SimpleConfigGroupImpl(
	  SimpleConfigGroupImpl parent, String name, String title,
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
	
	@Internal protected SimpleConfigGroupImpl(
	  SimpleConfigCategoryImpl parent, String name, String title,
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
	  Map<String, AbstractConfigEntry<?, ?, ?>> entries,
	  Map<String, SimpleConfigGroupImpl> groups, List<IGUIEntry> guiOrder,
	  @Nullable AbstractConfigEntry<?, ?, ?> heldEntry
	) {
		if (this.entries != null)
			throw new IllegalStateException("Called buildEntry() twice");
		if (heldEntry != null && !(heldEntry instanceof IKeyEntry<?>))
			throw new IllegalArgumentException(
			  "Held entry for group " + getPath() + " doesn't implement IKeyEntry");
		this.entries = entries;
		this.groups = groups;
		children = groups;
		this.order = guiOrder;
		this.heldEntry = heldEntry;
	}
	
	@Override
	protected String getPath() {
		return parentGroup != null
		       ? parentGroup.getPathPart() + name
		       : category.getPathPart() + name;
	}
	
	@Override public @Nullable AbstractSimpleConfigEntryHolder getParent() {
		return parentGroup != null
		       ? parentGroup
		       : category.isRoot? root : category;
	}
	
	@Override protected String getName() {
		return name;
	}
	
	@Override protected String getConfigComment() {
		StringBuilder builder = new StringBuilder();
		if (title != null && ServerI18n.hasKey(title)) {
			String name = stripFormattingCodes(
			  ServerI18n.format(title).trim());
			builder.append(name).append('\n');
			if (tooltip != null && ServerI18n.hasKey(tooltip)) {
				String tooltip = "  " + stripFormattingCodes(
				  ServerI18n.format(this.tooltip).trim().replace("\n", "\n  "));
				builder.append(tooltip).append('\n');
			}
		}
		return builder.toString();
	}
	
	@Override public SimpleConfigCategoryImpl getCategory() {
		return category;
	}
	
	
	@Override public SimpleConfigGroupImpl getGroup(String path) {
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
		if (ClientConfig.advanced.translation_debug_mode)
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
			// status = status.append(new StringTextComponent("⧉").modifyStyle(s -> s
			//   .setFormatting(TextFormatting.WHITE)
			//   .setHoverEvent(new HoverEvent(
			// 	 HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent(
			// 	 "simpleconfig.debug.copy").mergeStyle(TextFormatting.GRAY)))
			//   .setClickEvent(new ClickEvent(
			// 	 ClickEvent.Action.COPY_TO_CLIPBOARD, title)))
			// ).appendString(" ");
			// if (tooltip != null)
			// 	status = status.append(new StringTextComponent("⧉").modifyStyle(s -> s
			// 	  .setFormatting(TextFormatting.GRAY)
			// 	  .setHoverEvent(new HoverEvent(
			// 	    HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent(
			// 	      "simpleconfig.debug.copy.help").mergeStyle(TextFormatting.GRAY)))
			// 	  .setClickEvent(new ClickEvent(
			// 		 ClickEvent.Action.COPY_TO_CLIPBOARD, tooltip)))
			// 	).appendString(" ");
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
		if (ClientConfig.advanced.translation_debug_mode)
			return getDebugTooltip();
		if (tooltip != null && I18n.hasKey(tooltip))
			return Optional.of(
			  Arrays.stream(I18n.format(tooltip).split("\n"))
				 .map(StringTextComponent::new).toArray(ITextComponent[]::new));
		return Optional.empty();
	}
	
	@OnlyIn(Dist.CLIENT)
	protected CaptionedSubCategoryBuilder<?, ?, ?> buildGUI(
	  ConfigFieldBuilder entryBuilder, boolean forRemote
	) {
		CaptionedSubCategoryBuilder<?, ?, ?> group =
		  heldEntry != null ? createAndDecorateGUI(entryBuilder, heldEntry, forRemote) :
		  entryBuilder.startSubCategory(getTitle());
		group.setExpanded(expanded)
		  .setTooltipSupplier(this::getTooltip)
		  .setName(name);
		if (!order.isEmpty()) {
			for (IGUIEntry entry : order) {
				if (entry instanceof AbstractConfigEntry) {
					((AbstractConfigEntry<?, ?, ?>) entry).buildGUI(group, entryBuilder, forRemote);
				} else if (entry instanceof SimpleConfigGroupImpl) {
					group.add(((SimpleConfigGroupImpl) entry).buildGUI(entryBuilder, forRemote));
				}
			}
		}
		return group;
	}
	
	@OnlyIn(Dist.CLIENT) private <
	  T, CE extends AbstractConfigEntry<?, ?, T> & IKeyEntry<T>
	> CaptionedSubCategoryBuilder<T, ?, ?> createAndDecorateGUI(
	  ConfigFieldBuilder entryBuilder, AbstractConfigEntry<?, ?, T> heldEntry, boolean forRemote
	) {
		//noinspection unchecked
		final CE cast = (CE) heldEntry;
		FieldBuilder<T, ?, ?> builder = cast.buildChildGUIEntry(entryBuilder);
		cast.decorateGUIBuilder(builder, forRemote);
		return makeGUI(entryBuilder, builder)
		  .withSaveConsumer(cast.createSaveConsumer());
	}
	
	@SuppressWarnings("unchecked") @OnlyIn(Dist.CLIENT) private <
	  T, HE extends AbstractConfigListEntry<T> & IChildListEntry,
	  HEB extends FieldBuilder<T, HE, HEB>
	> CaptionedSubCategoryBuilder<T, ?, ?> makeGUI(
	  ConfigFieldBuilder entryBuilder, FieldBuilder<T, ?, ?> builder
	) {
		return entryBuilder.startCaptionedSubCategory(getTitle(), (HEB) builder);
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override @Internal public void buildGUI(
	  ConfigCategoryBuilder category, ConfigFieldBuilder entryBuilder,
	  boolean forRemote
	) {
		category.addEntry(buildGUI(entryBuilder, forRemote));
	}
	
	@Override
	protected void bake() {
		for (SimpleConfigGroupImpl group : groups.values())
			group.bake();
		if (baker != null)
			baker.accept(this);
	}
	
	/**
	 * Bakes all the backing fields<br>
	 */
	protected void bakeFields() {
		for (SimpleConfigGroupImpl group : groups.values())
			group.bakeFields();
		for (AbstractConfigEntry<?, ?, ?> entry : entries.values())
			entry.bakeField();
	}
	
	/**
	 * Commits any changes in the backing fields to the actual config file<br>
	 * You may also call this method on the root {@link SimpleConfigImpl}
	 * or on the parent {@link SimpleConfigCategoryImpl} of this group
	 * @throws InvalidConfigValueException if the current value of a field is invalid.
	 */
	@Override public void commitFields() {
		try {
			for (SimpleConfigGroupImpl group : groups.values())
				group.commitFields();
			for (AbstractConfigEntry<?, ?, ?> entry : entries.values())
				entry.commitField();
		} catch (IllegalAccessException e) {
			throw new ConfigReflectiveOperationException(
			  "Could not access mod config field during config commit\n  Details: " + e.getMessage(), e);
		}
	}
}
