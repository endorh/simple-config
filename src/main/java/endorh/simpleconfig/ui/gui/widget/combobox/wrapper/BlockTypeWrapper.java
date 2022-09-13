package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockTypeWrapper extends RegistryObjectTypeWrapper<Block> {
	public BlockTypeWrapper() {
		super(20);
	}
	
	@Override protected ResourceLocation getRegistryName(@NotNull Block element) {
		return element.getRegistryName();
	}
	
	@Override protected @Nullable Block getFromRegistryName(@NotNull ResourceLocation name) {
		return ForgeRegistries.BLOCKS.getValue(name);
	}
	
	@Override protected Component getUnknownError(ResourceLocation name) {
		return new TranslatableComponent("argument.block.id.invalid", name);
	}
	
	@Override public void renderIcon(
	  @Nullable Block element, String text, @NotNull PoseStack mStack, int x, int y,
	  int w, int h, int blitOffset, int mouseX, int mouseY, float delta
	) {
		if (element != null) {
			ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
			float prevBlitOffset = itemRenderer.blitOffset;
			itemRenderer.blitOffset = blitOffset;
			itemRenderer.renderGuiItem(new ItemStack(element), x + 2, y + 2);
			itemRenderer.blitOffset = prevBlitOffset;
		} else if (!text.isEmpty()) ICON_ERROR.renderCentered(mStack, x, y, w, h);
	}
}
