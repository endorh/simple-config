package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidTypeWrapper extends RegistryObjectTypeWrapper<Fluid> {
	public FluidTypeWrapper() {
		super(20);
	}
	
	@Override protected ResourceLocation getRegistryName(@NotNull Fluid element) {
		return element.getRegistryName();
	}
	
	@Override protected @Nullable Fluid getFromRegistryName(@NotNull ResourceLocation name) {
		return ForgeRegistries.FLUIDS.getValue(name);
	}
	
	@Override protected ITextComponent getUnknownError(ResourceLocation name) {
		return new TranslationTextComponent("argument.fluid.id.invalid", name);
	}
	
	@Override public void renderIcon(
	  @Nullable Fluid element, String text, @NotNull MatrixStack mStack, int x, int y,
	  int w, int h, int blitOffset, int mouseX, int mouseY, float delta
	) {
		if (element != null) {
			ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
			float prevBlitOffset = itemRenderer.zLevel;
			itemRenderer.zLevel = blitOffset;
			itemRenderer.renderItemIntoGUI(new ItemStack(element.getFilledBucket()), x + 2, y + 2);
			itemRenderer.zLevel = prevBlitOffset;
		} else if (!text.isEmpty()) ICON_ERROR.renderCentered(mStack, x, y, w, h);
	}
}
