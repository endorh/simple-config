package endorh.simpleconfig.ui.api;

import com.electronwill.nightconfig.core.CommentedConfig;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.widget.PresetPickerWidget.Preset;
import endorh.simpleconfig.ui.hotkey.ConfigHotKey;
import endorh.simpleconfig.ui.impl.ConfigEntryBuilderImpl;
import endorh.simpleconfig.ui.impl.ConfigScreenBuilderImpl;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public interface ConfigScreenBuilder {
	static ConfigScreenBuilder create() {
		return create("");
	}
	static ConfigScreenBuilder create(String modId) {
		return new ConfigScreenBuilderImpl(modId);
	}
	
	ConfigScreenBuilder setFallbackCategory(ConfigCategoryBuilder fallbackCategory);
	
	Screen getParentScreen();
	ConfigScreenBuilder setParentScreen(Screen parent);
	
	Component getTitle();
	ConfigScreenBuilder setTitle(Component title);
	
	ConfigHotKey getEditedConfigHotKey();
	Consumer<Boolean> getHotKeySaver();
	ConfigScreenBuilder setEditedConfigHotKey(ConfigHotKey hotkey, Consumer<Boolean> hotKeySaver);
	
	ConfigCategoryBuilder getSelectedCategory();
	ConfigScreenBuilder setSelectedCategory(ConfigCategoryBuilder category);
	default ConfigScreenBuilder setSelectedCategory(String name, EditType type) {
		return setSelectedCategory(getOrCreateCategory(name, type));
	}
	
	@Nullable IConfigScreenGUIState getPreviousGUIState();
	ConfigScreenBuilder setPreviousGUIState(@Nullable IConfigScreenGUIState state);
	
	boolean isEditable();
	ConfigScreenBuilder setEditable(boolean editable);
	
	@Internal List<ConfigCategoryBuilder> getCategories(EditType type);
	ConfigCategoryBuilder getOrCreateCategory(String name, EditType type);
	ConfigScreenBuilder removeCategory(String name, EditType type);
	ConfigScreenBuilder removeCategoryIfExists(String name, EditType type);
	boolean hasCategory(String name, EditType type);
	
	ResourceLocation getDefaultBackgroundTexture();
	ConfigScreenBuilder setDefaultBackgroundTexture(ResourceLocation texture);
	
	Runnable getSavingRunnable();
	ConfigScreenBuilder setSavingRunnable(Runnable runnable);
	
	Runnable getClosingRunnable();
	ConfigScreenBuilder setClosingRunnable(Runnable runnable);
	
	Consumer<Screen> getAfterInitConsumer();
	ConfigScreenBuilder setAfterInitConsumer(Consumer<Screen> afterInitConsumer);
	
	default ConfigScreenBuilder alwaysShowTabs() {
		return this.setAlwaysShowTabs(true);
	}
	
	boolean isAlwaysShowTabs();
	ConfigScreenBuilder setAlwaysShowTabs(boolean alwaysShowTabs);
	
	ConfigScreenBuilder setTransparentBackground(boolean transparentBackground);
	default ConfigScreenBuilder transparentBackground() {
		return this.setTransparentBackground(true);
	}
	default ConfigScreenBuilder solidBackground() {
		return this.setTransparentBackground(false);
	}
	boolean hasTransparentBackground();
	
	default ConfigFieldBuilder entryBuilder() {
		return ConfigEntryBuilderImpl.create();
	}
	
	AbstractConfigScreen build();
	
	ConfigScreenBuilder setSnapshotHandler(IConfigSnapshotHandler handler);
	ConfigScreenBuilder setRemoteCommonConfigProvider(IRemoteConfigProvider provider);
	
	interface IRemoteConfigProvider {
		CompletableFuture<CommentedConfig> getRemoteConfig(EditType type);
		boolean mayHaveRemoteConfig(EditType type);
		void loadRemoteConfig(EditType type, CommentedConfig config, boolean asExternal);
		void saveRemoteConfig(EditType type, boolean requiresRestart);
	}
	
	interface IConfigSnapshotHandler {
		default CommentedConfig preserve(Type type) {
			return preserve(type, null);
		}
		CommentedConfig preserve(Type type, @Nullable Set<String> selectedPaths);
		void restore(CommentedConfig config, Type type, @Nullable Set<String> selectedPaths);
		boolean canSaveRemote();
		CompletableFuture<CommentedConfig> getPresetSnapshot(Preset preset);
		CommentedConfig getLocal(String name, Type type);
		CompletableFuture<CommentedConfig> getRemote(String name, Type type);
		CommentedConfig getResource(String name, Type type);
		Optional<Throwable> saveLocal(String name, Type type, CommentedConfig config);
		CompletableFuture<Void> saveRemote(String name, Type type, CommentedConfig config);
		Optional<Throwable> deleteLocal(String name, Type type);
		CompletableFuture<Void> deleteRemote(String name, Type type);
		List<Preset> getLocalPresets();
		CompletableFuture<List<Preset>> getRemotePresets();
		List<Preset> getResourcePresets();
		IExternalChangeHandler getExternalChangeHandler();
		void setExternalChangeHandler(IExternalChangeHandler handler);
		
		interface IExternalChangeHandler {
			void handleExternalChange(EditType type);
			void handleRemoteConfigExternalChange(EditType type, CommentedConfig remoteConfig);
		}
	}
	
	interface IConfigScreenGUIState {
		EditType getEditedType();
		Map<EditType, String> getSelectedCategories();
		Map<EditType, Map<String, IConfigCategoryGUIState>> getCategoryStates();
		
		interface IConfigCategoryGUIState {
			Map<String, Boolean> getExpandStates();
			String getSelectedEntry();
			int getScrollOffset();
		}
	}
}