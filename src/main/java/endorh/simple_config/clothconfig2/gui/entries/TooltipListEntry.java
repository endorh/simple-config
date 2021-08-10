package endorh.simple_config.clothconfig2.gui.entries;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.Tooltip;
import endorh.simple_config.clothconfig2.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public abstract class TooltipListEntry<T>
  extends AbstractConfigListEntry<T> {
	@Nullable
	private Supplier<Optional<ITextComponent[]>> tooltipSupplier;
	
	@Deprecated
	@ApiStatus.Internal
	public TooltipListEntry(
	  ITextComponent fieldName, @Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this(fieldName, tooltipSupplier, false);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public TooltipListEntry(
	  ITextComponent fieldName, @Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier,
	  boolean requiresRestart
	) {
		super(fieldName, requiresRestart);
		this.tooltipSupplier = tooltipSupplier;
	}
	
	@Override
	public void render(
	  MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		Optional<ITextComponent[]> tooltip;
		super.render(
		  matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		if (this.isMouseInside(mouseX, mouseY, x, y, entryWidth, entryHeight) &&
		    (tooltip = this.getTooltip(mouseX, mouseY)).isPresent() && tooltip.get().length > 0) {
			this.addTooltip(
			  Tooltip.of(new Point(mouseX, mouseY), this.postProcessTooltip(tooltip.get())));
		}
	}
	
	private IReorderingProcessor[] postProcessTooltip(ITextComponent[] tooltip) {
		return Arrays.stream(tooltip).flatMap(
			 component -> Minecraft.getInstance().fontRenderer.trimStringToWidth(
            component, this.getConfigScreen().width).stream())
		  .toArray(IReorderingProcessor[]::new);
	}
	
	public Optional<ITextComponent[]> getTooltip() {
		if (this.tooltipSupplier != null) {
			return this.tooltipSupplier.get();
		}
		return Optional.empty();
	}
	
	public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		return this.getTooltip();
	}
	
	@Nullable
	public Supplier<Optional<ITextComponent[]>> getTooltipSupplier() {
		return this.tooltipSupplier;
	}
	
	public void setTooltipSupplier(@Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
		this.tooltipSupplier = tooltipSupplier;
	}
}

