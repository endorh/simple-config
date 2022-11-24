package endorh.simpleconfig.ui.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.SimpleConfigMod.KeyBindings;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Entries;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Status;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.api.EntryError;
import endorh.simpleconfig.ui.api.INavigableTarget;
import endorh.simpleconfig.ui.api.INavigableTarget.HighlightColors;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.ExternalChangesDialog.ExternalChangeResponse;
import endorh.simpleconfig.ui.gui.StatusDisplayBar.StatusState.StatusStyle;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.TintedButton;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;

public class StatusDisplayBar extends AbstractWidget implements IOverlayRenderer {
	protected SimpleConfigScreen screen;
	protected final NavigableSet<StatusState> states = new TreeSet<>(Comparator.naturalOrder());
	protected final MultiFunctionImageButton dialogButton;
	protected StatusState activeState = null;
	protected List<StatusState> activeStates = Collections.emptyList();
	protected int shadowColor = 0xFF000000;
	
	protected Rectangle rect = new Rectangle();
	protected boolean claimed = false;
	
	public StatusDisplayBar(
	  SimpleConfigScreen screen
	) {
		super(0, 0, screen.width, 14, Component.empty());
		this.screen = screen;
		dialogButton = new MultiFunctionImageButton(
		  0, 0, 15, 15, SimpleConfigIcons.Status.H_DOTS, ButtonAction.of(
		  () -> createDialog()
		).active(this::hasDialog).tooltip(this::getDialogTooltip));
		Stream.of(
		  ERROR_STATE, READ_ONLY, REQUIRES_RESTART, EXPERIMENTAL, ADVANCED, EXTERNAL_CHANGES
		).forEach(states::add);
	}
	
	public void tick() {
		activeStates = states.descendingSet().stream()
		  .filter(s -> s.isActive(screen)).toList();
		activeState = activeStates.stream().findFirst().orElse(null);
	}
	
	@Override public void renderButton(
	  @NotNull PoseStack mStack, int mouseX, int mouseY, float delta
	) {
		width = screen.width;
		height = 19;
		x = 0;
		y = screen.listWidget.bottom - height;
		rect.setBounds(x, y, width, height);
		if (!activeStates.isEmpty() && !claimed) {
			screen.addOverlay(rect, this);
			claimed = true;
			onShow();
		}
	}
	
	public void createDialog() {
		createDialog(activeState);
	}
	
	public void createDialog(StatusState state) {
		if (state != null) {
			AbstractDialog dialog = state.getDialog(screen);
			if (dialog != null) screen.addDialog(dialog);
		}
	}
	
	public boolean hasDialog() {
		return activeState != null && activeState.hasDialog(screen);
	}
	
	public List<Component> getDialogTooltip() {
		return activeState != null ? activeState.getTooltip(screen, true) : Collections.emptyList();
	}
	
	@Override public boolean overlayMouseClicked(Rectangle area, double mouseX, double mouseY, int button) {
		if (!isMouseOver(mouseX, mouseY)) return false;
		if (hasDialog() && dialogButton.mouseClicked(mouseX, mouseY, button)) return true;
		if (activeStates.size() > 1) {
			int i = ((int) mouseX - x) / height;
			if (i < activeStates.size() - 1) {
				StatusState state = activeStates.get(activeStates.size() - 1 - i);
				if (button == 2 && state.hasDialog(screen)) {
					createDialog(state);
				} else state.onClick(screen, mouseX, mouseY, button);
				return true;
			}
		}
		if (activeState != null) activeState.onClick(screen, mouseX, mouseY, button);
		return true;
	}
	
	protected void onShow() {
		screen.listWidget.setExtraScroll(screen.listWidget.getExtraScroll() + height);
	}
	
	protected void onHide() {
		screen.listWidget.setExtraScroll(screen.listWidget.getExtraScroll() - height);
	}
	
	protected void renderBox(
	  PoseStack mStack, StatusState state, int boxX, int mouseX, int mouseY
	) {
		StatusStyle style = state.getStyle(screen);
		Icon icon = style.icon();
		if (icon == null) icon = Icon.EMPTY;
		fill(mStack, boxX, y, boxX + height, y + height, style.backgroundColor());
		fill(mStack, boxX, y, boxX + height, y + 1, style.borderColor());
		fill(mStack, boxX, y + height - 1, boxX + height, y + height, style.borderColor());
		fill(mStack, boxX + height - 1, y + 1, boxX + height, y + height - 1, style.borderColor());
		icon.renderCentered(mStack, boxX + 2, y + 2, 15, 15);
		
		if (isMouseOver(mouseX, mouseY) && mouseX >= boxX && mouseX < boxX + height) {
			List<Component> tooltip = Stream.concat(
			  Stream.of(state.getTitle(screen)),
			  state.getTooltip(screen, false).stream()
			    .map(c -> c.copy().withStyle(ChatFormatting.GRAY))
			).toList();
			screen.addTooltip(Tooltip.of(
			  rect, Point.of(mouseX, mouseY), tooltip));
		}
	}
	
