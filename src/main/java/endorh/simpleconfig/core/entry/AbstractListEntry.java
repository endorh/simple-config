package endorh.simpleconfig.core.entry;

import com.electronwill.nightconfig.core.ConfigSpec;
import endorh.simpleconfig.ui.impl.builders.ListFieldBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.IErrorEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.core.NBTUtil.ExpectedType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractListEntry
  <V, Config, Gui, Self extends AbstractListEntry<V, Config, Gui, Self>>
  extends AbstractConfigEntry<List<V>, List<Config>, List<Gui>, Self> {
	protected Class<?> innerType;
	protected Function<V, Optional<ITextComponent>> validator;
	protected boolean expand;
	
	public AbstractListEntry(
	  ISimpleConfigEntryHolder parent, String name, @Nullable List<V> value
	) {
		super(parent, name, value != null ? value : new ArrayList<>());
	}
	
	public static abstract class Builder<V, Config, Gui,
	  Entry extends AbstractListEntry<V, Config, Gui, Entry>,
	  Self extends Builder<V, Config, Gui, Entry, Self>>
	  extends AbstractConfigEntryBuilder<List<V>, List<Config>, List<Gui>, Entry, Self> {
		protected Function<V, Optional<ITextComponent>> validator = v -> Optional.empty();
		protected boolean expand = false;
		protected Class<?> innerType;
		
		public Builder(List<V> value, Class<?> innerType) {
			super(value, List.class);
			this.innerType = innerType;
		}
		
		/**
		 * Set a validator for the elements of this list entry<br>
		 * You may also use {@link IErrorEntry#error(Function)} to
		 * validate instead the whole list
		 *
		 * @param validator Element validator. Should return true for all valid elements
		 * @deprecated Use {@link AbstractListEntry.Builder#elemError(Function)}
		 * 		      to provide users with explicative error messages.
		 */
		@Deprecated public Self setValidator(Predicate<V> validator) {
			return copy().elemError(
			  c -> validator.test(c) ? Optional.empty() :
			       Optional.of(new TranslationTextComponent(
				      "simpleconfig.config.error.list_element_does_not_match_validator", c)));
		}
		
		public Self expand() {
			return expand(true);
		}
		
		public Self expand(boolean expand) {
			Self copy = copy();
			copy.expand = expand;
			return copy;
		}
		
		/**
		 * Set an error message supplier for the elements of this list entry<br>
		 * You may also use {@link IErrorEntry#error(Function)} to check
		 * instead the whole list
		 *
		 * @param validator Error message supplier. Empty return values indicate
		 *                  correct values
		 */
		public Self elemError(Function<V, Optional<ITextComponent>> validator) {
			Self copy = copy();
			copy.validator = validator;
			return copy;
		}
		
		@Override
		protected Entry build(ISimpleConfigEntryHolder parent, String name) {
			final Entry e = super.build(parent, name);
			e.validator = validator;
			e.expand = expand;
			e.innerType = innerType;
			return e;
		}
		
		@Override protected Self copy() {
			final Self copy = super.copy();
			copy.validator = validator;
			copy.expand = expand;
			copy.innerType = innerType;
			return copy;
		}
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
	public List<Gui> forGui(List<V> list) {
		return list.stream().map(this::elemForGui).collect(Collectors.toList());
	}
	
	@Override
	@Nullable public List<V> fromGui(@Nullable List<Gui> list) {
		if (list == null) return null;
		return list.stream().map(this::elemFromGui).collect(Collectors.toList());
	}
	
	@Override public List<Config> forConfig(List<V> list) {
		return list.stream().map(this::elemForConfig).collect(Collectors.toList());
	}
	
	@Override
	@Nullable public List<V> fromConfig(@Nullable List<Config> list) {
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
		  "simpleconfig.config.error.at_index",
		  new StringTextComponent(String.format("%d", index + 1)).mergeStyle(TextFormatting.AQUA)));
	}
	
	@Override public List<ITextComponent> getErrors(List<Gui> value) {
		return Stream.concat(
		  Stream.of(getError(value)).filter(Optional::isPresent).map(Optional::get),
		  value.stream().flatMap(p -> getElementErrors(p).stream())
		).collect(Collectors.toList());
	}
	
	@Override public Optional<ITextComponent> getError(List<Gui> value) {
		for (int i = 0; i < value.size(); i++) {
			Config elem = elemForConfig(elemFromGui(value.get(i)));
			final Optional<ITextComponent> error = validator.apply(elemFromConfig(elem));
			if (error.isPresent()) return Optional.of(addIndex(error.get(), i));
		}
		return super.getError(value);
	}
	
	public Optional<ITextComponent> getElementError(Gui value) {
		return validator.apply(elemFromGui(value));
	}
	
	public List<ITextComponent> getElementErrors(Gui value) {
		return Stream.of(getElementError(value))
		  .filter(Optional::isPresent).map(Optional::get)
		  .collect(Collectors.toList());
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).defineList(name, forConfig(value), this::validateElement));
	}
	
	@Override protected void buildSpec(ConfigSpec spec, String parentPath) {
		spec.defineList(parentPath + name, forConfig(value), this::validateElement);
	}
	
	@OnlyIn(Dist.CLIENT) protected <F extends ListFieldBuilder<Gui, ?, F>> F decorate(F builder) {
		builder = super.decorate(builder);
		builder.setCellErrorSupplier(this::getElementError)
		  .setExpanded(expand)
		  .setCaptionControlsEnabled(false)
		  .setInsertInFront(false);
		return builder;
	}
	
	@Override public ExpectedType getExpectedType() {
		return new ExpectedType(typeClass, new ExpectedType(innerType));
	}
}
