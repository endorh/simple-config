package endorh.simpleconfig.clothconfig2.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ItemNameTypeWrapper extends ResourceLocationTypeWrapper {
	public ItemNameTypeWrapper() {
		super(20);
	}
	
	@Override public void renderIcon(
	  @Nullable ResourceLocation element, String text, @NotNull MatrixStack mStack,
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