	@Override public boolean renderOverlay(
	  PoseStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (activeStates.isEmpty()) {
			claimed = false;
			onHide();
			return false;
		}
		
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		
		StatusStyle style = activeState.getStyle(screen);
		fillGradient(mStack, x, y - 4, x + width, y, 0x00000000, shadowColor);
		
		int l = x;
		for (int i = activeStates.size() - 1; i >= 1; i--) {
			StatusState state = activeStates.get(i);
			renderBox(mStack, state, l, mouseX, mouseY);
			l += height;
		}
		
		fill(mStack, l, y, x + width, y + height, style.backgroundColor());
		fill(mStack, l, y, x + width, y + 1, style.borderColor());
		fill(mStack, l, y + height - 1, x + width, y + height, style.borderColor());
		if (style.icon != null)
			style.icon.renderCentered(mStack, l + 2, y + 2, 15, 15);
		Component title = activeState.getTitle(screen);
		drawString(mStack, font, title, l + 19, y + 6, 0xFFE0E0E0);
		if (hasDialog()) {
			dialogButton.x = x + width - 17;
			dialogButton.y = y + 2;
			dialogButton.visible = true;
		} else dialogButton.visible = false;
		dialogButton.render(mStack, mouseX, mouseY, delta);
		
		if (isMouseOver(mouseX, mouseY) && mouseX >= l && !dialogButton.isMouseOver(mouseX, mouseY)) {
			List<Component> tooltip = activeState.getTooltip(screen, false);
			if (!tooltip.isEmpty()) screen.addTooltip(Tooltip.of(
			  area, Point.of(mouseX, mouseY), tooltip));
		}
		return true;
	}
	
	@Override public void updateNarration(@NotNull NarrationElementOutput out) {}
	
	public static final StatusState READ_ONLY = new StatusState(30) {
		@Override public boolean isActive(SimpleConfigScreen screen) {
			return !screen.isEditable();
		}
		
		@Override public Component getTitle(SimpleConfigScreen screen) {
			return Component.translatable("simpleconfig.ui.read_only").withStyle(ChatFormatting.AQUA);
		}
		
		@Override public List<Component> getTooltip(SimpleConfigScreen screen, boolean menu) {
			return splitTtc("simpleconfig.ui.read_only:help");
		}
		
		@Override public StatusStyle getStyle(SimpleConfigScreen screen) {
			return new StatusStyle(SimpleConfigIcons.Status.INFO, 0xF08080F0, 0xA00C5281);
		}
	};
	
	public static final StatusState REQUIRES_RESTART = EditedStatusState.create(
	  -5, "requires_restart", AbstractConfigField::isRequiresRestart,
	  new StatusStyle(Entries.REQUIRES_RESTART, 0xF0A0A080, 0xA0646448, 0xF0E0E080));
	
	public static final StatusState EXPERIMENTAL = EditedStatusState.create(
	  -10, "experimental", e -> e.getEntryTags().contains(EntryTag.EXPERIMENTAL),
	  new StatusStyle(Entries.EXPERIMENTAL, 0xF0B0B080, 0xA0806448, 0xF0E0E080));
	
	public static final StatusState ADVANCED = EditedStatusState.create(
	  -15, "advanced", e -> e.getEntryTags().contains(EntryTag.ADVANCED),
	  new StatusStyle(Entries.WRENCH.withTint(ChatFormatting.GOLD), 0xF0B0A080, 0xA0906448, 0xF0F0E080));
	
