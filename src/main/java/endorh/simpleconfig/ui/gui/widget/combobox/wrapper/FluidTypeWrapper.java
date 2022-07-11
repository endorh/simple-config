package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidTypeWrapper extends RegistryObjectTypeWrapper<Fluid> {
	public FluidTypeWrapper() {
		super(20);
	}
	
	@Override protected ResourceLocation getRegistryName(@NotNull Fluid element) {
		return element.getRegistryName();
	}
	
	@Override protected @Nullable Fluid getFromRegistryName(@NotNull ResourceLocation name) {
		return Registry.FLUID.getOptional(name).orElse(null);
	}
	
	@Override protected ITextComponent getUnknownError(ResourceLocation name) {
		return new TranslationTextComponent("argument.fluid.id.invalid", name);
	}
	
	@Override public void renderIcon(
	  @Nullable Fluid element, String text, @NotNull MatrixStack mStack, int x, int y,
	  int w, int h, int mouseX, int mouseY, float delta
	) {
		if (element != null) {
			Minecraft.getInstance().getItemRenderer().renderGuiItem(
			  new ItemStack(element.getBucket()), x + 2, y + 2);
		} else if (!text.isEmpty()) ICON_ERROR.renderCentered(mStack, x, y, w, h);
	}
}
