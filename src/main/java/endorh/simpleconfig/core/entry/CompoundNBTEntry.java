package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.CompoundNBTEntryBuilder;
import endorh.simpleconfig.ui.api.ITextFormatter;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Pattern;

public class CompoundNBTEntry extends AbstractSerializableEntry<CompoundNBT, CompoundNBTEntry> {
	protected static final Pattern STYLE_COMPONENT = Pattern.compile("§[\\da-f]");
	
	@Internal public CompoundNBTEntry(ConfigEntryHolder parent, String name, CompoundNBT value) {
		super(parent, name, value, CompoundNBT.class);
	}
	
	public static class Builder extends AbstractSerializableEntry.Builder<CompoundNBT,
	  CompoundNBTEntry, CompoundNBTEntryBuilder, Builder>
	  implements CompoundNBTEntryBuilder {
		public Builder(CompoundNBT value) {
			super(value, CompoundNBT.class);
		}
		
		@Override
		protected CompoundNBTEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new CompoundNBTEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override protected String serialize(CompoundNBT value) {
		return STYLE_COMPONENT.matcher(value.toFormattedComponent().getString()).replaceAll("");
	}
	
	@Override protected @Nullable CompoundNBT deserialize(String value) {
		try {
			StringReader reader = new StringReader(value);
			CompoundNBT compound = new JsonToNBT(reader).readStruct();
			reader.skipWhitespace();
			return reader.canRead()? null : compound;
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

