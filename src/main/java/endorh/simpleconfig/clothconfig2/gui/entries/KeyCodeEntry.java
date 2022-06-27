package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.api.ModifierKeyCode;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
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

@OnlyIn(value = Dist.CLIENT)
public class KeyCodeEntry extends TooltipListEntry<ModifierKeyCode> implements IChildListEntry {
	protected ModifierKeyCode displayedValue;
	protected final Button buttonWidget;
	protected final List<IGuiEventListener> widgets;
	protected final List<IGuiEventListener> childWidgets;
	protected boolean allowMouse = true;
	protected boolean allowKey = true;
	protected boolean allowModifiers = true;
	
	@Internal public KeyCodeEntry(ITextComponent fieldName, ModifierKeyCode value) {
		super(fieldName);
		setOriginal(value.copy());
		setValue(value.copy());
		displayedValue = value.copy();
		buttonWidget = new Button(0, 0, 150, 20, NarratorChatListener.NO_TITLE,
		                               widget -> getScreen().setFocusedBinding(this));
		widgets = Lists.newArrayList(buttonWidget, resetButton);
		childWidgets = Lists.newArrayList(buttonWidget);
	}
	
	@Override public void resetValue() {
		super.resetValue();
		getScreen().setFocusedBinding(null);
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
	
	@Override public ModifierKeyCode getDisplayedValue() {
		return displayedValue;
	}
	
	@Override public void setDisplayedValue(ModifierKeyCode value) {
		displayedValue = value;
	}
	
	@Override public ModifierKeyCode getDefaultValue() {
		final ModifierKeyCode v = super.getDefaultValue();
		return v == null ? null : v.copy();
	}
	
	private ITextComponent getLocalizedName() {
		return getDisplayedValue().getLocalizedName();
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		buttonWidget.active = shouldRenderEditable();
		buttonWidget.setMessage(getLocalizedName());
		if (getScreen().getFocusedBinding() == this) {
			buttonWidget.setMessage(
			  new StringTextComponent("> ").withStyle(TextFormatting.WHITE).append(
				 buttonWidget.getMessage().plainCopy().withStyle(TextFormatting.YELLOW)).append(
				 new StringTextComponent(" <").withStyle(TextFormatting.WHITE)));
		}
		buttonWidget.x = x;
		buttonWidget.y = y;
		buttonWidget.setWidth(w);
		buttonWidget.setHeight(h);
		buttonWidget.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused)
			WidgetUtils.forceUnFocus(buttonWidget);
	}
	
	@Override protected @NotNull List<? extends IGuiEventListener> getEntryListeners() {
		return this.isChildSubEntry() ? childWidgets : widgets;
	}
}