package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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
	
	@Override protected ITextComponent getUnknownError(ResourceLocation name) {
		return new TranslationTextComponent("argument.block.id.invalid", name);
	}
	
	@Override public void renderIcon(
	  @Nullable Block element, String text, @NotNull MatrixStack mStack, int x, int y,
	  int w, int h, int blitOffset, int mouseX, int mouseY, float delta
	) {
		if (element != null) {
			ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
			float prevBlitOffset = itemRenderer.zLevel;
			itemRenderer.zLevel = blitOffset;
			itemRenderer.renderItemIntoGUI(new ItemStack(element), x + 2, y + 2);
			itemRenderer.zLevel = prevBlitOffset;
		} else if (!text.isEmpty()) ICON_ERROR.renderCentered(mStack, x, y, w, h);
	}
}
