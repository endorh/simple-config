package endorh.simpleconfig.ui.hotkey;

import com.electronwill.nightconfig.core.CommentedConfig.Entry;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Actions;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.entry.AbstractRangedEntry;
import endorh.simpleconfig.core.entry.BeanEntry;
import endorh.simpleconfig.core.entry.BeanEntry.ConfigBeanAccessException;
import endorh.simpleconfig.core.entry.BeanProxy;
import endorh.simpleconfig.core.entry.EntryPairEntry;
import endorh.simpleconfig.ui.hotkey.SimpleHotKeyActionType.ISimpleHotKeyAction;
import endorh.simpleconfig.ui.hotkey.SimpleHotKeyActionType.ISimpleHotKeyError;
import endorh.simpleconfig.ui.hotkey.StorageLessHotKeyActionType.IStorageLessHotKeyAction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static net.minecraft.util.math.MathHelper.clamp;

public class HotKeyActionTypes {
	public static final AssignHotKeyActionType ASSIGN = reg(new AssignHotKeyActionType());
	public static class AssignHotKeyActionType extends SimpleHotKeyActionType<Object, Object> {
		public AssignHotKeyActionType() {
			super("assign", Actions.ASSIGN, (entry, value, storage) -> storage);
		}
		
		@Override public @Nullable <T, C, E extends AbstractConfigEntry<T, C, Object>> SimpleHotKeyAction<Object, Object> create(
		  E entry, Object value
		) {
			return deserialize(entry, entry.forActualConfig(entry.forConfig(entry.fromGuiOrDefault(value))));
		}
		
		@Override public @Nullable <T, C, E extends AbstractConfigEntry<T, C, Object>> SimpleHotKeyAction<Object, Object> deserialize(
		  E entry, Object value
		) {
			T v = entry.fromConfig(entry.fromActualConfig(value));
			if (v == null) return null;
			Object g = entry.forGui(v);
			if (g == null) return null;
			return new SimpleHotKeyAction<>(this, entry, action, g);
		}
		
		@Override public <T, C, E extends AbstractConfigEntry<T, C, Object>> Object serialize(
		  E entry, SimpleHotKeyAction<Object, Object> action
		) {
			Object storage = action.getStorage();
			T v = entry.fromGui(storage);
			if (v == null) return null;
			return entry.forActualConfig(entry.forConfig(v));
		}
		
		@Override public ITextComponent formatAction(SimpleHotKeyAction<Object, Object> action) {
			return formatAction(action.getEntry(), action);
		}
		
		private <T> ITextComponent formatAction(
		  AbstractConfigEntry<T, ?, Object> entry, SimpleHotKeyAction<Object, Object> action
		) {
			String value = entry.forCommand(entry.fromGui(action.getStorage()));
			return new TranslationTextComponent(
			  "simpleconfig.hotkey.type.action." + getTranslationKey(), new StringTextComponent(
				 value != null? value : "null"
			).mergeStyle(TextFormatting.DARK_AQUA));
		}
		
		@SuppressWarnings("unchecked") public <V> SimpleHotKeyActionType<V, V> cast() {
			return (SimpleHotKeyActionType<V, V>) this;
		}
	}
	
