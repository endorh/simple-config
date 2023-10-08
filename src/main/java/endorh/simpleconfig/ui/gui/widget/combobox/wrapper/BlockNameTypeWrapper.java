package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockNameTypeWrapper extends ResourceLocationTypeWrapper {
	public BlockNameTypeWrapper() {
		super(20);
	}
	
	@Override public void renderIcon(
	  @Nullable ResourceLocation element, String text, @NotNull GuiGraphics gg,
	  int x, int y, int w, int h, int blitOffset, int mouseX, int mouseY, float delta
	) {
		final Block block = ForgeRegistries.BLOCKS.getValue(element);
		if (block != null) {
			ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
			// FIXME
			// float prevBlitOffset = itemRenderer.blitOffset;
			// itemRenderer.blitOffset = blitOffset;
			// TODO: FIX?
			gg.renderItem(new ItemStack(block), x + 2, y + 2);
			// itemRenderer.renderGuiItem(gg, new ItemStack(block), x + 2, y + 2);
			// itemRenderer.blitOffset = prevBlitOffset;
		} else {
			PoseStack mStack = gg.pose();
			mStack.pushPose(); {
				mStack.translate(0D, 0D, blitOffset);
				ICON_UNKNOWN.renderCentered(gg, x, y, w, h);
			} mStack.popPose();
		}
	}
}
