package endorh.simpleconfig.ui.gui.widget;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Presets;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigSnapshotHandler;
import endorh.simpleconfig.ui.gui.*;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable.IRectanglePositionableRenderable;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.Modifier;
import endorh.simpleconfig.ui.gui.widget.PresetPickerWidget.Preset.Location;
import endorh.simpleconfig.ui.gui.widget.combobox.ComboBoxWidget;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;

public class PresetPickerWidget extends AbstractContainerEventHandler implements IRectanglePositionableRenderable, NarratableEntry {
	protected List<GuiEventListener> listeners;
	protected long lastUpdate = 0L;
	protected long updateCooldown = 1000L;
	protected @Nullable CompletableFuture<Void> lastFuture = null;
	
	private final Rectangle area = new Rectangle();
	
	protected AbstractConfigScreen screen;
	
	protected MultiFunctionImageButton loadButton;
	protected MultiFunctionImageButton saveButton;
	
	protected Map<Type, Map<String, Preset>> knownLocalPresets = Maps.newHashMap();
	protected Map<Type, Map<String, Preset>> knownRemotePresets = Maps.newHashMap();
	protected Map<Type, Map<String, Preset>> knownResourcePresets = Maps.newHashMap();
	
	protected Style selectedCountStyle = Style.EMPTY.applyFormat(ChatFormatting.DARK_AQUA);
	protected Style nameStyle = Style.EMPTY.applyFormat(ChatFormatting.LIGHT_PURPLE);
	
	protected ComboBoxWidget<Preset> selector;
	
	private boolean active;
	
	public PresetPickerWidget(
	  AbstractConfigScreen screen, int x, int y, int w
	) {
		this.screen = screen;
		area.setBounds(x, y, w, 18);
		for (Type type: SimpleConfig.Type.values()) {
			knownLocalPresets.put(type, Maps.newHashMap());
			knownRemotePresets.put(type, Maps.newHashMap());
			knownResourcePresets.put(type, Maps.newHashMap());
		}
		loadButton = new MultiFunctionImageButton(0, 0, 20, 20, Buttons.LOAD, ButtonAction.of(() -> {
			  final Preset value = selector.getValue();
			  if (value != null) load(value);
		}).active(() -> getHandler() != null
		                && isKnownPreset(selector.getText(), screen.getEditedType().getType())
		                && screen.isEditable()
		  ).tooltip(() -> {
			  final Preset value = selector.getValue();
			  if (value == null) return Lists.newArrayList();
			  return Lists.newArrayList(
			    Component.translatable("simpleconfig.preset.load." + value.getLocation().getAlias(),
			      Component.translatable("simpleconfig.preset." + value.getType().getAlias())));
		  }), Component.translatable("simpleconfig.preset.load.label"));
		saveButton = new MultiFunctionImageButton(0, 0, 20, 20, Buttons.SAVE, ButtonAction.of(
		  () -> save(selector.getText(), false)
		).tooltip(() -> getSaveTooltip(false, false))
		  .active(() -> getHandler() != null
		                && isValidName(selector.getText())
		                && screen.isEditable()
		  ), Component.translatable("simpleconfig.preset.save.label")
		).on(Modifier.SHIFT, ButtonAction.of(() -> save(selector.getText(), true))
		  .icon(Buttons.SAVE_REMOTE)
		  .active(() -> getHandler() != null
		                && isValidName(selector.getText())
		                && getHandler().canSaveRemote()
		                && screen.isEditable()
		  ).tooltip(() -> getSaveTooltip(false, true))
		).on(Modifier.ALT, ButtonAction.of(() -> delete(selector.getValue()))
		  .icon(Buttons.DELETE)
		  .active(() -> getHandler() != null
		                && selector.getValue() != null
		                && (!selector.getValue().isRemote() || getHandler().canSaveRemote())
		                && screen.isEditable()
		  ).tooltip(() -> {
			  final Preset preset = selector.getValue();
			  return getSaveTooltip(true, preset != null && preset.isRemote());
		  }));
		selector = new ComboBoxWidget<>(
		  new Preset.TypeWrapper(this), () -> screen, x, y, w - 48, 24,
		  Component.translatable("simpleconfig.preset.picker.label"));
		selector.setHint(Component.translatable("simpleconfig.preset.picker.hint"));
		final SimpleComboBoxModel<Preset> provider = new SimpleComboBoxModel<>(
		  () -> getKnownPresets(screen.getEditedType().getType()));
		provider.setPlaceholder(Component.translatable("simpleconfig.ui.no_presets_found"));
		selector.setSuggestionProvider(provider);
		listeners = Lists.newArrayList(selector, loadButton, saveButton);
	}
	