	public static final SimpleHotKeyActionType<Integer, Number> INT_ADD = type(
	  "int:add", Actions.ADD, ranged((entry, a, b, min, max) -> clamp(
		 a + b.intValue(), min.intValue(), max.intValue())), notNull());
	public static final SimpleHotKeyActionType<Integer, Number> INT_ADD_CYCLE = type(
	  "int:cycle", Actions.ADD_CYCLE, ranged((entry, a, b, min, max) -> {
		  int mn = min.intValue();
		  int mx = max.intValue();
		  int r = mx - mn;
		  return mn + (a + b.intValue() - mn + r) % r;
	  }), notNull());
	public static final SimpleHotKeyActionType<Long, Number> LONG_ADD = type(
	  "long:add", Actions.ADD, ranged((entry, a, b, min, max) -> clamp(
		 a + b.longValue(), min.longValue(), max.longValue())), notNull());
	public static final SimpleHotKeyActionType<Long, Number> LONG_ADD_CYCLE = type(
	  "long:cycle", Actions.ADD_CYCLE, ranged((entry, a, b, min, max) -> {
		  long mn = min.longValue();
		  long mx = max.longValue();
		  long r = mx - mn;
		  return mn + (a + b.longValue() - mn + r) % r;
	  }), notNull());
	public static final SimpleHotKeyActionType<Float, Number> FLOAT_ADD = type(
	  "float:add", Actions.ADD, ranged((entry, a, b, min, max) -> clamp(
		 a + b.floatValue(), min.floatValue(), max.floatValue())), notNull());
	public static final SimpleHotKeyActionType<Float, Number> FLOAT_ADD_CYCLE = type(
	  "float:cycle", Actions.ADD_CYCLE, ranged((entry, a, b, min, max) -> {
		  float mn = min.floatValue();
		  float mx = max.floatValue();
		  float r = mx - mn;
		  return mn + (a + b.floatValue() - mn + r) % r;
	  }), notNull());
	public static final SimpleHotKeyActionType<Float, Number> FLOAT_MULTIPLY = type(
	  "float:mult", Actions.MULTIPLY, ranged((entry, a, b, min, max) -> clamp(
		 a * b.floatValue(), min.floatValue(), max.floatValue())), notNull());
	public static final SimpleHotKeyActionType<Float, Number> FLOAT_DIVIDE = type(
	  "float:div", Actions.DIVIDE, ranged(
		 (entry, a, b, min, max) -> b.floatValue() == 0F? null : clamp(
			a / b.floatValue(), min.floatValue(), max.floatValue())), divError());
	public static final SimpleHotKeyActionType<Double, Number> DOUBLE_ADD = type(
	  "double:add", Actions.ADD, ranged((entry, a, b, min, max) -> clamp(
		 a + b.doubleValue(), min.doubleValue(), max.doubleValue())), notNull());
	public static final SimpleHotKeyActionType<Double, Number> DOUBLE_ADD_CYCLE = type(
	  "double:cycle", Actions.ADD_CYCLE, ranged((entry, a, b, min, max) -> {
		  double mn = min.doubleValue();
		  double mx = max.doubleValue();
		  double r = mx - mn;
		  return mn + (a + b.doubleValue() - mn + r) % r;
	  }), notNull());
	public static final SimpleHotKeyActionType<Double, Number> DOUBLE_MULTIPLY = type(
	  "double:mult", Actions.MULTIPLY, ranged((entry, a, b, min, max) -> clamp(
		 a * b.doubleValue(), min.doubleValue(), max.doubleValue())), notNull());
	public static final SimpleHotKeyActionType<Double, Number> DOUBLE_DIVIDE = type(
	  "double:div", Actions.DIVIDE, ranged(
		 (entry, a, b, min, max) -> b.doubleValue() == 0D? null : clamp(
			a / b.doubleValue(), min.doubleValue(), max.doubleValue())), divError());
	public static final EnumAddSimpleHotKeyActionType<Enum<?>> ENUM_ADD = reg(new EnumAddSimpleHotKeyActionType<>());
	public static class EnumAddSimpleHotKeyActionType<E extends Enum<?>> extends SimpleHotKeyActionType<E, Integer> {
		public EnumAddSimpleHotKeyActionType() {
			super("enum.cycle", Actions.ADD_CYCLE, (entry, value, storage) -> {
				//noinspection unchecked
				E[] enums = ((Class<E>) value.getClass()).getEnumConstants();
				return enums[(enums.length + storage + value.ordinal()) % enums.length];
			});
		}
		
		@SuppressWarnings("unchecked") public <T extends Enum<?>> EnumAddSimpleHotKeyActionType<T> cast() {
			return (EnumAddSimpleHotKeyActionType<T>) this;
		}
		
		@Override
		public <T, C, EE extends AbstractConfigEntry<T, C, E>> Optional<ITextComponent> getActionError(
		  EE entry, Object value
		) {
			if (value == null) return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.missing_value"));
			return Optional.empty();
		}
	}
	
	public static final StorageLessHotKeyActionType<Boolean> BOOLEAN_TOGGLE = type(
	  "bool:toggle", Actions.CYCLE, (entry, b) -> !b);
	// public static final NestedHotKeyActionType<List<?>, Map<Integer, SimpleHotKeyAction<?, ?>>> LIST_NEST;
	
	// public static final BeanHotKeyActionType<Object> BEAN = type(new BeanHotKeyActionType<>());
	public static class BeanHotKeyActionType<B> extends NestedHotKeyActionType<B> {
		public BeanHotKeyActionType() {
			super("bean:sub", Actions.NONE, INestedHotKeyAction.of(
			  (entry, names) -> {
				  if (!(entry instanceof BeanEntry)) return null;
				  //noinspection unchecked
				  BeanEntry<B> e = (BeanEntry<B>) entry;
				  Map<String, AbstractConfigEntry<?, ?, ?>> map = new LinkedHashMap<>();
				  names.forEach(n -> {
					  AbstractConfigEntry<?, ?, ?> ee = e.getEntry(n);
					  if (ee != null) map.put(n, ee);
				  });
				  return map;
			  }, (entry, values) -> {
				 if (!(entry instanceof BeanEntry)) return null;
				  //noinspection unchecked
				  BeanEntry<B> e = (BeanEntry<B>) entry;
				  BeanProxy<B> proxy = e.getProxy();
				  B bean = proxy.createFrom(e.get());
				  for (Entry ee: values.entrySet()) {
					  try {
						  // bean = proxy.set(bean, ee.getKey(), ee.getValue());
					  } catch (ConfigBeanAccessException ignored) {}
				  }
				  return e.forActualConfig(e.forConfig(bean));
			  }
			));
		}
		
