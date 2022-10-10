package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface BooleanEntryBuilder
  extends ConfigEntryBuilder<Boolean, Boolean, Boolean, BooleanEntryBuilder>, KeyEntryBuilder<Boolean> {
	/**
	 * Change the text displayed in the entry's button<br>
	 * You may also pass a translation key to which '.true' and '.false'
	 * would be appended if you use {@link #text(String)}
	 */
	@Contract(pure=true) @NotNull BooleanEntryBuilder text(BooleanDisplayer displayAdapter);
	
	/**
	 * Change the text displayed in the entry's button<br>
	 * This method takes a translation key to which '.true' and '.false'
	 * are appended to retrieve the actual text that will be used.
	 * You may also provide your own logic using {@link #text(BooleanDisplayer)}
	 */
	@Contract(pure=true) @NotNull BooleanEntryBuilder text(String translation);
	
	/**
	 * Determines how a boolean value is displayed, both in the interface and in the config file
	 */
	interface BooleanDisplayer {
		static BooleanDisplayer forTranslation(String translation) {
			return forTranslation(translation, "true", "false");
		}
		
		static BooleanDisplayer forTranslation(
		  String translation, String serialTrue, String serialFalse
		) {
			return new BooleanDisplayer() {
				@Override public Component getDisplayName(boolean value) {
					return Component.translatable(translation + (value? ".true" : ".false"))
					  .withStyle(value? ChatFormatting.GREEN : ChatFormatting.RED);
				}
				@Override public String getSerializableName(boolean value) {
					return value? serialTrue : serialFalse;
				}
			};
		}
		
		BooleanDisplayer TRUE_FALSE = forTranslation("simpleconfig.format.bool.true_false");
		BooleanDisplayer YES_NO = forTranslation(
		  "simpleconfig.format.bool.yes_no", "yes", "no");
		BooleanDisplayer ENABLED_DISABLED = forTranslation(
		  "simpleconfig.format.bool.enabled_disabled", "enabled", "disabled");
		BooleanDisplayer ON_OFF = forTranslation(
		  "simpleconfig.format.bool.on_off", "on", "off");
		
		/**
		 * Get display name.<br>
		 * Can be formatted.
		 */
		Component getDisplayName(boolean value);
		
		/**
		 * Get serializable name for the config file.<br>
		 * <b>Must be a static, non-translated value.</b><br>
		 * Values for {@code true} and {@code false} must differ in lowercase after trimming.
		 */
		default String getSerializableName(boolean value) {
			return String.valueOf(value);
		}
	}
}
