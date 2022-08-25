package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import endorh.simpleconfig.api.ui.ITextFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
	
	@Override public Pair<Optional<ResourceLocation>, Optional<Component>> parseElement(
	  @NotNull String text
	) {
		try {
			return Pair.of(Optional.of(new ResourceLocation(text)), Optional.empty());
		} catch (ResourceLocationException e) {
			return Pair.of(
			  Optional.empty(), Optional.of(Component.literal(e.getLocalizedMessage())));
		}
	}
	
	@Override public Component getDisplayName(@NotNull ResourceLocation element) {
		if (element.getNamespace().equals("minecraft"))
			return Component.literal(element.getPath());
		return Component.literal(element.getNamespace()).withStyle(ChatFormatting.GRAY)
		  .append(Component.literal(":").withStyle(ChatFormatting.GRAY))
		  .append(Component.literal(element.getPath()).withStyle(ChatFormatting.WHITE));
	}
	
	@Override public String getName(@NotNull ResourceLocation element) {
		return element.getNamespace().equals("minecraft") ? element.getPath() : element.toString();
	}
	
	@Override public @Nullable ITextFormatter getTextFormatter() {
		return ITextFormatter.forResourceLocation();
	}
}
