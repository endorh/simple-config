package endorh.simpleconfig.clothconfig2.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FluidNameTypeWrapper extends ResourceLocationTypeWrapper {
	public FluidNameTypeWrapper() {
		super(20);
	}
	
	@Override public void renderIcon(
	  @Nullable ResourceLocation element, String text, @NotNull MatrixStack mStack,
	  int x, int y, int w, int h,
	  int mouseX, int mouseY, float delta
	) {
		final Optional<Fluid> opt = Registry.FLUID.getOptional(element);
		if (opt.isPresent()) {
			Minecraft.getInstance().getItemRenderer().renderGuiItem(
			  new ItemStack(opt.get().getBucket()), x + 2, y + 2);
		} else ICON_UNKNOWN.renderCentered(mStack, x, y, w, h);
	}
}