		@SuppressWarnings("unchecked") public <BB> BeanHotKeyActionType<BB> cast() {
			return (BeanHotKeyActionType<BB>) this;
		}
	}
	
	// public static final PairNestedHotKeyActionType<Object, Object> PAIR = type(new PairNestedHotKeyActionType<>());
	public static class PairNestedHotKeyActionType<L, R> extends NestedHotKeyActionType<Pair<L, R>> {
		public PairNestedHotKeyActionType() {
			super("pair:sub", Actions.NONE, INestedHotKeyAction.of(
			  (entry, names) -> {
				  if (!(entry instanceof EntryPairEntry)) return null;
				  //noinspection unchecked
				  EntryPairEntry<?, ?, ?, ?, L, R> e = (EntryPairEntry<?, ?, ?, ?, L, R>) entry;
				  Map<String, AbstractConfigEntry<?, ?, ?>> map = new LinkedHashMap<>();
				  if (names.contains("left")) map.put("left", e.leftEntry);
				  if (names.contains("right")) map.put("right", e.rightEntry);
				  return map;
			  }, (entry, values) -> {
				  if (!(entry instanceof EntryPairEntry)) return null;
				  //noinspection unchecked
				  EntryPairEntry<?, ?, ?, ?, L, R> e = (EntryPairEntry<?, ?, ?, ?, L, R>) entry;
				  Pair<L, R> value = e.apply(ee -> ee.forGui(ee.get()));
				  try {
					  return e.apply(ee -> ee.forActualConfig(ee.forConfig(ee.fromGuiOrDefault(Pair.of(
					    values.getOrElse("left", value.getLeft()),
					    values.getOrElse("right", value.getRight()))))));
				  } catch (ClassCastException ignored) {
					  return null;
				  }
			  }
			));
		}
		
		@SuppressWarnings("unchecked") public <L, R> PairNestedHotKeyActionType<L, R> cast() {
			return (PairNestedHotKeyActionType<L, R>) this;
		}
	}
	
	public static <T extends HotKeyActionType<?, ?>> T type(T type) {
		HotKeyActionTypeManager.INSTANCE.register(type);
		return type;
	}
	
	public static <V, S, T extends SimpleHotKeyActionType<V, S>> T reg(T type) {
		HotKeyActionTypeManager.INSTANCE.register(type);
		return type;
	}
	
	public static <V, S> SimpleHotKeyActionType<V, S> type(
	  String name, Icon button, ISimpleHotKeyAction<V, S> action
	) {
		return reg(new SimpleHotKeyActionType<>(name, button, action));
	}
	
	public static <V, S> SimpleHotKeyActionType<V, S> type(
	  String name, Icon button, ISimpleHotKeyAction<V, S> action,
	  ISimpleHotKeyError<V, S> error
	) {
		return reg(new SimpleHotKeyActionType<>(name, button, action, error));
	}
	
	public static <V> StorageLessHotKeyActionType<V> type(
	  String name, Icon button, IStorageLessHotKeyAction<V> action
	) {
		return reg(new StorageLessHotKeyActionType<>(name, button, action));
	}
	
	@FunctionalInterface public interface IRangedSimpleHotKeyAction<V, S> {
		V applyValue(
		  AbstractConfigEntry<?, ?, V> entry, V value, S storage, Number min, Number max);
	}
	
	public static <V, S> ISimpleHotKeyAction<V, S> ranged(
	  IRangedSimpleHotKeyAction<V, S> action
	) {
		return (entry, value, serialized) -> {
			if (entry instanceof AbstractRangedEntry) {
				//noinspection unchecked
				AbstractRangedEntry<?, ?, V> ranged = (AbstractRangedEntry<?, ?, V>) entry;
				Comparable<?> min = ranged.getMin();
				Comparable<?> max = ranged.getMax();
				if (min instanceof Number && max instanceof Number)
					return action.applyValue(
					  entry, value, serialized, (Number) min, (Number) max);
			}
			return null;
		};
	}
	
	protected static <V, S> ISimpleHotKeyError<V, S> notNull() {
		return (entry, v) -> v == null? Optional.of(new TranslationTextComponent(
		  "simpleconfig.config.error.missing_value")) : Optional.empty();
	}
	
	protected static <V, S> ISimpleHotKeyError<V, S> divError() {
		return (entry, v) -> {
			if (v == null) return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.missing_value"));
			if (v instanceof Number && ((Number) v).doubleValue() == 0.0)
				return Optional.of(new TranslationTextComponent(
				  "simpleconfig.config.error.zero_div"));
			return Optional.empty();
		};
	}
	
	@Internal protected static void registerTypes() {
		// Just loading the class is enough
	}
}
