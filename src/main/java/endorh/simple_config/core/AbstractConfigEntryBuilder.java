package endorh.simple_config.core;

import endorh.simple_config.core.SimpleConfig.IGUIEntryBuilder;
import endorh.simple_config.core.SimpleConfig.InvalidConfigValueTypeException;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An {@link AbstractConfigEntry} builder
 * @param <V> The type of the value held by the entry
 * @param <Config> The type of the associated config entry
 * @param <Gui> The type of the associated GUI entry
 * @param <Entry> The type of the entry built
 * @param <Self> The actual subtype of this builder to be returned
 *               by builder-like methods
 */
public abstract class AbstractConfigEntryBuilder<V, Config, Gui,
  Entry extends AbstractConfigEntry<V, Config, Gui, Entry>,
  Self extends AbstractConfigEntryBuilder<V, Config, Gui, Entry, Self>>
  implements IGUIEntryBuilder, ITooltipEntry<V, Gui, Self>, IErrorEntry<V, Gui, Self> {
	protected V value;
	
	protected @Nullable Function<V, Optional<ITextComponent>> errorSupplier = null;
	protected @Nullable Function<Gui, Optional<ITextComponent>> guiErrorSupplier = null;
	protected @Nullable Function<V, Optional<ITextComponent[]>> tooltipSupplier = null;
	protected @Nullable Function<Gui, Optional<ITextComponent[]>> guiTooltipSupplier = null;
	protected List<Object> nameArgs = new ArrayList<>();
	protected List<Object> tooltipArgs = new ArrayList<>();
	protected boolean requireRestart = false;
	protected @Nullable Field backingField = null;
	protected Class<?> typeClass;
	
	public AbstractConfigEntryBuilder(V value, Class<?> typeClass) {
		this.value = value;
		this.typeClass = typeClass;
	}
	
	@SuppressWarnings("unchecked")
	protected Self self() {
		return (Self) this;
	}
	
	protected abstract Entry buildEntry(ISimpleConfigEntryHolder parent, String name);
	
	@Override
	public Self guiError(Function<Gui, Optional<ITextComponent>> guiErrorSupplier) {
		this.guiErrorSupplier = guiErrorSupplier;
		return self();
	}
	
	@Override
	public Self error(Function<V, Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
		return self();
	}
	
	@Override
	public Self guiTooltip(Function<Gui, Optional<ITextComponent[]>> tooltipSupplier) {
		this.guiTooltipSupplier = tooltipSupplier;
		return self();
	}
	
	@Override
	public Self tooltip(Function<V, Optional<ITextComponent[]>> tooltipSupplier) {
		this.tooltipSupplier = tooltipSupplier;
		return self();
	}
	
	/**
	 * Set the arguments that will be passed to the tooltip translation key<br>
	 * As a special case, {@code Function}s and {@code Supplier}s passed
	 * will be invoked before being passed as arguments, with the entry value
	 * as argument
	 */
	public Self tooltipArgs(Object... args) {
		for (Object o : args) { // Check function types to fail fast
			if (o instanceof Function) {
				try {
					//noinspection unchecked
					((Function<V, ?>) o).apply(value);
				} catch (ClassCastException e) {
					throw new InvalidConfigValueTypeException("",
					  e, "A translation argument provider expected an invalid value type");
				}
			}
		}
		tooltipArgs.clear();
		tooltipArgs.addAll(Arrays.asList(args));
		return self();
	}
	
	@SafeVarargs
	public final Self tooltipArgs(Function<Gui, Object>... args) {
		return tooltipArgs((Object[]) args);
	}
	
	@SafeVarargs
	public final Self tooltipArgs(Supplier<Object>... args) {
		return tooltipArgs((Object[]) args);
	}
	
	/**
	 * Set the arguments that will be passed to the name translation key<br>
	 * As a special case, {@code Supplier}s passed
	 * will be invoked before being passed as arguments
	 */
	public Self nameArgs(Object... args) {
		for (Object arg : args) {
			if (arg instanceof Function)
				throw new IllegalArgumentException(
				  "Name args cannot be functions that depend on the value, since names aren't refreshed");
		}
		nameArgs.clear();
		nameArgs.addAll(Arrays.asList(args));
		return self();
	}
	
	/**
	 * Flag this entry as requiring a restart to be effective
	 */
	public Self restart() {
		return restart(true);
	}
	
	/**
	 * Flag this entry as requiring a restart to be effective
	 */
	public Self restart(boolean requireRestart) {
		this.requireRestart = requireRestart;
		return self();
	}
	
	/**
	 * Subclasses should instead override
	 * {@link AbstractConfigEntryBuilder#buildEntry(ISimpleConfigEntryHolder, String)}
	 * in most cases<br>
	 * Overrides should call super
	 */
	protected Entry build(ISimpleConfigEntryHolder parent, String name) {
		final Entry e = buildEntry(parent, name);
		e.requireRestart = requireRestart;
		e.errorSupplier = errorSupplier;
		e.guiErrorSupplier = guiErrorSupplier;
		e.tooltipSupplier = tooltipSupplier;
		e.guiTooltipSupplier = guiTooltipSupplier;
		e.nameArgs = nameArgs;
		e.tooltipArgs = tooltipArgs;
		e.backingField = backingField;
		e.typeClass = typeClass;
		return e;
	}
}
