package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.api.ModifierKeyCode;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.HotKeyButton;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(value = Dist.CLIENT)
public class KeyCodeEntry extends TooltipListEntry<ModifierKeyCode> implements IChildListEntry {
	protected final HotKeyButton hotKeyButton;
	protected final List<IGuiEventListener> widgets;
	protected final List<IGuiEventListener> childWidgets;
	
	@Internal public KeyCodeEntry(ITextComponent fieldName, ModifierKeyCode value) {
		super(fieldName);
		setOriginal(value.copy());
		setValue(value.copy());
		hotKeyButton = HotKeyButton.ofKey(this::getScreen, value);
		widgets = Lists.newArrayList(hotKeyButton, sideButtonReference);
		childWidgets = Lists.newArrayList(hotKeyButton);
		hotKeyActionTypes.remove(HotKeyActionTypes.ASSIGN.cast());
	}
	
	@Override public void resetValue() {
		super.resetValue();
		AbstractConfigScreen screen = getScreen();
		if (screen.getModalInputProcessor() == hotKeyButton)
			screen.cancelModalInput();
	}
	
	public boolean isAllowModifiers() {
		return hotKeyButton.isAllowModifiers();
	}
	
	public void setAllowModifiers(boolean allowModifiers) {
		hotKeyButton.setAllowModifiers(allowModifiers);
	}
	
	public boolean isAllowKey() {
		return hotKeyButton.isAllowKey();
	}
	
	public void setAllowKey(boolean allowKey) {
		hotKeyButton.setAllowKey(allowKey);
	}
	
	public boolean isAllowMouse() {
		return hotKeyButton.isAllowMouse();
	}
	
	public void setAllowMouse(boolean allowMouse) {
		hotKeyButton.setAllowMouse(allowMouse);
	}
	
	@Override public ModifierKeyCode getDisplayedValue() {
		return hotKeyButton.getKey();
	}
	
	@Override public void setDisplayedValue(ModifierKeyCode value) {
		hotKeyButton.setKey(value);
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
		hotKeyButton.active = shouldRenderEditable();
		hotKeyButton.x = x;
		hotKeyButton.y = y;
		hotKeyButton.setExactWidth(w);
		hotKeyButton.setHeight(h);
		hotKeyButton.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused)
			WidgetUtils.forceUnFocus(hotKeyButton);
	}
	
	@Override protected @NotNull List<? extends IGuiEventListener> getEntryListeners() {
		return this.isChildSubEntry() ? childWidgets : widgets;
	}
}