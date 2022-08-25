package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
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
		return ForgeRegistries.BLOCKS.getKey(element);
	}
	
	@Override protected @Nullable Block getFromRegistryName(@NotNull ResourceLocation name) {
		return Registry.BLOCK.getOptional(name).orElse(null);
	}
	
	@Override protected Component getUnknownError(ResourceLocation name) {
		return Component.translatable("argument.block.id.invalid", name);
	}
	
	@Override public void renderIcon(
	  @Nullable Block element, String text, @NotNull PoseStack mStack, int x, int y,
	  int w, int h, int mouseX, int mouseY, float delta
	) {
		if (element != null) {
			Minecraft.getInstance().getItemRenderer()
			  .renderGuiItem(new ItemStack(element), x + 2, y + 2);
		} else if (!text.isEmpty()) ICON_ERROR.renderCentered(mStack, x, y, w, h);
	}
}
