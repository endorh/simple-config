package endorh.simpleconfig.ui.gui.widget;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.AbstractConfigEntry;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigSnapshotHandler;
import endorh.simpleconfig.ui.gui.*;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons.Presets;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.Modifier;
import endorh.simpleconfig.ui.gui.widget.PresetPickerWidget.Preset.Location;
import endorh.simpleconfig.ui.gui.widget.PresetPickerWidget.Preset.TypeWrapper;
import endorh.simpleconfig.ui.gui.widget.combobox.ComboBoxWidget;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.ITypeWrapper;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.core.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.ui.gui.WidgetUtils.forceUnFocus;

public class PresetPickerWidget extends FocusableGui {
	protected List<IGuiEventListener> listeners;
	protected long lastUpdate = 0L;
	protected long updateCooldown = 1000L;
	protected @Nullable CompletableFuture<Void> lastFuture = null;
	
	public int x, y, w, h;
	
	protected AbstractConfigScreen screen;
	
	protected MultiFunctionImageButton loadButton;
	protected MultiFunctionImageButton saveButton;
	
	protected Map<String, Preset> knownLocalClientPresets = Maps.newHashMap();
	protected Map<String, Preset> knownLocalServerPresets = Maps.newHashMap();
	protected Map<String, Preset> knownRemoteClientPresets = Maps.newHashMap();
	protected Map<String, Preset> knownRemoteServerPresets = Maps.newHashMap();
	protected Map<String, Preset> knownResourceClientPresets = Maps.newHashMap();
	protected Map<String, Preset> knownResourceServerPresets = Maps.newHashMap();
	
	protected Style selectedCountStyle = Style.EMPTY.applyFormatting(TextFormatting.DARK_AQUA);
	protected Style nameStyle = Style.EMPTY.applyFormatting(TextFormatting.LIGHT_PURPLE);
	
	protected ComboBoxWidget<Preset> selector;
	
	public PresetPickerWidget(
	  AbstractConfigScreen screen, int x, int y, int w
	) {
		this.screen = screen;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = 18;
		loadButton = new MultiFunctionImageButton(0, 0, 20, 20, Buttons.LOAD, ButtonAction.of(() -> {
			  final Preset value = selector.getValue();
			  if (value != null) load(value);
		}).active(() -> getHandler() != null
		                && isKnownPreset(selector.getText(), screen.isSelectedCategoryServer())
		                && screen.isEditable()
		  ).tooltip(() -> {
			  final Preset value = selector.getValue();
			  if (value == null) return Lists.newArrayList();
			  return Lists.newArrayList(
			    new TranslationTextComponent(String.format(
			      "simpleconfig.preset.load.%s.%s",
			      value.isServer()? "server" : "client",
			      value.isResource()? "resource" : value.isRemote()? "remote" : "local")));
		  }), new TranslationTextComponent("simpleconfig.preset.load.label"));
		saveButton = new MultiFunctionImageButton(0, 0, 20, 20, Buttons.SAVE, ButtonAction.of(
		  () -> save(selector.getText(), false)
		).tooltip(() -> getSaveTooltip(false, false))
		  .active(() -> getHandler() != null
		                && isValidName(selector.getText())
		                && screen.isEditable()
		  ), new TranslationTextComponent("simpleconfig.preset.save.label")
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
		  new TypeWrapper(this), () -> screen, x, y, w - 48, 24,
		  new TranslationTextComponent("simpleconfig.preset.picker.label"));
		selector.setHint(new TranslationTextComponent("simpleconfig.preset.picker.hint"));
		final SimpleComboBoxModel<Preset> provider = new SimpleComboBoxModel<>(
		  () -> getKnownPresets(screen.isSelectedCategoryServer()));
		provider.setPlaceholder(new TranslationTextComponent("simpleconfig.ui.no_presets_found"));
		selector.setSuggestionProvider(provider);
		listeners = Lists.newArrayList(selector, loadButton, saveButton);
	}
	
