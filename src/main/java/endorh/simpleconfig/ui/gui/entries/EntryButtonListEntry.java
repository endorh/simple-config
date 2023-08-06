package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.api.IEntryHolder;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class EntryButtonListEntry<V, Entry extends AbstractConfigListEntry<V> & IChildListEntry>
  extends TooltipListEntry<V> implements IChildListEntry, IEntryHolder {
	
	public final Entry entry;
	protected final Button button;
	protected final Supplier<Component> buttonLabelSupplier;
	protected final List<AbstractConfigField<?>> heldEntries;
	protected List<GuiEventListener> listeners;
	protected List<GuiEventListener> childListeners;
	
	public EntryButtonListEntry(
	  Component fieldName, final Entry entry, Consumer<V> action,
	  Supplier<Component> buttonLabelSupplier
	) {
		super(fieldName);
		this.entry = entry;
		setValue(entry.getValue());
		setOriginal(entry.getOriginal());
		setDisplayedValue(entry.getDisplayedValue());
		entry.setChildSubEntry(true);
		entry.setParentEntry(this);
		entry.setName("entry");
		this.buttonLabelSupplier = buttonLabelSupplier;
		button = new MultiFunctionImageButton(
		  0, 0, 20, 20, SimpleConfigIcons.Buttons.ACCEPT,
		  ButtonAction.of(() -> action.accept(getValue()))
		    .active(() -> shouldRenderEditable() && getErrorMessage().isEmpty()));
		listeners = Lists.newArrayList(entry, button, sideButtonReference);
		childListeners = Lists.newArrayList(entry, button);
		heldEntries = Lists.newArrayList(entry);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		entry.updateFocused(isFocused);
		if (!isFocused) WidgetUtils.forceUnFocus(button);
	}
	
	@Override protected boolean computeIsEdited() {
		return getSaveConsumer() != null && super.computeIsEdited();
	}
	
	@Override public boolean isResettable() {
		return super.isResettable();
	}
	
	@Override public @Nullable V getDefaultValue() {
		return entry.getDefaultValue();
	}
	
	@Override public V getDisplayedValue() {
		return entry.getDisplayedValue();
	}
	@Override public void setDisplayedValue(V value) {
		entry.setDisplayedValue(value);
	}
	
	@Override public boolean isGroup() {
		return false;
	}
	
	@Override public int getItemHeight() {
		return entry.getItemHeight();
	}
	
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		switch (keyCode) {
			case 262: // Right
				if (getFocused() == entry) {
					setFocused(button);
					WidgetUtils.forceTransferFocus(entry, button);
					playFeedbackTap(0.4F);
					return true;
				}
				break;
			case 263: // Left
				if (getFocused() == button) {
					setFocused(entry);
					WidgetUtils.forceTransferFocus(button, entry);
					playFeedbackTap(0.4F);
					return true;
				}
				break;
		}
		return super.handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	@Override public void renderEntry(
	  PoseStack mStack, int index, int x, int y, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		entry.setEditable(isEditable());
		entry.setPreviewingExternal(isPreviewingExternal());
		super.renderEntry(mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
	}
	
	@Override public void renderChildEntry(
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		entry.renderChild(mStack, x, y, w - 22, h, mouseX, mouseY, delta);
		button.setX(x + w - 20);
		button.setY(y);
		button.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void navigate() {
		super.navigate();
		setFocused(entry);
	}
	
	@Override public List<HotKeyActionType<V, ?>> getHotKeyActionTypes() {
		return Collections.emptyList();
	}
	
	@Override public String seekableValueText() {
		return entry.seekableValueText();
	}
	
	@Override public Optional<Component[]> getTooltip(int mouseX, int mouseY) {
		if (button.isMouseOver(mouseX, mouseY))
			return Optional.of(new Component[]{buttonLabelSupplier.get()});
		if (entry instanceof TooltipListEntry) {
			if (((TooltipListEntry<?>) entry).getTooltip(mouseX, mouseY).isEmpty()
			    && ((TooltipListEntry<?>) entry).getTooltip().isPresent())
				return Optional.empty();
		}
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public List<AbstractConfigField<?>> getHeldEntries() {
		return heldEntries;
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return this.isChildSubEntry() ? childListeners : listeners;
	}
}
