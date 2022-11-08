package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface BooleanEntryBuilder
  extends ConfigEntryBuilder<@NotNull Boolean, Boolean, Boolean, BooleanEntryBuilder>,
          AtomicEntryBuilder {
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
		/**
		 * Creates a {@link BooleanDisplayer} that uses the given translation key
		 * as base for the true and false values.<br>
		 * The translation key is appended with '.true' and '.false' to get the actual
		 * translation keys used.<br>
		 * @param translation The translation key to use as base
		 */
		static BooleanDisplayer forTranslation(String translation) {
			return forTranslation(translation, "true", "false");
		}
		
		/**
		 * Creates a {@link BooleanDisplayer} that uses the given translation key
		 * as base for the true and false values.<br>
		 * The translation key is appended with '.true' and '.false' to get the actual
		 * translation keys used.<br>
		 * @param translation The translation key to use as base
		 * @param serialTrue The value to use in the config file when the boolean
		 * is true (default: "true")
		 * @param serialFalse The value to use in the config file when the boolean
		 * is false (default: "false")
		 */
		static BooleanDisplayer forTranslation(
		  String translation, String serialTrue, String serialFalse
		) {
			return new BooleanDisplayer() {
				@Override public Component getDisplayName(boolean value) {
					return new TranslatableComponent(translation + (value? ".true" : ".false"))
					  .withStyle(value? ChatFormatting.GREEN : ChatFormatting.RED);
				}
				@Override public String getSerializableName(boolean value) {
					return value? serialTrue : serialFalse;
				}
			};
		}
		
		/**
		 * Default boolean displayer<br>
		 * Uses "True"/"False" as display names and "true"/"false" as serial names.
		 */
		BooleanDisplayer TRUE_FALSE = forTranslation("simpleconfig.format.bool.true_false");
		/**
		 * Uses "Yes"/"No" as display names and "yes"/"no" as serial names.
		 */
		BooleanDisplayer YES_NO = forTranslation(
		  "simpleconfig.format.bool.yes_no", "yes", "no");
		/**
		 * Uses "Enabled"/"Disabled" as display names and "enabled"/"disabled" as serial names.
		 */
		BooleanDisplayer ENABLED_DISABLED = forTranslation(
		  "simpleconfig.format.bool.enabled_disabled", "enabled", "disabled");
		/**
		 * Uses "On"/"Off" as display names and "on"/"off" as serial names.
		 */
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
