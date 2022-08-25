package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidTypeWrapper extends RegistryObjectTypeWrapper<Fluid> {
	public FluidTypeWrapper() {
		super(20);
	}
	
	@Override protected ResourceLocation getRegistryName(@NotNull Fluid element) {
		return ForgeRegistries.FLUIDS.getKey(element);
	}
	
	@Override protected @Nullable Fluid getFromRegistryName(@NotNull ResourceLocation name) {
		return Registry.FLUID.getOptional(name).orElse(null);
	}
	
	@Override protected Component getUnknownError(ResourceLocation name) {
		return Component.translatable("argument.fluid.id.invalid", name);
	}
	
	@Override public void renderIcon(
	  @Nullable Fluid element, String text, @NotNull PoseStack mStack, int x, int y,
	  int w, int h, int mouseX, int mouseY, float delta
	) {
		if (element != null) {
			Minecraft.getInstance().getItemRenderer().renderGuiItem(
			  new ItemStack(element.getBucket()), x + 2, y + 2);
		} else if (!text.isEmpty()) ICON_ERROR.renderCentered(mStack, x, y, w, h);
	}
}
