package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Pattern;

public class CompoundNBTEntry
  extends AbstractSerializableEntry<CompoundNBT, CompoundNBTEntry> {
	
	protected static final Pattern STYLE_COMPONENT = Pattern.compile("§[0-9a-f]");
	
	@Internal public CompoundNBTEntry(ISimpleConfigEntryHolder parent, String name, CompoundNBT value) {
		super(parent, name, value, CompoundNBT.class);
	}
	
	public static class Builder extends AbstractSerializableEntry.Builder<CompoundNBT, CompoundNBTEntry, Builder> {
		public Builder(CompoundNBT value) {
			super(value, CompoundNBT.class);
		}
		
		@Override
		protected CompoundNBTEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new CompoundNBTEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override protected String serialize(CompoundNBT value) {
		return STYLE_COMPONENT.matcher(value.getPrettyDisplay().getString()).replaceAll("");
	}
	
	@Override protected @Nullable CompoundNBT deserialize(String value) {
		try {
			return new JsonToNBT(new StringReader(value)).readStruct();
		} catch (CommandSyntaxException ignored) {
			return null;
		}
	}
	
	@Override
	protected Optional<ITextComponent> getErrorMessage(String value) {
		return Optional.of(new TranslationTextComponent(
		  "simpleconfig.config.error.invalid_nbt"));
	}
}

