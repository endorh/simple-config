package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.INBTEntryBuilder;
import endorh.simpleconfig.api.ui.ITextFormatter;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class INBTEntry extends AbstractSerializableEntry<INBT, INBTEntry> {
	@Internal public INBTEntry(ConfigEntryHolder parent, String name, INBT value) {
		super(parent, name, value, INBT.class);
	}
	
	public static class Builder extends AbstractSerializableEntry.Builder<INBT, INBTEntry, INBTEntryBuilder, Builder>
	  implements INBTEntryBuilder {
		public Builder(INBT value) {
			super(value, INBT.class);
		}
		
		@Override
		protected INBTEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new INBTEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy(INBT value) {
			return new Builder(value);
		}
	}
	
	@Override
	protected String serialize(INBT value) {
		return CompoundNBTEntry.STYLE_COMPONENT.matcher(value.toFormattedComponent().getString()).replaceAll("");
	}
	
	@Override
	protected @Nullable INBT deserialize(String value) {
		try {
			StringReader reader = new StringReader(value);
			INBT tag = new JsonToNBT(reader).readValue();
			reader.skipWhitespace();
			return reader.canRead()? null : tag;
		} catch (CommandSyntaxException ignored) {
			return null;
		}
	}
	
	@Override
	protected Optional<ITextComponent> getErrorMessage(String value) {
		return Optional.of(new TranslationTextComponent(
		  "simpleconfig.config.error.invalid_nbt"));
	}
	
	@Override protected ITextFormatter getTextFormatter() {
		return ITextFormatter.forLanguageOrDefault("snbt", ITextFormatter.DEFAULT);
	}
}