	protected List<ITextComponent> getSaveTooltip(
	  boolean delete, boolean remote
	) {
		final IConfigSnapshotHandler handler = getHandler();
		if (handler == null || !isValidName(selector.getText()))
			return Lists.newArrayList();
		boolean server = screen.isSelectedCategoryServer();
		final ArrayList<ITextComponent> tt = Lists.newArrayList(
		  new TranslationTextComponent(String.format(
		    "simpleconfig.preset.%s.%s.%s",
		    delete? "delete" : "save", server ? "server" : "client",
			 remote? "remote" : "local")));
		if (!delete && !remote && handler.canSaveRemote())
			tt.add(new TranslationTextComponent("simpleconfig.preset.save.remote.shift"));
		if (!delete && selector.getValue() != null)
			tt.add(new TranslationTextComponent("simpleconfig.preset.delete.alt"));
		return tt;
	}
	
	public List<Preset> getKnownPresets() {
		return Stream.of(
		  knownLocalClientPresets.values(), knownRemoteClientPresets.values(),
		  knownLocalServerPresets.values(), knownRemoteServerPresets.values(),
		  knownResourceClientPresets.values(), knownResourceServerPresets.values()
		).flatMap(Collection::stream).collect(Collectors.toList());
	}
	
	protected void position() {
		if (screen.getListener() != this) forceUnFocus(listeners);
		
		selector.x = x;
		selector.y = y + 2;
		selector.setHeight(h - 2);
		selector.setWidth(w - 42);
		
		loadButton.x = x + w - 40;
		loadButton.y = y;
		
		saveButton.x = x + w - 20;
		saveButton.y = y;
	}
	
	protected IConfigSnapshotHandler getHandler() {
		return screen.getSnapshotHandler();
	}
	
