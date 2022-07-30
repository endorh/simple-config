package endorh.simpleconfig.ui.hotkey;

import org.yaml.snakeyaml.nodes.Tag;

import java.util.HashMap;
import java.util.Map;

public class HotKeyActionTypeManager {
	public static final HotKeyActionTypeManager INSTANCE = new HotKeyActionTypeManager();
	private final Map<Tag, HotKeyActionType<?, ?>> types = new HashMap<>();
	
	static {
		HotKeyActionTypes.registerTypes();
	}
	
	public HotKeyActionType<?, ?> getType(Tag tag) {
		return types.get(tag);
	}
	
	public HotKeyActionType<?, ?> register(HotKeyActionType<?, ?> type) {
		types.put(type.getTag(), type);
		return type;
	}
}
