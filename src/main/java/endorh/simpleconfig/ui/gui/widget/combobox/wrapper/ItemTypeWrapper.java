package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemTypeWrapper extends RegistryObjectTypeWrapper<Item> {
	public ItemTypeWrapper() {
		super(20);
	}
	
	@Override protected ResourceLocation getRegistryName(@NotNull Item element) {
		return element.getRegistryName();
	}
	
	@Override protected @Nullable Item getFromRegistryName(@NotNull ResourceLocation name) {
		return ForgeRegistries.ITEMS.getValue(name);
	}
	
	@Override protected Component getUnknownError(ResourceLocation name) {
		return new TranslatableComponent("argument.item.id.invalid", name);
	}
	
	@Override public void renderIcon(
	  @Nullable Item element, String text, @NotNull PoseStack mStack, int x, int y,
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
