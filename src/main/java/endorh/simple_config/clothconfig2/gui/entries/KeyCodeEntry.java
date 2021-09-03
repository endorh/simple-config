package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.api.IChildListEntry;
import endorh.simple_config.clothconfig2.api.ModifierKeyCode;
import endorh.simple_config.clothconfig2.gui.widget.ResetButton;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@OnlyIn(value = Dist.CLIENT)
public class KeyCodeEntry
  extends TooltipListEntry<ModifierKeyCode> implements IChildListEntry {
	protected ModifierKeyCode value;
	protected final Button buttonWidget;
	protected final ResetButton resetButton;
	protected final List<IGuiEventListener> widgets;
	protected final List<IGuiEventListener> childWidgets;
	protected boolean allowMouse = true;
	protected boolean allowKey = true;
	protected boolean allowModifiers = true;
	protected boolean child = false;
	
	@Internal public KeyCodeEntry(ITextComponent fieldName, ModifierKeyCode value) {
		super(fieldName);
		this.value = value.copy();
		original = value.copy();
		buttonWidget = new Button(0, 0, 150, 20, NarratorChatListener.EMPTY,
		                               widget -> getConfigScreen().setFocusedBinding(this));
		resetButton = new ResetButton(this);
		widgets = Lists.newArrayList(buttonWidget, resetButton);
		childWidgets = Lists.newArrayList(buttonWidget);
	}
	
	@Override public void resetValue(boolean commit) {
		super.resetValue(commit);
		getConfigScreen().setFocusedBinding(null);
	}
	
	public boolean isAllowModifiers() {
		return allowModifiers;
	}
	
	public void setAllowModifiers(boolean allowModifiers) {
		this.allowModifiers = allowModifiers;
	}
	
	public boolean isAllowKey() {
		return allowKey;
	}
	
	public void setAllowKey(boolean allowKey) {
		this.allowKey = allowKey;
	}
	
	public boolean isAllowMouse() {
		return allowMouse;
	}
	
	public void setAllowMouse(boolean allowMouse) {
		this.allowMouse = allowMouse;
	}
	
	@Override
	public ModifierKeyCode getValue() {
		return value;
	}
	
	@Override public void setValue(ModifierKeyCode value) {
		this.value = value;
	}
	
	@Override public ModifierKeyCode getDefaultValue() {
		final ModifierKeyCode v = super.getDefaultValue();
		return v == null ? null : v.copy();
	}
	
	private ITextComponent getLocalizedName() {
		return value.getLocalizedName();
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (resetButton.isMouseOver(mouseX, mouseY))
			return resetButton.getTooltip(mouseX, mouseY);
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override
	public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(
		  mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		MainWindow window = Minecraft.getInstance().getMainWindow();
		resetButton.y = y;
		int buttonX;
		ITextComponent name = getDisplayedFieldName();
		final FontRenderer font = Minecraft.getInstance().fontRenderer;
		if (font.getBidiFlag()) {
			font.func_238407_a_(
			  mStack, name.func_241878_f(),
			  (float) (window.getScaledWidth() - x - font.getStringPropertyWidth(name)),
			  (float) (y + 6), 0xFFFFFF);
			resetButton.x = x;
			buttonX = x + resetButton.getWidth() + 2;
		} else {
			font.func_238407_a_(
			  mStack, name.func_241878_f(), (float) x, (float) (y + 6), getPreferredTextColor());
			resetButton.x = x + entryWidth - resetButton.getWidth();
			buttonX = x + entryWidth - 150;
		}
		renderChild(mStack, buttonX, y, 150 - resetButton.getWidth() - 2, 20, mouseX, mouseY, delta);
		resetButton.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		buttonWidget.active = isEditable();
		buttonWidget.setMessage(getLocalizedName());
		if (getConfigScreen().getFocusedBinding() == this) {
			buttonWidget.setMessage(
			  new StringTextComponent("> ").mergeStyle(TextFormatting.WHITE).append(
				 buttonWidget.getMessage().copyRaw().mergeStyle(TextFormatting.YELLOW)).append(
				 new StringTextComponent(" <").mergeStyle(TextFormatting.WHITE)));
		}
		buttonWidget.x = x;
		buttonWidget.y = y;
		buttonWidget.setWidth(w);
		buttonWidget.setHeight(h);
		buttonWidget.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		if (!isSelected)
			forceUnFocus(buttonWidget, resetButton);
	}
	
	public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return isChild()? childWidgets : widgets;
	}
	
	@Override public boolean isChild() {
		return child;
	}
	
	@Override public void setChild(boolean child) {
		this.child = child;
	}
}

