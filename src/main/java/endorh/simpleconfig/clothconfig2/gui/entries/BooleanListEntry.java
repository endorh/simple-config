package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
import endorh.simpleconfig.clothconfig2.gui.widget.ResetButton;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public class BooleanListEntry extends TooltipListEntry<Boolean> implements IChildListEntry {
	protected final AtomicBoolean bool;
	protected final Button buttonWidget;
	protected final ResetButton resetButton;
	protected final List<IGuiEventListener> widgets;
	protected final List<IGuiEventListener> childWidgets;
	protected @NotNull Function<Boolean, ITextComponent> yesNoSupplier = bool ->
	  new TranslationTextComponent("text.cloth-config.boolean.value." + bool);
	protected boolean child = false;
	
	@Deprecated
	@ApiStatus.Internal
	public BooleanListEntry(
	  ITextComponent fieldName, boolean value
	) {
		super(fieldName);
		this.original = value;
		this.bool = new AtomicBoolean(value);
		this.buttonWidget = new Button(
		  0, 0, 150, 20, NarratorChatListener.NO_TITLE, widget -> {
			  if (!isSelected) {
				  preserveState();
				  isSelected = true;
			  }
			  bool.set(!bool.get());
		  });
		this.resetButton = new ResetButton(this);
		this.widgets = Lists.newArrayList(buttonWidget, resetButton);
		this.childWidgets = Lists.newArrayList(buttonWidget);
	}
	
	public void setYesNoSupplier(
	  @NotNull Function<Boolean, ITextComponent> yesNoSupplier
	) {
		this.yesNoSupplier = yesNoSupplier;
	}
	
	@Override public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		if (!isSelected)
			WidgetUtils.forceUnFocus(buttonWidget, resetButton);
	}
	
	@Override
	public Boolean getValue() {
		return this.bool.get();
	}
	
	@Override public void setValue(Boolean value) {
		this.bool.set(value);
	}
	
	@Override
	public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(
		  mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		MainWindow window = Minecraft.getInstance().getWindow();
		resetButton.y = y;
		int buttonX;
		ITextComponent name = getDisplayedFieldName();
		final FontRenderer font = Minecraft.getInstance().font;
		if (font.isBidirectional()) {
			font.drawShadow(
			  mStack, name.getVisualOrderText(),
			  (float) (window.getGuiScaledWidth() - x - font.width(name)),
			  (float) (y + 6), 0xFFFFFF);
			resetButton.x = x;
			buttonX = x + resetButton.getWidth() + 2;
		} else {
			font.drawShadow(
			  mStack, name.getVisualOrderText(), (float) x, (float) (y + 6),
			  getPreferredTextColor());
			resetButton.x = x + entryWidth - resetButton.getWidth();
			buttonX = x + entryWidth - 150;
		}
		resetButton.render(mStack, mouseX, mouseY, delta);
		renderChild(mStack, buttonX, y, 150 - resetButton.getWidth() - 2, 20, mouseX, mouseY, delta);
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		buttonWidget.active = isEditable();
		buttonWidget.x = x;
		buttonWidget.y = y;
		buttonWidget.setWidth(w);
		buttonWidget.setHeight(h);
		buttonWidget.setMessage(getYesNoText(bool.get()));
		buttonWidget.render(mStack, mouseX, mouseY, delta);
	}
	
	public ITextComponent getYesNoText(boolean bool) {
		return yesNoSupplier.apply(bool);
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (resetButton.isMouseOver(mouseX, mouseY))
			return resetButton.getTooltip(mouseX, mouseY);
		if (buttonWidget.isMouseOver(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	public @NotNull List<? extends IGuiEventListener> children() {
		return isChild()? childWidgets : this.widgets;
	}
	
	@Override public boolean isChild() {
		return child;
	}
	
	@Override public void setChild(boolean child) {
		this.child = child;
	}
	
	@Override public String seekableValueText() {
		return getUnformattedString(yesNoSupplier.apply(getValue()));
	}
}

