package endorh.simpleconfig.ui.gui.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.IMultiTooltipScreen;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction.ButtonActionBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class MultiFunctionImageButton extends ImageButton {
	protected Icon defaultIcon;
	protected Supplier<Boolean> defaultActivePredicate;
	protected Supplier<List<Component>> defaultTooltip;
	protected Supplier<Optional<SoundInstance>> defaultSound;
	protected TreeSet<Pair<Modifier, ButtonAction>> actions =
	  new TreeSet<>(Comparator.naturalOrder());
	protected @NotNull ButtonAction activeAction;
	protected @NotNull ButtonAction defaultAction;
	protected Boolean activeOverride = null;
	
	public static MultiFunctionImageButton of(
	  @NotNull Icon icon, ButtonActionBuilder action
	) {
		return of(NarratorChatListener.NO_TITLE, icon, action);
	}
	
	public static MultiFunctionImageButton of(
	  Component title, @NotNull Icon icon, ButtonActionBuilder action
	) {
		return of(title, icon.w, icon.h, icon, action);
	}
	
	public static MultiFunctionImageButton of(
	  int width, int height, @NotNull Icon icon, ButtonActionBuilder action
	) {
		return of(NarratorChatListener.NO_TITLE, width, height, icon, action);
	}
	
	public static MultiFunctionImageButton of(
	  Component title, int width, int height, @NotNull Icon icon, ButtonActionBuilder action
	) {
		return new MultiFunctionImageButton(0, 0, width, height, icon, action, title);
	}
	
	public MultiFunctionImageButton(
	  int x, int y, int width, int height, @NotNull Icon icon, ButtonActionBuilder action
	) {
		this(x, y, width, height, icon, action, NarratorChatListener.NO_TITLE);
	}
	
	public MultiFunctionImageButton(
	  int x, int y, int width, int height, @NotNull Icon icon,
	  ButtonActionBuilder action, Component title
	) {
		super(
		  x, y, width, height, icon.getU(), icon.getV(), icon.h, icon.getTexture(),
		  icon.tw, icon.th, b -> {}, NO_TOOLTIP, title);
		final ButtonAction defaultAction = action.build();
		defaultIcon = icon;
		defaultActivePredicate = defaultAction.activePredicate != null? defaultAction.activePredicate : () -> true;
		defaultTooltip = defaultAction.tooltipSupplier != null? defaultAction.tooltipSupplier : Collections::emptyList;
		defaultSound = defaultAction.sound != null? defaultAction.sound : () -> Optional.of(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		actions.add(Pair.of(Modifier.NONE, defaultAction));
		this.defaultAction = activeAction = defaultAction;
	}
	
	public boolean click(int button) {
		return onClick(activeAction, button);
	}
	
	public boolean click(boolean ctrl, boolean alt, boolean shift, int button) {
		return click(modifiers(ctrl, alt, shift), button);
	}
	
	public boolean click(int modifiers, int button) {
		return onClick(getActiveAction(modifiers), button);
	}
	
	protected boolean onClick(ButtonAction action, int button) {
		if ((action.activePredicate != null? action.activePredicate : defaultActivePredicate).get()) {
			action.action.accept(button);
			(action.sound != null ? action.sound : defaultSound).get()
			  .ifPresent(s -> Minecraft.getInstance().getSoundManager().play(s));
			return true;
		}
		return false;
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		updateState();
		return active && visible && clicked(mouseX, mouseY) && click(button);
	}
	
	public MultiFunctionImageButton on(
	  Modifier modifier, ButtonActionBuilder action
	) {
		final ButtonAction a = action.build();
		actions.add(Pair.of(modifier, a));
		return this;
	}
	
	public @NotNull ButtonAction getActiveAction() {
		return activeAction;
	}
	
	public @NotNull ButtonAction getActiveAction(int modifiers) {
		return actions.stream().filter(
		  p -> p.getLeft().isActive(modifiers)
		).map(Pair::getRight).filter(
		  a -> a.activePredicate == null || a.activePredicate.get()
		).findFirst().orElse(defaultAction);
	}
	
	protected void updateState() {
		activeAction = actions.stream().filter(
		  p -> p.getLeft().isActive()
		).map(Pair::getRight).filter(
		  a -> a.activePredicate == null || a.activePredicate.get()
		).findFirst().orElse(defaultAction);
		final ButtonAction action = activeAction;
		active = activeOverride != null? activeOverride : (action.activePredicate != null? action.activePredicate : defaultActivePredicate).get();
	}
	
	@Override public void renderButton(
	  @NotNull PoseStack mStack, int mouseX, int mouseY, float partialTicks
	) {
		updateState();
		final ButtonAction action = activeAction;
		int level = active? isHovered()? 2 : 1 : 0;
		
		Icon icon = action.icon != null? action.icon.get() : null;
		if (icon == null) icon = defaultIcon;
		icon.renderStretch(mStack, x, y, width, height, level);
		if (isHovered())
			renderToolTip(mStack, mouseX, mouseY);
	}
	
	@Override public void renderToolTip(@NotNull PoseStack mStack, int mouseX, int mouseY) {
		final List<Component> ls = getTooltip();
		if (!ls.isEmpty()) {
			final Screen screen = Minecraft.getInstance().screen;
			boolean hovered = isMouseOver(mouseX, mouseY);
			int tooltipX = hovered? mouseX : x + width / 2;
			int tooltipY = hovered? mouseY : y < 64? y + height : y;
			if (screen instanceof IMultiTooltipScreen ts) {
				ts.addTooltip(Tooltip.of(
				  Rectangle.of(x, y, width, height),
				  Point.of(tooltipX, tooltipY), ls
				).asKeyboardTooltip(!hovered));
			} else if (screen != null) screen.renderComponentTooltip(mStack, ls, tooltipX, tooltipY);
		}
	}
	
	public List<Component> getTooltip() {
		ButtonAction action = activeAction;
		return (action.tooltipSupplier != null? action.tooltipSupplier : defaultTooltip).get();
	}
	
	
	public boolean press(boolean ctrl, boolean alt, boolean shift) {
		return press(modifiers(ctrl, alt, shift));
	}
	
	private static int modifiers(boolean ctrl, boolean alt, boolean shift) {
		int modifiers = 0;
		if (ctrl) modifiers |= GLFW.GLFW_MOD_CONTROL;
		if (alt) modifiers |= GLFW.GLFW_MOD_ALT;
		if (shift) modifiers |= GLFW.GLFW_MOD_SHIFT;
		return modifiers;
	}
	
	public boolean press(int modifiers) {
		if (active && visible) {
			final ButtonAction action = getActiveAction(modifiers);
			return onClick(action, -1);
		}
		return false;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		updateState();
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_KP_ENTER)
			return press(modifiers);
		return false;
	}
	
	@Override public boolean changeFocus(boolean focus) {
		updateState();
		return super.changeFocus(focus);
	}
	
	public Boolean getActiveOverride() {
		return activeOverride;
	}
	
	public void setActiveOverride(Boolean activeOverride) {
		this.activeOverride = activeOverride;
	}
	
	public void setDefaultIcon(Icon defaultIcon) {
		this.defaultIcon = defaultIcon;
	}
	
	public static class ButtonAction implements Comparable<ButtonAction> {
		private static int counter;
		private final int stamp = counter++;
		public final int priority;
		public final @Nullable Supplier<Icon> icon;
		public final @Nullable Supplier<Integer> tint;
		public final Consumer<Integer> action;
		public final @Nullable Supplier<Component> titleSupplier;
		public final @Nullable Supplier<List<Component>> tooltipSupplier;
		public final @Nullable Supplier<Boolean> activePredicate;
		public final @Nullable Supplier<Optional<SoundInstance>> sound;
		
		public ButtonAction(
		  int priority,
		  @Nullable Supplier<Icon> icon, @Nullable Supplier<Integer> tint,
		  Consumer<Integer> action,
		  @Nullable Supplier<Component> titleSupplier,
		  @Nullable Supplier<List<Component>> tooltipSupplier,
		  @Nullable Supplier<Boolean> activePredicate,
		  @Nullable Supplier<Optional<SoundInstance>> sound
		) {
			this.priority = priority;
			this.icon = icon;
			this.tint = tint;
			this.action = action;
			this.titleSupplier = titleSupplier;
			this.tooltipSupplier = tooltipSupplier;
			this.activePredicate = activePredicate;
			this.sound = sound;
		}
		
		public static ButtonActionBuilder of(Consumer<Integer> action) {
			return new ButtonActionBuilder(action);
		}
		
		public static ButtonActionBuilder of(
		  @Nullable Runnable left, @Nullable Runnable right
		) {return of(left, right, null);}
		
		public static ButtonActionBuilder of(
		  @Nullable Runnable left
		) {return of(left, null, null);}
		
		public static ButtonActionBuilder of(
		  @Nullable Runnable left, @Nullable Runnable right, @Nullable Runnable middle
		) {
			return of(b -> {
				switch (b) {
					case 1:
						if (right != null) right.run();
						break;
					case 2:
						if (middle != null) middle.run();
						break;
					default:
						if (left != null) left.run();
				}
			});
		}
		
		@Override public int compareTo(@NotNull MultiFunctionImageButton.ButtonAction o) {
			int c = Integer.compare(priority, o.priority);
			if (c != 0) return c;
			return Integer.compare(stamp, o.stamp);
		}
		
		public static class ButtonActionBuilder {
			protected int priority = 0;
			protected final Consumer<Integer> action;
			protected @Nullable Supplier<Icon> icon = null;
			protected @Nullable Supplier<Integer> tint = null;
			protected @Nullable Supplier<Component> titleSupplier = null;
			protected @Nullable Supplier<List<Component>> tooltipSupplier = null;
			protected @Nullable Supplier<Boolean> activePredicate = null;
			protected @Nullable Supplier<Optional<SoundInstance>> sound = null;
			
			private ButtonActionBuilder(Consumer<Integer> action) {
				this.action = action;
			}
			
			public ButtonActionBuilder priority(int priority) {
				this.priority = priority;
				return this;
			}
			
			public ButtonActionBuilder icon(Icon icon) {
				return icon(() -> icon);
			}
			
			public ButtonActionBuilder tint(@Nullable Integer tint) {
				return tint(() -> tint);
			}
			
			public ButtonActionBuilder icon(Supplier<Icon> icon) {
				this.icon = icon;
				return this;
			}
			
			public ButtonActionBuilder tint(Supplier<Integer> tint) {
				this.tint = tint;
				return this;
			}
			
			public ButtonActionBuilder title(Supplier<Component> title) {
				titleSupplier = title;
				return this;
			}
			
			public ButtonActionBuilder tooltip(Component tooltip) {
				return tooltip(() -> Lists.newArrayList(tooltip));
			}
			
			public ButtonActionBuilder tooltip(List<Component> tooltip) {
				return tooltip(() -> tooltip);
			}
			
			public ButtonActionBuilder tooltip(Supplier<List<Component>> tooltip) {
				tooltipSupplier = tooltip;
				return this;
			}
			
			public ButtonActionBuilder active(Supplier<Boolean> activePredicate) {
				this.activePredicate = activePredicate;
				return this;
			}
			
			public ButtonActionBuilder sound(Supplier<Optional<SoundInstance>> sound) {
				this.sound = sound;
				return this;
			}
			
			protected ButtonAction build() {
				return new ButtonAction(priority, icon, tint, action, titleSupplier, tooltipSupplier, activePredicate, sound);
			}
		}
	}
	
	public static class Modifier implements Comparable<Modifier> {
		public static final Modifier NONE = new Modifier();
		public static final Modifier CTRL = NONE.ctrl();
		public static final Modifier ALT = NONE.alt();
		public static final Modifier SHIFT = NONE.shift();
		
		private final ModifierBehaviour ctrl;
		private final ModifierBehaviour alt;
		private final ModifierBehaviour shift;
		
		private Modifier() {
			this(ModifierBehaviour.IGNORE, ModifierBehaviour.IGNORE, ModifierBehaviour.IGNORE);
		}
		
		private Modifier(ModifierBehaviour ctrl, ModifierBehaviour alt, ModifierBehaviour shift) {
			this.ctrl = ctrl;
			this.alt = alt;
			this.shift = shift;
		}
		
		public Modifier ctrl() {
			return new Modifier(ModifierBehaviour.REQUIRE, alt, shift);
		}
		
		public Modifier alt() {
			return new Modifier(ctrl, ModifierBehaviour.REQUIRE, shift);
		}
		
		public Modifier shift() {
			return new Modifier(ctrl, alt, ModifierBehaviour.REQUIRE);
		}
		
		public Modifier noCtrl() {
			return new Modifier(ModifierBehaviour.REJECT, alt, shift);
		}
		
		public Modifier noAlt() {
			return new Modifier(ctrl, ModifierBehaviour.REJECT, shift);
		}
		
		public Modifier noShift() {
			return new Modifier(ctrl, alt, ModifierBehaviour.REJECT);
		}
		
		public boolean isActive() {
			return isActive(Screen.hasControlDown(), Screen.hasAltDown(), Screen.hasShiftDown());
		}
		
		public boolean isActive(int modifiers) {
			return isActive((modifiers & GLFW.GLFW_MOD_CONTROL) != 0,
			                (modifiers & GLFW.GLFW_MOD_ALT) != 0,
			                (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
		}
		
		public boolean isActive(boolean ctrl, boolean alt, boolean shift) { // @formatter:off
			return (this.ctrl == ModifierBehaviour.IGNORE || ctrl == (this.ctrl == ModifierBehaviour.REQUIRE))
			       && (this.alt == ModifierBehaviour.IGNORE || alt == (this.alt == ModifierBehaviour.REQUIRE))
			       && (this.shift == ModifierBehaviour.IGNORE || shift == (this.shift == ModifierBehaviour.REQUIRE));
		} // @formatter:on
		
		// Sorts in decreasing order of priority
		// More priority is given, in order, to modifiers with:
		//  - more required keys
		//  - less ignored keys
		//  - the first required key before in order [ctrl, alt, shift]
		//  - the first rejected key before in order [ctrl, alt, shift]
		//  - the last ignored key after in order [ctrl, alt, shift]
		@Override public int compareTo(@NotNull MultiFunctionImageButton.Modifier o) {
			int c = -Integer.compare(requireCount(), o.requireCount());
			if (c != 0) return c;
			if ((c = -Integer.compare(nonIgnoreCount(), o.nonIgnoreCount())) != 0) return c;
			if ((c = Integer.compare(compareHead(), o.compareHead())) != 0) return c;
			if ((c = Integer.compare(compareMiddle(), o.compareMiddle())) != 0) return c;
			return Integer.compare(compareTail(), o.compareTail());
		}
		
		private int compareHead() {
			return ctrl == ModifierBehaviour.REQUIRE? 0 :
			       alt == ModifierBehaviour.REQUIRE? 1 :
			       shift == ModifierBehaviour.REQUIRE? 2 : 3;
		}
		
		private int compareMiddle() {
			return ctrl == ModifierBehaviour.REJECT? 0 :
			       alt == ModifierBehaviour.REJECT? 1 :
			       shift == ModifierBehaviour.REJECT? 2 : 3;
		}
		
		private int compareTail() {
			return shift == ModifierBehaviour.IGNORE? 3 :
			       alt == ModifierBehaviour.IGNORE? 2 :
			       ctrl == ModifierBehaviour.IGNORE? 1 : 0;
		}
		
		private int requireCount() {
			return (int) Stream.of(ctrl, alt, shift).filter(b -> b == ModifierBehaviour.REQUIRE).count();
		}
		
		private int nonIgnoreCount() {
			return (int) Stream.of(ctrl, alt, shift).filter(b -> b != ModifierBehaviour.IGNORE).count();
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Modifier modifier = (Modifier) o;
			return ctrl == modifier.ctrl && alt == modifier.alt && shift == modifier.shift;
		}
		
		@Override public int hashCode() {
			return Objects.hash(ctrl, alt, shift);
		}
		
		private enum ModifierBehaviour {
			REQUIRE, REJECT, IGNORE
		}
	}
}
