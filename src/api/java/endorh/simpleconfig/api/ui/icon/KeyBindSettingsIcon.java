package endorh.simpleconfig.api.ui.icon;

import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindSettings;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindActivation;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindContext;
import endorh.simpleconfig.api.ui.icon.LayeredIcon.SimpleIconLayer;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Visually resumes the state of {@link ExtendedKeyBindSettings}.
 */
public class KeyBindSettingsIcon extends LayeredIcon<SimpleIconLayer> {
	@SuppressWarnings("UnusedAssignment") private static IconBuilder b = IconBuilder
	  .ofTexture(SimpleConfigIcons.TEXTURE, 256, 256)
	  .offset(140, 216);
	
	/** Size 20×20 */
	public static final Icon
	  BACKGROUND = b.size(20, 20).at(0, 0),
	  BACKGROUND_HIGHLIGHT = b.at(0, 20),
	  BACKGROUND_WARN = BACKGROUND_HIGHLIGHT.withTint(0xEEFFFF00),
	  BACKGROUND_HIGHLIGHT_WARN = BACKGROUND_HIGHLIGHT.withTint(0xFFFFFF64),
	  CONTEXT_GAME = b.cropArea(1, 1, 8, 9).cropAt(20, 0),
	  CONTEXT_MENU = b.cropAt(20, 20),
	  CONTEXT_ALL = b.cropAt(40, 0),
	  ACTIVATION_PRESS = b.cropArea(10, 1, 5, 9).cropAt(20, 0),
	  ACTIVATION_RELEASE = b.cropAt(20, 20),
	  ACTIVATION_BOTH = b.cropAt(40, 0),
	  ACTIVATION_TOGGLE = b.cropAt(40, 20),
	  ACTIVATION_TOGGLE_RELEASE = b.cropFor(55, 21),
	  ACTIVATION_REPEAT = b.cropFor(40, 21),
	  ORDER_INSENSITIVE = b.cropArea(1, 10, 8, 5).cropAt(20, 0),
	  ORDER_SENSITIVE = b.cropAt(20, 20),
	  EXTRA_KEYS_ALLOW = b.cropArea(1, 15, 8, 4).cropAt(20, 0),
	  EXTRA_KEYS_BLOCK = b.cropAt(20, 20),
	  EXCLUSIVE_NOT = b.cropArea(15, 1, 4, 9).cropAt(20, 0),
	  EXCLUSIVE_YES = b.cropAt(20, 20),
	  STORE_CODE = b.cropArea(9, 11, 5, 7).cropAt(20, 0),
	  STORE_CHAR = b.cropAt(20, 20),
	  PREVENT_NO = b.cropArea(14, 11, 5, 7).cropAt(20, 0),
	  PREVENT_YES = b.cropAt(20, 20);
	static { b = null; }
	
	private static final Map<KeyBindActivation, Icon> ACTIVATION_ICONS = Util.make(new EnumMap<>(KeyBindActivation.class), m -> {
		m.put(KeyBindActivation.PRESS, ACTIVATION_PRESS);
		m.put(KeyBindActivation.RELEASE, ACTIVATION_RELEASE);
		m.put(KeyBindActivation.BOTH, ACTIVATION_BOTH);
		m.put(KeyBindActivation.TOGGLE, ACTIVATION_TOGGLE);
		m.put(KeyBindActivation.TOGGLE_RELEASE, ACTIVATION_TOGGLE_RELEASE);
		m.put(KeyBindActivation.REPEAT, ACTIVATION_REPEAT);
	});
	
	protected SimpleIconLayer
	  backgroundLayer = new SimpleIconLayer(BACKGROUND),
	  contextLayer = new SimpleIconLayer(CONTEXT_GAME),
	  activationLayer = new SimpleIconLayer(ACTIVATION_PRESS),
	  orderLayer = new SimpleIconLayer(ORDER_INSENSITIVE),
	  extraKeysLayer = new SimpleIconLayer(EXTRA_KEYS_ALLOW),
	  exclusiveLayer = new SimpleIconLayer(EXCLUSIVE_NOT),
	  storeLayer = new SimpleIconLayer(STORE_CODE),
	  preventLayer = new SimpleIconLayer(PREVENT_NO);
	
	protected List<SimpleIconLayer> layers = Util.make(new ArrayList<>(), l -> Stream.of(
	  backgroundLayer,
	  contextLayer,
	  activationLayer,
	  orderLayer,
	  extraKeysLayer,
	  exclusiveLayer,
	  storeLayer,
	  preventLayer
	).forEach(l::add));
	
	private ExtendedKeyBindSettings settings = ExtendedKeyBindSettings.ingame().build();
	private boolean warning;
	private boolean highlight;
	
	public KeyBindSettingsIcon() {
		this(0);
	}
	
	public KeyBindSettingsIcon(int tint) {
		super(20, 20, tint);
	}
	
	protected void updateBackground() {
		Icon background =
		  warning? highlight? BACKGROUND_HIGHLIGHT_WARN : BACKGROUND_WARN
		         : highlight? BACKGROUND_HIGHLIGHT : BACKGROUND;
		backgroundLayer.setIcon(background);
	}
	
	@Override public int translateLevel(int level) {
		return 0;
	}
	
	protected void updateLayers() {
		updateBackground();
		ExtendedKeyBindSettings settings = getSettings();
		KeyBindContext context = settings.getContext();
		contextLayer.setIcon(context.getCropIcon());
		KeyBindActivation activation = settings.getActivation();
		activationLayer.setIcon(ACTIVATION_ICONS.get(activation));
		
		orderLayer.setIcon(settings.isOrderSensitive()? ORDER_SENSITIVE : ORDER_INSENSITIVE);
		extraKeysLayer.setIcon(settings.isAllowExtraKeys()? EXTRA_KEYS_ALLOW : EXTRA_KEYS_BLOCK);
		exclusiveLayer.setIcon(settings.isExclusive()? EXCLUSIVE_YES : EXCLUSIVE_NOT);
		storeLayer.setIcon(settings.isMatchByChar()? STORE_CHAR : STORE_CODE);
		preventLayer.setIcon(settings.isPreventFurther()? PREVENT_YES : PREVENT_NO);
	}
	
	@Override protected void beforeRender(int level) {
		updateBackground();
		setHighlight(level == 2);
	}
	
	@Override public @NotNull Icon withTint(int tint) {
		return new KeyBindSettingsIcon(tint);
	}
	@Override public List<SimpleIconLayer> getRenderedLayers() {
		return layers;
	}
	public ExtendedKeyBindSettings getSettings() {
		return settings;
	}
	
	public void setSettings(ExtendedKeyBindSettings settings) {
		this.settings = settings;
		updateLayers();
	}
	
	public boolean isWarning() {
		return warning;
	}
	public void setWarning(boolean warning) {
		this.warning = warning;
	}
	
	public boolean isHighlight() {
		return highlight;
	}
	public void setHighlight(boolean highlight) {
		this.highlight = highlight;
	}
}
