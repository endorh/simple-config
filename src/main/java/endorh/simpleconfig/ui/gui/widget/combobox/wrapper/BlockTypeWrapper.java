package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockTypeWrapper extends RegistryObjectTypeWrapper<Block> {
	public BlockTypeWrapper() {
		super(20);
	}
	
	@Override protected ResourceLocation getRegistryName(@NotNull Block element) {
		return ForgeRegistries.BLOCKS.getKey(element);
	}
	
	@Override protected @Nullable Block getFromRegistryName(@NotNull ResourceLocation name) {
		return ForgeRegistries.BLOCKS.getValue(name);
	}
	
	@Override protected Component getUnknownError(ResourceLocation name) {
		return Component.translatable("argument.block.id.invalid", name);
	}
	
	@Override public void renderIcon(
      @Nullable Block element, String text, @NotNull GuiGraphics gg, int x, int y,
      int w, int h, int blitOffset, int mouseX, int mouseY, float delta
	) {
		if (element != null) {
			ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
			// float prevBlitOffset = itemRenderer.blitOffset;
			// itemRenderer.blitOffset = blitOffset;
			// itemRenderer.renderGuiItem(new ItemStack(element), x + 2, y + 2);
			// itemRenderer.blitOffset = prevBlitOffset;
			// FIXEEd
		} else if (!text.isEmpty()) ICON_ERROR.renderCentered(gg, x, y, w, h);
	}
}
