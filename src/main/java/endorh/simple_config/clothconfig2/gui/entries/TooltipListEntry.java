package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.api.AbstractConfigEntry;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.IChildListEntry;
import endorh.simple_config.clothconfig2.api.Tooltip;
import endorh.simple_config.clothconfig2.gui.ClothConfigScreen.TooltipSearchBarWidget;
import endorh.simple_config.clothconfig2.gui.widget.SearchBarWidget;
import endorh.simple_config.clothconfig2.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.minecraft.util.math.MathHelper.clamp;

@OnlyIn(value = Dist.CLIENT)
public abstract class TooltipListEntry<T>
  extends AbstractConfigListEntry<T> {
	@Nullable private Supplier<Optional<ITextComponent[]>> tooltipSupplier;
	protected String matchedTooltipText = null;
	
	public TooltipListEntry(ITextComponent fieldName) {
		super(fieldName);
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		Optional<ITextComponent[]> tooltip;
		super.renderEntry(
		  mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		if (getParent().isMouseOver(mouseX, mouseY)
		    && shouldProvideTooltip(mouseX, mouseY, x, y, entryWidth, entryHeight)
		    && (tooltip = this.getTooltip(mouseX, mouseY)).isPresent() && tooltip.get().length > 0) {
			addTooltip(Tooltip.of(new Point(mouseX, mouseY), postProcessTooltip(tooltip.get())));
		}
		if (matchedTooltipText != null && !matchedTooltipText.isEmpty()) {
			//noinspection ConstantConditions
			final Color color =
			  new Color((focusedMatch ? TextFormatting.GOLD : TextFormatting.YELLOW).getColor());
			bindTexture();
			RenderSystem.color4f(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, 1F);
			blit(mStack, x + entryWidth + 8, y + 1, 120, 0, 18, 18);
			RenderSystem.color4f(1F, 1F, 1F, 1F);
		}
	}
	
	protected boolean shouldProvideTooltip(
	  int mouseX, int mouseY, int x, int y, int width, int height
	) {
		// Do not show the tooltip in annoying positions
		return isMouseInside(mouseX, mouseY, x, y, width, height)
		       && mouseX > 24
		       && (mouseX < entryArea.getMaxX() - 170
		           || matchedTooltipText != null && !matchedTooltipText.isEmpty()
		              && mouseX > entryArea.getMaxX() + 8 && mouseX < entryArea.getMaxX() + 26
		           || mouseX >= entryArea.getMaxX() - 20 && mouseX < entryArea.getMaxX());
	}
	
	protected IReorderingProcessor[] postProcessTooltip(ITextComponent[] tooltip) {
		// Trim tooltip to readable width
		return Arrays.stream(tooltip).flatMap(
			 component -> Minecraft.getInstance().fontRenderer.trimStringToWidth(
            component, (int) (getConfigScreen().width * 0.6F)).stream())
		  .toArray(IReorderingProcessor[]::new);
	}
	
	public Optional<ITextComponent[]> getTooltip() {
		if (this.tooltipSupplier != null)
			return this.tooltipSupplier.get().map(this::decorateTooltip);
		return Optional.empty();
	}
	
	public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		return this.getTooltip();
	}
	
	@Nullable public Supplier<Optional<ITextComponent[]>> getTooltipSupplier() {
		return this.tooltipSupplier;
	}
	
	public void setTooltipSupplier(@Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
		this.tooltipSupplier = tooltipSupplier;
	}
	
	// Search
	
	@Override protected boolean shouldHighlight() {
		return super.shouldHighlight() || matchedTooltipText != null && !matchedTooltipText.isEmpty();
	}
	
	protected static final Pattern NEW_LINE = Pattern.compile("\\R");
	protected ITextComponent[] decorateTooltip(ITextComponent[] tooltip) {
		if (matchedTooltipText != null && !matchedTooltipText.isEmpty()) {
			final String tooltipText = Arrays.stream(tooltip).map(AbstractConfigEntry::getUnformattedString)
			  .collect(Collectors.joining("\n"));
			int i = tooltipText.indexOf(matchedTooltipText);
			if (i != -1) {
				int j = i + matchedTooltipText.length();
				List<ITextComponent> tt = Lists.newArrayList();
				final String[] lines = NEW_LINE.split(tooltipText);
				Style style = Style.EMPTY
				  .applyFormatting(focusedMatch? TextFormatting.GOLD : TextFormatting.YELLOW)
				  // .applyFormatting(TextFormatting.BOLD)
				  .applyFormatting(TextFormatting.UNDERLINE);
				for (String line : lines) {
					final int l = line.length();
					int a = clamp(i, 0, l);
					int b = clamp(j, 0, l);
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
		final SearchBarWidget searchBar = getConfigScreen().getSearchBar();
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
		if (this instanceof IChildListEntry && ((IChildListEntry) this).isChild())
			return "";
		return getTooltip().map(t -> Arrays.stream(t).map(AbstractConfigEntry::getUnformattedString)
		  .collect(Collectors.joining("\n"))).orElse("");
	}
}

