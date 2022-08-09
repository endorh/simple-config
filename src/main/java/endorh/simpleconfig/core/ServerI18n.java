package endorh.simpleconfig.core;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.LanguageMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ForgeI18n;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.IllegalFormatException;
import java.util.Map;

@Internal public class ServerI18n {
	public static String format(String translateKey, Object... parameters) {
		if (FMLEnvironment.dist == Dist.CLIENT)
			return I18n.format(translateKey, parameters);
		String s = ForgeI18n.getPattern(translateKey);
		
		try {
			return String.format(s, parameters);
		} catch (IllegalFormatException illegalformatexception) {
			return "Format error: " + s;
		}
	}
	
	public static boolean hasKey(String key) {
		if (FMLEnvironment.dist == Dist.CLIENT)
			return I18n.hasKey(key);
		Map<String, String> i18n = ObfuscationReflectionHelper.getPrivateValue(
		  ForgeI18n.class, null, "i18n");
		if (i18n == null) return false;
		return i18n.containsKey(key);
	}
	
	public static String getCurrentLanguage() {
		String KEY = "language.code";
		String lang = KEY;
		if (FMLEnvironment.dist == Dist.CLIENT) {
			lang = I18n.format(KEY);
		} else if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
			lang = LanguageMap.getInstance().func_230503_a_(KEY);
		}
		return KEY.equals(lang)? "<not loaded>" : lang;
	}
}