	@Override public boolean isActive() {
		return active;
	}
	@Override public void setActive(boolean active) {
		this.active = active;
	}
	
	protected List<Component> getSaveTooltip(
	  boolean delete, boolean remote
	) {
		final IConfigSnapshotHandler handler = getHandler();
		if (handler == null || !isValidName(selector.getText()))
			return Lists.newArrayList();
		Type type = screen.getEditedType().getType();
		final List<Component> tt = Lists.newArrayList(
		  Component.translatable(String.format(
		    "simpleconfig.preset.%s.%s",
		    delete? "delete" : "save",
			 remote? "remote" : "local"
		  ), Component.translatable("simpleconfig.preset." + type.getAlias())));
		if (!delete && !remote && handler.canSaveRemote())
			tt.add(Component.translatable("simpleconfig.preset.save.remote.shift"));
		if (!delete && selector.getValue() != null)
			tt.add(Component.translatable("simpleconfig.preset.delete.alt"));
		return tt;
	}
	
	public List<Preset> getKnownPresets() {
		return Stream.of(
		  knownLocalPresets, knownRemotePresets, knownResourcePresets
		).flatMap(m -> m.values().stream()).flatMap(m -> m.values().stream())
		  .collect(Collectors.toList());
	}
	
	@Override public Rectangle getArea() {
		return area;
	}
	
	protected void position() {
		if (screen.getFocused() != this)
			listeners.forEach(l -> l.setFocused(false));
		
		selector.setX(area.x + 1);
		selector.setY(area.y + 2);
		selector.setHeight(area.height - 2);
		selector.setWidth(area.width - 42);
		
		loadButton.setX(area.getMaxX() - 40);
		loadButton.setY(area.y);
		
		saveButton.setX(area.getMaxX() - 20);
		saveButton.setY(area.y);
	}
	
	protected IConfigSnapshotHandler getHandler() {
		return screen.getSnapshotHandler();
	}
	
	protected void update() {
		knownLocalPresets.values().forEach(Map::clear);
		for (Preset preset: getHandler().getLocalPresets())
			knownLocalPresets.get(preset.getType()).put(preset.getName(), preset);
		knownResourcePresets.values().forEach(Map::clear);
		for (Preset preset: getHandler().getResourcePresets())
		  knownResourcePresets.get(preset.getType()).put(preset.getName(), preset);
		refreshSelector();
		if (lastFuture == null) {
			lastFuture = getHandler().getRemotePresets().thenAccept(presets -> {
				knownRemotePresets.values().forEach(Map::clear);
				for (Preset p: presets) knownRemotePresets.get(p.getType()).put(p.getName(), p);
				lastFuture = null;
				refreshSelector();
			});
		}
	}
	
	protected void refreshSelector() {
		final Preset value = selector.getValue();
		String text = selector.getText();
		if (value != null && !isKnownPreset(value))
			selector.setText(selector.getText());
		if (value == null && isKnownPreset(text)) {
			selector.setText("");
			selector.setText(text);
		}
	}
	
