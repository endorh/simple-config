package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.core.SimpleConfig;
import endorh.simpleconfig.ui.api.IModalInputCapableScreen;
import endorh.simpleconfig.ui.api.IModalInputProcessor;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer;
import endorh.simpleconfig.ui.api.RedirectGuiEventListener;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable.IRectanglePositionableRenderable;
import endorh.simpleconfig.ui.gui.widget.KeyBindSettingsButton.KeyBindSettingsOverlay;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.hotkey.*;
import endorh.simpleconfig.ui.icon.AnimatedIcon;
import endorh.simpleconfig.ui.icon.Icon;
import endorh.simpleconfig.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.ui.icon.SimpleConfigIcons.Entries;
import endorh.simpleconfig.ui.math.Rectangle;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static endorh.simpleconfig.ui.gui.WidgetUtils.pos;
import static java.lang.Math.max;

public class KeyBindButton extends FocusableGui
  implements IRectanglePositionableRenderable, IModalInputProcessor {
	private static int ID;
	private final int id = ID++;
	private final Supplier<IModalInputCapableScreen> screenSupplier;
	private final Supplier<IOverlayCapableContainer> container;
	private final Rectangle area = new Rectangle(0, 0, 80, 20);
	private final MultiFunctionIconButton button;
	private final KeyBindSettingsButton settingsButton;
	private final MultiFunctionIconButton cancelButton;
	private final RedirectGuiEventListener sideButtonReference;
	private Supplier<List<ITextComponent>> tooltipSupplier = null;
	protected final List<IGuiEventListener> listeners = new ArrayList<>();
	
	private final AnimatedIcon recordingIcon = SimpleConfigIcons.HOTKEY_RECORDING.copy();
	private final IntList keys = new IntArrayList();
	private final Int2ObjectMap<String> chars = new Int2ObjectOpenHashMap<>();
	private IntList startedKeys;
	private Int2ObjectMap<String> startedChars;
	private KeyBindMapping startedMapping;
	private @Nullable ExtendedKeyBindImpl keyBind;
	private boolean reportOverlaps = true;
	private final List<ExtendedKeyBindImpl> overlaps = new ArrayList<>();
	private int overlapTicks = 0;
	
	private boolean capturingInput = false;
	private boolean error = false;
	
	private Style hotKeyStyle = Style.EMPTY;
	private Style conflictStyle = Style.EMPTY.applyFormatting(TextFormatting.GOLD);
	private Style errorStyle = Style.EMPTY.applyFormatting(TextFormatting.RED);
	private Style recordStyle = Style.EMPTY.applyFormatting(TextFormatting.YELLOW);
	private int idleTint;
	
	public static KeyBindButton of(
	  Supplier<IModalInputCapableScreen> screen,
	  Supplier<IOverlayCapableContainer> container
	) {
		return of(screen, container, null);
	}
	
	public static KeyBindButton of(
	  Supplier<IModalInputCapableScreen> screen,
	  Supplier<IOverlayCapableContainer> container,
	  @Nullable ExtendedKeyBindImpl keyBind
	) {
		return new KeyBindButton(screen, container, keyBind);
	}
	
	public KeyBindButton(
	  Supplier<IModalInputCapableScreen> screenSupplier,
	  Supplier<IOverlayCapableContainer> container,
	  @Nullable ExtendedKeyBindImpl keyBind
	) {
		// super(0, 0, width, width, Icon.EMPTY, ButtonAction.of(() -> {}));
		this.container = container;
		this.screenSupplier = screenSupplier;
		this.keyBind = keyBind;
		button = MultiFunctionIconButton.of(
		  Icon.EMPTY, ButtonAction.of(this::startKeyInput)
		    .title(this::getDisplayedText)
			 .tooltip(this::getTooltipLines));
		idleTint = button.defaultTint;
		settingsButton = new KeyBindSettingsButton(this::getOverlayContainer);
		settingsButton.setParentRectangle(getArea());
		settingsButton.setListener(s -> updateKeyBind());
		cancelButton = MultiFunctionIconButton.of(
		  Entries.CLOSE_X, ButtonAction.of(this::cancelModalInputProcessing));
		cancelButton.setExactWidth(20);
		sideButtonReference = new RedirectGuiEventListener(settingsButton);
		if (keyBind != null) setMapping(keyBind.getDefinition());
		updateOverlaps();
		Stream.of(button, sideButtonReference, settingsButton.getOverlayReference()).forEach(listeners::add);
	}
	
	public IModalInputCapableScreen getScreen() {
		return screenSupplier.get();
	}
	public IOverlayCapableContainer getOverlayContainer() {
		return container.get();
	}
	
	public @Nullable ExtendedKeyBind getKeyBind() {
		return keyBind;
	}
	public void setKeyBind(@Nullable ExtendedKeyBindImpl keyBind) {
		this.keyBind = keyBind;
		updateKeyBind();
		updateOverlaps();
	}
	
	protected void updateKeyBind() {
		if (keyBind != null) keyBind.setCandidateDefinition(getMapping());
	}
	
	public boolean isReportOverlaps() {
		return reportOverlaps;
	}
	public void setReportOverlaps(boolean reportOverlaps) {
		this.reportOverlaps = reportOverlaps;
	}
	
	public void setTooltip(Supplier<List<ITextComponent>> tooltip) {
		tooltipSupplier = tooltip;
	}
	public void setTooltip(List<ITextComponent> tooltip) {
		setTooltip(() -> tooltip);
	}
	
	@Override public Rectangle getArea() {
		return area;
	}
	@Override public @NotNull List<IGuiEventListener> getEventListeners() {
		return listeners;
	}
	
	@Override public boolean isActive() {
		return button.active;
	}
	@Override public void setActive(boolean active) {
		button.active = active;
		settingsButton.active = active;
	}
	
	@Override public boolean isFocused() {
		return getListener() != null;
	}
	@Override public void setFocused(boolean focused) {
		setListener(focused && !listeners.isEmpty()? listeners.get(0) : null);
	}
	
	public KeyBindMapping getMapping() {
		ExtendedKeyBindSettings settings = getSettings();
		return new KeyBindMappingImpl(keys, settings.isMatchByChar()? chars : null, settings);
	}
	public void setMapping(KeyBindMapping mapping) {
		keys.clear();
		IntList keys = mapping.getRequiredKeys();
		if (keys != null) this.keys.addAll(keys);
		chars.clear();
		Int2ObjectMap<String> chars = mapping.getCharMap();
		if (chars != null) this.chars.putAll(chars);
		settingsButton.applySettings(mapping.getSettings());
		updateKeyBind();
		updateKeys();
		updateOverlaps();
	}
	
	public ExtendedKeyBindSettings getSettings() {
		return settingsButton.getSettings();
	}
	public void setSettings(ExtendedKeyBindSettings settings) {
		settingsButton.applySettings(settings);
		updateKeyBind();
		updateOverlaps();
	}
	
	public ExtendedKeyBindSettings getDefaultSettings() {
		return settingsButton.getDefaultSettings();
	}
	public void setDefaultSettings(ExtendedKeyBindSettings settings) {
		settingsButton.setDefaultSettings(settings);
	}
	
	public void updateKeys() {
		ExtendedKeyBindSettings settings = settingsButton.getSettings();
		if (settings.isMatchByChar()) {
			ListIterator<Integer> iter = keys.listIterator();
			int unmatched = Keys.FIRST_UNASSIGNED_KEY;
			while (iter.hasNext()) {
				int k = iter.next();
				if (chars.containsKey(k)) {
					String ch = chars.remove(k);
					int kk = Keys.getKeyFromChar(ch);
					if (kk == -1) kk = unmatched--;
					iter.set(kk);
					chars.put(kk, ch);
				}
			}
		} else {
			chars.clear();
			for (int k: keys) {
				String ch = Keys.getCharFromKey(k);
				if (ch != null) chars.put(k, ch);
			}
		}
	}
	
	public void updateOverlaps() {
		overlaps.clear();
		if (reportOverlaps) {
			if (keyBind != null) {
				overlaps.addAll(ExtendedKeyBindDispatcher.INSTANCE.getOverlaps(keyBind));
			} else overlaps.addAll(ExtendedKeyBindDispatcher.INSTANCE.getOverlaps(getMapping()));
		}
	}
	
	public void tick() {
		if ((++overlapTicks + id) % 20 == 0) updateOverlaps();
	}
	
	public void startKeyInput() {
		capturingInput = true;
		getScreen().claimModalInput(this);
		sideButtonReference.setTarget(cancelButton);
		startedKeys = new IntArrayList();
		startedChars = new Int2ObjectOpenHashMap<>();
		startedMapping = new KeyBindMappingImpl(startedKeys, startedChars, getSettings());
		recordingIcon.reset();
		button.setDefaultIcon(recordingIcon);
		button.setTintColor(0x80A04242);
	}
	
	public boolean isCapturingModalInput() {
		return capturingInput;
	}
	
	@Override public boolean modalKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			commitInput();
			return false;
		}
		int key = Keys.getKeyFromInput(keyCode, scanCode);
		if (startedKeys.contains(key)) return true;
		startedKeys.add(key);
		String ch = Keys.getCharFromKey(key);
		if (ch != null) startedChars.put(key, ch);
		return true;
	}
	
	@Override public boolean modalMouseClicked(double mouseX, double mouseY, int button) {
		if (cancelButton.isMouseOver(mouseX, mouseY)) return false;
		int k = Keys.getKeyFromMouseInput(button);
		if (!startedKeys.contains(k)) startedKeys.add(k);
		return true;
	}
	
	@Override public boolean modalMouseScrolled(double mouseX, double mouseY, double amount) {
		int k = Keys.getKeyFromScroll(amount);
		// Scroll keys can only be the final key in a sequence, since they can't be
		//   held down. It's impossible to use both of them in a hotkey.
		int other = Keys.getKeyFromScroll(-amount);
		if (startedKeys.contains(other)) startedKeys.remove((Integer) other);
		if (!startedKeys.contains(k)) startedKeys.add(k);
		return true;
	}
	
	@Override public boolean shouldConsumeModalClicks(double mouseX, double mouseY, int button) {
		return isMouseOver(mouseX, mouseY);
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return area.contains(mouseX, mouseY);
	}
	
	protected void commitInput() {
		if (startedKeys != null) {
			keys.clear();
			keys.addAll(startedKeys);
			chars.clear();
			if (startedChars != null) chars.putAll(startedChars);
			updateKeyBind();
			updateOverlaps();
		}
	}
	
	@Override public void cancelModalInputProcessing() {
		startedKeys = null;
		startedChars = null;
		startedMapping = null;
		capturingInput = false;
		sideButtonReference.setTarget(settingsButton);
		button.setDefaultIcon(Icon.EMPTY);
		button.setTintColor(idleTint);
	}
	
	protected Style getHotKeyStyle() {
		return isError()? errorStyle : hasOverlaps()? conflictStyle : hotKeyStyle;
	}
	
	public ITextComponent getDisplayedText() {
		if (isCapturingModalInput()) {
			if (startedKeys.isEmpty()) return new StringTextComponent(">  <").mergeStyle(recordStyle);
			return new StringTextComponent("")
			  .append(new StringTextComponent("> ").mergeStyle(recordStyle))
			  .append(startedMapping.getDisplayName())
			  .append(new StringTextComponent(" <").mergeStyle(recordStyle));
		} else {
			KeyBindMapping mapping = getMapping();
			return mapping.isUnset()
			       ? new TranslationTextComponent("key.abbrev.unset").mergeStyle(TextFormatting.GRAY)
			       : mapping.getDisplayName(getHotKeyStyle());
		}
	}
	
	public List<ITextComponent> getTooltipLines() {
		List<ITextComponent> tooltip = null;
		if (tooltipSupplier != null) tooltip = tooltipSupplier.get();
		if (overlaps.isEmpty() || isCapturingModalInput()) return tooltip != null? tooltip : Collections.emptyList();
		if (tooltip == null) {
			tooltip = new ArrayList<>();
		} else tooltip.add(new StringTextComponent(""));
		tooltip.add(new TranslationTextComponent("simpleconfig.keybind.overlaps")
		              .mergeStyle(TextFormatting.GOLD));
		overlaps.stream().map(o -> {
			IFormattableTextComponent title = o.getCandidateName().deepCopy();
			if (o.getModId() != null) title.appendString(" ").append(new StringTextComponent(
			  "(" + SimpleConfig.getModNameOrId(o.getModId()) + ")"
			).mergeStyle(TextFormatting.GRAY));
			return title
			  .append(new StringTextComponent(": ").mergeStyle(TextFormatting.DARK_GRAY))
			  .append(o.getCandidateDefinition().getDisplayName(TextFormatting.GRAY));
		}).forEach(tooltip::add);
		return tooltip;
	}
	
	@Override public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		pos(button, area.x, area.y);
		button.setExactWidth(area.width - 22);
		pos(settingsButton, area.getMaxX() - 20, area.y);
		pos(cancelButton, area.getMaxX() - 20, area.y);
		settingsButton.setWarning(!overlaps.isEmpty());
		
		button.render(mStack, mouseX, mouseY, delta);
		if (isCapturingModalInput()) {
			cancelButton.render(mStack, mouseX, mouseY, delta);
		} else settingsButton.render(mStack, mouseX, mouseY, delta);
	}
	
	public int getExtraHeight() {
		if (!settingsButton.isOverlayShown()) return 0;
		KeyBindSettingsOverlay overlay = settingsButton.getOverlay();
		return max(0, overlay.getArea().getMaxY() - area.getMaxY());
	}
	
	public void setTintColor(int color) {
		button.setTintColor(color);
		idleTint = color;
	}
	
	public boolean hasOverlaps() {
		return !overlaps.isEmpty();
	}
	
	public MultiFunctionIconButton getButton() {
		return button;
	}
	public KeyBindSettingsButton getSettingsButton() {
		return settingsButton;
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
