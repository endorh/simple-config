package endorh.simpleconfig.ui.gui;

import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.ui.gui.widget.TintedButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.ui.gui.ExternalChangesDialog.ExternalChangeResponse.*;

public class ExternalChangesDialog extends ConfirmDialog {
	TintedButton acceptAllButton;
	protected EditType type;
	protected @Nullable Consumer<ExternalChangeResponse> responseAction;
	
	public static ExternalChangesDialog create(
	  EditType type, Consumer<ExternalChangeResponse> handler
	) {
		return create(type, handler, null);
	}
	
	public static ExternalChangesDialog create(
	  EditType type, Consumer<ExternalChangeResponse> handler,
	  @Nullable Consumer<ExternalChangesDialog> builder
	) {
		ExternalChangesDialog dialog = new ExternalChangesDialog(type, handler);
		if (builder != null) builder.accept(dialog);
		return dialog;
	}
	
	protected ExternalChangesDialog(
	  EditType type, @NotNull Consumer<ExternalChangeResponse> action
	) {
		super(Component.translatable(type == EditType.SERVER? "simpleconfig.ui.remote_changes_detected.title" :
		                             "simpleconfig.ui.external_changes_detected.title"));
		this.type = type;
		responseAction = action;
		setPersistent(true);
		setBody(Stream.concat(
		  splitTtc(type == SimpleConfig.EditType.SERVER? "simpleconfig.ui.remote_changes_detected.body" :
		           "simpleconfig.ui.external_changes_detected.body"
		  ).stream(), Stream.of(
			 Component.empty(), Component.translatable("simpleconfig.ui.prompt_accept_changes"))).collect(Collectors.toList()));
		
		acceptAllButton = new TintedButton(
		  0, 0, 120, 20, Component.translatable("simpleconfig.ui.action.accept_all_changes"),
		  p -> {
			  if (responseAction != null) {
				  responseAction.accept(ACCEPT_ALL);
				  responseAction = null;
				  cancel(true);
			  }
		  });
		addButton(1, acceptAllButton);
		cancelButton.setTintColor(0x807F2424);
		acceptAllButton.setTintColor(0x8081542F);
		confirmButton.setTintColor(0x80683498);
		withAction(this::action);
		
		setCancelText(Component.translatable("simpleconfig.ui.action.reject_changes"));
		setConfirmText(Component.translatable("simpleconfig.ui.action.accept_non_conflicting_changes"));
	}
	
	public void action(boolean acceptUnedited) {
		if (responseAction != null)
			responseAction.accept(acceptUnedited? ACCEPT_NON_CONFLICTING : REJECT);
	}
	
	public enum ExternalChangeResponse {
		REJECT, ACCEPT_ALL, ACCEPT_NON_CONFLICTING
	}
}
