package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import endorh.simpleconfig.api.ui.ITextFormatter;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
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
	
	protected abstract ITextComponent getUnknownError(ResourceLocation name);
	
	@Override public String getName(@NotNull T element) {
		final ResourceLocation name = getRegistryName(element);
		return name.getNamespace().equals("minecraft") ? name.getPath() : name.toString();
	}
	
	@Override public Pair<Optional<T>, Optional<ITextComponent>> parseElement(
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
			  Optional.empty(), Optional.of(new StringTextComponent(e.getLocalizedMessage())));
		}
	}
	
	@Override public ITextComponent getDisplayName(@NotNull T element) {
		final ResourceLocation name = getRegistryName(element);
		if (name.getNamespace().equals("minecraft"))
			return new StringTextComponent(name.getPath());
		return new StringTextComponent(name.getNamespace()).mergeStyle(TextFormatting.GRAY)
		  .append(new StringTextComponent(":").mergeStyle(TextFormatting.GRAY))
		  .append(new StringTextComponent(name.getPath()).mergeStyle(TextFormatting.WHITE));
	}
	
	@Override public @Nullable ITextFormatter getTextFormatter() {
		return ITextFormatter.forResourceLocation();
	}
}
