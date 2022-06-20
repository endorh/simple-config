package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ButtonListEntry extends TooltipListEntry<Runnable> implements IChildListEntry {
	protected final Supplier<ITextComponent> buttonLabelSupplier;
	protected final AtomicReference<Runnable> value = new AtomicReference<>();
	protected final Button button;
	protected List<IGuiEventListener> listeners;
	protected List<IGuiEventListener> childListeners;
	protected boolean child = false;
	
	public ButtonListEntry(
	  Runnable value, ITextComponent fieldName, Supplier<ITextComponent> buttonLabelSupplier
	) {
		super(fieldName);
		original = value;
		this.buttonLabelSupplier = buttonLabelSupplier;
		this.value.set(value);
		button = new Button(
		  0, 0, 150, 20, buttonLabelSupplier.get(), p -> getValue().run());
		listeners = Lists.newArrayList(button);
		childListeners = Lists.newArrayList(button);
	}
	
	@Override public Runnable getValue() {
		return value.get();
	}
	
	@Override public void setValue(Runnable value) {
		this.value.set(value);
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		MainWindow window = Minecraft.getInstance().getWindow();
		int buttonX;
		final FontRenderer font = Minecraft.getInstance().font;
		final ITextComponent name = getDisplayedFieldName();
		if (font.isBidirectional()) {
			font.drawShadow(
			  mStack, name.getVisualOrderText(),
			  (float)(window.getGuiScaledWidth() - x - font.width(name)),
			  (float)(y + 6), getPreferredTextColor());
			buttonX = x;
		} else {
			font.drawShadow(
			  mStack, name.getVisualOrderText(), (float) x,
			  (float) (y + 6), getPreferredTextColor());
			buttonX = x + entryWidth - 150;
		}
		renderChild(mStack, buttonX, y, 150, 20, mouseX, mouseY, delta);
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		button.setMessage(buttonLabelSupplier.get());
		button.active = isEditable();
		button.x = x;
		button.y = y;
		button.setWidth(w);
		button.setHeight(h);
		button.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		if (!isSelected)
			WidgetUtils.forceUnFocus(button);
	}
	
	@Override public boolean isEdited() {
		return false;
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> children() {
		return isChild()? childListeners : listeners;
	}
	
	@Override public boolean isChild() {
		return child;
	}
	
	@Override public void setChild(boolean child) {
		this.child = child;
	}
	
	@Override public String seekableValueText() {
		return getUnformattedString(buttonLabelSupplier.get());
	}
}
