package endorh.simpleconfig.api;

import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface ConfigEntryHolderBuilder<Builder extends ConfigEntryHolderBuilder<Builder>> {
	/**
	 * Flag this config section as requiring a restart to be effective
	 */
	@Contract("-> this") @NotNull Builder restart();
	
	/**
	 * Add an entry to the config
	 */
	@Contract("_, _ -> this") @NotNull Builder add(
	  String name, ConfigEntryBuilder<?, ?, ?, ?> entryBuilder
	);
	
	/**
	 * Create a text entry in the config<br>
	 *
	 * @param name Name of the entry
	 * @param args Args to be passed to the translation<br>
	 *   As a special case, {@code Supplier} args will be
	 *   called before being filled in
	 */
	@Contract("_, _ -> this") @NotNull Builder text(String name, Object... args);
	
	/**
	 * Create a text entry in the config
	 */
	@Contract("_ -> this") @NotNull Builder text(ITextComponent text);
	
	/**
	 * Create a text entry in the config
	 */
	@Contract("_ -> this") @NotNull Builder text(Supplier<ITextComponent> textSupplier);
	
	/**
	 * Add a config group
	 */
	@Contract("_ -> this") @NotNull Builder n(ConfigGroupBuilder group);
}
