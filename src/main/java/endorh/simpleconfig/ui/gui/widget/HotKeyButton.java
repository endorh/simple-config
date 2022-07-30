package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.IModalInputCapableScreen;
import endorh.simpleconfig.ui.api.IModalInputProcessor;
import endorh.simpleconfig.ui.api.Modifier;
import endorh.simpleconfig.ui.api.ModifierKeyCode;
import endorh.simpleconfig.ui.gui.AnimatedIcon;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class HotKeyButton extends MultiFunctionIconButton implements IModalInputProcessor {
	private final Supplier<IModalInputCapableScreen> screenSupplier;
	private AnimatedIcon recordingIcon = SimpleConfigIcons.HOTKEY_RECORDING.copy();
	private ModifierKeyCode key = ModifierKeyCode.unknown();
	private @Nullable ModifierKeyCode startedKeyCode = null;
	private boolean allowKey = true;
	private boolean allowMouse = true;
	private boolean allowModifiers = true;
	private boolean capturingInput = false;
	private boolean conflict = false;
	private boolean error = false;
	private Style hotKeyStyle = Style.EMPTY;
	private Style conflictStyle = Style.EMPTY.applyFormatting(TextFormatting.GOLD);
	private Style errorStyle = Style.EMPTY.applyFormatting(TextFormatting.RED);
	private Style recordStyle = Style.EMPTY.applyFormatting(TextFormatting.YELLOW);
	private int idleTint = defaultTint;
	
	public static HotKeyButton ofKey(Supplier<IModalInputCapableScreen> screen, ModifierKeyCode key) {
		HotKeyButton button = new HotKeyButton(screen, 80);
		button.setKey(key);
		return button;
	}
	
	public static HotKeyButton ofMouse(Supplier<IModalInputCapableScreen> screen, ModifierKeyCode key) {
		HotKeyButton button = new HotKeyButton(screen, 80);
		button.setKey(key);
		button.setAllowMouse(true);
		button.setAllowKey(false);
		return button;
	}
	
	public static HotKeyButton ofKeyAndMouse(Supplier<IModalInputCapableScreen> screen, ModifierKeyCode key) {
		HotKeyButton button = new HotKeyButton(screen, 80);
		button.setKey(key);
		button.setAllowMouse(true);
		return button;
	}
	
	public HotKeyButton(Supplier<IModalInputCapableScreen> screenSupplier, int width) {
		super(0, 0, width, width, Icon.EMPTY, ButtonAction.of(() -> {}));
		actions.clear();
		on(MultiFunctionImageButton.Modifier.NONE, ButtonAction.of(this::startKeyInput)
		  .title(this::getDisplayedText));
		this.screenSupplier = screenSupplier;
	}
	
	public IModalInputCapableScreen getScreen() {
		return screenSupplier.get();
	}
	
	public ModifierKeyCode getKey() {
		return key;
	}
	
	public void setKey(ModifierKeyCode key) {
		this.key = key;
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
	
	public boolean isAllowModifiers() {
		return allowModifiers;
	}
	
	public void setAllowModifiers(boolean allowModifiers) {
		this.allowModifiers = allowModifiers;
	}
	
	public void startKeyInput() {
		capturingInput = true;
		getScreen().claimModalInput(this);
		recordingIcon.reset();
		setDefaultIcon(recordingIcon);
		super.setTintColor(0x80A04242);
	}
	
	public boolean isCapturingModalInput() {
		return capturingInput;
	}
	
	@Override public boolean modalKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			setKey(ModifierKeyCode.unknown());
			startedKeyCode = null;
			return false;
		}
		ModifierKeyCode key = startedKeyCode;
		if (key == null) key = startedKeyCode = ModifierKeyCode.unknown();
		if (isAllowModifiers()) {
			Modifier mod = Modifier.ofKeyCode(keyCode);
			if (mod != null) {
				key.addModifier(mod);
				return true;
			}
		}
		if (isAllowKey()) {
			key.setKeyCode(InputMappings.getInputByCode(keyCode, scanCode));
			setKey(key);
			cancelModalInputProcessing();
			return false;
		}
		return true;
	}
	
	@Override public boolean modalKeyReleased(int keyCode, int scanCode, int modifiers) {
		// if (startedKeyCode != null && isAllowKey()) {
		// 	setKey(startedKeyCode);
		// 	cancelModalInputProcessing();
		// 	return false;
		// }
		return true;
	}
	
	@Override public boolean modalMouseClicked(double mouseX, double mouseY, int button) {
		ModifierKeyCode key = startedKeyCode;
		if (key == null) key = startedKeyCode = ModifierKeyCode.unknown();
		if (isAllowMouse()) {
			key.setKeyCode(Type.MOUSE.getOrMakeInput(button));
			setKey(key);
			cancelModalInputProcessing();
			return false;
		}
		return true;
	}
	
	@Override public boolean modalMouseReleased(double mouseX, double mouseY, int button) {
		// if (startedKeyCode != null && !startedKeyCode.isUnknown() && isAllowMouse()) {
		// 	setKey(startedKeyCode);
		// 	cancelModalInputProcessing();
		// 	return false;
		// }
		return true;
	}
	
	@Override public boolean shouldConsumeModalClicks(double mouseX, double mouseY, int button) {
		return isMouseOver(mouseX, mouseY);
	}
	
	@Override public void cancelModalInputProcessing() {
		startedKeyCode = null;
		capturingInput = false;
		setDefaultIcon(Icon.EMPTY);
		super.setTintColor(idleTint);
	}
	
	protected Style getHotKeyStyle() {
		return isError()? errorStyle : isConflict()? conflictStyle : hotKeyStyle;
	}
	
	public ITextComponent getDisplayedText() {
		if (isCapturingModalInput()) {
			ModifierKeyCode key = startedKeyCode;
			if (key == null) return new StringTextComponent(">  <").mergeStyle(recordStyle);
			return new StringTextComponent("")
			  .append(new StringTextComponent("> ").mergeStyle(recordStyle))
			  .append(key.getLocalizedName(hotKeyStyle, hotKeyStyle))
			  .append(new StringTextComponent(" <").mergeStyle(recordStyle));
		} else {
			Style style = getHotKeyStyle();
			return getKey().getLocalizedName(style, style);
		}
	}
	
	@Override public void renderButton(
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float partialTicks
	) {
		super.renderButton(mStack, mouseX, mouseY, partialTicks);
	}
	
	@Override public void setTintColor(int color) {
		super.setTintColor(color);
		idleTint = color;
	}
	
	public boolean isConflict() {
		return conflict;
	}
	
	public void setConflict(boolean conflict) {
		this.conflict = conflict;
	}
	
	public boolean isError() {
		return error;
	}
	
	public void setError(boolean error) {
		this.error = error;
	}
	
	public void setHotKeyStyle(Style hotKeyStyle) {
		this.hotKeyStyle = hotKeyStyle;
	}
	
	public Style getConflictStyle() {
		return conflictStyle;
	}
	
	public void setConflictStyle(Style conflictStyle) {
		this.conflictStyle = conflictStyle;
	}
	
	public Style getErrorStyle() {
		return errorStyle;
	}
	
	public void setErrorStyle(Style errorStyle) {
		this.errorStyle = errorStyle;
	}
	
	public Style getRecordStyle() {
		return recordStyle;
	}
	
	public void setRecordStyle(Style recordStyle) {
		this.recordStyle = recordStyle;
	}
}
