package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.IMultiTooltipScreen;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer;
import endorh.simpleconfig.ui.api.RedirectGuiEventListener;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons.Hotkeys;
import endorh.simpleconfig.ui.gui.icon.KeyBindSettingsIcon;
import endorh.simpleconfig.ui.gui.widget.SelectorButton.BooleanButton;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindSettings;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindSettingsBuilder;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindSettingsImpl;
import endorh.simpleconfig.ui.hotkey.KeyBindMapping.KeyBindActivation;
import endorh.simpleconfig.ui.hotkey.KeyBindMapping.KeyBindContext;
import endorh.simpleconfig.ui.math.Point;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static endorh.simpleconfig.core.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.ui.gui.AbstractConfigScreen.drawBorderRect;
import static endorh.simpleconfig.ui.gui.WidgetUtils.pos;

public class KeyBindSettingsButton extends MultiFunctionImageButton {
	public static @Nullable ExtendedKeyBindSettings CLIPBOARD = null;
	private long copyTimestamp = 0;
	private final Supplier<IOverlayCapableContainer> container;
	private ExtendedKeyBindSettings defaultSettings = new ExtendedKeyBindSettingsImpl();
	private ExtendedKeyBindSettings settings = defaultSettings;
	private boolean overlayShown;
	private final KeyBindSettingsIcon icon;
	private final KeyBindSettingsOverlay overlay = new KeyBindSettingsOverlay(this);
	private final RedirectGuiEventListener overlayReference = new RedirectGuiEventListener(null);
	private Rectangle parentRectangle = new Rectangle();
	private @Nullable Consumer<ExtendedKeyBindSettings> listener = null;
	
	public KeyBindSettingsButton(Supplier<IOverlayCapableContainer> container) {
		this(container, 0, 0);
	}
	
	public KeyBindSettingsButton(Supplier<IOverlayCapableContainer> container, int x, int y) {
		super(x, y, 20, 20, new KeyBindSettingsIcon(),
		      ButtonAction.of(() -> {}));
		this.container = container;
		icon = (KeyBindSettingsIcon) defaultIcon;
		actions.clear();
		defaultAction = ButtonAction.of(
		  this::showOverlay, this::copySettings, this::pasteSettings
		).tooltip(this::getHint).build();
	}
	
	public void setListener(@Nullable Consumer<ExtendedKeyBindSettings> listener) {
		this.listener = listener;
	}
	
	protected void setSettings(ExtendedKeyBindSettings settings) {
		this.settings = settings;
		if (listener != null) listener.accept(settings);
	}
	public void applySettings(ExtendedKeyBindSettings settings) {
		setSettings(settings);
		overlay.applySettings(settings);
	}
	
	public void setWarning(boolean warning) {
		icon.setWarning(warning);
	}
	
	public ExtendedKeyBindSettings getDefaultSettings() {
		return defaultSettings;
	}
	public void setDefaultSettings(ExtendedKeyBindSettings settings) {
		defaultSettings = settings;
	}
	
	public void setParentRectangle(Rectangle parentRectangle) {
		this.parentRectangle = parentRectangle;
	}
	
	public ExtendedKeyBindSettings getSettings() {
		return settings;
	}
	public IOverlayCapableContainer getContainer() {
		return container.get();
	}
	public RedirectGuiEventListener getOverlayReference() {
		return overlayReference;
	}
	
	public void copySettings() {
		CLIPBOARD = getSettings();
		copyTimestamp = System.currentTimeMillis();
	}
	
	public void pasteSettings() {
		applySettings(CLIPBOARD == null? defaultSettings : CLIPBOARD);
	}
	
	public void showOverlay() {
		if (overlayShown) {
			// Click through
			hideOverlay();
		} else {
			Rectangle area = positionOverlay();
			getContainer().addOverlay(area, overlay, 10);
			overlayShown = true;
			overlayReference.setTarget(overlay);
		}
	}
	
	public void hideOverlay() {
		overlayShown = false;
		overlayReference.setTarget(null);
	}
	
