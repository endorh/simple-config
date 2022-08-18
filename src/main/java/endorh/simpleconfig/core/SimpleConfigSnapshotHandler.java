package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import endorh.simpleconfig.api.ISimpleConfig;
import endorh.simpleconfig.api.ISimpleConfig.EditType;
import endorh.simpleconfig.api.ISimpleConfig.Type;
import endorh.simpleconfig.config.ServerConfig;
import endorh.simpleconfig.config.ServerConfig.permissions;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigSnapshotHandler;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IRemoteConfigProvider;
import endorh.simpleconfig.ui.gui.widget.PresetPickerWidget.Preset;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import javax.naming.NoPermissionException;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.core.SimpleConfigPaths.LOCAL_PRESETS_DIR;
import static java.util.Collections.emptyList;

class SimpleConfigSnapshotHandler implements IConfigSnapshotHandler, IRemoteConfigProvider {
	private final String modId;
	private final Map<Type, SimpleConfig> configMap;
	private IExternalChangeHandler externalChangeHandler;
	
	public SimpleConfigSnapshotHandler(
	  Map<Type, SimpleConfig> configMap
	) {
		this.configMap = configMap;
		modId = configMap.values().stream().findFirst()
		  .map(SimpleConfig::getModId).orElse("");
	}
	
	@Override public CommentedConfig preserve(Type type, @Nullable Set<String> selectedPaths) {
		final SimpleConfig c = configMap.get(type);
		if (c == null) throw new IllegalArgumentException("Unsupported config type: " + type);
		return c.takeSnapshot(true, false, selectedPaths);
	}
	
	@Override public void restore(
	  CommentedConfig config, Type type, @Nullable Set<String> selectedPaths
	) {
		SimpleConfig c = configMap.get(type);
		if (c == null) return;
		c.loadSnapshot(config, true, false, selectedPaths);
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public boolean canSaveRemote() {
		final Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null || mc.player == null) return false;
		return ServerConfig.permissions.permissionFor(mc.player, modId).getRight().canSave();
	}
	
	@Override public CompletableFuture<CommentedConfig> getPresetSnapshot(Preset preset) {
		final SimpleConfig c = configMap.get(preset.getType());
		if (c == null) return failedFuture(new IllegalArgumentException("Missing config type"));
		switch (preset.getLocation()) {
			case LOCAL:
				CompletableFuture<CommentedConfig> future = c.getLocalPreset(preset.getName());
				if (!future.isDone()) future.completeExceptionally(new IllegalStateException(
				  "Local presets must resolve immediately"));
				return future;
			case REMOTE: return c.getRemotePreset(preset.getName());
			case RESOURCE: return CompletableFuture.completedFuture(
			  SimpleConfigResourcePresetHandler.INSTANCE.getResourcePreset(modId, preset));
			default: return failedFuture(new IllegalArgumentException(
			  "Unknown preset location: " + preset.getLocation()));
		}
	}
	
	@Override public CommentedConfig getLocal(String name, Type type) {
		final SimpleConfig c = configMap.get(type);
		if (c == null)
			throw new IllegalArgumentException("Missing config type");
		final CompletableFuture<CommentedConfig> future = c.getLocalPreset(name);
		if (!future.isDone())
			throw new IllegalStateException("Uncompleted future");
		return future.getNow(null);
	}
	
	@Override public CompletableFuture<CommentedConfig> getRemote(String name, Type type) {
		final SimpleConfig c = configMap.get(type);
		return c != null? c.getRemotePreset(name) :
		       failedFuture(new IllegalArgumentException("Missing config type"));
	}
	
	@Override public CommentedConfig getResource(String name, Type type) {
		Preset p = Preset.remote(name, type);
		return SimpleConfigResourcePresetHandler.INSTANCE.getResourcePreset(modId, p);
	}
	
	@Override public Optional<Throwable> saveLocal(
	  String name, Type type, CommentedConfig config
	) {
		final SimpleConfig c = configMap.get(type);
		if (c != null) {
			return getException(c.saveLocalPreset(name, config));
		} else return Optional.empty();
	}
	
	@Override public CompletableFuture<Void> saveRemote(
	  String name, Type type, CommentedConfig config
	) {
		if (!canSaveRemote())
			return failedFuture(new NoPermissionException("Cannot save remote preset"));
		final SimpleConfig c = configMap.get(type);
		return c != null? c.saveRemotePreset(name, config) :
		       failedFuture(new IllegalArgumentException("Missing config type"));
	}
	
	@Override public Optional<Throwable> deleteLocal(
	  String name, Type type
	) {
		return saveLocal(name, type, null);
	}
	
	@Override public CompletableFuture<Void> deleteRemote(
	  String name, Type type
	) {
		return saveRemote(name, type, null);
	}
	
