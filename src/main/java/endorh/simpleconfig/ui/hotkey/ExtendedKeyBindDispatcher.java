package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBind;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindProvider;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindProxy.ExtendedKeyBindRegistrar;
import endorh.simpleconfig.api.ui.hotkey.InputMatchingContext;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class ExtendedKeyBindDispatcher {
	public static final ExtendedKeyBindDispatcher INSTANCE = new ExtendedKeyBindDispatcher();
	
	// Minecraft can sometimes miss release events for the Windows keys, and
	//   having hotkeys on release for them is a bad idea anyways.
	private static final IntSet AUTO_RELEASED_KEYS = Util.make(
	  new IntOpenHashSet(2), s -> IntStream.of(
		 GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER).forEach(s::add));
	
	// Accessed from ExtendedKeyBindProxy by reflection
	@SuppressWarnings("unused") protected static final ExtendedKeyBindRegistrar REGISTRAR =
	  new ExtendedKeyBindRegistrar() {
		  @Override public void registerProvider(ExtendedKeyBindProvider provider) {
			  ExtendedKeyBindDispatcher.registerProvider(provider);
		  }
		
		  @Override public void unregisterProvider(ExtendedKeyBindProvider provider) {
			  ExtendedKeyBindDispatcher.unregisterProvider(provider);
		  }
	  };
	
	protected InputMatchingContext ctx = new InputMatchingContextImpl();
	protected WeakHashMap<ExtendedKeyBindProvider, ExtendedKeyBindProviderTicket> tickets = new WeakHashMap<>();
	protected SortedMap<ExtendedKeyBindProviderTicket, Integer> providers = new TreeMap<>();
	
	public static void registerProvider(ExtendedKeyBindProvider provider) {
		ExtendedKeyBindProviderTicket ticket = INSTANCE.tickets.computeIfAbsent(
		  provider, p -> new ExtendedKeyBindProviderTicket(provider));
		INSTANCE.providers.remove(ticket);
		INSTANCE.providers.put(ticket, provider.getPriority());
	}
	
	public static void unregisterProvider(ExtendedKeyBindProvider provider) {
		ExtendedKeyBindProviderTicket ticket = INSTANCE.tickets.remove(provider);
		if (ticket != null) INSTANCE.providers.remove(ticket);
	}
	
	protected Collection<ExtendedKeyBindProvider> getSortedProviders() {
		// Revise priorities
		Iterator<Entry<ExtendedKeyBindProviderTicket, Integer>> iter = this.providers.entrySet().iterator();
		List<ExtendedKeyBindProviderTicket> added = new ArrayList<>();
		while (iter.hasNext()) {
			Entry<ExtendedKeyBindProviderTicket, Integer> e = iter.next();
			ExtendedKeyBindProviderTicket ticket = e.getKey();
			ExtendedKeyBindProvider provider = ticket.getProvider();
			if (provider.getPriority() != e.getValue()) {
				iter.remove();
				added.add(ticket);
			}
		}
		added.forEach(p -> this.providers.put(p, p.getProvider().getPriority()));
		return providers.keySet().stream()
		  .map(ExtendedKeyBindProviderTicket::getProvider)
		  .collect(Collectors.toList());
	}
	
	protected boolean press(int key, int scanCode) {
		int k = Keys.getKeyFromInput(key, scanCode);
		Int2ObjectMap<String> charMap = ctx.getCharMap();
		if (!charMap.containsKey(k) && scanCode > 0) {
			String ch = GLFW.glfwGetKeyName(key, scanCode);
			charMap.put(k, ch);
			if (ch != null) ctx.getPressedChars().add(ch);
		} else {
			String ch = charMap.get(k);
			if (ch != null) ctx.getPressedChars().add(ch);
		}
		return press(k);
	}
	
	protected boolean press(int key) {
		ctx.setPreventFurther(false);
		ctx.getSortedPressedKeys().add(key);
		ctx.getPressedKeys().add(key);
		updateKeyBinds();
		return ctx.isPreventFurther();
	}
	
	protected boolean release(int key) {
		ctx.setPreventFurther(false);
		ctx.getSortedPressedKeys().remove((Integer) key);
		IntSet pressedKeys = ctx.getPressedKeys();
		pressedKeys.remove(key);
		updateKeyBinds();
		if (pressedKeys.isEmpty() || pressedKeys.size() == 1 && AUTO_RELEASED_KEYS.containsAll(pressedKeys)) {
			resetContext();
		} else {
			String ch = ctx.getCharMap().get(key);
			if (ch != null) ctx.getPressedChars().remove(ch);
		}
		return ctx.isPreventFurther();
	}
	
	public void resetContext() {
		ctx.getPressedKeys().clear();
		ctx.getSortedPressedKeys().clear();
		// The char map could change on keyboard layout changes
		// We only keep it while at least one key is pressed
		ctx.getCharMap().clear();
		ctx.getPressedChars().clear();
		ctx.getRepeatableKeyBinds().clear();
		ctx.setCancelled(false);
		ctx.setTriggered(false);
		ctx.setPreventFurther(false);
	}
	
	public void updateKeyBinds() {
		Collection<ExtendedKeyBindProvider> providers = getSortedProviders();
		// The cancel function isn't used
		if (!ctx.isCancelled()) for (ExtendedKeyBindProvider ticket: providers) {
			for (ExtendedKeyBind keyBind: ticket.getActiveKeyBinds()) {
				keyBind.updatePressed(ctx);
				if (ctx.isCancelled()) return;
			}
		}
	}
	
	// Highest should probably be left for mods that remap events.
	@SubscribeEvent(priority=EventPriority.HIGH)
	public void onKeyInput(InputEvent.Key event) {
		int action = event.getAction();
		int key = event.getKey();
		int sc = event.getScanCode();
		boolean preventFurther = false;
		switch (action) {
			case GLFW.GLFW_PRESS:
				preventFurther = press(key, sc);
				break;
			case GLFW.GLFW_RELEASE:
				preventFurther = release(Keys.getKeyFromInput(key, sc));
				break;
			case GLFW.GLFW_REPEAT:
				for (ExtendedKeyBind keyBind: ctx.getRepeatableKeyBinds()) keyBind.onRepeat();
				break;
		}
		// Since the event is not cancellable, we have to play dirty
		if (preventFurther && action != GLFW.GLFW_RELEASE)
			KeyMapping.releaseAll();
	}
	
	@SubscribeEvent(priority=EventPriority.HIGH)
	public void onMouseInput(InputEvent.MouseButton.Pre event) {
		int action = event.getAction();
		int button = event.getButton();
		int key = Keys.getKeyFromMouseInput(button);
		if (switch (action) {
			case GLFW.GLFW_PRESS -> press(key);
			case GLFW.GLFW_RELEASE -> release(key);
			default -> false;
		}) event.setCanceled(true);
	}
	
	@SubscribeEvent(priority=EventPriority.HIGH)
	public void onMouseScrolled(InputEvent.MouseScrollingEvent event) {
		int key = Keys.getKeyFromScroll(event.getScrollDelta());
		boolean preventFurther = press(key, 0);
		// While not making much sense, having a release event could allow users
		// to define an exclusive fallback hotkey for mouse wheel scrolling.
		preventFurther |= release(key);
		// Otherwise, the wheel key should be removed from the context directly
		if (preventFurther) event.setCanceled(true);
	}
	
	@SubscribeEvent(priority=EventPriority.HIGH)
	public void onMouseScrolledInGUI(ScreenEvent.MouseScrolled.Pre event) {
		int key = Keys.getKeyFromScroll(event.getScrollDelta());
		boolean preventFurther = press(key, 0);
		preventFurther |= release(key);
		if (preventFurther) event.setCanceled(true);
	}
	
	@SubscribeEvent public void onGuiOpenEvent(ScreenEvent.Opening event) {
		resetContext();
	}
	
	public static class InputMatchingContextImpl implements InputMatchingContext {
		private final IntList sortedPressedKeys = new IntArrayList();
		private final IntSet pressedKeys = new IntOpenHashSet();
		private final Int2ObjectMap<String> charMap = new Int2ObjectOpenHashMap<>();
		private final Set<String> pressedChars = new HashSet<>();
		private final Set<ExtendedKeyBind> repeatableKeyBinds = new HashSet<>();
		private boolean triggered = false;
		private boolean preventFurther = false;
		private boolean cancelled = false;
		
		@Override public @NotNull IntList getSortedPressedKeys() {
			return sortedPressedKeys;
		}
		@Override public @NotNull IntSet getPressedKeys() {
			return pressedKeys;
		}
		@Override public @NotNull Int2ObjectMap<String> getCharMap() {
			return charMap;
		}
		@Override public @NotNull Set<String> getPressedChars() {
			return pressedChars;
		}
		@Override public @NotNull Set<ExtendedKeyBind> getRepeatableKeyBinds() {
			return repeatableKeyBinds;
		}
		
		@Override public boolean isTriggered() {
			return triggered;
		}
		@Override public void setTriggered(boolean triggered) {
			this.triggered = triggered;
		}
		@Override public boolean isPreventFurther() {
			return preventFurther;
		}
		@Override public void setPreventFurther(boolean matched) {
			this.preventFurther = matched;
		}
		@Override public boolean isCancelled() {
			return cancelled;
		}
		@Override public void setCancelled(boolean cancelled) {
			this.cancelled = cancelled;
		}
	}
	
	private static class ExtendedKeyBindProviderTicket implements Comparable<ExtendedKeyBindProviderTicket> {
		private static int ID;
		private final ExtendedKeyBindProvider provider;
		private final int tieBreaker = ID++;
		
		private ExtendedKeyBindProviderTicket(ExtendedKeyBindProvider provider) {
			this.provider = provider;
		}
		
		public ExtendedKeyBindProvider getProvider() {
			return provider;
		}
		public int getTieBreaker() {
			return tieBreaker;
		}
		
		@Override public int compareTo(@NotNull ExtendedKeyBindProviderTicket o) {
			return new CompareToBuilder()
			  .append(-provider.getPriority(), -o.provider.getPriority())
			  .append(tieBreaker, o.tieBreaker)
			  .toComparison();
		}
	}
	
	public List<ExtendedKeyBindImpl> getOverlaps(ExtendedKeyBindImpl keyBind) {
		return getSortedProviders().stream()
		  .flatMap(p -> StreamSupport.stream(p.getAllKeyBinds().spliterator(), false))
		  .filter(o -> o instanceof ExtendedKeyBindImpl && keyBind != o && keyBind.overlaps(o))
		  .map(o -> (ExtendedKeyBindImpl) o)
		  .collect(Collectors.toList());
	}
	
	public List<ExtendedKeyBindImpl> getOverlaps(KeyBindMapping mapping) {
		return getSortedProviders().stream()
		  .flatMap(p -> StreamSupport.stream(p.getAllKeyBinds().spliterator(), false))
		  .filter(o -> o instanceof ExtendedKeyBindImpl).map(o -> (ExtendedKeyBindImpl) o)
		  .filter(o -> mapping.overlaps(o.getCandidateDefinition()))
		  .collect(Collectors.toList());
	}
}
