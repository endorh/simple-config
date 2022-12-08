package endorh.simpleconfig.api;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Optional;

class SimpleConfigProxy {
	private static Boolean runtimePresent = null;
	public static boolean isRuntimePresent() {
		if (runtimePresent != null) return runtimePresent;
		try {
			Class.forName("endorh.simpleconfig.core.SimpleConfigImpl");
			return runtimePresent = true;
		} catch (ClassNotFoundException e) {
			return runtimePresent = false;
		}
	}
	
	/**
	 * Get the display name of the mod, or just its mod id if not found
	 */
	@Internal public static String getModNameOrId(String modId) {
		final Optional<ModInfo> first = ModList.get().getMods().stream()
		  .filter(m -> modId.equals(m.getModId())).findFirst();
		if (first.isPresent())
			return first.get().getDisplayName();
		return modId;
	}
}