	protected void clickedOutside(int mouseX, int mouseY) {
		if (!isMouseOver(mouseX, mouseY)) hideOverlay();
	}
	
	protected Rectangle positionOverlay() {
		IOverlayCapableContainer container = getContainer();
		Rectangle area = overlay.getArea();
		Rectangle parent = parentRectangle.isEmpty()
		                   ? new Rectangle(x, y, getWidth(), getHeightRealms())
		                   : parentRectangle;
		int w = container.getScreenWidth();
		int h = container.getScreenHeight();
		int offset = 2;
		boolean right = parent.getCenterX() > w / 2;
		boolean bottom = parent.getCenterY() > h / 2;
		area.x = right? parent.getMinX() - area.width - offset
		              : parent.getMaxX() + offset;
		area.y = bottom? parent.getMaxY() - area.height
		               : parent.getMinY();
		if (area.getMaxX() > w || area.x < 0 || area.y < 10) {
			area.x = right? parent.getMaxX() - area.width
			              : parent.getMinX();
			area.y = bottom? parent.getMinY() - area.height - offset
			               : parent.getMaxY() + offset;
		}
		return area;
	}
	
	@Override public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (overlayShown) positionOverlay();
		icon.setHighlight(isMouseOver(mouseX, mouseY) || overlayShown);
		if (icon.getSettings() != settings) icon.setSettings(settings);
		super.render(mStack, mouseX, mouseY, delta);
	}
	
	public List<ITextComponent> getHint() {
		return overlayShown? Collections.emptyList() : splitTtc(
		  System.currentTimeMillis() - copyTimestamp < 1000
		  ? "simpleconfig.keybind.settings.copied" : "simpleconfig.keybind.settings:help");
	}
	
	public boolean isOverlayShown() {
		return overlayShown;
	}
	
	public KeyBindSettingsOverlay getOverlay() {
		return overlay;
	}
	
	public static class KeyBindSettingsOverlay extends FocusableOverlayRenderer {
		private final KeyBindSettingsButton button;
		private final SelectorButton<KeyBindActivation> activationButton;
		private final SelectorButton<KeyBindContext> contextButton;
		private final BooleanButton allowExtraKeysCheckBox;
		private final BooleanButton orderSensitiveCheckBox;
		private final BooleanButton exclusiveCheckBox;
		private final BooleanButton matchByCharCheckBox;
		private final BooleanButton preventFurtherCheckBox;
		
		public KeyBindSettingsOverlay(KeyBindSettingsButton button) {
			this.button = button;
			activationButton = SelectorButton.of(
			  KeyBindActivation.class,
			  t -> new TranslationTextComponent("simpleconfig.keybind.activation." + t.name().toLowerCase()),
			  KeyBindActivation::getIcon, t -> update());
			contextButton = SelectorButton.of(
			  KeyBindContext.getAllContexts(), KeyBindContext::getDisplayName,
			  KeyBindContext::getIcon, t -> update());
			allowExtraKeysCheckBox = BooleanButton.of(
			  TextFormatting.GREEN, TextFormatting.LIGHT_PURPLE,
			  Hotkeys.EXTRA_KEYS_ALLOW, Hotkeys.EXTRA_KEYS_BLOCK, b -> update());
			orderSensitiveCheckBox = BooleanButton.of(
			  TextFormatting.GOLD, TextFormatting.AQUA,
			  Hotkeys.ORDER_SENSITIVE, Hotkeys.ORDER_INSENSITIVE, b -> update());
			exclusiveCheckBox = BooleanButton.of(
			  TextFormatting.GOLD, TextFormatting.AQUA,
			  Hotkeys.EXCLUSIVE_YES, Hotkeys.EXCLUSIVE_NO, b -> update());
			matchByCharCheckBox = BooleanButton.of(
			  TextFormatting.LIGHT_PURPLE, TextFormatting.GREEN,
			  Hotkeys.MATCH_BY_NAME, Hotkeys.MATCH_BY_CODE, b -> update());
			preventFurtherCheckBox = BooleanButton.of(
			  TextFormatting.GREEN, TextFormatting.GOLD,
			  Hotkeys.PREVENT_FURTHER_YES, Hotkeys.PREVENT_FURTHER_NO, b -> update());
			Stream.of(
			  activationButton, contextButton, allowExtraKeysCheckBox, orderSensitiveCheckBox,
			  exclusiveCheckBox, matchByCharCheckBox, preventFurtherCheckBox
			).forEach(listeners::add);
		}
		
		protected void update() {
			setSettings(new ExtendedKeyBindSettingsBuilder(getSettings())
			  .withActivation(activationButton.getValue())
			  .withContext(contextButton.getValue())
			  .setAllowExtraKeys(allowExtraKeysCheckBox.getToggle())
			  .setOrderSensitive(orderSensitiveCheckBox.getToggle())
			  .setExclusive(exclusiveCheckBox.getToggle())
			  .setMatchByChar(matchByCharCheckBox.getToggle())
			  .setPreventFurther(preventFurtherCheckBox.getToggle())
			  .build());
		}
		
		@Override public Rectangle getArea() {
			Rectangle area = super.getArea();
			area.width = 180;
			area.height = 8 + 20 * 7 + 2 * 6;
			return area;
		}
		
		@Override public void overlayMouseClickedOutside(
		  Rectangle area, double mouseX, double mouseY, int button
		) {
			super.overlayMouseClickedOutside(area, mouseX, mouseY, button);
			this.button.clickedOutside((int) mouseX, (int) mouseY);
		}
		
		@Override public boolean overlayEscape() {
			button.hideOverlay();
			hide();
			return true;
		}
		
		public ExtendedKeyBindSettings getSettings() {
			return button.getSettings();
		}
		public void setSettings(ExtendedKeyBindSettings settings) {
			button.setSettings(settings);
		}
		
		public void applySettings(ExtendedKeyBindSettings settings) {
			activationButton.setValue(settings.getActivation());
			contextButton.setValue(settings.getContext());
			allowExtraKeysCheckBox.setToggle(settings.isAllowExtraKeys());
			orderSensitiveCheckBox.setToggle(settings.isOrderSensitive());
			exclusiveCheckBox.setToggle(settings.isExclusive());
			matchByCharCheckBox.setToggle(settings.isMatchByChar());
			preventFurtherCheckBox.setToggle(settings.isPreventFurther());
		}
		
		@Override public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
			Rectangle area = getArea();
			drawBorderRect(mStack, area, 1, 0xEEEEEEEE, 0xEE242424);
			FontRenderer font = Minecraft.getInstance().fontRenderer;
			int to = 10 - (font.FONT_HEIGHT + 1) / 2;
			int xx = area.x + 4;
			int yy = area.y + 4;
			int bw = 70;
			int bx = area.getMaxX() - bw - 4;
			List<ITextComponent> tooltip = null;
			boolean tooltipInXRange = mouseX >= xx + 2 && mouseX < bx - 2;
			drawString(mStack, font, getLabel("activation"), xx + 2, yy + to, 0xE0E0E0E0);
			if (tooltipInXRange && mouseY >= yy + 2 && mouseY < yy + 18) tooltip = getTooltip("activation");
			pos(activationButton, bx, yy, bw);
			yy += 22;
			drawString(mStack, font, getLabel("context"), xx + 2, yy + to, 0xE0E0E0E0);
			if (tooltipInXRange && mouseY >= yy + 2 && mouseY < yy + 18) tooltip = getTooltip("context");
			pos(contextButton, bx, yy, bw);
			yy += 22;
			drawString(mStack, font, getLabel("allow_extra_keys"), xx + 2, yy + to, 0xE0E0E0E0);
			if (tooltipInXRange && mouseY >= yy + 2 && mouseY < yy + 18) tooltip = getTooltip("allow_extra_keys");
			pos(allowExtraKeysCheckBox, bx, yy, bw);
			yy += 22;
			drawString(mStack, font, getLabel("order_sensitive"), xx + 2, yy + to, 0xE0E0E0E0);
			if (tooltipInXRange && mouseY >= yy + 2 && mouseY < yy + 18) tooltip = getTooltip("order_sensitive");
			pos(orderSensitiveCheckBox, bx, yy, bw);
			yy += 22;
			drawString(mStack, font, getLabel("exclusive"), xx + 2, yy + to, 0xE0E0E0E0);
			if (tooltipInXRange && mouseY >= yy + 2 && mouseY < yy + 18) tooltip = getTooltip("exclusive");
			pos(exclusiveCheckBox, bx, yy, bw);
			yy += 22;
			drawString(mStack, font, getLabel("match_by_char"), xx + 2, yy + to, 0xE0E0E0E0);
			if (tooltipInXRange && mouseY >= yy + 2 && mouseY < yy + 18) tooltip = getTooltip("match_by_char");
			pos(matchByCharCheckBox, bx, yy, bw);
			yy += 22;
			drawString(mStack, font, getLabel("prevent_further"), xx + 2, yy + to, 0xE0E0E0E0);
			if (tooltipInXRange && mouseY >= yy + 2 && mouseY < yy + 18) tooltip = getTooltip("prevent_further");
			pos(preventFurtherCheckBox, bx, yy, bw);
			
			ExtendedKeyBindSettings def = button.defaultSettings;
			int editedColor = getEditedTint(), defaultColor = getDefaultTint();
			activationButton.setTintColor(def.getActivation() != activationButton.getValue()? editedColor : defaultColor);
			contextButton.setTintColor(def.getContext() != contextButton.getValue()? editedColor : defaultColor);
			allowExtraKeysCheckBox.setTintColor(def.isAllowExtraKeys() != allowExtraKeysCheckBox.getToggle()? editedColor : defaultColor);
			orderSensitiveCheckBox.setTintColor(def.isOrderSensitive() != orderSensitiveCheckBox.getToggle()? editedColor : defaultColor);
			exclusiveCheckBox.setTintColor(def.isExclusive() != exclusiveCheckBox.getToggle()? editedColor : defaultColor);
			matchByCharCheckBox.setTintColor(def.isMatchByChar() != matchByCharCheckBox.getToggle()? editedColor : defaultColor);
			preventFurtherCheckBox.setTintColor(def.isPreventFurther() != preventFurtherCheckBox.getToggle()? editedColor : defaultColor);
			
			activationButton.render(mStack, mouseX, mouseY, delta);
			contextButton.render(mStack, mouseX, mouseY, delta);
			allowExtraKeysCheckBox.render(mStack, mouseX, mouseY, delta);
			orderSensitiveCheckBox.render(mStack, mouseX, mouseY, delta);
			exclusiveCheckBox.render(mStack, mouseX, mouseY, delta);
			matchByCharCheckBox.render(mStack, mouseX, mouseY, delta);
			preventFurtherCheckBox.render(mStack, mouseX, mouseY, delta);
			
			if (tooltip != null) renderTooltip(tooltip, mouseX, mouseY);
		}
		
		protected void renderTooltip(List<ITextComponent> tooltip, int mouseX, int mouseY) {
			Screen screen = Minecraft.getInstance().currentScreen;
			if (screen instanceof IMultiTooltipScreen)
				((IMultiTooltipScreen) screen).addTooltip(Tooltip.of(
				  Point.of(mouseX, mouseY), tooltip));
		}
		
		protected int getEditedTint() {
			return 0x64646424;
		}
		
		protected int getDefaultTint() {
			return 0x64424242;
		}
		
		protected IFormattableTextComponent getLabel(String translationKey) {
			return new TranslationTextComponent("simpleconfig.keybind.setting." + translationKey);
		}
		
		protected List<ITextComponent> getTooltip(String translationKey) {
			return splitTtc("simpleconfig.keybind.setting." + translationKey + ":help");
		}
	}
}