	@Override public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float delta) {
		final long time = System.currentTimeMillis();
		if ((selector.isFocused() || lastUpdate == 0L) && time - lastUpdate > updateCooldown) {
			lastUpdate = time;
			update();
		}
		position();
		selector.active = getHandler() != null && screen.isEditable();
		selector.render(gg, mouseX, mouseY, delta);
		loadButton.render(gg, mouseX, mouseY, delta);
		saveButton.render(gg, mouseX, mouseY, delta);
	}
	
	public List<Preset> getKnownPresets(Type type) {
		return Stream.of(
		  knownLocalPresets.get(type),
		  knownRemotePresets.get(type),
		  knownResourcePresets.get(type)
		).flatMap(m -> m.values().stream()).collect(Collectors.toList());
	}
	
	public Map<String, Preset> getKnownPresets(Type type, Location location) {
		return switch (location) {
			case LOCAL -> knownLocalPresets.get(type);
			case REMOTE -> knownRemotePresets.get(type);
			case RESOURCE -> knownResourcePresets.get(type);
		};
	}
	
	public boolean isKnownPreset(Preset preset) {
		return getKnownPresets(preset.getType(), preset.getLocation()).containsValue(preset);
	}
	
	public void load(Preset preset) {
		final Type type = screen.getEditedType().getType();
		final IConfigSnapshotHandler handler = getHandler();
		Set<String> s = screen.getSelectedEntries().stream()
		  .map(AbstractConfigField::getRelPath)
		  .collect(Collectors.toSet());
		final Set<String> selection = s.isEmpty()? null : s;
		if (preset.isRemote()) {
			final CompletableFuture<Void> future = handler.getRemote(preset.getName(), type)
			  .thenAccept(config -> doLoad(config, preset, type, selection));
			screen.addDialog(ProgressDialog.create(
			  Component.translatable("simpleconfig.preset.dialog.loading.title"),
			  future, d -> d.setBody(splitTtc(
				 "simpleconfig.preset.dialog.loading.remote.body", presetName(preset.getName())))));
		} else {
			// Local and resource presets always return completed futures
			handler.getPresetSnapshot(preset).thenAccept(
			  snapshot -> doLoad(snapshot, preset, type, selection));
		}
	}
	
	protected void doLoad(
	  CommentedConfig snapshot, Preset preset, Type type, @Nullable Set<String> selection
	) {
		final IConfigSnapshotHandler handler = getHandler();
		String ll = preset.getLocation().getAlias();
		MutableComponent tt = Component.translatable("simpleconfig.preset." + type.getAlias());
		int matches = getMatches(snapshot, selection);
		int total = getTotalSize(snapshot);
		screen.runAtomicTransparentAction(
		  () -> handler.restore(snapshot, type, selection));
		screen.addDialog(InfoDialog.create(
		  Component.translatable("simpleconfig.preset.dialog.load.success.title"), Util.make(new ArrayList<>(), b -> {
			b.addAll(splitTtc(
			  "simpleconfig.preset.dialog.load.success." + ll,
			  tt, presetName(preset.getName())));
			if (selection != null && matches > 0) {
				b.addAll(splitTtc(
				  "simpleconfig.preset.dialog.load.success.selected",
				  selectedCount(matches), selectedCount(total)));
			} else if (selection != null) {
				b.addAll(splitTtc(
				  "simpleconfig.preset.dialog.load.success.none", selectedCount(total)));
			} else b.addAll(splitTtc(
			  "simpleconfig.preset.dialog.load.success.all", selectedCount(total)));
			b.addAll(splitTtc("simpleconfig.preset.dialog.load.success.undo"));
		})));
	}
	
	protected int getMatches(CommentedConfig snapshot, Set<String> selection) {
		if (selection == null) return 0;
		return (int) selection.stream()
		  .map(p -> p.startsWith(".")? p.substring(1) : p)
		  .filter(snapshot::contains).count();
	}
	
	protected int getTotalSize(CommentedConfig snapshot) {
		int total = 0;
		for (Object obj: snapshot.valueMap().values()) {
			if (obj instanceof CommentedConfig) {
				total += getTotalSize((CommentedConfig) obj);
			} else total++;
		}
		return total;
	}
	
	public void save(String name, boolean remote) {
		save(name, remote, false);
	}
	
	public static boolean userSkipOverwriteDialog = false;
	public void save(String name, boolean remote, boolean overwrite) {
		final Type type = screen.getEditedType().getType();
		final IConfigSnapshotHandler handler = getHandler();
		Set<String> s = screen.getSelectedEntries().stream()
		  .map(AbstractConfigField::getRelPath)
		  .collect(Collectors.toSet());
		int selected = s.size();
		Set<String> selection = selected > 0? s : null;
		final CommentedConfig preserved = handler.preserve(type, selection);
		final Map<String, Preset> presetMap = getKnownPresets(type, remote? Location.REMOTE : Location.LOCAL);
		final Map<String, Preset> otherMap = getKnownPresets(type, remote? Location.LOCAL : Location.REMOTE);
		final Map<String, Preset> resourceMap = getKnownPresets(type, Location.RESOURCE);
		String ll = remote? "remote" : "local";
		Component tt = Component.translatable("simpleconfig.preset." + type.getAlias());
		if (!overwrite && !userSkipOverwriteDialog && presetMap.containsKey(name)
		    && presetMap.get(name).getType() == type) {
			screen.addDialog(ConfirmDialog.create(
			  Component.translatable("simpleconfig.preset.dialog.overwrite.title"), d -> {
				  d.withCheckBoxes((b, cs) -> {
					  if (b) {
						  if (cs[0]) userSkipOverwriteDialog = true;
						  save(name, remote, true);
					  }
				  }, CheckboxButton.of(false, Component.translatable("simpleconfig.ui.do_not_ask_again_this_session")));
				  d.setBody(splitTtc(
				    "simpleconfig.preset.dialog.overwrite." + ll, tt, presetName(name)));
				  d.setConfirmText(
				    Component.translatable("simpleconfig.preset.dialog.overwrite.confirm"));
				  d.setConfirmButtonTint(0xAAAA00AA);
			  }));
		} else if (!overwrite && (otherMap.containsKey(name) || resourceMap.containsKey(name)) && !presetMap.containsKey(name)) {
			boolean resource = !otherMap.containsKey(name);
			String r = resource? "resource." : "";
			screen.addDialog(ConfirmDialog.create(
			  Component.translatable("simpleconfig.preset.dialog.mistaken.title"), d -> {
				  d.withAction(b -> {
					  if (b) save(name, remote, true);
				  });
				  d.setBody(splitTtc("simpleconfig.preset.dialog.mistaken." + r + ll + ".body", presetName(name)));
				  d.setConfirmText(
				    Component.translatable("simpleconfig.preset.dialog.mistaken.option." + ll + ".create"));
				  d.setConfirmButtonTint(remote ? 0xAA429090 : 0xAA429042);
				  if (!resource) {
					  d.addButton(1, TintedButton.of(
						 Component.translatable("simpleconfig.preset.dialog.mistaken.option." + ll + ".overwrite"),
						 remote? 0xAA429042 : 0xAA429090, p -> {
							 save(name, !remote, true);
							 d.cancel(false);
						 }));
				  }
			  }));
		} else if (remote) {
			final CompletableFuture<Void> future = handler.saveRemote(name, type, preserved);
			screen.addDialog(ProgressDialog.create(
			  Component.translatable("simpleconfig.preset.dialog.saving.title"),
			  future, d -> {
				  d.setBody(splitTtc("simpleconfig.preset.dialog.saving.remote.body", presetName(name)));
				  d.setCancellableByUser(false);
				  d.setSuccessDialog(getSaveSuccessDialog(true, type, name, selected));
			  }));
			future.thenAccept(v -> update());
			// Once the packet has been sent, cancellation is not possible
		} else {
			Optional<Throwable> result = handler.saveLocal(name, type, preserved);
			if (result.isPresent()) {
				screen.addDialog(ErrorDialog.create(
				  Component.translatable("simpleconfig.preset.dialog.saving.error.title"), result.get(), d -> d.setBody(
					 splitTtc("simpleconfig.preset.dialog.saving.error.local.body", presetName(name)))));
			} else {
				screen.addDialog(getSaveSuccessDialog(false, type, name, selected));
				update();
			}
		}
	}
	
	protected AbstractDialog getSaveSuccessDialog(
	  boolean remote, Type type, String name, int selected
	) {
		String ll = remote? "remote" : "local";
		Component tt = Component.translatable("simpleconfig.preset." + type.getAlias());
		return InfoDialog.create(
		  Component.translatable("simpleconfig.preset.dialog.save.success.title"),
		  Util.make(new ArrayList<>(), b -> {
			  b.addAll(splitTtc("simpleconfig.preset.dialog.save.success." + ll, tt, presetName(name)));
			  if (selected > 0) {
				  b.addAll(splitTtc(
					 "simpleconfig.preset.dialog.save.success.selected", selectedCount(selected)));
			  } else b.addAll(splitTtc("simpleconfig.preset.dialog.save.success.all"));
		  }));
	}
	
	public static boolean userSkipConfirmDeleteDialog = false;
	public void delete(Preset preset) {
		delete(preset, false);
	}
	public void delete(Preset preset, boolean skipConfirm) {
		final Type type = screen.getEditedType().getType();
		final IConfigSnapshotHandler handler = getHandler();
		String ll = preset.isRemote()? "remote" : "local";
		Component tt = Component.translatable("simpleconfig.preset." + preset.getType().getAlias());
		if (!userSkipConfirmDeleteDialog && !skipConfirm) {
			screen.addDialog(ConfirmDialog.create(
			  Component.translatable("simpleconfig.preset.dialog.delete.confirm.title"), d -> {
				  d.withCheckBoxes((b, cs) -> {
					  if (b) {
						  if (cs[0]) userSkipConfirmDeleteDialog = true;
						  delete(preset, true);
					  }
				  }, CheckboxButton.of(false, Component.translatable("simpleconfig.ui.do_not_ask_again_this_session")));
				  d.setBody(splitTtc(
					 "simpleconfig.preset.dialog.delete.confirm." + ll, tt, presetName(preset.getName())));
				  d.setConfirmText(
				    Component.translatable("simpleconfig.preset.dialog.delete.confirm.delete"));
				  d.setConfirmButtonTint(0x80BD2424);
			  }));
		} else if (preset.isRemote()) {
			final CompletableFuture<Void> future = handler.deleteRemote(preset.getName(), type);
			screen.addDialog(ProgressDialog.create(
			  Component.translatable("simpleconfig.preset.dialog.deleting.title"),
			  future, d -> {
				  d.setBody(splitTtc("simpleconfig.preset.dialog.deleting.remote.body", presetName(
				    preset.getName())));
				  d.setCancellableByUser(false);
				  d.setSuccessDialog(getDeleteSuccessDialog(preset));
			  }));
			future.thenAccept(v -> update());
		} else {
			handler.deleteLocal(preset.getName(), type);
			screen.addDialog(getDeleteSuccessDialog(preset));
			update();
		}
	}
	
	protected AbstractDialog getDeleteSuccessDialog(
	  Preset preset
	) {
		String ll = preset.isRemote()? "remote" : "local";
		Component tt = Component.translatable("simpleconfig.preset." + preset.getType().getAlias());
		return InfoDialog.create(
		  Component.translatable("simpleconfig.preset.dialog.delete.success.title"),
		  splitTtc("simpleconfig.preset.dialog.delete.success." + ll, tt, presetName(preset.getName())));
	}
	
	protected MutableComponent selectedCount(int count) {
		return Component.literal(String.valueOf(count)).withStyle(selectedCountStyle);
	}
	
	protected MutableComponent presetName(String name) {
		return Component.literal(name).withStyle(nameStyle);
	}
	
	public boolean isKnownPreset(String name) {
		return Stream.of(
		  knownLocalPresets, knownRemotePresets, knownResourcePresets
		).flatMap(m -> m.values().stream()).anyMatch(m -> m.containsKey(name));
	}
	
	public boolean isKnownPreset(String name, Type type) {
		return Stream.of(
		  knownLocalPresets, knownRemotePresets, knownResourcePresets
		).map(m -> m.get(type)).anyMatch(m -> m.containsKey(name));
	}
	
	private static final Pattern NAME_PATTERN = Pattern.compile(
	  "^[^/\\n\\r\\t\0\\f`?*\\\\<>|\":]+$");
	public boolean isValidName(String text) {
		return NAME_PATTERN.matcher(text).matches();
	}
	
	public void refresh() {
		String text = selector.getText();
		selector.setValue(null);
		selector.setText(text);
	}
	
	@Override public @NotNull NarrationPriority narrationPriority() {
		return NarrationPriority.NONE;
	}
	@Override public void updateNarration(@NotNull NarrationElementOutput out) {}
	
	public static class Preset {
		private final String name;
		private final Type type;
		private final Location location;
		
		private Preset(String name, Type type, Location location) {
			this.name = name;
			this.type = type;
			this.location = location;
		}
		
		public static Preset local(String name, Type type) {
			return new Preset(name, type, Location.LOCAL);
		}
		
		public static Preset remote(String name, Type type) {
			return new Preset(name, type, Location.REMOTE);
		}
		
		public static Preset resource(String name, Type type) {
			return new Preset(name, type, Location.RESOURCE);
		}
		
		public String getName() {
			return name;
		}
		
		public Type getType() {
			return type;
		}
		
		public boolean isClient() {
			return type == SimpleConfig.Type.CLIENT;
		}
		public boolean isCommon() {
			return type == SimpleConfig.Type.COMMON;
		}
		public boolean isServer() {
			return type == SimpleConfig.Type.SERVER;
		}
		
		public Location getLocation() {
			return location;
		}
		
		public boolean isLocal() {
			return location == Location.LOCAL;
		}
		public boolean isRemote() {
			return location == Location.REMOTE;
		}
		public boolean isResource() {
			return location == Location.RESOURCE;
		}
		
		@Override public String toString() {
			return getName();
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Preset preset = (Preset) o;
			return getType() == preset.getType()
			       && getLocation() == preset.getLocation()
			       && getName().equals(preset.getName());
		}
		
		@Override public int hashCode() {
			return Objects.hash(getName(), getType(), getLocation());
		}
		
		public enum Location {
			LOCAL, REMOTE, RESOURCE;
			
			private final String alias;
			Location() {
				alias = name().toLowerCase();
			}
			public String getAlias() {
				return alias;
			}
		}
		
		@SuppressWarnings("ClassCanBeRecord")
		public static class TypeWrapper implements
		                                endorh.simpleconfig.ui.gui.widget.combobox.wrapper.TypeWrapper<Preset> {
			public final PresetPickerWidget widget;
			
			public TypeWrapper(PresetPickerWidget widget) {
				this.widget = widget;
			}
			
			@Override public Pair<Optional<Preset>, Optional<Component>> parseElement(
			  @NotNull String text
			) {
				final Type type = widget.screen.getEditedType().getType();
				for (Location location: Location.values()) {
					Map<String, Preset> presets = widget.getKnownPresets(type, location);
					if (presets.containsKey(text))
						return Pair.of(Optional.of(presets.get(text)), Optional.empty());
				}
				return Pair.of(Optional.empty(), Optional.of(
				  Component.translatable("simpleconfig.config.error.unknown_preset")));
			}
			
			@Override public String getName(@NotNull Preset element) {
				return element.getName();
			}
			@Override public Component getDisplayName(@NotNull Preset element) {
				return Component.literal(element.getName()).withStyle(
				  element.isRemote()? ChatFormatting.AQUA : ChatFormatting.GREEN);
			}
			
			@Override public boolean hasIcon() {
				return true;
			}
			@Override public int getIconHeight() {
				return 14;
			}
			
			@Override public Optional<Icon> getIcon(
			  @Nullable Preset element, String text
			) { // @formatter:off
				if (element == null) {
					return text.isEmpty()? Optional.empty() : Optional.ofNullable(
					  saveIconFor(widget.screen.getEditedType().getType()));
				} else {
					return Optional.ofNullable(iconFor(element.getType(), element.getLocation()));
				}
			} // @formatter:on
			
			public static Icon saveIconFor(Type type) {
				return switch (type) {
					case CLIENT -> Presets.CLIENT_SAVE;
					case COMMON -> Presets.COMMON_SAVE;
					case SERVER -> Presets.SERVER_SAVE;
				};
			}
			
			public static Icon iconFor(Type type, Location location) {
				return switch (type) {
					case CLIENT -> switch (location) {
						case LOCAL -> Presets.CLIENT_LOCAL;
						case REMOTE -> Presets.CLIENT_REMOTE;
						case RESOURCE -> Presets.CLIENT_RESOURCE;
					};
					case COMMON -> switch (location) {
						case LOCAL -> Presets.COMMON_LOCAL;
						case REMOTE -> Presets.COMMON_REMOTE;
						case RESOURCE -> Presets.COMMON_RESOURCE;
					};
					case SERVER -> switch (location) {
						case LOCAL -> Presets.SERVER_LOCAL;
						case REMOTE -> Presets.SERVER_REMOTE;
						case RESOURCE -> Presets.SERVER_RESOURCE;
					};
				};
			}
		}
	}
	
	@Override public @NotNull List<? extends GuiEventListener> children() {
		return listeners;
	}
}
