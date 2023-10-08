package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
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
		return ForgeRegistries.FLUIDS.getValue(name);
	}
	
	@Override protected Component getUnknownError(ResourceLocation name) {
		return Component.translatable("argument.fluid.id.invalid", name);
	}
	
	@Override public void renderIcon(
      @Nullable Fluid element, String text, @NotNull GuiGraphics gg, int x, int y,
      int w, int h, int blitOffset, int mouseX, int mouseY, float delta
	) {
		if (element != null) {
			ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
			// float prevBlitOffset = itemRenderer.blitOffset;
			// itemRenderer.blitOffset = blitOffset;
			// TODO: FIXME
			gg.renderItem(new ItemStack(element.getBucket()), x + 2, y + 2);
			// itemRenderer.blitOffset = prevBlitOffset;
		} else if (!text.isEmpty()) ICON_ERROR.renderCentered(gg, x, y, w, h);
	}
}
