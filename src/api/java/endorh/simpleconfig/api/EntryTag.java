package endorh.simpleconfig.api;

import com.google.common.collect.Maps;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Entries;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.toTitleCase;

/**
 * Entry tag, used to decorate entries.<br>
 * Entries may have any number of tags, which are displayed besides their names.<br><br>
 * For example, entries that require restart, or are not persistent display so using
 * the tags {@link EntryTag#REQUIRES_RESTART} and {@link EntryTag#NON_PERSISTENT}.<br>
 */
public class EntryTag implements Comparable<EntryTag> {
	private static final Map<TextFormatting, EntryTag> TAGS = Maps.newHashMap();
	private static final Map<TextFormatting, EntryTag> BOOKMARKS = Maps.newHashMap();
	private static final Map<TextFormatting, EntryTag> WRENCHES = Maps.newHashMap();
	
	public static final EntryTag REQUIRES_RESTART = translated(
	  200, "Requires Restart!", Entries.REQUIRES_RESTART,
	  "simpleconfig.config.tag.requires_restart", null);
	public static final EntryTag EXPERIMENTAL = translated(
	  300, "EXPERIMENTAL!", Entries.EXPERIMENTAL, "simpleconfig.config.tag.experimental", null);
	public static final EntryTag ADVANCED = coloredWrench(TextFormatting.GOLD);
	public static final EntryTag OPERATOR = translated(
	  99, "Operator", Entries.WRENCH.withTint(TextFormatting.BLUE), "simpleconfig.config.tag.operator", null);
	public static final EntryTag NON_PERSISTENT = translated(
	  400, null, Entries.NOT_PERSISTENT, "simpleconfig.config.tag.not_persistent", null);
	
	public static @NotNull EntryTag coloredTag(TextFormatting color) {
		String name = toTitleCase(color.getFriendlyName());
		return TAGS.computeIfAbsent(color, c -> of(
		  500 + color.getColorIndex(), name + " tag", Entries.TAG.withTint(color), () -> SimpleConfigTextUtil.splitTtc(
		  "simpleconfig.config.tag.tag", new StringTextComponent(name)
				.mergeStyle(color)), null));
	}
	public static @NotNull EntryTag coloredBookmark(TextFormatting color) {
		String name = toTitleCase(color.getFriendlyName());
		return BOOKMARKS.computeIfAbsent(color, c -> of(
		  600 + color.getColorIndex(), name + " bookmark", Entries.BOOKMARK.withTint(color), () -> SimpleConfigTextUtil.splitTtc(
		  "simpleconfig.config.tag.bookmark", new StringTextComponent(name)
				.mergeStyle(color)), null));
	}
	public static @NotNull EntryTag coloredWrench(TextFormatting color) {
		return WRENCHES.computeIfAbsent(color, c -> translated(
		  100 + color.getColorIndex(), "Advanced!", Entries.WRENCH.withTint(color),
		  "simpleconfig.config.tag.advanced", null));
	}
	
	public static @NotNull EntryTag copyTag(int order, String text, String tooltipTranslationKey) {
		return copyTag(order, text, () -> SimpleConfigTextUtil.splitTtc(tooltipTranslationKey));
	}
	
	public static @NotNull EntryTag copyTag(int order, String text, Supplier<List<ITextComponent>> tooltip) {
		return of(
		  order, null, Entries.COPY, tooltip,
		  b -> Minecraft.getInstance().keyboardListener.setClipboardString(text));
	}
	
	public static @NotNull EntryTag translated(
	  @Nullable String comment, Icon icon, String tooltipTranslationKey,
	  @Nullable Consumer<Integer> clickAction
	) {
		return translated(0, comment, icon, tooltipTranslationKey, clickAction);
	}
	
	public static @NotNull EntryTag translated(
	  int order, @Nullable String comment, Icon icon, String tooltipTranslationKey,
	  @Nullable Consumer<Integer> clickAction
	) {
		return of(order, comment, icon, () -> SimpleConfigTextUtil.splitTtc(tooltipTranslationKey), clickAction);
	}
	
	public static @NotNull EntryTag of(
	  @Nullable String comment, Icon icon, @Nullable Supplier<List<ITextComponent>> tooltip,
	  @Nullable Consumer<Integer> clickAction
	) {
		return of(0, comment, icon, tooltip, clickAction);
	}
	
	public static @NotNull EntryTag of(
	  int order, @Nullable String comment, Icon icon,
	  @Nullable Supplier<List<ITextComponent>> tooltip, @Nullable Consumer<Integer> clickAction
	) {
		return new EntryTag(order, comment, icon, tooltip, clickAction);
	}
	
	private static int STAMP;
	private final int id = STAMP++;
	private final @Nullable String comment;
	private final int order;
	private final Icon icon;
	private final @Nullable Supplier<List<ITextComponent>> tooltip;
	private final Consumer<Integer> clickAction;
	
	public EntryTag(
	  int order, @Nullable String comment, Icon icon,
	  @Nullable Supplier<List<ITextComponent>> tooltip, @Nullable Consumer<Integer> clickAction
	) {
		this.order = order;
		this.comment = comment;
		this.icon = icon;
		this.tooltip = tooltip;
		this.clickAction = clickAction;
	}
	
	public void onClick(int button) {
		if (clickAction != null) clickAction.accept(button);
	}
	
	public Icon getIcon() {
		return icon;
	}
	
	public List<ITextComponent> getTooltip() {
		return tooltip != null? tooltip.get() : Collections.emptyList();
	}
	
	public @Nullable String getComment() {
		return comment;
	}
	
	public int getOrder() {
		return order;
	}
	
	public int getId() {
		return id;
	}
	
	@Override public int compareTo(@NotNull EntryTag o) {
		return new CompareToBuilder()
		  .append(order, o.order)
		  .append(id, o.id).build();
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EntryTag entryFlag = (EntryTag) o;
		return order == entryFlag.order && Objects.equals(icon, entryFlag.icon)
		       && Objects.equals(comment, entryFlag.comment)
		       && Objects.equals(tooltip, entryFlag.tooltip);
	}
	
	@Override public int hashCode() {
		return Objects.hash(order, icon, comment, tooltip);
	}
}
