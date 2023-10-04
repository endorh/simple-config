package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindSettings;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.widget.KeyBindButton;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindImpl;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@OnlyIn(value = Dist.CLIENT)
public class KeyBindListEntry extends TooltipListEntry<KeyBindMapping> implements IChildListEntry {
	protected final KeyBindButton hotKeyButton;
	protected final List<GuiEventListener> widgets;
	protected final List<GuiEventListener> childWidgets;
	
	@Internal public KeyBindListEntry(
	  Component fieldName, KeyBindMapping value, @Nullable ExtendedKeyBindImpl keyBind
	) {
		super(fieldName);
		setOriginal(value.copy());
		setValue(value.copy());
		hotKeyButton = KeyBindButton.of(this::getScreen, this::getScreen, keyBind);
		hotKeyButton.setMapping(value);
		widgets = Lists.newArrayList(hotKeyButton, sideButtonReference);
		childWidgets = Lists.newArrayList(hotKeyButton);
	}
	
	@Override public void tick() {
		super.tick();
		hotKeyButton.tick();
	}
	
	@Override public int getExtraScrollHeight() {
		return hotKeyButton.getExtraHeight();
	}
	
	@Override public void resetValue() {
		super.resetValue();
		AbstractConfigScreen screen = getScreen();
		if (screen.getModalInputProcessor() == hotKeyButton)
			screen.cancelModalInput();
	}
	
	public ExtendedKeyBindSettings getDefaultSettings() {
		return hotKeyButton.getDefaultSettings();
	}
	public void setDefaultSettings(ExtendedKeyBindSettings settings) {
		hotKeyButton.setDefaultSettings(settings);
	}
	
	public boolean isReportOverlaps() {
		return hotKeyButton.isReportOverlaps();
	}
	public void setReportOverlaps(boolean reportOverlaps) {
		hotKeyButton.setReportOverlaps(reportOverlaps);
	}
	
	@Override public KeyBindMapping getDisplayedValue() {
		return hotKeyButton.getMapping();
	}
	
	@Override public void setDisplayedValue(KeyBindMapping value) {
		hotKeyButton.setMapping(value);
	}
	
	@Override public KeyBindMapping getDefaultValue() {
		final KeyBindMapping v = super.getDefaultValue();
		return v == null ? null : v.copy();
	}
	
	private Component getLocalizedName() {
		return getDisplayedValue().getDisplayName();
	}
	
	@Override public void renderChildEntry(
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		hotKeyButton.setActive(shouldRenderEditable());
		hotKeyButton.setPosition(x, y, w);
		hotKeyButton.setHeight(h);
		hotKeyButton.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused)
         ((GuiEventListener) hotKeyButton).setFocused(false);
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return this.isChildSubEntry() ? childWidgets : widgets;
	}
}