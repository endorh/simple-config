package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BlockNameTypeWrapper extends ResourceLocationTypeWrapper {
	public BlockNameTypeWrapper() {
		super(20);
	}
	
	@Override public void renderIcon(
	  @Nullable ResourceLocation element, String text, @NotNull PoseStack mStack,
	  int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		final Optional<Block> opt = Registry.BLOCK.getOptional(element);
		if (opt.isPresent()) {
			Minecraft.getInstance().getItemRenderer()
			  .renderGuiItem(new ItemStack(opt.get()), x + 2, y + 2);
		} else ICON_UNKNOWN.renderCentered(mStack, x, y, w, h);
	}
}
