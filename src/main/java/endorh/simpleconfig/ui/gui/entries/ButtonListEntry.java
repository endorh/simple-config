package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.widget.ResetButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ButtonListEntry extends TooltipListEntry<Runnable> implements IChildListEntry {
	protected final Supplier<Component> buttonLabelSupplier;
	protected final Button button;
	protected List<GuiEventListener> listeners;
	protected List<GuiEventListener> childListeners;
	
	public ButtonListEntry(
	  Runnable value, Component fieldName, Supplier<Component> buttonLabelSupplier
	) {
		super(fieldName);
		this.buttonLabelSupplier = buttonLabelSupplier;
		setOriginal(value);
		setValue(value);
		button = new Button.Builder(buttonLabelSupplier.get(), p -> getValue().run()).bounds(0, 0, 150, 20).build();
		listeners = Lists.newArrayList(button);
		childListeners = Lists.newArrayList(button);
	}
	
	@Override public void setDisplayedValue(Runnable value) {}
	
	@Override public void renderChildEntry(
      GuiGraphics gg, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		button.setMessage(buttonLabelSupplier.get());
		button.active = shouldRenderEditable();
		button.setX(x);
		button.setY(y);
		button.setWidth(w);
		button.setHeight(h);
		button.render(gg, mouseX, mouseY, delta);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused)
         ((GuiEventListener) button).setFocused(false);
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
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return this.isChildSubEntry() ? childListeners : listeners;
	}
	
	@Override public String seekableValueText() {
		return getUnformattedString(buttonLabelSupplier.get());
	}
}
