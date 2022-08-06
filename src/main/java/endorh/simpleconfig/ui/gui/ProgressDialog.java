package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ProgressDialog extends ConfirmDialog {
	private static final Logger LOGGER = LogManager.getLogger();
	protected List<ITextComponent> actualBody = Lists.newArrayList();
	protected boolean cancellableByUser = true;
	protected boolean closeOnExceptions = false;
	protected CompletableFuture<?> future;
	protected @Nullable List<ITextComponent> error = null;
	protected @Nullable Icon iconSave = null;
	protected Supplier<@Nullable AbstractDialog> successDialog = () -> null;
	
	public static ProgressDialog create(
	  ITextComponent title, CompletableFuture<?> future
	) {
		return create(title, future, null);
	}
	
	public static ProgressDialog create(
	  ITextComponent title, CompletableFuture<?> future,
	  @Nullable Consumer<ProgressDialog> builder
	) {
		ProgressDialog dialog = new ProgressDialog(title, future);
		if (builder != null) builder.accept(dialog);
		return dialog;
	}
	
	protected ProgressDialog(
	  ITextComponent title, CompletableFuture<?> future
	) {
		super(title);
		this.future = future;
		setCancelButtonTint(0x80BD2424);
		removeButton(confirmButton);
		setPersistent(true);
		updateActualBody();
		setIcon(SimpleConfigIcons.SPINNING_CUBE);
	}
	
	@Override public boolean escapeKeyPressed() {
		if (getListener() != cancelButton) {
			setListener(cancelButton);
		} else cancel(false);
		return true;
	}
	
	@Override public void cancel(boolean success) {
		super.cancel(success);
		if (future != null) {
			future.cancel(true);
			future = null;
		}
		if (success) {
			AbstractDialog next = successDialog.get();
			if (next != null)
				getScreen().addDialog(next);
		}
	}
	
	protected void update() {
		if (future != null) {
			if (future.isDone() && !future.isCancelled() && !future.isCompletedExceptionally()) {
				future = null;
				cancel(true);
			} else if (future.isCancelled()) {
				future = null;
				cancel(false);
			} else if (future.isCompletedExceptionally()) {
				try {
					future.join();
				} catch (CompletionException e) {
					final Throwable cause = e.getCause();
					LOGGER.error("Operation completed exceptionally, cause:", cause);
					setError(renderException(cause));
				}
				future = null;
				if (closeOnExceptions)
					cancel(false);
			}
		}
	}
	
	protected List<ITextComponent> renderException(Throwable e) {
		final StackTraceElement[] trace = e.getStackTrace();
		final List<ITextComponent> l = Lists.newArrayList(
		  new StringTextComponent(e.getClass().getSimpleName() + ": " + e.getLocalizedMessage())
			 .mergeStyle(TextFormatting.RED));
		if (trace.length > 0)
			l.add(new StringTextComponent("  at " + trace[0].getFileName() + ":" + trace[0].getLineNumber()).mergeStyle(TextFormatting.RED));
		if (e.getCause() != null) {
			final List<ITextComponent> c = renderException(e.getCause());
			c.set(0, new StringTextComponent("caused by: ").mergeStyle(TextFormatting.RED)
			  .append(c.get(0)));
			l.addAll(c);
		}
		return l;
	}
	
	@Override protected void layout() {
		super.layout();
	}
	
	@Override public void renderInner(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		update();
		
		int tx = x + 4;
		int ty = y + 4;
		for (List<IReorderingProcessor> line : lines) {
			for (IReorderingProcessor l : line) {
				font.func_238407_a_(mStack, l, tx, ty, bodyColor);
				ty += lineHeight;
			}
			ty += paragraphMarginDown;
		}
		cancelButton.active = cancellableByUser || future == null || future.isDone();
	}
	
	protected void updateActualBody() {
		actualBody.clear();
		actualBody.addAll(body);
		if (error != null) actualBody.addAll(error);
	}
	
	@Override public List<ITextComponent> getBody() {
		return actualBody;
	}
	
	@Override public void setBody(List<? extends ITextComponent> body) {
		this.body = new ArrayList<>(body);
		updateActualBody();
	}
	
	/**
	 * @see ProgressDialog#setCancellableByUser
	 */
	public boolean isCancellableByUser() {
		return cancellableByUser;
	}
	
	/**
	 * By default, progress dialogs are always cancellable by users.<br>
	 * Do not prevent user cancellation without a solid motive.
	 */
	public void setCancellableByUser(boolean cancellableByUser) {
		this.cancellableByUser = cancellableByUser;
	}
	
	/**
	 * @see ProgressDialog#setCloseOnExceptions
	 */
	public boolean isCloseOnExceptions() {
		return closeOnExceptions;
	}
	
	/**
	 * By default, progress dialogs remain open when their operation
	 * completes exceptionally, displaying the exception message
	 * to the user.<br>
	 * It's possible to close the dialog instead on errors automatically.
	 */
	public void setCloseOnExceptions(boolean closeOnExceptions) {
		this.closeOnExceptions = closeOnExceptions;
	}
	
	/**
	 * Set a dialog to be displayed on success.<br>
	 * On error, the progress dialog itself will display the error, unless
	 * isCloseOnExceptions is set to true.
	 */
	public void setSuccessDialog(@Nullable AbstractDialog dialog) {
		setSuccessDialog(() -> dialog);
	}
	
	/**
	 * Set a dialog to be displayed on success.<br>
	 * On error, the progress dialog itself will display the error, unless
	 * isCloseOnExceptions is set to true.
	 */
	public void setSuccessDialog(Supplier<@Nullable AbstractDialog> dialogSupplier) {
		this.successDialog = dialogSupplier;
	}
	
	@Nullable public List<ITextComponent> getError() {
		return error;
	}
	
	@Override public void setIcon(Icon icon) {
		super.setIcon(icon);
		this.iconSave = icon;
	}
	
	protected List<ITextComponent> decorateError(List<ITextComponent> error) {
		return error.stream()
		  .map(t -> t.deepCopy().mergeStyle(TextFormatting.RED))
		  .collect(Collectors.toList());
	}
	
	public void setError(@Nullable List<ITextComponent> error) {
		if (error != null) error = decorateError(error);
		this.error = error;
		final int height = getInnerHeight();
		updateActualBody();
		if (error != null)
			scroller.scrollTo(height, true);
		final Icon iconSave = this.iconSave;
		setIcon(error != null ? null : iconSave);
		this.iconSave = iconSave;
	}
	
	public void setError(Throwable error) {
		setError(renderException(error));
	}
}
