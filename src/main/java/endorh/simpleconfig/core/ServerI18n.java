package endorh.simpleconfig.core;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeI18n;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.IllegalFormatException;
import java.util.Map;

@Internal public class ServerI18n {
	public static String format(String translateKey, Object... parameters) {
		if (FMLEnvironment.dist == Dist.CLIENT)
			return I18n.get(translateKey, parameters);
		String s = ForgeI18n.getPattern(translateKey);
		
		try {
			return String.format(s, parameters);
		} catch (IllegalFormatException illegalformatexception) {
			return "Format error: " + s;
		}
	}
	
	public static boolean hasKey(String key) {
		if (FMLEnvironment.dist == Dist.CLIENT)
			return I18n.exists(key);
		Map<String, String> i18n = ObfuscationReflectionHelper.getPrivateValue(
		  ForgeI18n.class, null, "i18n");
		if (i18n == null) return false;
		return i18n.containsKey(key);
	}
	
	public static String getCurrentLanguage() {
		String KEY = "language.code";
		String lang = KEY;
		if (FMLEnvironment.dist == Dist.CLIENT) {
			lang = I18n.get(KEY);
		} else if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
			lang = Language.getInstance().getOrDefault(KEY);
		}
		return KEY.equals(lang)? "<not loaded>" : lang;
	}
}