	@Override public List<Preset> getLocalPresets() {
		final SimpleConfig c = configMap.get(ISimpleConfig.Type.CLIENT);
		if (c == null) return emptyList();
		final File dir = LOCAL_PRESETS_DIR.toFile();
		Pattern pattern = Pattern.compile(
		  "^(?<file>" + c.getModId() + "-(?<type>\\w++)-(?<name>.+))\\.yaml$");
		final File[] files =
		  dir.listFiles((d, name) -> pattern.matcher(name).matches());
		return files == null? emptyList() : Arrays.stream(files).map(f -> {
			Matcher m = pattern.matcher(f.getName());
			if (!m.matches()) return null;
			return Preset.local(
			  m.group("name"), ISimpleConfig.Type.fromAlias(m.group("type")));
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}
	
	@Override public CompletableFuture<List<Preset>> getRemotePresets() {
		final SimpleConfig c = configMap.get(ISimpleConfig.Type.SERVER);
		if (c == null) return CompletableFuture.completedFuture(emptyList());
		return SimpleConfigNetworkHandler.requestPresetList(c.getModId());
	}
	
	@Override public List<Preset> getResourcePresets() {
		return SimpleConfigResourcePresetHandler.INSTANCE.getResourcePresets(modId);
	}
	
	@Override public IExternalChangeHandler getExternalChangeHandler() {
		return externalChangeHandler;
	}
	
	@Override public void setExternalChangeHandler(IExternalChangeHandler handler) {
		externalChangeHandler = handler;
	}
	
	public void notifyExternalChanges(SimpleConfig config) {
		if (externalChangeHandler != null) {
			if (configMap.containsValue(config)) {
				config.loadGUIExternalChanges();
				externalChangeHandler.handleExternalChange(config.getType().asEditType(false));
			}
		}
	}
	
	protected static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
		final CompletableFuture<T> future = new CompletableFuture<>();
		future.completeExceptionally(throwable);
		return future;
	}
	protected static Optional<Throwable> getException(CompletableFuture<?> future) {
		if (future.isCompletedExceptionally()) {
			try {
				future.getNow(null);
				return Optional.empty();
			} catch (CompletionException e) {
				return Optional.of(e.getCause());
			}
		}
		return Optional.empty();
	}
	
	@Override public CompletableFuture<CommentedConfig> getRemoteConfig(EditType type) {
		if (!type.isOnlyRemote()) return failedFuture(new IllegalArgumentException(
		  "Config type is not remote! Cannot get from remote: " + type.getAlias()));
		Type configType = type.getType();
		if (configType != ISimpleConfig.Type.COMMON) return failedFuture(new IllegalArgumentException(
		  "Unsupported remote config type: " + type.getAlias()));
		SimpleConfig config = configMap.get(configType);
		if (config == null) return failedFuture(new IllegalArgumentException(
		  "Missing config type: " + type.getAlias()));
		return SimpleConfigNetworkHandler.requestServerCommonConfig(modId);
	}
	
	@Override public boolean mayHaveRemoteConfig(EditType type) {
		if (type != ISimpleConfig.EditType.SERVER_COMMON) return false;
		SimpleConfig config = configMap.get(ISimpleConfig.Type.COMMON);
		return config != null
		       && !Minecraft.getInstance().isIntegratedServerRunning()
		       && permissions.permissionFor(modId).getLeft().canView();
	}
	
	@Override public void loadRemoteConfig(
	  EditType type, CommentedConfig snapshot, boolean asExternal
	) {
		if (!type.isOnlyRemote()) throw new IllegalArgumentException(
		  "Config type is not remote! Cannot get from remote: " + type.getAlias());
		Type configType = type.getType();
		if (configType != ISimpleConfig.Type.COMMON) throw new IllegalArgumentException(
		  "Unsupported remote config type: " + type.getAlias());
		SimpleConfig config = configMap.get(configType);
		if (asExternal) {
			config.loadGUIRemoteExternalChanges(snapshot);
		} else config.loadSnapshot(snapshot, true, true, null);
	}
	
	@Override public void saveRemoteConfig(EditType type, boolean requiresRestart) {
		if (!type.isOnlyRemote()) throw new IllegalArgumentException(
		  "Config type is not remote! Cannot save to remote: " + type.getAlias());
		Type configType = type.getType();
		if (configType != ISimpleConfig.Type.COMMON) throw new IllegalArgumentException(
		  "Unsupported remote config type: " + type.getAlias());
		SimpleConfig config = configMap.get(configType);
		CommentedConfig snapshot = config.takeSnapshot(true, true, null);
		SimpleConfigNetworkHandler.saveServerCommonConfig(modId, config, requiresRestart, snapshot);
	}
}
