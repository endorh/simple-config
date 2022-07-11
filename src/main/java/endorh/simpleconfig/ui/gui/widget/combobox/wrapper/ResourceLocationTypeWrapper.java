package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import endorh.simpleconfig.ui.api.ITextFormatter;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ResourceLocationTypeWrapper implements ITypeWrapper<ResourceLocation> {
	
	boolean hasIcon = false;
	int iconHeight = 20;
	int iconWidth = 20;
	
	public ResourceLocationTypeWrapper() {}
	
	protected ResourceLocationTypeWrapper(int iconSize) {
		this(iconSize, iconSize);
	}
	
	protected ResourceLocationTypeWrapper(int iconWidth, int iconHeight) {
		hasIcon = true;
		this.iconHeight = iconHeight;
		this.iconWidth = iconWidth;
	}
	
	@Override public boolean hasIcon() {
		return hasIcon;
	}
	
	@Override public int getIconHeight() {
		return hasIcon ? iconHeight : ITypeWrapper.super.getIconHeight();
	}
	
	@Override public int getIconWidth() {
		return hasIcon ? iconWidth : ITypeWrapper.super.getIconWidth();
	}
	
	@Override public Pair<Optional<ResourceLocation>, Optional<ITextComponent>> parseElement(
	  @NotNull String text
	) {
		try {
			return Pair.of(Optional.of(new ResourceLocation(text)), Optional.empty());
		} catch (ResourceLocationException e) {
			return Pair.of(
			  Optional.empty(), Optional.of(new StringTextComponent(e.getLocalizedMessage())));
		}
	}
	
	@Override public ITextComponent getDisplayName(@NotNull ResourceLocation element) {
		if (element.getNamespace().equals("minecraft"))
			return new StringTextComponent(element.getPath());
		return new StringTextComponent(element.getNamespace()).withStyle(TextFormatting.GRAY)
		  .append(new StringTextComponent(":").withStyle(TextFormatting.GRAY))
		  .append(new StringTextComponent(element.getPath()).withStyle(TextFormatting.WHITE));
	}
	
	@Override public String getName(@NotNull ResourceLocation element) {
		return element.getNamespace().equals("minecraft") ? element.getPath() : element.toString();
	}
	
	@Override public @Nullable ITextFormatter getTextFormatter() {
		return ITextFormatter.forResourceLocation();
	}
}
