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

public abstract class RegistryObjectTypeWrapper<T> implements ITypeWrapper<T> {
	protected boolean hasIcon = false;
	protected int iconSize = 20;
	
	protected RegistryObjectTypeWrapper() {}
	
	protected RegistryObjectTypeWrapper(int iconSize) {
		hasIcon = true;
		this.iconSize = iconSize;
	}
	
	@Override public boolean hasIcon() {
		return hasIcon;
	}
	
	@Override public int getIconHeight() {
		return iconSize;
	}
	
	protected abstract ResourceLocation getRegistryName(@NotNull T element);
	
	protected abstract @Nullable T getFromRegistryName(@NotNull ResourceLocation name);
	
	protected abstract Component getUnknownError(ResourceLocation name);
	
	@Override public String getName(@NotNull T element) {
		final ResourceLocation name = getRegistryName(element);
		return name.getNamespace().equals("minecraft") ? name.getPath() : name.toString();
	}
	
	@Override public Pair<Optional<T>, Optional<Component>> parseElement(
	  @NotNull String text
	) {
		try {
			final ResourceLocation name = new ResourceLocation(text);
			final T element = getFromRegistryName(name);
			if (element != null)
				return Pair.of(Optional.of(element), Optional.empty());
			return Pair.of(Optional.empty(), Optional.of(getUnknownError(name)));
		} catch (ResourceLocationException e) {
			return Pair.of(
			  Optional.empty(), Optional.of(Component.literal(e.getLocalizedMessage())));
		}
	}
	
	@Override public Component getDisplayName(@NotNull T element) {
		final ResourceLocation name = getRegistryName(element);
		if (name.getNamespace().equals("minecraft"))
			return Component.literal(name.getPath());
		return Component.literal(name.getNamespace()).withStyle(ChatFormatting.GRAY)
		  .append(Component.literal(":").withStyle(ChatFormatting.GRAY))
		  .append(Component.literal(name.getPath()).withStyle(ChatFormatting.WHITE));
	}
	
	@Override public @Nullable ITextFormatter getTextFormatter() {
		return ITextFormatter.forResourceLocation();
	}
}
