package endorh.simpleconfig.ui.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.SimpleConfigMod.KeyBindings;
import endorh.simpleconfig.core.SimpleConfigTextUtil;
import endorh.simpleconfig.ui.api.AbstractConfigEntry;
import endorh.simpleconfig.ui.api.EntryError;
import endorh.simpleconfig.ui.api.INavigableTarget;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.ExternalChangesDialog.ExternalChangeResponse;
import endorh.simpleconfig.ui.gui.StatusDisplayBar.StatusState.StatusStyle;
import endorh.simpleconfig.ui.gui.icon.Icon;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.TintedButton;
import endorh.simpleconfig.ui.math.Point;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StatusDisplayBar extends Widget implements IOverlayRenderer {
	protected SimpleConfigScreen screen;
	protected final NavigableSet<StatusState> states = new TreeSet<>(Comparator.naturalOrder());
	protected final MultiFunctionImageButton dialogButton;
	protected StatusState activeState = null;
	protected int shadowColor = 0xFF000000;
	
	protected Rectangle rect = new Rectangle();
	protected boolean claimed = false;
	
	public StatusDisplayBar(
	  SimpleConfigScreen screen
	) {
		super(0, 0, screen.width, 14, StringTextComponent.EMPTY);
		this.screen = screen;
		dialogButton = new MultiFunctionImageButton(
		  0, 0, 15, 15, SimpleConfigIcons.Status.H_DOTS, ButtonAction.of(
		  this::createDialog
		).active(this::hasDialog).tooltip(this::getDialogTooltip));
		states.add(StatusState.ERROR_STATE);
		states.add(StatusState.READ_ONLY);
		states.add(StatusState.REQUIRES_RESTART);
		states.add(StatusState.EXTERNAL_CHANGES);
	}
	
	public void tick() {
		activeState = states.descendingSet().stream()
		  .filter(s -> s.isActive(screen)).findFirst().orElse(null);
	}
	
	@Override public void renderButton(
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float delta
	) {
		width = screen.width;
		height = 19;
		x = 0;
		y = screen.listWidget.bottom - height;
		rect.setBounds(x, y, width, height);
		if (activeState != null && !claimed) {
			screen.addOverlay(rect, this);
			claimed = true;
			onShow();
		}
	}
	
	public void createDialog() {
		if (activeState != null) {
			AbstractDialog dialog = activeState.getDialog(screen);
			if (dialog != null) screen.addDialog(dialog);
		}
	}
	
	public boolean hasDialog() {
		return activeState != null && activeState.hasDialog(screen);
	}
	
	public List<ITextComponent> getDialogTooltip() {
		return activeState != null ? activeState.getTooltip(screen, true) : Collections.emptyList();
	}
	
	@Override public boolean overlayMouseClicked(Rectangle area, double mouseX, double mouseY, int button) {
		if (!isMouseOver(mouseX, mouseY)) return false;
		if (hasDialog() && dialogButton.mouseClicked(mouseX, mouseY, button)) return true;
		if (activeState != null) activeState.onClick(screen, mouseX, mouseY, button);
		return true;
	}
	
	protected void onShow() {
		screen.listWidget.setExtraScroll(screen.listWidget.getExtraScroll() + height);
	}
	
	protected void onHide() {
		screen.listWidget.setExtraScroll(screen.listWidget.getExtraScroll() - height);
	}
	
	@Override public boolean renderOverlay(
	  MatrixStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (activeState == null) {
			claimed = false;
			onHide();
			return false;
		}
		
		Minecraft mc = Minecraft.getInstance();
		FontRenderer font = mc.fontRenderer;
		
		StatusStyle style = activeState.getStyle(screen);
		fillGradient(mStack, x, y - 4, x + width, y, 0x00000000, shadowColor);
		fill(mStack, x, y, x + width, y + height, style.backgroundColor);
		fill(mStack, x, y, x + width, y + 1, style.borderColor);
		fill(mStack, x, y + height - 1, x + width, y + height, style.borderColor);
		if (style.icon != null)
			style.icon.renderCentered(mStack, x + 2, y + 2, 15, 15);
		ITextComponent title = activeState.getTitle(screen);
		drawString(mStack, font, title, x + 19, y + 6, 0xFFE0E0E0);
		if (hasDialog()) {
			dialogButton.x = x + width - 17;
			dialogButton.y = y + 2;
			dialogButton.visible = true;
		} else dialogButton.visible = false;
		dialogButton.render(mStack, mouseX, mouseY, delta);
		
		if (isMouseOver(mouseX, mouseY) && !dialogButton.isMouseOver(mouseX, mouseY)) {
			List<ITextComponent> tooltip = activeState.getTooltip(screen, false);
			if (!tooltip.isEmpty()) {
				screen.addTooltip(Tooltip.of(
				  Point.of(mouseX, mouseY), tooltip.toArray(new ITextComponent[0])));
			}
		}
		return true;
	}
	
	public static abstract class StatusState implements Comparable<StatusState> {
		public static final StatusState READ_ONLY = new StatusState(30) {
			@Override public boolean isActive(SimpleConfigScreen screen) {
				return !screen.isEditable();
			}
			
			@Override public ITextComponent getTitle(SimpleConfigScreen screen) {
				return new TranslationTextComponent("simpleconfig.ui.read_only").mergeStyle(TextFormatting.AQUA);
			}
			
			@Override public List<ITextComponent> getTooltip(SimpleConfigScreen screen, boolean menu) {
				return SimpleConfigTextUtil.splitTtc("simpleconfig.ui.read_only:help");
			}
			
			@Override public StatusStyle getStyle(SimpleConfigScreen screen) {
				return new StatusStyle(SimpleConfigIcons.Status.INFO, 0xF08080F0, 0xA00C5281);
			}
		};
		
		public static final StatusState REQUIRES_RESTART = new StatusState(5) {
			@Override public boolean isActive(SimpleConfigScreen screen) {
				return screen.isRequiresRestart();
			}
			
			@Override public ITextComponent getTitle(SimpleConfigScreen screen) {
				return new TranslationTextComponent("simpleconfig.ui.status.requires_restart")
				  .mergeStyle(TextFormatting.GOLD);
			}
			
			@Override public boolean hasDialog(SimpleConfigScreen screen) {
				return true;
			}
			
			@Override public StatusStyle getStyle(SimpleConfigScreen screen) {
				return new StatusStyle(SimpleConfigIcons.Entries.REQUIRES_RESTART, 0xF0A0A080, 0xA0646448);
			}
			
			@Override
			public void onClick(SimpleConfigScreen screen, double mouseX, double mouseY, int button) {
				screen.focusNextRequiresRestart(!Screen.hasShiftDown());
			}
			
			@Override public AbstractDialog getDialog(SimpleConfigScreen screen) {
				final List<AbstractConfigEntry<?>> entries = screen.getAllMainEntries().stream()
				  .filter(e -> e.isRequiresRestart() && e.isEdited())
				  .collect(Collectors.toList());
				final List<ITextComponent> lines = IntStream.range(0, entries.size()).mapToObj(i -> {
					AbstractConfigEntry<?> entry = entries.get(i);
					IFormattableTextComponent title = entry.getTitle().deepCopy();
					title.append(new StringTextComponent(" [" + entry.getPath() + "]")
					               .mergeStyle(TextFormatting.GRAY));
					title.modifyStyle(s -> s.setFormatting(TextFormatting.GOLD)
					  .setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new TranslationTextComponent(
						 "simpleconfig.ui.go_to_entry")))
					  .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "action:goto/" + i)));
					return title;
				}).collect(Collectors.toList());
				return InfoDialog.create(
				  new TranslationTextComponent("simpleconfig.ui.status.requires_restart.all.title"),
				  lines,
				  d -> {
					  d.setIcon(SimpleConfigIcons.Entries.REQUIRES_RESTART);
					  d.setLinkActionHandler(s -> {
						  if (s.startsWith("goto/")) {
							  try {
								  int pos = Integer.parseInt(s.substring("goto/".length()));
								  if (pos >= 0 && pos < entries.size()) {
									  d.cancel(true);
									  INavigableTarget target = entries.get(pos);
									  target.navigate();
									  target.applyWarningHighlight();
								  }
							  } catch (NumberFormatException ignored) {}
						  }
					  });
					  d.addButton(TintedButton.of(
						 new TranslationTextComponent(
							"simpleconfig.ui.action.restore.all"), 0x806480FF, b -> {
							 d.cancel(true);
							 screen.runAtomicTransparentAction(
								() -> entries.forEach(AbstractConfigEntry::restoreValue));
						 }
					  ));
				  });
			}
			
			@Override public List<ITextComponent> getTooltip(SimpleConfigScreen screen, boolean menu) {
				return SimpleConfigTextUtil.splitTtc("simpleconfig.ui.status.requires_restart.click");
			}
		};
		
		public static final StatusState EXTERNAL_CHANGES = new StatusState(10) {
			@Override public boolean isActive(SimpleConfigScreen screen) {
				return screen.getAllMainEntries().stream()
				  .anyMatch(AbstractConfigEntry::hasConflictingExternalDiff);
			}
			
			@Override
			public void onClick(SimpleConfigScreen screen, double mouseX, double mouseY, int button) {
				screen.focusNextExternalConflict(!Screen.hasShiftDown());
			}
			
			public String getTypeKey(SimpleConfigScreen screen) {
				return screen.isEditingServer() && screen.hasConflictingRemoteChanges()
				       || !screen.hasConflictingExternalChanges() ? "remote" : "external";
			}
			
			@Override public ITextComponent getTitle(SimpleConfigScreen screen) {
				return new TranslationTextComponent(
				  "simpleconfig.ui." + getTypeKey(screen) + "_changes_detected");
			}
			
			@Override
			public List<ITextComponent> getTooltip(SimpleConfigScreen screen, boolean menu) {
				return SimpleConfigTextUtil.splitTtc(
				  "simpleconfig.ui." + getTypeKey(screen) + "_changes_detected."
				  + (menu? "all" : "click"),
				  KeyBindings.NEXT_ERROR.func_238171_j_().deepCopy().mergeStyle(TextFormatting.DARK_AQUA),
				  KeyBindings.PREV_ERROR.func_238171_j_().deepCopy().mergeStyle(TextFormatting.DARK_AQUA));
			}
			
			@Override public boolean hasDialog(SimpleConfigScreen screen) {
				return true;
			}
			
			@Override public AbstractDialog getDialog(SimpleConfigScreen screen) {
				final List<AbstractConfigEntry<?>> conflicts = screen.getAllExternalConflicts();
				final List<ITextComponent> lines = IntStream.range(0, conflicts.size()).mapToObj(i -> {
					AbstractConfigEntry<?> entry = conflicts.get(i);
					IFormattableTextComponent title = entry.getTitle().deepCopy();
					title.append(new StringTextComponent(" [" + entry.getPath() + "]")
					               .mergeStyle(TextFormatting.GRAY));
					title.modifyStyle(s -> s.setFormatting(TextFormatting.GOLD)
					  .setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new TranslationTextComponent(
						 "simpleconfig.ui.changes.all.link:help")))
					  .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "action:goto/" + i)));
					return title;
				}).collect(Collectors.toList());
				return InfoDialog.create(
				  new TranslationTextComponent("simpleconfig.ui.changes.all.title"), lines,
				  d -> {
					  d.setIcon(SimpleConfigIcons.Entries.MERGE_CONFLICT);
					  d.setLinkActionHandler(s -> {
						  if (s.startsWith("goto/")) {
							  try {
								  int pos = Integer.parseInt(s.substring("goto/".length()));
								  if (pos >= 0 && pos < conflicts.size()) {
									  d.cancel(true);
									  INavigableTarget target = conflicts.get(pos);
									  target.navigate();
									  target.applyMergeHighlight();
								  }
							  } catch (NumberFormatException ignored) {}
						  }
					  });
					  d.addButton(TintedButton.of(
					    new TranslationTextComponent(
						   "simpleconfig.ui.action.accept_all_changes"), 0x8081542F, b -> {
						    d.cancel(true);
						    screen.handleExternalChangeResponse(ExternalChangeResponse.ACCEPT_ALL);
					    }
					  ));
					  d.addButton(Util.make(TintedButton.of(
					    new TranslationTextComponent(
						   "simpleconfig.ui.action.accept_non_conflicting_changes"), 0x80683498, b -> {
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
				screen.focusNextError(!Screen.hasShiftDown());
			}
			
			@Override public ITextComponent getTitle(SimpleConfigScreen screen) {
				List<ITextComponent> errors = screen.getErrorsMessages();
				if (errors.isEmpty()) return StringTextComponent.EMPTY;
				return (errors.size() == 1 ? errors.get(0).deepCopy() : new TranslationTextComponent(
				  "simpleconfig.ui.errors.multiple", errors.size(), errors.get(0).deepCopy().getString()
				)).mergeStyle(TextFormatting.RED);
			}
			
			@Override public boolean hasDialog(SimpleConfigScreen screen) {
				return true;
			}
			
			@Override public AbstractDialog getDialog(SimpleConfigScreen screen) {
				final List<EntryError> errors = screen.getErrors();
				final List<ITextComponent> lines = IntStream.range(0, errors.size()).mapToObj(i -> {
					TranslationTextComponent goToError = new TranslationTextComponent(
					  "simpleconfig.ui.errors.all.link:help");
					HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, goToError);
					ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, "action:goto/" + i);
					EntryError error = errors.get(i);
					AbstractConfigEntry<?> entry = error.getEntry();
					IFormattableTextComponent title = entry.getTitle().deepCopy();
					title.append(new StringTextComponent(" [" + entry.getPath() + "]")
					               .mergeStyle(TextFormatting.DARK_RED));
					title.modifyStyle(s -> s.setFormatting(TextFormatting.RED)
					  .setHoverEvent(hoverEvent).setClickEvent(clickEvent));
					IFormattableTextComponent err = new StringTextComponent("  ").append(
					  error.getError().deepCopy().modifyStyle(s -> s.setFormatting(TextFormatting.RED)
					    .setHoverEvent(hoverEvent).setClickEvent(clickEvent)));
					return new ITextComponent[] {title, err};
				}).flatMap(Arrays::stream).collect(Collectors.toList());
				return InfoDialog.create(
				  new TranslationTextComponent("simpleconfig.ui.errors.all.title"), lines,
				  d -> {
					  d.setIcon(SimpleConfigIcons.Status.ERROR);
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
									  target.applyErrorHighlight();
								  }
							  } catch (NumberFormatException ignored) {}
						  }
					  });
				  });
			}
			
			@Override public List<ITextComponent> getTooltip(SimpleConfigScreen screen, boolean menu) {
				return SimpleConfigTextUtil.splitTtc(
				  menu? "simpleconfig.ui.errors.extra:help" : "simpleconfig.ui.errors:help",
				  KeyBindings.NEXT_ERROR.func_238171_j_().deepCopy().mergeStyle(TextFormatting.DARK_AQUA),
				  KeyBindings.PREV_ERROR.func_238171_j_().deepCopy().mergeStyle(TextFormatting.DARK_AQUA)
				);
			}
			
			@Override public StatusStyle getStyle(SimpleConfigScreen screen) {
				return new StatusStyle(SimpleConfigIcons.Status.ERROR, 0xA0F08080, 0xBD600000);
			}
		};
		
		public final int priority;
		
		protected StatusState(int priority) {
			this.priority = priority;
		}
		
		public abstract boolean isActive(SimpleConfigScreen screen);
		public void onClick(SimpleConfigScreen screen, double mouseX, double mouseY, int button) {}
		public abstract ITextComponent getTitle(SimpleConfigScreen screen);
		public boolean hasDialog(SimpleConfigScreen screen) {
			return getDialog(screen) != null;
		}
		public @Nullable AbstractDialog getDialog(SimpleConfigScreen screen) {
			return null;
		}
		public List<ITextComponent> getTooltip(SimpleConfigScreen screen, boolean menu) {
			return Collections.emptyList();
		}
		public abstract StatusStyle getStyle(SimpleConfigScreen screen);
		
		@Override public int compareTo(@NotNull StatusDisplayBar.StatusState o) {
			return Integer.compare(priority, o.priority);
		}
		
		public static class StatusStyle {
			public final @Nullable Icon icon;
			public final int borderColor;
			public final int backgroundColor;
			public StatusStyle(@Nullable Icon icon, int borderColor, int backgroundColor) {
				this.icon = icon;
				this.borderColor = borderColor;
				this.backgroundColor = backgroundColor;
			}
		}
	}
}
