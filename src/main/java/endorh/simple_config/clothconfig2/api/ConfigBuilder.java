package endorh.simple_config.clothconfig2.api;

import com.electronwill.nightconfig.core.CommentedConfig;
import endorh.simple_config.clothconfig2.impl.ConfigBuilderImpl;
import endorh.simple_config.clothconfig2.impl.ConfigEntryBuilderImpl;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@OnlyIn(value = Dist.CLIENT)
public interface ConfigBuilder {
	static ConfigBuilder create() {
		return new ConfigBuilderImpl();
	}
	
	ConfigBuilder setFallbackCategory(ConfigCategory fallbackCategory);
	
	Screen getParentScreen();
	
	ConfigBuilder setParentScreen(Screen parent);
	
	ITextComponent getTitle();
	
	ConfigBuilder setTitle(ITextComponent title);
	
	boolean isEditable();
	
	ConfigBuilder setEditable(boolean editable);
	
	ConfigCategory getOrCreateCategory(String name);
	
	ConfigBuilder removeCategory(String name);
	
	ConfigBuilder removeCategoryIfExists(String name);
	
	boolean hasCategory(String name);
	
	ResourceLocation getDefaultBackgroundTexture();
	
	ConfigBuilder setDefaultBackgroundTexture(ResourceLocation texture);
	
	Runnable getSavingRunnable();
	
	ConfigBuilder setSavingRunnable(Runnable runnable);
	
	Consumer<Screen> getAfterInitConsumer();
	
	ConfigBuilder setAfterInitConsumer(Consumer<Screen> afterInitConsumer);
	
	default ConfigBuilder alwaysShowTabs() {
		return this.setAlwaysShowTabs(true);
	}
	
	// @Deprecated void setGlobalized(boolean globalized);
	//
	// @Deprecated void setGlobalizedExpanded(boolean globalizedExpanded);
	
	boolean isAlwaysShowTabs();
	
	ConfigBuilder setAlwaysShowTabs(boolean alwaysShowTabs);
	
	ConfigBuilder setTransparentBackground(boolean transparentBackground);
	
	default ConfigBuilder transparentBackground() {
		return this.setTransparentBackground(true);
	}
	
	default ConfigBuilder solidBackground() {
		return this.setTransparentBackground(false);
	}
	
	default ConfigEntryBuilder entryBuilder() {
		return ConfigEntryBuilderImpl.create();
	}
	
	Screen build();
	
	boolean hasTransparentBackground();
	
	ConfigBuilder setSnapshotHandler(SnapshotHandler handler);
	
	interface SnapshotHandler {
		CommentedConfig preserve(Type type);
		void restore(CommentedConfig config, Type type);
		boolean canSaveRemote();
		CommentedConfig getLocal(String name, Type type);
		CompletableFuture<CommentedConfig> getRemote(String name, Type type);
		Optional<Throwable> saveLocal(String name, Type type, CommentedConfig config);
		CompletableFuture<Void> saveRemote(String name, Type type, CommentedConfig config);
		Optional<Throwable> deleteLocal(String name, Type type);
		CompletableFuture<Void> deleteRemote(String name, Type type);
		List<String> getLocalSnapshotNames();
		CompletableFuture<List<String>> getRemoteSnapshotNames();
	}
}

