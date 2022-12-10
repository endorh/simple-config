package endorh.simpleconfig.core;

import endorh.simpleconfig.api.SimpleConfig.InvalidConfigValueTypeException;
import endorh.simpleconfig.core.SimpleConfigClassParser.SimpleConfigClassParseException;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.lang.reflect.Field;
import java.util.function.Function;

import static endorh.simpleconfig.core.BackingField.BackingFieldBuilder.of;

public class BackingField<V, F> {
	private final Field field;
	private final Function<V, F> fieldMapper;
	private final Function<F, V> committer;
	
	public static <V, F> BackingFieldBuilder<V, F> field(Function<V, F> mapper, Class<F> type) {
		return of(mapper, EntryType.unchecked(type));
	}
	
	private BackingField(Field field, Function<V, F> fieldMapper, Function<F, V> committer) {
		this.field = field;
		this.fieldMapper = fieldMapper;
		this.committer = committer;
	}
	
	public F transformValue(V value) {
		return fieldMapper.apply(value);
	}
	
	public void setValue(V value) throws IllegalAccessException {
		ReflectionUtil.setBackingField(getField(), transformValue(value));
	}
	
	public V readValue() throws IllegalAccessException {
		if (committer == null)
			throw new IllegalStateException(
			  "Cannot commit field " + field.getDeclaringClass().getCanonicalName() + "." + field.getName());
		try {
			//noinspection unchecked
			return (V) field.get(null);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException("", e);
		}
	}
	
	public boolean canBeRead() {
		return committer != null;
	}
	
	public Field getField() {
		return field;
	}
	
	public static class BackingFieldBinding<V, F> {
		private final String name;
		private final String suffix;
		private final BackingFieldBuilder<V, F> builder;
		
		public static <V, F> BackingFieldBinding<V, F> sameName(BackingFieldBuilder<V, F> builder) {
			return new BackingFieldBinding<>(null, "", builder);
		}
		
		public static <V, F> BackingFieldBinding<V, F> withName(String name, BackingFieldBuilder<V, F> builder) {
			return new BackingFieldBinding<>(name, null, builder);
		}
		
		public static <V, F> BackingFieldBinding<V, F> withSuffix(String suffix, BackingFieldBuilder<V, F> builder) {
			return new BackingFieldBinding<>(null, suffix, builder);
		}
		
		private BackingFieldBinding(
		  String name, String suffix, BackingFieldBuilder<V, F> builder
		) {
			this.name = name;
			this.suffix = suffix;
			this.builder = builder;
		}
		
		@Internal public String buildName(String name) {
			return suffix != null? name + suffix : this.name != null? this.name : name;
		}
		
		@Internal public BackingField<V, F> build(Field field) {
			return builder.build(field);
		}
	}
	
	public static class BackingFieldBuilder<V, F> {
		private final Function<V, F> fieldMapper;
		private final Function<F, V> committer;
		private final EntryType<?> type;
		
		@Internal public static <V, F> BackingFieldBuilder<V, F> of(
		  Function<V, F> mapper, EntryType<?> type
		) {
			return new BackingFieldBuilder<>(mapper, null, type);
		}
		
		private BackingFieldBuilder(
		  Function<V, F> fieldMapper, Function<F, V> committer,
		  EntryType<?> type
		) {
			this.fieldMapper = fieldMapper;
			this.committer = committer;
			this.type = type;
		}
		
		public BackingFieldBuilder<V, F> withCommitter(Function<F, V> committer) {
			return new BackingFieldBuilder<>(fieldMapper, committer, type);
		}
		
		@Internal protected BackingField<V, F> build(Field field) {
			if (!matchesType(field)) {
				throw new SimpleConfigClassParseException(
				  field.getDeclaringClass(),
				  "Backing field " + field.getDeclaringClass().getCanonicalName() + "." +
				  field.getName() + " doesn't match its expected type: " + type.type().getSimpleName() +
				  "\nIf this is the default field for this entry, you may annotate it with @NotEntry " +
				  "to suppress this error");
			}
			return new BackingField<>(field, fieldMapper, committer);
		}
		
		@Internal protected boolean matchesType(Field field) {
			return type == null || type.matches(EntryType.fromField(field));
		}
	}
}
