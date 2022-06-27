package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.impl.builders.BooleanToggleBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public class BooleanEntry
  extends AbstractConfigEntry<Boolean, Boolean, Boolean, BooleanEntry> implements
                                                                       IKeyEntry<Boolean, Boolean> {
	protected Function<Boolean, ITextComponent> yesNoSupplier;
	
	@Internal public BooleanEntry(ISimpleConfigEntryHolder parent, String name, boolean value) {
		super(parent, name, value);
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Boolean, Boolean, Boolean,
	  BooleanEntry, Builder> {
		protected Function<Boolean, ITextComponent> yesNoSupplier = null;
		
		public Builder(Boolean value) {
			super(value, Boolean.class);
		}
		
		/**
		 * Change the text displayed in the entry's button<br>
		 * You may also pass a translation key to which '.true' and '.false'
		 * would be appended if you use {@link Builder#text(String)}
		 */
		public Builder text(Function<Boolean, ITextComponent> displayAdapter) {
			Builder copy = copy();
			copy.yesNoSupplier = displayAdapter;
			return copy;
		}
		
		/**
		 * Change the text displayed in the entry's button<br>
		 * This method takes a translation key to which '.true' and '.false'
		 * are appended to retrieve the actual text that will be used.
		 * You may also provide your own logic using {@link Builder#text(Function)}
		 */
		public Builder text(String translation) {
			Builder copy = copy();
			final TranslationTextComponent yes =
			  new TranslationTextComponent(translation + ".true");
			final TranslationTextComponent no =
			  new TranslationTextComponent(translation + ".false");
			copy.yesNoSupplier = b -> b? yes : no;
			return copy;
		}
		
		@Override
		protected BooleanEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final BooleanEntry e = new BooleanEntry(parent, name, value);
			e.yesNoSupplier = yesNoSupplier;
			return e;
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value);
			copy.yesNoSupplier = yesNoSupplier;
			return copy;
		}
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, value, createConfigValidator()));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<Boolean>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final BooleanToggleBuilder valBuilder = builder
		  .startBooleanToggle(getDisplayName(), get())
		  .setYesNoTextSupplier(yesNoSupplier);
		return Optional.of(decorate(valBuilder).build());
	}
	
	protected static final Pattern truePattern = Pattern.compile(
	  "\\s*+(?:(?i)true|0*+1(?:\\.0*+)?|0x0*+1)\\s*+");
	protected static final Pattern falsePattern = Pattern.compile(
	  "\\s*+(?:(?i)false|0++(?:\\.0*+)?|0x0++)\\s*+");
	@Override public Optional<Boolean> deserializeStringKey(
	  @NotNull String key
	) {
		if (truePattern.matcher(key).matches())
			return Optional.of(true);
		else if (falsePattern.matcher(key).matches())
			return Optional.of(false);
		return Optional.empty();
	}
}
