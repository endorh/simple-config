package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.ResetButton;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ButtonListEntry extends TooltipListEntry<Runnable> implements IChildListEntry {
	protected final Supplier<ITextComponent> buttonLabelSupplier;
	protected final Button button;
	protected List<IGuiEventListener> listeners;
	protected List<IGuiEventListener> childListeners;
	
	public ButtonListEntry(
	  Runnable value, ITextComponent fieldName, Supplier<ITextComponent> buttonLabelSupplier
	) {
		super(fieldName);
		this.buttonLabelSupplier = buttonLabelSupplier;
		setOriginal(value);
		setValue(value);
		button = new Button(
		  0, 0, 150, 20, buttonLabelSupplier.get(), p -> getValue().run());
		listeners = Lists.newArrayList(button);
		childListeners = Lists.newArrayList(button);
	}
	
	@Override public void setDisplayedValue(Runnable value) {}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		button.setMessage(buttonLabelSupplier.get());
		button.active = shouldRenderEditable();
		button.x = x;
		button.y = y;
		button.setWidth(w);
		button.setHeight(h);
		button.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused)
			WidgetUtils.forceUnFocus(button);
	}
	
	@Override public boolean isSelectable() {
		return false;
	}
	
	@Override protected boolean computeIsEdited() {
		return false;
	}
	
	@Override public @Nullable ResetButton getResetButton() {
		return null;
	}
	
	@Override public boolean isResettable() {
		return false;
	}
	
	@Override public boolean isRestorable() {
		return false;
	}
	
	@Override public boolean hasExternalDiff() {
		return false;
	}
	
	@Override public boolean hasAcceptedExternalDiff() {
		return false;
	}
	
	@Override protected @NotNull List<? extends IGuiEventListener> getEntryListeners() {
		return this.isChildSubEntry() ? childListeners : listeners;
	}
	
	@Override public String seekableValueText() {
		return getUnformattedString(buttonLabelSupplier.get());
	}
}
