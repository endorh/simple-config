package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.SimpleConfigTextUtil;
import endorh.simpleconfig.config.ClientConfig.advanced;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen.TooltipSearchBarWidget;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.SearchBarWidget;
import endorh.simpleconfig.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.ui.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public abstract class TooltipListEntry<T> extends AbstractConfigListEntry<T> {
	@Nullable private Supplier<Optional<ITextComponent[]>> tooltipSupplier;
	protected String matchedTooltipText = null;
	protected EntryTag helpEntryFlag;
	protected EntryTag matchedHelpEntryFlag;
	protected MultiFunctionImageButton matchedHelpButton;
	private @Nullable ITextComponent[] lastTooltip = null;
	
	protected TooltipListEntry(ITextComponent fieldName) {
		super(fieldName);
		helpEntryFlag = new EntryTag(
		  -200, null, SimpleConfigIcons.Entries.HELP,
		  () -> getTooltip().map(Arrays::asList).orElse(Collections.emptyList()), null);
		matchedHelpEntryFlag = new EntryTag(
		  -200, null, SimpleConfigIcons.Entries.HELP_SEARCH_MATCH,
		  () -> getTooltip().map(Arrays::asList).orElse(Collections.emptyList()), null);
		matchedHelpButton = new MultiFunctionImageButton(
		  0, 0, 20, 20, SimpleConfigIcons.Entries.HELP_SEARCH_MATCH, ButtonAction.of(() -> {})
		  .active(() -> false).sound(Optional::empty)
		  .tooltip(() -> getTooltip().map(Arrays::asList).orElse(Collections.emptyList()))
		  .icon(() -> isFocusedMatch()? SimpleConfigIcons.Entries.HELP_SEARCH_FOCUSED_MATCH
		                              : SimpleConfigIcons.Entries.HELP_SEARCH_MATCH));
	}
	
	@Override public void tick() {
		super.tick();
		if (tooltipSupplier != null) {
			lastTooltip = tooltipSupplier.get().map(this::decorateTooltip).orElse(null);
		} else lastTooltip = null;
		NavigableSet<EntryTag> flags = getEntryTags();
		if (lastTooltip != null) {
			if (matchesTooltipSearch()) {
				flags.remove(helpEntryFlag);
				flags.add(matchedHelpEntryFlag);
			} else {
				flags.remove(matchedHelpEntryFlag);
				flags.add(helpEntryFlag);
			}
		} else {
			flags.remove(helpEntryFlag);
			flags.remove(matchedHelpEntryFlag);
		}
	}
	
	protected boolean shouldUseHelpButton() {
		return true;
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int x, int y, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		Optional<ITextComponent[]> tooltip;
		if (getScreen().isShowingHelp() && getEntryList().getSelectedEntry() == this) {
			getTooltip().ifPresent(
			  t -> addTooltip(Tooltip.of(Point.of(rowArea.x - 12, rowArea.getMaxY() + 12), t)));
		} else if (getEntryList().isMouseOver(mouseX, mouseY)
		    && shouldProvideTooltip(mouseX, mouseY, x, y, entryWidth, entryHeight)
		    && (tooltip = getTooltip(mouseX, mouseY)).isPresent() && tooltip.get().length > 0) {
			addTooltip(Tooltip.of(Point.of(mouseX, mouseY), postProcessTooltip(tooltip.get())));
		}
	}
	
	protected boolean shouldProvideTooltip(
	  int mouseX, int mouseY, int x, int y, int width, int height
	) {
		// Do not show the tooltip in annoying positions
		return !getScreen().isDragging()
		       && (isMouseOverFlags(mouseX, mouseY) || !shouldUseHelpButton())
		       && isMouseOverRow(mouseX, mouseY);
	}
	
	protected IReorderingProcessor[] postProcessTooltip(ITextComponent[] tooltip) {
		// Trim tooltip to readable width
		return Arrays.stream(tooltip).flatMap(
			 component -> Minecraft.getInstance().font.split(
            component, (int) (getScreen().width * advanced.tooltip_max_width)).stream())
		  .toArray(IReorderingProcessor[]::new);
	}
	
	public Optional<ITextComponent[]> getTooltip() {
		return Optional.ofNullable(lastTooltip);
	}
	
	protected Optional<ITextComponent[]> updateTooltip() {
		if (tooltipSupplier != null)
			return tooltipSupplier.get().map(this::decorateTooltip);
		return Optional.empty();
	}
	
	@Override public boolean onMouseClicked(double mouseX, double mouseY, int button) {
		if (flagsRectangle.contains(mouseX, mouseY)) {
			NavigableSet<EntryTag> entryTags = getEntryTags();
			int index = ((int) mouseX - flagsRectangle.x) / 14;
			if (index >= 0 && index < entryTags.size()) {
				Iterator<EntryTag> iterator = entryTags.iterator();
				EntryTag flag = null;
				for (int i = 0; i <= index; i++) flag = iterator.next();
				flag.onClick(button);
			}
			return true;
		}
		return super.onMouseClicked(mouseX, mouseY, button);
	}
	
	public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (flagsRectangle.contains(mouseX, mouseY)) {
			int index = (mouseX - flagsRectangle.x) / 14;
			final NavigableSet<EntryTag> entryFlags = getEntryTags();
			if (index >= 0 && index < entryFlags.size()) {
				Iterator<EntryTag> iterator = entryFlags.iterator();
				EntryTag flag = null;
				for (int i = 0; i <= index; i++) flag = iterator.next();
				List<ITextComponent> tooltip = flag.getTooltip();
				return Optional.of(tooltip.toArray(new ITextComponent[0]));
			}
		}
		return getTooltip();
	}
	
	@Nullable public Supplier<Optional<ITextComponent[]>> getTooltipSupplier() {
		return tooltipSupplier;
	}
	
	public void setTooltipSupplier(@Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
		this.tooltipSupplier = tooltipSupplier;
	}
	
	// Search
	
	@Override protected boolean matchesSearch() {
		return super.matchesSearch() || matchesTooltipSearch();
	}
	
	protected boolean matchesTooltipSearch() {
		return matchedTooltipText != null && !matchedTooltipText.isEmpty();
	}
	
	protected static final Pattern NEW_LINE = Pattern.compile("\\R");
	protected ITextComponent[] decorateTooltip(ITextComponent[] tooltip) {
		if (matchedTooltipText != null && !matchedTooltipText.isEmpty()) {
			final String tooltipText = Arrays.stream(tooltip)
			  .map(AbstractConfigField::getUnformattedString)
			  .collect(Collectors.joining("\n"));
			int i = tooltipText.indexOf(matchedTooltipText);
			if (i != -1) {
				int j = i + matchedTooltipText.length();
				List<ITextComponent> tt = Lists.newArrayList();
				Style style = Style.EMPTY
				  .applyFormat(isFocusedMatch()? TextFormatting.GOLD : TextFormatting.YELLOW)
				  // .applyFormatting(TextFormatting.BOLD)
				  .applyFormat(TextFormatting.UNDERLINE);
				for (ITextComponent line: tooltip) {
					final int l = getUnformattedString(line).length();
					int a = MathHelper.clamp(i, 0, l);
					int b = MathHelper.clamp(j, 0, l);
					IFormattableTextComponent ln;
					if (a != b) {
						ln = SimpleConfigTextUtil.applyStyle(line, style, a, b);
					} else ln = line.copy();
					tt.add(ln);
					i -= l + 1;
					j -= l + 1;
				}
				return tt.toArray(new ITextComponent[0]);
			}
		}
		return tooltip;
	}
	
	@Override protected boolean searchSelf(Pattern query) {
		final SearchBarWidget searchBar = getScreen().getSearchBar();
		boolean matches = false;
		matchedTooltipText = null;
		if (searchBar instanceof TooltipSearchBarWidget && ((TooltipSearchBarWidget) searchBar).isSearchTooltips()) {
			final String tooltipText = seekableTooltipString();
			if (!tooltipText.isEmpty()) {
				final Matcher m = query.matcher(tooltipText);
				while (m.find()) {
					if (!m.group().isEmpty()) {
						matches = true;
						matchedTooltipText = m.group();
						break;
					}
				}
			}
		}
		return super.searchSelf(query) || matches;
	}
	
	protected String seekableTooltipString() {
		if (isChildSubEntry()) return "";
		return getTooltip().map(t -> Arrays.stream(t).map(
		  AbstractConfigField::getUnformattedString
		  ).collect(Collectors.joining("\n"))).orElse("");
	}
}
