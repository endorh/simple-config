package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.SimpleConfigMod.ClientConfig.advanced;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.gui.ClothConfigScreen.TooltipSearchBarWidget;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.ResetButton;
import endorh.simpleconfig.ui.gui.widget.SearchBarWidget;
import endorh.simpleconfig.ui.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.*;
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
	protected EntryFlag helpEntryFlag;
	protected EntryFlag matchedHelpEntryFlag;
	protected MultiFunctionImageButton matchedHelpButton;
	private @Nullable ITextComponent[] lastTooltip = null;
	
	public TooltipListEntry(ITextComponent fieldName) {
		super(fieldName);
		helpEntryFlag = new EntryFlag(
		  0, SimpleConfigIcons.Entries.HELP,
		  () -> getTooltip().map(Arrays::asList).orElse(Collections.emptyList()));
		matchedHelpEntryFlag = new EntryFlag(
		  0, SimpleConfigIcons.Entries.HELP_SEARCH_MATCH,
		  () -> getTooltip().map(Arrays::asList).orElse(Collections.emptyList()));
		matchedHelpButton = new MultiFunctionImageButton(
		  0, 0, 20, 20, SimpleConfigIcons.Entries.HELP_SEARCH_MATCH, ButtonAction.of(() -> {})
		  .active(() -> false).sound(Optional::empty)
		  .tooltip(() -> getTooltip().map(Arrays::asList).orElse(Collections.emptyList()))
		  .icon(() -> isFocusedMatch()? SimpleConfigIcons.Entries.HELP_SEARCH_FOCUSED_MATCH
		                              : SimpleConfigIcons.Entries.HELP_SEARCH_MATCH));
	}
	
	@Override public void tick() {
		super.tick();
		if (this.tooltipSupplier != null) {
			lastTooltip = this.tooltipSupplier.get().map(this::decorateTooltip).orElse(null);
		} else lastTooltip = null;
		NavigableSet<EntryFlag> flags = getEntryFlags();
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
		if (getEntryList().isMouseOver(mouseX, mouseY)
		    && shouldProvideTooltip(mouseX, mouseY, x, y, entryWidth, entryHeight)
		    && (tooltip = this.getTooltip(mouseX, mouseY)).isPresent() && tooltip.get().length > 0) {
			addTooltip(Tooltip.of(new Point(mouseX, mouseY), postProcessTooltip(tooltip.get())));
		}
	}
	
	@Override protected Optional<ImageButton> getMarginButton() {
		return super.getMarginButton();
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
			 component -> Minecraft.getInstance().fontRenderer.trimStringToWidth(
            component, (int) (getScreen().width * advanced.tooltip_max_width)).stream())
		  .toArray(IReorderingProcessor[]::new);
	}
	
	public Optional<ITextComponent[]> getTooltip() {
		return Optional.ofNullable(lastTooltip);
	}
	
	protected Optional<ITextComponent[]> updateTooltip() {
		if (this.tooltipSupplier != null)
			return this.tooltipSupplier.get().map(this::decorateTooltip);
		return Optional.empty();
	}
	
	public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		final ResetButton resetButton = getResetButton();
		if (resetButton != null && resetButton.isMouseOver(mouseX, mouseY))
			return resetButton.getTooltip(mouseX, mouseY);
		if (flagsRectangle.contains(mouseX, mouseY)) {
			int index = (mouseX - flagsRectangle.x) / 14;
			final NavigableSet<EntryFlag> entryFlags = getEntryFlags();
			if (index >= 0 && index < entryFlags.size()) {
				Iterator<EntryFlag> iterator = entryFlags.iterator();
				EntryFlag flag = null;
				for (int i = 0; i <= index; i++) flag = iterator.next();
				List<ITextComponent> tooltip = flag.tooltip.get();
				return Optional.of(tooltip.toArray(new ITextComponent[0]));
			}
		}
		return this.getTooltip();
	}
	
	@Nullable public Supplier<Optional<ITextComponent[]>> getTooltipSupplier() {
		return this.tooltipSupplier;
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
			  .map(AbstractConfigEntry::getUnformattedString)
			  .collect(Collectors.joining("\n"));
			int i = tooltipText.indexOf(matchedTooltipText);
			if (i != -1) {
				int j = i + matchedTooltipText.length();
				List<ITextComponent> tt = Lists.newArrayList();
				final String[] lines = NEW_LINE.split(tooltipText);
				Style style = Style.EMPTY
				  .applyFormatting(isFocusedMatch()? TextFormatting.GOLD : TextFormatting.YELLOW)
				  // .applyFormatting(TextFormatting.BOLD)
				  .applyFormatting(TextFormatting.UNDERLINE);
				for (String line : lines) {
					final int l = line.length();
					int a = MathHelper.clamp(i, 0, l);
					int b = MathHelper.clamp(j, 0, l);
					IFormattableTextComponent ln;
					if (a != b) {
						ln = new StringTextComponent(line.substring(0, a))
						  .append(new StringTextComponent(line.substring(a, b)).setStyle(style))
						  .append(new StringTextComponent(line.substring(b)));
					} else ln = new StringTextComponent(line);
					tt.add(ln.mergeStyle(TextFormatting.GRAY));
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
		  AbstractConfigEntry::getUnformattedString
		  ).collect(Collectors.joining("\n"))).orElse("");
	}
}
