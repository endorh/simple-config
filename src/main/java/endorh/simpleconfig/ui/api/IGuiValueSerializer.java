package endorh.simpleconfig.ui.api;

public interface IGuiValueSerializer<T> {
	T deserialize(Object value);
	Object serialize(T value);
}
