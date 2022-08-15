package endorh.simpleconfig.ui.api;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface ConfigScreen {
	void setSavingRunnable(@Nullable Runnable runnable);
	void setClosingRunnable(@Nullable Runnable runnable);
	void setAfterInitConsumer(@Nullable Consumer<Screen> consumer);
	
	ResourceLocation getBackgroundLocation();
	
	boolean isRequiresRestart();
	boolean isEdited();
	
	void saveAll(boolean var1);
}

