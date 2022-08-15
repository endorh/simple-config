package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.ui.hotkey.KeyBindMapping.KeyBindActivation;
import endorh.simpleconfig.ui.hotkey.KeyBindMapping.KeyBindContext;
import endorh.simpleconfig.ui.hotkey.KeyBindMapping.VanillaKeyBindContext;

public class ExtendedKeyBindSettingsBuilder {
	private KeyBindActivation activation = KeyBindActivation.PRESS;
	private KeyBindContext context = VanillaKeyBindContext.GAME;
	private boolean allowExtraKeys = true;
	private boolean orderSensitive = false;
	private boolean exclusive = false;
	private boolean matchByChar = false;
	private boolean preventFurther = true;
	
	public ExtendedKeyBindSettingsBuilder() {}
	
	public ExtendedKeyBindSettingsBuilder(ExtendedKeyBindSettings settings) {
		activation = settings.getActivation();
		context = settings.getContext();
		allowExtraKeys = settings.isAllowExtraKeys();
		orderSensitive = settings.isOrderSensitive();
		exclusive = settings.isExclusive();
		matchByChar = settings.isMatchByChar();
		preventFurther = settings.isPreventFurther();
	}
	
	public KeyBindActivation getActivation() {
		return activation;
	}
	
	public ExtendedKeyBindSettingsBuilder withActivation(KeyBindActivation activation) {
		this.activation = activation;
		return this;
	}
	
	public KeyBindContext getContext() {
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
