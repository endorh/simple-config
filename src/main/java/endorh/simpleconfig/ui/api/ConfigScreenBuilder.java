package endorh.simpleconfig.ui.api;

import com.electronwill.nightconfig.core.CommentedConfig;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.hotkey.ConfigHotKey;
import endorh.simpleconfig.ui.impl.ConfigEntryBuilderImpl;
import endorh.simpleconfig.ui.impl.ConfigScreenBuilderImpl;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@OnlyIn(value = Dist.CLIENT)
public interface ConfigScreenBuilder {
	static ConfigScreenBuilder create() {
		return create("");
	}
	static ConfigScreenBuilder create(String modId) {
		return new ConfigScreenBuilderImpl(modId);
	}
	
	ConfigScreenBuilder setFallbackCategory(ConfigCategory fallbackCategory);
	
	Screen getParentScreen();
	ConfigScreenBuilder setParentScreen(Screen parent);
	
	ITextComponent getTitle();
	ConfigScreenBuilder setTitle(ITextComponent title);
	
	ConfigHotKey getEditedConfigHotKey();
	Consumer<Boolean> getHotKeySaver();
	ConfigScreenBuilder setEditedConfigHotKey(ConfigHotKey hotkey, Consumer<Boolean> hotKeySaver);
	
	ConfigCategory getSelectedCategory();
	ConfigScreenBuilder setSelectedCategory(ConfigCategory category);
	default ConfigScreenBuilder setSelectedCategory(String name, boolean isServer) {
		return setSelectedCategory(getOrCreateCategory(name, isServer));
	}
	
	IConfigScreenGUIState getPreviousGUIState();
	ConfigScreenBuilder setPreviousGUIState(IConfigScreenGUIState state);
	
	boolean isEditable();
	ConfigScreenBuilder setEditable(boolean editable);
	
	ConfigCategory getOrCreateCategory(String name, boolean isServer);
	ConfigScreenBuilder removeCategory(String name, boolean isServer);
	ConfigScreenBuilder removeCategoryIfExists(String name, boolean isServer);
	boolean hasCategory(String name, boolean isServer);
	
	ResourceLocation getDefaultBackgroundTexture();
	ConfigScreenBuilder setDefaultBackgroundTexture(ResourceLocation texture);
	
	Runnable getSavingRunnable();
	ConfigScreenBuilder setSavingRunnable(Runnable runnable);
	
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
	
	default ConfigEntryBuilder entryBuilder() {
		return ConfigEntryBuilderImpl.create();
	}
	
	AbstractConfigScreen build();
	
	ConfigScreenBuilder setSnapshotHandler(IConfigSnapshotHandler handler);
	
	interface IConfigSnapshotHandler {
		default CommentedConfig preserve(Type type) {
			return preserve(type, null);
		}
		CommentedConfig preserve(Type type, @Nullable Set<String> selectedPaths);
		void restore(CommentedConfig config, Type type, @Nullable Set<String> selectedPaths);
		boolean canSaveRemote();
		CommentedConfig getLocal(String name, Type type);
		CompletableFuture<CommentedConfig> getRemote(String name, Type type);
		Optional<Throwable> saveLocal(String name, Type type, CommentedConfig config);
		CompletableFuture<Void> saveRemote(String name, Type type, CommentedConfig config);
		Optional<Throwable> deleteLocal(String name, Type type);
		CompletableFuture<Void> deleteRemote(String name, Type type);
		List<String> getLocalSnapshotNames();
		CompletableFuture<List<String>> getRemoteSnapshotNames();
		void setExternalChangeHandler(IExternalChangeHandler handler);
		
		interface IExternalChangeHandler {
			void handleExternalChange(Type type);
		}
	}
	
	interface IConfigScreenGUIState {
		boolean isServerSelected();
		String getClientCategory();
		String getServerCategory();
		Map<String, Boolean> getClientExpandedStates();
		Map<String, Boolean> getServerExpandedStates();
		Map<String, String> getClientSelectedEntries();
		Map<String, String> getServerSelectedEntries();
	}
}