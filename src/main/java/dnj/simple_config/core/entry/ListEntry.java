package dnj.simple_config.core.entry;

import dnj.simple_config.core.AbstractConfigEntry;
import dnj.simple_config.core.IErrorEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class ListEntry
  <V, Config, Gui, Self extends ListEntry<V, Config, Gui, Self>>
  extends AbstractConfigEntry<List<V>, List<Config>, List<Gui>, Self> {
	public Function<V, Optional<ITextComponent>> validator = t -> Optional.empty();
	protected boolean expand;
	
	public ListEntry(@Nullable List<V> value) {
		super(value != null ? value : new ArrayList<>());
	}
	
	/**
	 * Set a validator for the elements of this list entry<br>
	 * You may also use {@link dnj.simple_config.core.entry.ListEntry#setValidator(Function)}
	 * to provide users with more explicative error messages<br>
	 * You may also use {@link IErrorEntry#error(Function)} to
	 * validate instead the whole list
	 *
	 * @param validator Element validator. Should return true for all valid elements
	 */
	public Self setValidator(Predicate<V> validator) {
		return this.setValidator(
		  (Function<V, Optional<ITextComponent>>)
			 c -> validator.test(c) ? Optional.empty() :
			      Optional.of(new TranslationTextComponent(
				     "simple-config.config.error.list_element_does_not_match_validator", c)));
	}
	
	/**
	 * Set an error message supplier for the elements of this list entry<br>
	 * You may also use {@link IErrorEntry#error(Function)} to check
	 * instead the whole list
	 *
	 * @param validator Error message supplier. Empty return values indicate
	 *                  correct values
	 */
	public Self setValidator(Function<V, Optional<ITextComponent>> validator) {
		this.validator = validator;
		return self();
	}
	
	/**
	 * Expand this list automatically in the GUI
	 */
	public Self expand() {
		return expand(true);
	}
	
	/**
	 * Expand this list automatically in the GUI
	 */
	public Self expand(boolean expand) {
		this.expand = expand;
		return self();
	}
	
	@Override
	protected List<Gui> forGui(List<V> list) {
		return list.stream().map(this::elemForGui).collect(Collectors.toList());
	}
	
	@Override
	protected @Nullable
	List<V> fromGui(@Nullable List<Gui> list) {
		if (list == null) return null;
		return list.stream().map(this::elemFromGui).collect(Collectors.toList());
	}
	
	@Override
	protected List<Config> forConfig(List<V> list) {
		return list.stream().map(this::elemForConfig).collect(Collectors.toList());
	}
	
	@Override
	protected @Nullable
	List<V> fromConfig(@Nullable List<Config> list) {
		if (list == null) return null;
		return list.stream().map(this::elemFromConfig).collect(Collectors.toList());
	}
	
	protected Gui elemForGui(V value) {
		//noinspection unchecked
		return (Gui) value;
	}
	
	protected V elemFromGui(Gui value) {
		//noinspection unchecked
		return (V) value;
	}
	
	protected Config elemForConfig(V value) {
		//noinspection unchecked
		return (Config) value;
	}
	
	protected V elemFromConfig(Config value) {
		//noinspection unchecked
		return (V) value;
	}
	
	protected boolean validateElement(Object o) {
		try {
			//noinspection unchecked
			return !validator.apply(elemFromConfig((Config) o)).isPresent();
		} catch (ClassCastException ignored) {
			return false;
		}
	}
	
	protected static ITextComponent addIndex(ITextComponent message, int index) {
		return message.copyRaw().appendString(", ").append(new TranslationTextComponent(
		  "simple-config.config.error.at_index",
		  new StringTextComponent(String.format("%d", index + 1)).mergeStyle(TextFormatting.AQUA)));
	}
	
	@Override
	protected Optional<ITextComponent> supplyError(List<Gui> value) {
		for (int i = 0; i < value.size(); i++) {
			Config elem = elemForConfig(elemFromGui(value.get(i)));
			final Optional<ITextComponent> error = validator.apply(elemFromConfig(elem));
			if (error.isPresent()) return Optional.of(addIndex(error.get(), i));
		}
		return super.supplyError(value);
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(
	  Builder builder
	) {
		return Optional.of(decorate(builder).defineList(name, value, this::validateElement));
	}
}
