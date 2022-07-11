package endorh.simpleconfig.ui.gui.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.IMultiTooltipScreen;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction.ButtonActionBuilder;
import endorh.simpleconfig.ui.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
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
	protected Supplier<List<ITextComponent>> defaultTooltip;
	protected Supplier<Optional<ISound>> defaultSound;
	protected TreeSet<Pair<Modifier, ButtonAction>> actions =
	  new TreeSet<>(Comparator.naturalOrder());
	protected @NotNull ButtonAction activeAction;
	protected @NotNull ButtonAction defaultAction;
	protected Boolean activeOverride = null;
	
	public MultiFunctionImageButton(
	  int x, int y, int width, int height, @NotNull Icon icon,
	  ButtonActionBuilder action
	) {
		this(x, y, width, height, icon, action, NarratorChatListener.NO_TITLE);
	}
	
	public MultiFunctionImageButton(
	  int x, int y, int width, int height, @NotNull Icon icon,
	  ButtonActionBuilder action, ITextComponent title
	) {
		super(
		  x, y, width, height, icon.u, icon.v, icon.h, icon.location,
		  icon.tw, icon.th, b -> {}, NO_TOOLTIP, title);
		final ButtonAction defaultAction = action.build();
		defaultIcon = icon;
		defaultActivePredicate = defaultAction.activePredicate != null? defaultAction.activePredicate : () -> true;
		defaultTooltip = defaultAction.tooltipSupplier != null? defaultAction.tooltipSupplier : Collections::emptyList;
		defaultSound = defaultAction.sound != null? defaultAction.sound : () -> Optional.of(SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float partialTicks
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
	
	private static final ITextComponent[] EMPTY_TEXT_COMPONENT_ARRAY = new ITextComponent[0];
	@Override public void renderToolTip(@NotNull MatrixStack mStack, int mouseX, int mouseY) {
		final ButtonAction action = activeAction;
		final List<ITextComponent> ls = (action.tooltipSupplier != null? action.tooltipSupplier : defaultTooltip).get();
		if (!ls.isEmpty()) {
			final Screen screen = Minecraft.getInstance().screen;
			if (screen instanceof IMultiTooltipScreen) {
				((IMultiTooltipScreen) screen).addTooltip(Tooltip.of(
				  new Point(mouseX, mouseY), ls.toArray(EMPTY_TEXT_COMPONENT_ARRAY)));
			} else if (screen != null)
				screen.renderWrappedToolTip(mStack, ls, mouseX, mouseY, Minecraft.getInstance().font);
		}
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
		if (keyCode == 257 || keyCode == 32 || keyCode == 335) // Enter | Space | NumPadEnter
			return press(modifiers);
		return false;
	}
	
	@Override public boolean changeFocus(boolean p_231049_1_) {
		updateState();
		return super.changeFocus(p_231049_1_);
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
		public final @Nullable Supplier<ITextComponent> titleSupplier;
		public final @Nullable Supplier<List<ITextComponent>> tooltipSupplier;
		public final @Nullable Supplier<Boolean> activePredicate;
		public final @Nullable Supplier<Optional<ISound>> sound;
		
		public ButtonAction(
		  int priority,
		  @Nullable Supplier<Icon> icon, @Nullable Supplier<Integer> tint,
		  Consumer<Integer> action,
		  @Nullable Supplier<ITextComponent> titleSupplier,
		  @Nullable Supplier<List<ITextComponent>> tooltipSupplier,
		  @Nullable Supplier<Boolean> activePredicate,
		  @Nullable Supplier<Optional<ISound>> sound
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
			protected @Nullable Supplier<ITextComponent> titleSupplier = null;
			protected @Nullable Supplier<List<ITextComponent>> tooltipSupplier = null;
			protected @Nullable Supplier<Boolean> activePredicate = null;
			protected @Nullable Supplier<Optional<ISound>> sound = null;
			
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
			
			public ButtonActionBuilder title(Supplier<ITextComponent> title) {
				titleSupplier = title;
				return this;
			}
			
			public ButtonActionBuilder tooltip(ITextComponent tooltip) {
				return tooltip(() -> Lists.newArrayList(tooltip));
			}
			
			public ButtonActionBuilder tooltip(List<ITextComponent> tooltip) {
				return tooltip(() -> tooltip);
			}
			
			public ButtonActionBuilder tooltip(Supplier<List<ITextComponent>> tooltip) {
				tooltipSupplier = tooltip;
				return this;
			}
			
			public ButtonActionBuilder active(Supplier<Boolean> activePredicate) {
				this.activePredicate = activePredicate;
				return this;
			}
			
			public ButtonActionBuilder sound(Supplier<Optional<ISound>> sound) {
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