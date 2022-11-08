package endorh.simpleconfig.api.ui.hotkey;

import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindActivation;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindContext;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.VanillaKeyBindContext;
import org.jetbrains.annotations.NotNull;

public class ExtendedKeyBindSettingsBuilder {
	private @NotNull KeyBindActivation activation = KeyBindActivation.PRESS;
	private @NotNull KeyBindContext context = VanillaKeyBindContext.GAME;
	private boolean allowExtraKeys = true;
	private boolean orderSensitive = false;
	private boolean exclusive = false;
	private boolean matchByChar = false;
	private boolean preventFurther = true;
	
	public ExtendedKeyBindSettingsBuilder() {}
	
	public ExtendedKeyBindSettingsBuilder(ExtendedKeyBindSettings settings) {
		activation = settings.activation();
		context = settings.context();
		allowExtraKeys = settings.allowExtraKeys();
		orderSensitive = settings.orderSensitive();
		exclusive = settings.exclusive();
		matchByChar = settings.matchByChar();
		preventFurther = settings.preventFurther();
	}
	
	public @NotNull KeyBindActivation getActivation() {
		return activation;
	}
	public ExtendedKeyBindSettingsBuilder withActivation(KeyBindActivation activation) {
		this.activation = activation;
		return this;
	}
	
	public @NotNull KeyBindContext getContext() {
		return context;
	}
	public ExtendedKeyBindSettingsBuilder withContext(KeyBindContext context) {
		this.context = context;
		return this;
	}
	
	public boolean isAllowExtraKeys() {
		return allowExtraKeys;
	}
	public ExtendedKeyBindSettingsBuilder setAllowExtraKeys(boolean allowExtraKeys) {
		this.allowExtraKeys = allowExtraKeys;
		return this;
	}
	
	public boolean isOrderSensitive() {
		return orderSensitive;
	}
	public ExtendedKeyBindSettingsBuilder setOrderSensitive(boolean orderSensitive) {
		this.orderSensitive = orderSensitive;
		return this;
	}
	
	public boolean isExclusive() {
		return exclusive;
	}
	public ExtendedKeyBindSettingsBuilder setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
		return this;
	}
	
	public boolean isMatchByChar() {
		return matchByChar;
	}
	public ExtendedKeyBindSettingsBuilder setMatchByChar(boolean matchByChar) {
		this.matchByChar = matchByChar;
		return this;
	}
	
	public boolean isPreventFurther() {
		return preventFurther;
	}
	public ExtendedKeyBindSettingsBuilder setPreventFurther(boolean preventFurther) {
		this.preventFurther = preventFurther;
		return this;
	}
	
	public ExtendedKeyBindSettings build() {
		return new ExtendedKeyBindSettingsImpl(
		  activation, context, allowExtraKeys, orderSensitive,
		  exclusive, matchByChar, preventFurther);
	}
}
