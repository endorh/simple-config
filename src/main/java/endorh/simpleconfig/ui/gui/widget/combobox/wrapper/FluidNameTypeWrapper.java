package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidNameTypeWrapper extends ResourceLocationTypeWrapper {
	public FluidNameTypeWrapper() {
		super(20);
	}
	
	@Override public void renderIcon(
	  @Nullable ResourceLocation element, String text, @NotNull PoseStack mStack,
	  int x, int y, int w, int h,
	  int blitOffset, int mouseX, int mouseY, float delta
	) {
		final Fluid fluid = ForgeRegistries.FLUIDS.getValue(element);
		if (fluid != null) {
			ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
			// float prevBlitOffset = itemRenderer.blitOffset;
			// itemRenderer.blitOffset = blitOffset;
			itemRenderer.renderGuiItem(mStack, new ItemStack(fluid.getBucket()), x + 2, y + 2);
			// itemRenderer.blitOffset = prevBlitOffset;
		} else {
			mStack.pushPose(); {
				mStack.translate(0D, 0D, blitOffset);
				ICON_UNKNOWN.renderCentered(mStack, x, y, w, h);
			} mStack.popPose();
		}
	}
}