	public static final StatusState EXTERNAL_CHANGES = new StatusState(10) {
		@Override public boolean isActive(SimpleConfigScreen screen) {
			return screen.getAllMainEntries().stream()
			  .anyMatch(AbstractConfigField::hasConflictingExternalDiff);
		}
		
		@Override
		public void onClick(SimpleConfigScreen screen, double mouseX, double mouseY, int button) {
			if (button == 2) {
				screen.addDialog(getDialog(screen));
				screen.playFeedbackClick(1F);
				return;
			}
			screen.focusNextExternalConflict(Screen.hasShiftDown() ^ button != 1);
		}
		
		public String getTypeKey(SimpleConfigScreen screen) {
			return screen.isEditingServer() && screen.hasConflictingRemoteChanges()
			       || !screen.hasConflictingExternalChanges()? "remote" : "external";
		}
		
		@Override public Component getTitle(SimpleConfigScreen screen) {
			return Component.translatable(
			  "simpleconfig.ui." + getTypeKey(screen) + "_changes_detected");
		}
		
		@Override
		public List<Component> getTooltip(SimpleConfigScreen screen, boolean menu) {
			return splitTtc(
			  "simpleconfig.ui." + getTypeKey(screen) + "_changes_detected."
			  + (menu? "all" : "click"),
			  KeyBindings.NEXT_ERROR.getTranslatedKeyMessage().copy()
			    .withStyle(ChatFormatting.DARK_AQUA),
			  KeyBindings.PREV_ERROR.getTranslatedKeyMessage().copy()
			    .withStyle(ChatFormatting.DARK_AQUA));
		}
		
		@Override public boolean hasDialog(SimpleConfigScreen screen) {
			return true;
		}
		
		@Override public AbstractDialog getDialog(SimpleConfigScreen screen) {
			final List<AbstractConfigField<?>> conflicts = screen.getAllExternalConflicts();
			final List<Component> lines = IntStream.range(0, conflicts.size()).mapToObj(i -> {
				AbstractConfigField<?> entry = conflicts.get(i);
				MutableComponent title = entry.getTitle().copy();
				title.append(Component.literal(" [" + entry.getPath() + "]")
				               .withStyle(ChatFormatting.GRAY));
				title.withStyle(s -> s.withColor(ChatFormatting.GOLD)
				  .withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Component.translatable(
				    "simpleconfig.ui.changes.all.link:help")))
				  .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "action:goto/" + i)));
				return title;
			}).collect(Collectors.toList());
			return InfoDialog.create(
			  Component.translatable("simpleconfig.ui.changes.all.title"), lines,
			  d -> {
				  d.setIcon(Entries.MERGE_CONFLICT);
				  d.setLinkActionHandler(s -> {
					  if (s.startsWith("goto/")) {
						  try {
							  int pos = Integer.parseInt(s.substring("goto/".length()));
							  if (pos >= 0 && pos < conflicts.size()) {
								  d.cancel(true);
								  INavigableTarget target = conflicts.get(pos);
								  target.navigate();
								  target.applyFocusHighlight(HighlightColors.MERGE);
							  }
						  } catch (NumberFormatException ignored) {}
					  }
				  });
				  d.addButton(TintedButton.of(
					 Component.translatable("simpleconfig.ui.action.accept_all_changes"), 0x8081542F,
					 b -> {
						 d.cancel(true);
						 screen.handleExternalChangeResponse(ExternalChangeResponse.ACCEPT_ALL);
					 }
				  ));
				  d.addButton(Util.make(TintedButton.of(
					 Component.translatable("simpleconfig.ui.action.accept_non_conflicting_changes"),
					 0x80683498, b -> {
						 d.cancel(true);
						 screen.handleExternalChangeResponse(
							ExternalChangeResponse.ACCEPT_NON_CONFLICTING);
					 }
				  ), b -> b.active = screen.getAllMainEntries().stream()
					 .anyMatch(e -> e.hasConflictingExternalDiff() && !e.isEdited())));
			  });
		}
		
		@Override public StatusStyle getStyle(SimpleConfigScreen screen) {
			return new StatusStyle(SimpleConfigIcons.Entries.MERGE_CONFLICT, 0xA0F080F0, 0xBD600060);
		}
	};
	
	public static final StatusState ERROR_STATE = new StatusState(20) {
		@Override public boolean isActive(SimpleConfigScreen screen) {
			return screen.hasErrors();
		}
		
		@Override public void onClick(
		  SimpleConfigScreen screen, double mouseX, double mouseY, int button
		) {
			if (button == 2) {
				screen.addDialog(getDialog(screen));
				screen.playFeedbackClick(1F);
				return;
			}
			screen.focusNextError(Screen.hasShiftDown() ^ button != 1);
		}
		
		@Override public Component getTitle(SimpleConfigScreen screen) {
			List<Component> errors = screen.getErrorsMessages();
			if (errors.isEmpty()) return Component.empty();
			return (errors.size() == 1
			        ? errors.get(0).copy() : Component.translatable(
						 "simpleconfig.ui.errors.multiple", errors.size(),
						 errors.get(0).copy().getString())).withStyle(ChatFormatting.RED);
		}
		
		@Override public boolean hasDialog(SimpleConfigScreen screen) {
			return true;
		}
		
		@Override public AbstractDialog getDialog(SimpleConfigScreen screen) {
			final List<EntryError> errors = screen.getErrors();
			final List<Component> lines = IntStream.range(0, errors.size()).mapToObj(i -> {
				MutableComponent goToError =
				  Component.translatable("simpleconfig.ui.errors.all.link:help");
				HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, goToError);
				ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, "action:goto/" + i);
				EntryError error = errors.get(i);
				AbstractConfigField<?> entry = error.getEntry();
				MutableComponent title = entry.getTitle().copy();
				title.append(Component.literal(" [" + entry.getPath() + "]")
				               .withStyle(ChatFormatting.DARK_RED));
				title.withStyle(s -> s.withColor(ChatFormatting.RED)
				  .withHoverEvent(hoverEvent).withClickEvent(clickEvent));
				MutableComponent err = Component.literal("  ").append(
				  error.getError().copy().withStyle(s -> s.withColor(ChatFormatting.RED)
					 .withHoverEvent(hoverEvent).withClickEvent(clickEvent)));
				return new Component[]{title, err};
			}).flatMap(Arrays::stream).collect(Collectors.toList());
			return InfoDialog.create(
			  Component.translatable("simpleconfig.ui.errors.all.title"), lines,
			  d -> {
				  d.setIcon(Status.ERROR);
				  d.titleColor = 0xFFFF8080;
				  d.borderColor = 0xFFFF8080;
				  d.subBorderColor = 0xFF800000;
				  d.setLinkActionHandler(s -> {
					  if (s.startsWith("goto/")) {
						  try {
							  int pos = Integer.parseInt(s.substring("goto/".length()));
							  if (pos >= 0 && pos < errors.size()) {
								  d.cancel(true);
								  INavigableTarget target = errors.get(pos).getSource();
								  target.navigate();
								  target.applyFocusHighlight(HighlightColors.ERROR);
							  }
						  } catch (NumberFormatException ignored) {}
					  }
				  });
			  });
		}
		
		@Override public List<Component> getTooltip(SimpleConfigScreen screen, boolean menu) {
			return splitTtc(
			  menu? "simpleconfig.ui.errors.extra:help" : "simpleconfig.ui.errors:help",
			  KeyBindings.NEXT_ERROR.getTranslatedKeyMessage().copy()
			    .withStyle(ChatFormatting.DARK_AQUA),
			  KeyBindings.PREV_ERROR.getTranslatedKeyMessage().copy()
			    .withStyle(ChatFormatting.DARK_AQUA)
			);
		}
		
		@Override public StatusStyle getStyle(SimpleConfigScreen screen) {
			return new StatusStyle(SimpleConfigIcons.Status.ERROR, 0xA0F08080, 0xBD600000);
		}
	};
	
	public static abstract class StatusState implements Comparable<StatusState> {
		public final int priority;
		
		protected StatusState(int priority) {
			this.priority = priority;
		}
		
		public abstract boolean isActive(SimpleConfigScreen screen);
		public void onClick(SimpleConfigScreen screen, double mouseX, double mouseY, int button) {}
		public abstract Component getTitle(SimpleConfigScreen screen);
		public boolean hasDialog(SimpleConfigScreen screen) {
			return getDialog(screen) != null;
		}
		public @Nullable AbstractDialog getDialog(SimpleConfigScreen screen) {
			return null;
		}
		public List<Component> getTooltip(SimpleConfigScreen screen, boolean menu) {
			return Collections.emptyList();
		}
		public abstract StatusStyle getStyle(SimpleConfigScreen screen);
		
		@Override public int compareTo(@NotNull StatusDisplayBar.StatusState o) {
			return Integer.compare(priority, o.priority);
		}
		
		public record StatusStyle(
		  @Nullable Icon icon, int borderColor, int backgroundColor, int textColor, int highlightColor
		) {
			public StatusStyle(@Nullable Icon icon, int borderColor, int backgroundColor) {
				this(icon, borderColor, backgroundColor, borderColor, backgroundColor);
			}
			
			public StatusStyle(@Nullable Icon icon, int borderColor, int backgroundColor, int textColor) {
				this(icon, borderColor, backgroundColor, textColor, backgroundColor);
			}
		}
	}
	
	public static class EditedStatusState extends StatusState {
		private final String name;
		private final Predicate<SimpleConfigScreen> isActive;
		private final Predicate<AbstractConfigField<?>> entryPredicate;
		private final StatusStyle style;
		
		public static EditedStatusState create(
		  int priority, String name,
		  Predicate<AbstractConfigField<?>> entryPredicate,
		  StatusStyle style
		) {
			return create(
			  priority, name, s -> s.getAllMainEntries().stream()
				 .anyMatch(entryPredicate.and(AbstractConfigField::isEdited)),
			  entryPredicate, style);
		}
		
		public static EditedStatusState create(
		  int priority, String name, Predicate<SimpleConfigScreen> isActive,
		  Predicate<AbstractConfigField<?>> entryPredicate,
		  StatusStyle style
		) {
			return new EditedStatusState(priority, name, isActive, entryPredicate, style);
		}
		
		protected EditedStatusState(
		  int priority, String name, Predicate<SimpleConfigScreen> isActive,
		  Predicate<AbstractConfigField<?>> entryPredicate,
		  StatusStyle style
		) {
			super(priority);
			this.name = name;
			this.isActive = isActive;
			this.entryPredicate = entryPredicate;
			this.style = style;
		}
		
		@Override public boolean isActive(SimpleConfigScreen screen) {
			return isActive.test(screen);
		}
		
		@Override public Component getTitle(SimpleConfigScreen screen) {
			return Component.translatable("simpleconfig.ui.status." + name)
			  .withStyle(s -> s.withColor(style.textColor()));
		}
		
		@Override public StatusStyle getStyle(SimpleConfigScreen screen) {
			return style;
		}
		
		@Override public List<Component> getTooltip(SimpleConfigScreen screen, boolean menu) {
			return splitTtc(menu? "simpleconfig.ui.status.all.view" : "simpleconfig.ui.status." + name + ".click");
		}
		
		@Override public void onClick(SimpleConfigScreen screen, double mouseX, double mouseY, int button) {
			if (button == 2) {
				screen.addDialog(getDialog(screen));
				screen.playFeedbackClick(1F);
				return;
			}
			screen.focusNextEntry(entryPredicate, !Screen.hasShiftDown() ^ button != 1, false, style.highlightColor());
		}
		
		@Override public boolean hasDialog(SimpleConfigScreen screen) {
			return true;
		}
		
		@Override public @NotNull AbstractDialog getDialog(SimpleConfigScreen screen) {
			final List<AbstractConfigField<?>> entries = screen.getAllMainEntries().stream()
			  .filter(e -> entryPredicate.test(e) && e.isEdited()).toList();
			final List<Component> lines = IntStream.range(0, entries.size()).mapToObj(i -> {
				AbstractConfigField<?> entry = entries.get(i);
				MutableComponent title = entry.getTitle().copy();
				title.append(Component.literal(" [" + entry.getPath() + "]")
				               .withStyle(ChatFormatting.GRAY));
				title.withStyle(s -> s.withColor(style.textColor())
				  .withHoverEvent(new HoverEvent(
					 Action.SHOW_TEXT, Component.translatable("simpleconfig.ui.go_to_entry")))
				  .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "action:goto/" + i)));
				return title;
			}).collect(Collectors.toList());
			return InfoDialog.create(
			  Component.translatable("simpleconfig.ui.status." + name + ".all.title"),
			  lines,
			  d -> {
				  d.setIcon(style.icon());
				  d.setLinkActionHandler(s -> {
					  if (s.startsWith("goto/")) {
						  try {
							  int pos = Integer.parseInt(s.substring("goto/".length()));
							  if (pos >= 0 && pos < entries.size()) {
								  d.cancel(true);
								  INavigableTarget target = entries.get(pos);
								  target.navigate();
								  target.applyFocusHighlight(style.highlightColor());
							  }
						  } catch (NumberFormatException ignored) {}
					  }
				  });
				  d.addButton(TintedButton.of(
					 Component.translatable("simpleconfig.ui.action.restore.all"), 0x806480FF, b -> {
						 d.cancel(true);
						 screen.runAtomicTransparentAction(
							() -> entries.forEach(AbstractConfigField::restoreValue));
					 }
				  ));
			  });
		}
	}
}
