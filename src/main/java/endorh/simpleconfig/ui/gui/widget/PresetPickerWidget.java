package endorh.simpleconfig.ui.gui.widget;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.AbstractConfigEntry;
import endorh.simpleconfig.ui.api.ConfigBuilder.IConfigSnapshotHandler;
import endorh.simpleconfig.ui.gui.*;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons.Presets;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.Modifier;
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
		}).active(() -> getHandler() != null && isKnownPreset(selector.getText(), screen.isSelectedCategoryServer()))
		  .tooltip(() -> {
			  final Preset value = selector.getValue();
			  if (value == null) return Lists.newArrayList();
			  return Lists.newArrayList(
			    new TranslationTextComponent(String.format(
					"simpleconfig.preset.load.%s.%s",
					value.server ? "server" : "client",
					value.remote ? "remote" : "local")));
		  }), new TranslationTextComponent("simpleconfig.preset.load.label"));
		saveButton = new MultiFunctionImageButton(0, 0, 20, 20, Buttons.SAVE, ButtonAction.of(
		  () -> save(selector.getText(), false)
		).tooltip(() -> getSaveTooltip(false, false))
		  .active(() -> getHandler() != null && isValidName(selector.getText())), new TranslationTextComponent(
		  "simpleconfig.preset.save.label")
		).on(Modifier.SHIFT, ButtonAction.of(() -> save(selector.getText(), true))
		  .icon(Buttons.SAVE_REMOTE)
		  .active(() -> getHandler() != null && isValidName(selector.getText()) && getHandler().canSaveRemote())
		  .tooltip(() -> getSaveTooltip(false, true))
		).on(Modifier.ALT, ButtonAction.of(() -> delete(selector.getValue()))
		  .icon(Buttons.DELETE)
		  .active(() -> getHandler() != null && selector.getValue() != null && (!selector.getValue().remote || getHandler().canSaveRemote()))
		  .tooltip(() -> {
			  final Preset preset = selector.getValue();
			  return getSaveTooltip(true, preset != null && preset.remote);
		  }));
		selector = new ComboBoxWidget<>(
		  new TypeWrapper(this), () -> screen, x, y, w - 48, 24,
		  new TranslationTextComponent("simpleconfig.preset.picker.label"));
		selector.setHint(new TranslationTextComponent("simpleconfig.preset.picker.hint"));
		final SimpleComboBoxModel<Preset> provider = new SimpleComboBoxModel<>(
		  () -> {
			  final boolean server = screen.isSelectedCategoryServer();
			  return Stream.concat(
			    getKnownPresets(server, false).values().stream(),
			    getKnownPresets(server, true).values().stream()
			  ).collect(Collectors.toList());
		  });
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
		return Stream.concat(Stream.concat(
		  knownLocalClientPresets.values().stream(), knownRemoteClientPresets.values().stream()
		), Stream.concat(
		  knownLocalServerPresets.values().stream(), knownRemoteServerPresets.values().stream()
		)).collect(Collectors.toList());
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
		getHandler().getLocalSnapshotNames().stream()
		  .map(n -> HYPHEN.split(n, 2)).filter(a -> a.length == 2).map(
			 a -> "client".equals(a[0]) ? new Preset(a[1], false, false) :
			      "server".equals(a[0]) ? new Preset(a[1], true, false) : null
		  ).filter(Objects::nonNull).forEach(
			 p -> (p.server ? knownLocalServerPresets : knownLocalClientPresets).put(p.name, p));
		final Preset value = selector.getValue();
		String text = selector.getText();
		if (value != null && !isKnownPreset(value))
			selector.setText(selector.getText());
		if (value == null && isKnownPreset(text)) {
			selector.setText("");
			selector.setText(text);
		}
		if (lastFuture == null) {
			lastFuture = getHandler().getRemoteSnapshotNames().thenAccept(l -> {
				knownRemoteClientPresets.clear();
				knownRemoteServerPresets.clear();
				l.stream()
				  .map(n -> HYPHEN.split(n, 2)).filter(a -> a.length == 2).map(
					 a -> "client".equals(a[0]) ? new Preset(a[1], false, true) :
					      "server".equals(a[0]) ? new Preset(a[1], true, true) : null
				  ).filter(Objects::nonNull).forEach(
					 p -> (p.server ? knownRemoteServerPresets : knownRemoteClientPresets).put(p.name, p)
				  );
				lastFuture = null;
				final Preset vv = selector.getValue();
				if (vv != null && !isKnownPreset(vv))
					selector.setText(selector.getText());
			});
		}
	}
	
	public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		final long time = System.currentTimeMillis();
		if ((selector.isFocused() || lastUpdate == 0L) && time - lastUpdate > updateCooldown) {
			lastUpdate = time;
			update();
		}
		position();
		selector.active = getHandler() != null;
		selector.render(mStack, mouseX, mouseY, delta);
		loadButton.render(mStack, mouseX, mouseY, delta);
		saveButton.render(mStack, mouseX, mouseY, delta);
	}
	
	public Map<String, Preset> getKnownPresets(boolean server, boolean remote) {
		return remote?
		       server? knownRemoteServerPresets : knownRemoteClientPresets :
		       server? knownLocalServerPresets : knownLocalClientPresets;
	}
	
	public boolean isKnownPreset(Preset preset) {
		return getKnownPresets(preset.server, preset.remote).containsValue(preset);
	}
	
	public void load(Preset preset) {
		final Type type = screen.currentConfigType();
		final IConfigSnapshotHandler handler = getHandler();
		Set<String> s = screen.getSelectedEntries().stream()
		  .map(AbstractConfigEntry::getPath)
		  .collect(Collectors.toSet());
		final Set<String> selection = s.isEmpty()? null : s;
		if (preset.remote) {
			final CompletableFuture<Void> future = handler.getRemote(preset.name, type)
			  .thenAccept(config -> doLoad(config, preset, type, selection));
			screen.addDialog(ProgressDialog.create(
			  new TranslationTextComponent("simpleconfig.preset.dialog.loading.title"),
			  future, d -> d.setBody(splitTtc(
				 "simpleconfig.preset.dialog.loading.remote.body", presetName(preset.name)))));
		} else doLoad(handler.getLocal(preset.name, type), preset, type, selection);
	}
	
	protected void doLoad(
	  CommentedConfig snapshot, Preset preset, Type type, @Nullable Set<String> selection
	) {
		final IConfigSnapshotHandler handler = getHandler();
		String ll = preset.remote? "remote." : "local.";
		String tt = type == Type.SERVER? "server" : "client";
		int matches = getMatches(snapshot, selection);
		int total = getTotalSize(snapshot);
		screen.runAtomicTransparentAction(
		  () -> handler.restore(snapshot, type, selection));
		screen.addDialog(InfoDialog.create(new TranslationTextComponent(
		  "simpleconfig.preset.dialog.load.success.title"
		), Util.make(new ArrayList<>(), b -> {
			b.addAll(splitTtc(
			  "simpleconfig.preset.dialog.load.success." + ll + tt, presetName(preset.name)));
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
		final Map<String, Preset> presetMap = getKnownPresets(server, remote);
		final Map<String, Preset> otherMap = getKnownPresets(server, !remote);
		String ll = remote? "remote." : "local.";
		String tt = server? "server" : "client";
		if (!overwrite && !userSkipOverwriteDialog && presetMap.containsKey(name)
		    && presetMap.get(name).server == server) {
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
		} else if (!overwrite && otherMap.containsKey(name) && !presetMap.containsKey(name)) {
			screen.addDialog(ConfirmDialog.create(
			  new TranslationTextComponent("simpleconfig.preset.dialog.mistaken.title"), d -> {
				  d.withAction(b -> {
					  if (b) save(name, remote, true);
				  });
				  d.setBody(splitTtc("simpleconfig.preset.dialog.mistaken." + ll + "body", presetName(name)));
				  d.setConfirmText(new TranslationTextComponent(
				    "simpleconfig.preset.dialog.mistaken.option." + ll + "create"));
				  d.setConfirmButtonTint(remote ? 0xAA429090 : 0xAA429042);
				  d.addButton(1, TintedButton.of(
				    0, 20, remote ? 0xAA429042 : 0xAA429090, new TranslationTextComponent(
					   "simpleconfig.preset.dialog.mistaken.option." + ll + "overwrite"), p -> {
						 save(name, !remote, true);
						 d.cancel(false);
					 }));
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
		String ll = preset.remote? "remote." : "local.";
		String tt = preset.server? "server" : "client";
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
					 "simpleconfig.preset.dialog.delete.confirm." + ll + tt, presetName(preset.name)));
				  d.setConfirmText(new TranslationTextComponent(
					 "simpleconfig.preset.dialog.delete.confirm.delete"));
				  d.setConfirmButtonTint(0x80BD2424);
			  }));
		} else if (preset.remote) {
			final CompletableFuture<Void> future = handler.deleteRemote(preset.name, type);
			screen.addDialog(ProgressDialog.create(
			  new TranslationTextComponent("simpleconfig.preset.dialog.deleting.title"),
			  future, d -> {
				  d.setBody(splitTtc("simpleconfig.preset.dialog.deleting.remote.body", presetName(preset.name)));
				  d.setCancellableByUser(false);
				  d.setSuccessDialog(getDeleteSuccessDialog(preset));
			  }));
			future.thenAccept(v -> update());
		} else {
			handler.deleteLocal(preset.name, type);
			screen.addDialog(getDeleteSuccessDialog(preset));
			update();
		}
	}
	
	protected AbstractDialog getDeleteSuccessDialog(
	  Preset preset
	) {
		final String ll = preset.remote? "remote." : "local.";
		final String tt = preset.server? "server" : "client";
		return InfoDialog.create(
		  new TranslationTextComponent("simpleconfig.preset.dialog.delete.success.title"),
		  splitTtc("simpleconfig.preset.dialog.delete.success." + ll + tt, presetName(preset.name)));
	}
	
	protected IFormattableTextComponent selectedCount(int count) {
		return new StringTextComponent(String.valueOf(count)).mergeStyle(selectedCountStyle);
	}
	
	protected IFormattableTextComponent presetName(String name) {
		return new StringTextComponent(name).mergeStyle(nameStyle);
	}
	
	public boolean isKnownPreset(String name) {
		return getKnownPresets(true, true).containsKey(name)
		  || getKnownPresets(true, false).containsKey(name)
		  || getKnownPresets(false, true).containsKey(name)
		  || getKnownPresets(false, false).containsKey(name);
	}
	
	public boolean isKnownPreset(String name, boolean server) {
		return getKnownPresets(server, false).containsKey(name)
		       || getKnownPresets(server, true).containsKey(name);
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
		public final String name;
		public final boolean server;
		public final boolean remote;
		
		public Preset(String name, boolean server, boolean remote) {
			this.name = name;
			this.server = server;
			this.remote = remote;
		}
		
		@Override public String toString() {
			return name;
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Preset preset = (Preset) o;
			return server == preset.server && remote == preset.remote && name.equals(preset.name);
		}
		
		@Override public int hashCode() {
			return Objects.hash(name, server, remote);
		}
		
		public static class TypeWrapper implements ITypeWrapper<Preset> {
			public final PresetPickerWidget widget;
			
			public TypeWrapper(PresetPickerWidget widget) {
				this.widget = widget;
			}
			
			@Override public Pair<Optional<Preset>, Optional<ITextComponent>> parseElement(
			  @NotNull String text
			) {
				final boolean server = widget.screen.isSelectedCategoryServer();
				final Map<String, Preset> localPresets = widget.getKnownPresets(server, false);
				if (localPresets.containsKey(text)) {
					return Pair.of(Optional.of(localPresets.get(text)), Optional.empty());
				} else {
					final Map<String, Preset> remotePresets = widget.getKnownPresets(server, true);
					if (remotePresets.containsKey(text)) {
						return Pair.of(Optional.of(remotePresets.get(text)), Optional.empty());
					} else return Pair.of(Optional.empty(), Optional.of(new TranslationTextComponent(
					  "simpleconfig.config.error.unknown_preset")));
				}
			}
			
			@Override public String getName(@NotNull Preset element) {
				return element.name;
			}
			@Override public ITextComponent getDisplayName(@NotNull Preset element) {
				return new StringTextComponent(element.name).mergeStyle(
				  element.remote ? TextFormatting.AQUA : TextFormatting.GREEN);
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
					  widget.screen.isSelectedCategoryServer()?
					  Presets.SERVER_SAVE : Presets.CLIENT_SAVE);
				} else {
					return Optional.of(
					  element.server?
					  element.remote? Presets.SERVER_REMOTE : Presets.SERVER_LOCAL :
					  element.remote? Presets.CLIENT_REMOTE : Presets.CLIENT_LOCAL);
				}
			} // @formatter:on
		}
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return listeners;
	}
}
