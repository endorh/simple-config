package endorh.simple_config.clothconfig2.api;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.SimpleConfigMod;
import endorh.simple_config.clothconfig2.gui.ClothConfigScreen;
import endorh.simple_config.clothconfig2.gui.entries.BaseListEntry;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static java.lang.Math.round;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractConfigListEntry<T> extends AbstractConfigEntry<T> {
	protected static final ResourceLocation CONFIG_TEX =
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png");
	private final ITextComponent fieldName;
	private boolean editable = true;
	private boolean requiresRestart;
	protected final Rectangle entryArea = new Rectangle();
	
	public AbstractConfigListEntry(ITextComponent fieldName) {
		this.fieldName = fieldName;
		setName(fieldName.getString().replace(".", ""));
	}
	
	@Override public boolean isRequiresRestart() {
		return requiresRestart;
	}
	
	@Override public void setRequiresRestart(boolean requiresRestart) {
		this.requiresRestart = requiresRestart;
	}
	
	public boolean isEditable() {
		final BaseListEntry<?, ?, ?> listParent = getListParent();
		return getConfigScreen().isEditable() && editable
		       && (listParent == null || listParent.isEditable());
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	public final int getPreferredTextColor() {
		return isEditable() ? hasErrors() ? 0xFFFF5555 : 0xFFFFFFFF : 0xFFA0A0A0;
	}
	
	public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
		return new Rectangle(
		  getParent().left, y, getParent().right - getParent().left, getItemHeight() - 4);
	}
	
	public boolean isMouseInside(
	  int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight
	) {
		return getParent().isMouseOver(mouseX, mouseY) &&
		       getEntryArea(x, y, entryWidth, entryHeight).contains(mouseX, mouseY);
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		entryArea.setBounds(x, y, entryWidth, entryHeight);
		if (isMouseInside(mouseX, mouseY, x, y, entryWidth, entryHeight)) {
			Rectangle area = getEntryArea(x, y, entryWidth, entryHeight);
			if (getParent() instanceof ClothConfigScreen.ListWidget)
				((ClothConfigScreen.ListWidget<?>) getParent()).thisTimeTarget = area;
		}
	}
	
	protected void bindTexture() {
		Minecraft.getInstance().getTextureManager().bindTexture(CONFIG_TEX);
		RenderHelper.disableStandardItemLighting();
		RenderSystem.enableBlend();
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return entryArea.contains(mouseX, mouseY);
	}
	
	@Override public int getScrollY() {
		return (int) round(entryArea.y + getParent().getScroll());
	}
	
	@Override public ITextComponent getFieldName() {
		return fieldName;
	}
}

