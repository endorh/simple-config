package endorh.simpleconfig.core;

import com.google.common.collect.Lists;
import net.minecraft.nbt.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.*;
import java.util.regex.Pattern;

public class NBTUtil {
	
	protected static final Pattern SPLIT = Pattern.compile("^(\\d++):([\\s\\S]++)$");
	
	@Internal public static INBT toNBT(Object o) {
		if (o instanceof INBT) {
			return (INBT) o;
		} else if (o instanceof Map) {
			final CompoundNBT c = new CompoundNBT();
			for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
				if (!(e.getKey() instanceof String))
					throw new IllegalArgumentException(
					  "Unsupported map key type: " + e.getKey().getClass().getName());
				c.put((String) e.getKey(), toNBT(e.getValue()));
			}
			return c;
		} else if (o instanceof Boolean) {
			return ByteNBT.valueOf((byte) ((Boolean) o ? 1 : 0));
		} else if (o instanceof String) {
			return StringNBT.valueOf((String) o);
		} else if (o instanceof Byte) {
			return ByteNBT.valueOf((Byte) o);
		} else if (o instanceof Integer) {
			return IntNBT.valueOf((Integer) o);
		} else if (o instanceof Double) {
			return DoubleNBT.valueOf((Double) o);
		} else if (o instanceof Long) {
			return LongNBT.valueOf((Long) o);
		} else if (o instanceof Short) {
			return ShortNBT.valueOf((Short) o);
		} else if (o instanceof Float) {
			return FloatNBT.valueOf((Float) o);
		} else if (o instanceof int[]) {
			return new IntArrayNBT((int[]) o);
		} else if (o instanceof byte[]) {
			return new ByteArrayNBT((byte[]) o);
		} else if (o instanceof long[]) {
			return new LongArrayNBT((long[]) o);
		} else if (o instanceof List) {
			final ListNBT list = new ListNBT();
			for (Object e : (List<?>) o)
				list.add(toNBT(e));
			return list;
		} else if (o instanceof Pair) {
			//noinspection unchecked
			final Pair<Object, Object> p = (Pair<Object, Object>) o;
			final CompoundNBT c = new CompoundNBT();
			c.put("k", toNBT(p.getKey()));
			c.put("v", toNBT(p.getValue()));
			return c;
		} else if (o instanceof Enum) {
			return StringNBT.valueOf(((Enum<?>) o).name());
		} else throw new IllegalArgumentException(
		  "Unsupported value for NBT serialization: " + o.getClass().getName());
	}
	
	@Internal public static Object fromNBT(INBT o) {
		return fromNBT(o, null);
	}
	
	// TODO: Add lenient parameter
	@Internal public static Object fromNBT(INBT o, ExpectedType expected) {
		if (o == null) return null;
		Class<?> cls = expected != null? expected.type : null;
		if (o instanceof CompoundNBT) {
			final CompoundNBT c = (CompoundNBT) o;
			if (cls == Pair.class) {
				if (!c.contains("k") || !c.contains("v"))
					throw new IllegalArgumentException("NBT pair does not contain the required keys 'k' and 'v'");
				final List<ExpectedType> next = expected.next;
				if (next != null && next.size() < 2) throw new IllegalArgumentException(
				  "Cannot deserialize an NBT pair without two expected types");
				return Pair.of(fromNBT(c.get("k"), next != null ? next.get(0) : null),
				               fromNBT(c.get("v"), next != null ? next.get(1) : null));
			}
			final Map<String, Object> m = new LinkedHashMap<>();
			INBTType<?> type = null;
			for (String k : c.keySet()) {
				final INBT e = c.get(k);
				if (e == null) throw new IllegalStateException("Null NBT entry in compound");
				if (type == null)
					type = e.getType();
				else if (type != e.getType())
					throw new IllegalArgumentException("Deserialized NBT compound has values of different types");
				m.put(k, fromNBT(e, expected != null && expected.next != null && !expected.next.isEmpty() ? expected.next.get(0) : null));
			}
			return m;
		} else if (o instanceof StringNBT) {
			if (expected != null && Enum.class.isAssignableFrom(expected.type)) {
				final String s = o.getString();
				final Object[] constants = expected.type.getEnumConstants();
				return Arrays.stream(constants).filter(
				  c -> ((Enum<?>) c).name().equalsIgnoreCase(s)
				).findFirst().orElse(constants[0]);
			}
			return o.getString();
		} else if (o instanceof ByteNBT) {
			return cast(((ByteNBT) o).getByte(), cls);
		} else if (o instanceof IntNBT) {
			return cast(((IntNBT) o).getInt(), cls);
		} else if (o instanceof DoubleNBT) {
			return cast(((DoubleNBT) o).getDouble(), cls);
		} else if (o instanceof LongNBT) {
			return cast(((LongNBT) o).getLong(), cls);
		} else if (o instanceof ShortNBT) {
			return cast(((ShortNBT) o).getShort(), cls);
		} else if (o instanceof FloatNBT) {
			return cast(((FloatNBT) o).getFloat(), cls);
		} else if (o instanceof ByteArrayNBT) {
			return ((ByteArrayNBT) o).getByteArray();
		} else if (o instanceof IntArrayNBT) {
			return ((IntArrayNBT) o).getIntArray();
		} else if (o instanceof LongArrayNBT) {
			return ((LongArrayNBT) o).getAsLongArray();
		} else if (o instanceof ListNBT) {
			final List<Object> list = new ArrayList<>();
			final ListNBT l = (ListNBT) o;
			INBTType<?> type = null;
			for (INBT e : l) {
				if (type == null)
					type = e.getType();
				else if (type != e.getType())
					throw new IllegalArgumentException("Deserialized NBT list has elements of different types");
				list.add(fromNBT(e, expected != null && expected.next != null && !expected.next.isEmpty()? expected.next.get(0) : null));
			}
			return list;
		} else throw new IllegalArgumentException(
		  "Unsupported NBT type for deserialization: " + o.getType().getName());
	}
	
	@Internal public static Object cast(Object value, Class<?> type) {
		if (type == null) return value;
		if (Number.class.isAssignableFrom(type)) {
			if (value instanceof Boolean)
				value = ((Boolean) value) ? 0 : 1;
			if (!(value instanceof Number))
				throw new IllegalArgumentException(
				  "Expected value of numeric type (" + type.getSimpleName() + ") but found " + value.getClass().getName());
			final Number n = (Number) value;
			if (type == Byte.class)
				return n.byteValue();
			if (type == Short.class)
				return n.shortValue();
			if (type == Integer.class)
				return n.intValue();
			if (type == Long.class)
				return n.longValue();
			if (type == Float.class)
				return n.floatValue();
			if (type == Double.class)
				return n.doubleValue();
			throw new IllegalStateException("Unknown numeric type: " + type.getName());
		}
		if (type == Boolean.class) {
			if (value instanceof Boolean) return value;
			if (value instanceof Number) return ((Number) value).doubleValue() != 0;
			throw new IllegalArgumentException("Expected boolean value but found " + value.getClass().getName());
		}
		return value;
	}
	
	public static class ExpectedType {
		public Class<?> type;
		public List<ExpectedType> next;
		
		public ExpectedType(Class<?> type, ExpectedType... next) {
			this.type = type;
			this.next = Lists.newArrayList(next);
		}
	}
}
