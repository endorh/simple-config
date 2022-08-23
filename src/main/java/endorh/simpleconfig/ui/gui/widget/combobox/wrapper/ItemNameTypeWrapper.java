package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ItemNameTypeWrapper extends ResourceLocationTypeWrapper {
	public ItemNameTypeWrapper() {
		super(20);
	}
	
	@Override public void renderIcon(
	  @Nullable ResourceLocation element, String text, @NotNull PoseStack mStack,
	  int x, int y, int w, int h, int mouseX,
	  int mouseY, float delta
	) {
		final Optional<Item> opt = Registry.ITEM.getOptional(element);
		if (opt.isPresent()) {
			Minecraft.getInstance().getItemRenderer()
			  .renderGuiItem(new ItemStack(opt.get()), x + 2, y + 2);
		} else ICON_UNKNOWN.renderCentered(mStack, x, y, w, h);
	}
}