	private static final Pattern HYPHEN = Pattern.compile("-");
	protected void update() {
		knownLocalClientPresets.clear();
		knownLocalServerPresets.clear();
		for (Preset preset: getHandler().getLocalPresets()) (
		  preset.isServer()? knownLocalServerPresets : knownLocalClientPresets
		).put(preset.getName(), preset);
		knownResourceClientPresets.clear();
		knownRemoteServerPresets.clear();
		for (Preset preset: getHandler().getResourcePresets()) (
		  preset.isServer()? knownResourceServerPresets : knownResourceClientPresets
		).put(preset.getName(), preset);
		refreshSelector();
		if (lastFuture == null) {
			lastFuture = getHandler().getRemotePresets().thenAccept(presets -> {
				knownRemoteClientPresets.clear();
				knownRemoteServerPresets.clear();
				for (Preset p: presets) (
				  p.isServer()? knownRemoteServerPresets : knownRemoteClientPresets
				).put(p.getName(), p);
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
	
	public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		final long time = System.currentTimeMillis();
		if ((selector.isFocused() || lastUpdate == 0L) && time - lastUpdate > updateCooldown) {
			lastUpdate = time;
			update();
		}
		position();
		selector.active = getHandler() != null && screen.isEditable();
		selector.render(mStack, mouseX, mouseY, delta);
		loadButton.render(mStack, mouseX, mouseY, delta);
		saveButton.render(mStack, mouseX, mouseY, delta);
	}
	
	public List<Preset> getKnownPresets(boolean server) {
		return Stream.of(
		  server? knownLocalServerPresets : knownLocalClientPresets,
		  server? knownRemoteServerPresets : knownRemoteClientPresets,
		  server? knownResourceServerPresets : knownResourceClientPresets
		).flatMap(m -> m.values().stream()).collect(Collectors.toList());
	}
	
	public Map<String, Preset> getKnownPresets(boolean server, Location location) {
		switch (location) {
			case LOCAL: return server? knownLocalServerPresets : knownLocalClientPresets;
			case REMOTE: return server? knownRemoteServerPresets : knownRemoteClientPresets;
			case RESOURCE: return server? knownResourceServerPresets : knownResourceClientPresets;
			default: return Collections.emptyMap();
		}
	}
	
	public boolean isKnownPreset(Preset preset) {
		return getKnownPresets(preset.isServer(), preset.getLocation()).containsValue(preset);
	}
	
	public void load(Preset preset) {
		final Type type = screen.currentConfigType();
		final IConfigSnapshotHandler handler = getHandler();
		Set<String> s = screen.getSelectedEntries().stream()
		  .map(AbstractConfigEntry::getPath)
		  .collect(Collectors.toSet());
		final Set<String> selection = s.isEmpty()? null : s;
		if (preset.isRemote()) {
			final CompletableFuture<Void> future = handler.getRemote(preset.getName(), type)
			  .thenAccept(config -> doLoad(config, preset, type, selection));
			screen.addDialog(ProgressDialog.create(
			  new TranslationTextComponent("simpleconfig.preset.dialog.loading.title"),
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
		String ll = preset.isResource()? "resource." : preset.isRemote()? "remote." : "local.";
		String tt = type == Type.SERVER? "server" : "client";
		int matches = getMatches(snapshot, selection);
		int total = getTotalSize(snapshot);
		screen.runAtomicTransparentAction(
		  () -> handler.restore(snapshot, type, selection));
		screen.addDialog(InfoDialog.create(new TranslationTextComponent(
		  "simpleconfig.preset.dialog.load.success.title"
		), Util.make(new ArrayList<>(), b -> {
			b.addAll(splitTtc(
			  "simpleconfig.preset.dialog.load.success." + ll + tt, presetName(preset.getName())));
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
		return (int) selection.stream().filter(snapshot::contains).count();
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
		final Type type = screen.currentConfigType();
		final boolean server = type == Type.SERVER;
		final IConfigSnapshotHandler handler = getHandler();
		Set<String> s = screen.getSelectedEntries().stream()
		  .map(AbstractConfigEntry::getPath)
		  .collect(Collectors.toSet());
		int selected = s.size();
		Set<String> selection = selected > 0? s : null;
		final CommentedConfig preserved = handler.preserve(type, selection);
		final Map<String, Preset> presetMap = getKnownPresets(server, remote? Location.REMOTE : Location.LOCAL);
		final Map<String, Preset> otherMap = getKnownPresets(server, remote? Location.LOCAL : Location.REMOTE);
		final Map<String, Preset> resourceMap = getKnownPresets(server, Location.RESOURCE);
		String ll = remote? "remote." : "local.";
		String tt = server? "server" : "client";
		if (!overwrite && !userSkipOverwriteDialog && presetMap.containsKey(name)
		    && presetMap.get(name).isServer() == server) {
			screen.addDialog(ConfirmDialog.create(
			  new TranslationTextComponent("simpleconfig.preset.dialog.overwrite.title"), d -> {
				  d.withCheckBoxes((b, cs) -> {
					  if (b) {
						  if (cs[0]) userSkipOverwriteDialog = true;
						  save(name, remote, true);
					  }
				  }, CheckboxButton.of(false, new TranslationTextComponent(
				    "simpleconfig.ui.do_not_ask_again_this_session")));
				  d.setBody(splitTtc(
					 "simpleconfig.preset.dialog.overwrite." + ll + tt, presetName(name)));
				  d.setConfirmText(new TranslationTextComponent("simpleconfig.preset.dialog.overwrite.confirm"));
				  d.setConfirmButtonTint(0xAAAA00AA);
			  }));
		} else if (!overwrite && (otherMap.containsKey(name) || resourceMap.containsKey(name)) && !presetMap.containsKey(name)) {
			boolean resource = !otherMap.containsKey(name);
			String r = resource? "resource." : "";
			screen.addDialog(ConfirmDialog.create(
			  new TranslationTextComponent("simpleconfig.preset.dialog.mistaken.title"), d -> {
				  d.withAction(b -> {
					  if (b) save(name, remote, true);
				  });
				  d.setBody(splitTtc("simpleconfig.preset.dialog.mistaken." + r + ll + "body", presetName(name)));
				  d.setConfirmText(new TranslationTextComponent(
				    "simpleconfig.preset.dialog.mistaken.option." + ll + "create"));
				  d.setConfirmButtonTint(remote ? 0xAA429090 : 0xAA429042);
				  if (!resource) {
					  d.addButton(1, TintedButton.of(
						 new TranslationTextComponent(
							"simpleconfig.preset.dialog.mistaken.option." + ll + "overwrite"),
						 remote? 0xAA429042 : 0xAA429090, p -> {
							 save(name, !remote, true);
							 d.cancel(false);
						 }));
				  }
			  }));
		} else if (remote) {
			final CompletableFuture<Void> future = handler.saveRemote(name, type, preserved);
			screen.addDialog(ProgressDialog.create(
			  new TranslationTextComponent("simpleconfig.preset.dialog.saving.title"),
			  future, d -> {
				  d.setBody(splitTtc("simpleconfig.preset.dialog.saving.remote.body", presetName(name)));
				  d.setCancellableByUser(false);
				  d.setSuccessDialog(getSaveSuccessDialog(true, server, name, selected));
			  }));
			future.thenAccept(v -> update());
			// Once the packet has been sent, cancellation is not possible
		} else {
			Optional<Throwable> result = handler.saveLocal(name, type, preserved);
			if (result.isPresent()) {
				screen.addDialog(ErrorDialog.create(
				  new TranslationTextComponent(
					 "simpleconfig.preset.dialog.saving.error.title"), result.get(), d -> d.setBody(
					 splitTtc("simpleconfig.preset.dialog.saving.error.local.body", presetName(name)))));
			} else {
				screen.addDialog(getSaveSuccessDialog(false, server, name, selected));
				update();
			}
		}
	}
	
	protected AbstractDialog getSaveSuccessDialog(
	  boolean remote, boolean server, String name, int selected
	) {
		final String ll = remote? "remote." : "local.";
		final String tt = server? "server" : "client";
		return InfoDialog.create(
		  new TranslationTextComponent("simpleconfig.preset.dialog.save.success.title"),
		  Util.make(new ArrayList<>(), b -> {
			  b.addAll(splitTtc("simpleconfig.preset.dialog.save.success." + ll + tt, presetName(name)));
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
		final Type type = screen.currentConfigType();
		final IConfigSnapshotHandler handler = getHandler();
		String ll = preset.isRemote()? "remote." : "local.";
		String tt = preset.isServer()? "server" : "client";
		if (!userSkipConfirmDeleteDialog && !skipConfirm) {
			screen.addDialog(ConfirmDialog.create(
			  new TranslationTextComponent("simpleconfig.preset.dialog.delete.confirm.title"), d -> {
				  d.withCheckBoxes((b, cs) -> {
					  if (b) {
						  if (cs[0]) userSkipConfirmDeleteDialog = true;
						  delete(preset, true);
					  }
				  }, CheckboxButton.of(false, new TranslationTextComponent(
				    "simpleconfig.ui.do_not_ask_again_this_session")));
				  d.setBody(splitTtc(
					 "simpleconfig.preset.dialog.delete.confirm." + ll + tt, presetName(preset.getName())));
				  d.setConfirmText(new TranslationTextComponent(
					 "simpleconfig.preset.dialog.delete.confirm.delete"));
				  d.setConfirmButtonTint(0x80BD2424);
			  }));
		} else if (preset.isRemote()) {
			final CompletableFuture<Void> future = handler.deleteRemote(preset.getName(), type);
			screen.addDialog(ProgressDialog.create(
			  new TranslationTextComponent("simpleconfig.preset.dialog.deleting.title"),
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
		final String ll = preset.isRemote()? "remote." : "local.";
		final String tt = preset.isServer()? "server" : "client";
		return InfoDialog.create(
		  new TranslationTextComponent("simpleconfig.preset.dialog.delete.success.title"),
		  splitTtc("simpleconfig.preset.dialog.delete.success." + ll + tt, presetName(
		    preset.getName())));
	}
	
	protected IFormattableTextComponent selectedCount(int count) {
		return new StringTextComponent(String.valueOf(count)).mergeStyle(selectedCountStyle);
	}
	
	protected IFormattableTextComponent presetName(String name) {
		return new StringTextComponent(name).mergeStyle(nameStyle);
	}
	
	public boolean isKnownPreset(String name) {
		return Stream.of(
		  knownLocalClientPresets.keySet(), knownLocalServerPresets.keySet(),
		  knownRemoteClientPresets.keySet(), knownRemoteServerPresets.keySet(),
		  knownResourceClientPresets.keySet(), knownResourceServerPresets.keySet()
		).anyMatch(s -> s.contains(name));
	}
	
	public boolean isKnownPreset(String name, boolean server) {
		return Stream.of(
		  server? knownLocalServerPresets : knownLocalClientPresets,
		  server? knownRemoteServerPresets : knownRemoteClientPresets,
		  server? knownResourceServerPresets : knownResourceClientPresets
		).anyMatch(m -> m.containsKey(name));
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
	
	public static class Preset {
		private final String name;
		private final boolean server;
		private final Location location;
		
		private Preset(String name, boolean server, Location location) {
			this.name = name;
			this.server = server;
			this.location = location;
		}
		
		public static Preset local(String name, boolean server) {
			return new Preset(name, server, Location.LOCAL);
		}
		
		public static Preset remote(String name, boolean server) {
			return new Preset(name, server, Location.REMOTE);
		}
		
		public static Preset resource(String name, boolean server) {
			return new Preset(name, server, Location.RESOURCE);
		}
		
		public String getName() {
			return name;
		}
		
		public boolean isClient() {
			return !server;
		}
		
		public boolean isServer() {
			return server;
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
			return isServer() == preset.isServer()
			       && getLocation() == preset.getLocation()
			       && getName().equals(preset.getName());
		}
		
		@Override public int hashCode() {
			return Objects.hash(getName(), isServer(), getLocation());
		}
		
		public enum Location { LOCAL, REMOTE, RESOURCE }
		
		public static class TypeWrapper implements ITypeWrapper<Preset> {
			public final PresetPickerWidget widget;
			
			public TypeWrapper(PresetPickerWidget widget) {
				this.widget = widget;
			}
			
			@Override public Pair<Optional<Preset>, Optional<ITextComponent>> parseElement(
			  @NotNull String text
			) {
				final boolean server = widget.screen.isSelectedCategoryServer();
				for (Location location: Location.values()) {
					Map<String, Preset> presets = widget.getKnownPresets(server, location);
					if (presets.containsKey(text))
						return Pair.of(Optional.of(presets.get(text)), Optional.empty());
				}
				return Pair.of(Optional.empty(), Optional.of(new TranslationTextComponent(
				  "simpleconfig.config.error.unknown_preset")));
			}
			
			@Override public String getName(@NotNull Preset element) {
				return element.getName();
			}
			@Override public ITextComponent getDisplayName(@NotNull Preset element) {
				return new StringTextComponent(element.getName()).mergeStyle(
				  element.isRemote()? TextFormatting.AQUA : TextFormatting.GREEN);
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
					return text.isEmpty()? Optional.empty() : Optional.of(
					  widget.screen.isSelectedCategoryServer()
					  ? Presets.SERVER_SAVE : Presets.CLIENT_SAVE);
				} else {
					switch (element.getLocation()) {
						case LOCAL: return Optional.of(element.isServer()? Presets.SERVER_LOCAL : Presets.CLIENT_LOCAL);
						case REMOTE: return Optional.of(element.isServer()? Presets.SERVER_REMOTE : Presets.CLIENT_REMOTE);
						case RESOURCE: return Optional.of(element.isServer()? Presets.SERVER_RESOURCE : Presets.CLIENT_RESOURCE);
						default: return Optional.empty();
					}
				}
			} // @formatter:on
		}
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return listeners;
	}
}
