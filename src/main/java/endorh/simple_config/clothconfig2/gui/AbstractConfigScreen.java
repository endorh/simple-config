package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.ClothConfigInitializer;
import endorh.simple_config.clothconfig2.api.*;
import endorh.simple_config.clothconfig2.gui.entries.KeyCodeEntry;
import endorh.simple_config.clothconfig2.math.Rectangle;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.ConfirmOpenLinkScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractConfigScreen extends Screen implements ConfigScreen {
   protected static final ResourceLocation CONFIG_TEX = new ResourceLocation("cloth-config2", "textures/gui/cloth_config.png");
   private boolean legacyEdited = false;
   private final ResourceLocation backgroundLocation;
   protected boolean legacyRequiresRestart = false;
   protected boolean confirmSave;
   protected final Screen parent;
   private boolean alwaysShowTabs = false;
   private boolean transparentBackground = false;
   @Nullable
   private ITextComponent defaultFallbackCategory = null;
   public int selectedCategoryIndex = 0;
   private boolean editable = true;
   private KeyCodeEntry focusedBinding;
   private ModifierKeyCode startedKeyCode = null;
   private final List<Tooltip> tooltips = Lists.newArrayList();
   @Nullable
   private Runnable savingRunnable = null;
   @Nullable
   protected Consumer<Screen> afterInitConsumer = null;

   protected AbstractConfigScreen(Screen parent, ITextComponent title, ResourceLocation backgroundLocation) {
      super(title);
      this.parent = parent;
      this.backgroundLocation = backgroundLocation;
   }

   public void setSavingRunnable(@Nullable Runnable savingRunnable) {
      this.savingRunnable = savingRunnable;
   }

   public void setAfterInitConsumer(@Nullable Consumer<Screen> afterInitConsumer) {
      this.afterInitConsumer = afterInitConsumer;
   }

   public ResourceLocation getBackgroundLocation() {
      return this.backgroundLocation;
   }

   public boolean isRequiresRestart() {
      if (this.legacyRequiresRestart) {
         return true;
      } else {
   
         for (List<AbstractConfigEntry<?>> entries : this.getCategorizedEntries().values()) {
            for (AbstractConfigEntry<?> entry : entries) {
               if (!entry.getConfigError().isPresent() && entry.isEdited() &&
                   entry.isRequiresRestart()) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public abstract Map<ITextComponent, List<AbstractConfigEntry<?>>> getCategorizedEntries();

   public boolean isEdited() {
      if (this.legacyEdited) {
         return true;
      } else {
         for (List<AbstractConfigEntry<?>> entries : this.getCategorizedEntries().values()) {
            for (AbstractConfigEntry<?> entry : entries) {
               if (entry.isEdited()) {
                  return true;
               }
            }
         }
         return false;
      }
   }

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public void setEdited(boolean edited) {
      this.legacyEdited = edited;
   }

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public void setEdited(boolean edited, boolean legacyRequiresRestart) {
      this.setEdited(edited);
      if (!this.legacyRequiresRestart && legacyRequiresRestart) {
         this.legacyRequiresRestart = legacyRequiresRestart;
      }

   }

   public boolean isShowingTabs() {
      return this.isAlwaysShowTabs() || this.getCategorizedEntries().size() > 1;
   }

   public boolean isAlwaysShowTabs() {
      return this.alwaysShowTabs;
   }

   @Internal
   public void setAlwaysShowTabs(boolean alwaysShowTabs) {
      this.alwaysShowTabs = alwaysShowTabs;
   }

   public boolean isTransparentBackground() {
      return this.transparentBackground && Minecraft.getInstance().world != null;
   }

   @Internal
   public void setTransparentBackground(boolean transparentBackground) {
      this.transparentBackground = transparentBackground;
   }

   public ITextComponent getFallbackCategory() {
      return this.defaultFallbackCategory != null ? this.defaultFallbackCategory : this.getCategorizedEntries().keySet().iterator().next();
   }

   @Internal
   public void setFallbackCategory(@Nullable ITextComponent defaultFallbackCategory) {
      this.defaultFallbackCategory = defaultFallbackCategory;
      List<ITextComponent> categories = Lists.newArrayList(this.getCategorizedEntries().keySet());

      for(int i = 0; i < categories.size(); ++i) {
         ITextComponent category = categories.get(i);
         if (category.equals(this.getFallbackCategory())) {
            this.selectedCategoryIndex = i;
            break;
         }
      }

   }

   public void saveAll(boolean openOtherScreens) {
   
      for (List<AbstractConfigEntry<?>> abstractConfigEntries : Lists.newArrayList(
        this.getCategorizedEntries().values())) {
         List<AbstractConfigEntry<?>> entries = (List) abstractConfigEntries;
      
         for (AbstractConfigEntry<?> abstractConfigEntry : entries) {
            AbstractConfigEntry<?> entry = (AbstractConfigEntry) abstractConfigEntry;
            entry.save();
         }
      }

      this.save();
      this.setEdited(false);
      if (openOtherScreens) {
         if (this.isRequiresRestart()) {
            this.minecraft.displayGuiScreen(new ClothRequiresRestartScreen(this.parent));
         } else {
            this.minecraft.displayGuiScreen(this.parent);
         }
      }

      this.legacyRequiresRestart = false;
   }

   public void save() {
      Optional.ofNullable(this.savingRunnable).ifPresent(Runnable::run);
   }

   public boolean isEditable() {
      return this.editable;
   }

   @Internal
   public void setEditable(boolean editable) {
      this.editable = editable;
   }

   @Internal
   public void setConfirmSave(boolean confirmSave) {
      this.confirmSave = confirmSave;
   }

   public KeyCodeEntry getFocusedBinding() {
      return this.focusedBinding;
   }

   @Internal
   public void setFocusedBinding(KeyCodeEntry focusedBinding) {
      this.focusedBinding = focusedBinding;
      if (focusedBinding != null) {
         this.startedKeyCode = this.focusedBinding.getValue();
         this.startedKeyCode.setKeyCodeAndModifier(InputMappings.INPUT_INVALID, Modifier.none());
      } else {
         this.startedKeyCode = null;
      }

   }

   public boolean mouseReleased(double double_1, double double_2, int int_1) {
      if (this.focusedBinding != null && this.startedKeyCode != null && !this.startedKeyCode.isUnknown() && this.focusedBinding.isAllowMouse()) {
         this.focusedBinding.setValue(this.startedKeyCode);
         this.setFocusedBinding(null);
         return true;
      } else {
         return super.mouseReleased(double_1, double_2, int_1);
      }
   }

   public boolean keyReleased(int int_1, int int_2, int int_3) {
      if (this.focusedBinding != null && this.startedKeyCode != null && this.focusedBinding.isAllowKey()) {
         this.focusedBinding.setValue(this.startedKeyCode);
         this.setFocusedBinding(null);
         return true;
      } else {
         return super.keyReleased(int_1, int_2, int_3);
      }
   }

   public boolean mouseClicked(double double_1, double double_2, int int_1) {
      if (this.focusedBinding != null && this.startedKeyCode != null && this.focusedBinding.isAllowMouse()) {
         if (this.startedKeyCode.isUnknown()) {
            this.startedKeyCode.setKeyCode(Type.MOUSE.getOrMakeInput(int_1));
         } else if (this.focusedBinding.isAllowModifiers() && this.startedKeyCode.getType() == Type.KEYSYM) {
            int code;
            Modifier modifier;
            label51: {
               code = this.startedKeyCode.getKeyCode().getKeyCode();
               if (Minecraft.IS_RUNNING_ON_MAC) {
                  if (code != 343 && code != 347) {
                     break label51;
                  }
               } else if (code != 341 && code != 345) {
                  break label51;
               }

               modifier = this.startedKeyCode.getModifier();
               this.startedKeyCode.setModifier(Modifier.of(modifier.hasAlt(), true, modifier.hasShift()));
               this.startedKeyCode.setKeyCode(Type.MOUSE.getOrMakeInput(int_1));
               return true;
            }

            if (code == 344 || code == 340) {
               modifier = this.startedKeyCode.getModifier();
               this.startedKeyCode.setModifier(Modifier.of(modifier.hasAlt(), modifier.hasControl(), true));
               this.startedKeyCode.setKeyCode(Type.MOUSE.getOrMakeInput(int_1));
               return true;
            }

            if (code == 342 || code == 346) {
               modifier = this.startedKeyCode.getModifier();
               this.startedKeyCode.setModifier(Modifier.of(true, modifier.hasControl(), modifier.hasShift()));
               this.startedKeyCode.setKeyCode(Type.MOUSE.getOrMakeInput(int_1));
               return true;
            }
         }

         return true;
      } else {
         return this.focusedBinding != null ? true : super.mouseClicked(double_1, double_2, int_1);
      }
   }

   public boolean keyPressed(int int_1, int int_2, int int_3) {
      if (this.focusedBinding != null && (this.focusedBinding.isAllowKey() || int_1 == 256)) {
         if (int_1 == 256) {
            this.focusedBinding.setValue(ModifierKeyCode.unknown());
            this.setFocusedBinding(null);
         } else if (this.startedKeyCode.isUnknown()) {
            this.startedKeyCode.setKeyCode(InputMappings.getInputByCode(int_1, int_2));
         } else if (this.focusedBinding.isAllowModifiers()) {
            if (this.startedKeyCode.getType() == Type.KEYSYM) {
               int code;
               Modifier modifier;
               label92: {
                  code = this.startedKeyCode.getKeyCode().getKeyCode();
                  if (Minecraft.IS_RUNNING_ON_MAC) {
                     if (code != 343 && code != 347) {
                        break label92;
                     }
                  } else if (code != 341 && code != 345) {
                     break label92;
                  }

                  modifier = this.startedKeyCode.getModifier();
                  this.startedKeyCode.setModifier(Modifier.of(modifier.hasAlt(), true, modifier.hasShift()));
                  this.startedKeyCode.setKeyCode(InputMappings.getInputByCode(int_1, int_2));
                  return true;
               }

               if (code == 344 || code == 340) {
                  modifier = this.startedKeyCode.getModifier();
                  this.startedKeyCode.setModifier(Modifier.of(modifier.hasAlt(), modifier.hasControl(), true));
                  this.startedKeyCode.setKeyCode(InputMappings.getInputByCode(int_1, int_2));
                  return true;
               }

               if (code == 342 || code == 346) {
                  modifier = this.startedKeyCode.getModifier();
                  this.startedKeyCode.setModifier(Modifier.of(true, modifier.hasControl(), modifier.hasShift()));
                  this.startedKeyCode.setKeyCode(InputMappings.getInputByCode(int_1, int_2));
                  return true;
               }
            }

            Modifier modifier;
            label75: {
               if (Minecraft.IS_RUNNING_ON_MAC) {
                  if (int_1 != 343 && int_1 != 347) {
                     break label75;
                  }
               } else if (int_1 != 341 && int_1 != 345) {
                  break label75;
               }

               modifier = this.startedKeyCode.getModifier();
               this.startedKeyCode.setModifier(Modifier.of(modifier.hasAlt(), true, modifier.hasShift()));
               return true;
            }

            if (int_1 == 344 || int_1 == 340) {
               modifier = this.startedKeyCode.getModifier();
               this.startedKeyCode.setModifier(Modifier.of(modifier.hasAlt(), modifier.hasControl(), true));
               return true;
            }

            if (int_1 == 342 || int_1 == 346) {
               modifier = this.startedKeyCode.getModifier();
               this.startedKeyCode.setModifier(Modifier.of(true, modifier.hasControl(), modifier.hasShift()));
               return true;
            }
         }

         return true;
      } else if (this.focusedBinding != null && int_1 != 256) {
         return true;
      } else {
         return int_1 == 256 && this.shouldCloseOnEsc() ? this.quit() : super.keyPressed(int_1, int_2, int_3);
      }
   }

   protected final boolean quit() {
      if (this.confirmSave && this.isEdited()) {
         this.minecraft.displayGuiScreen(new ConfirmScreen(new AbstractConfigScreen.QuitSaveConsumer(), new TranslationTextComponent("text.cloth-config.quit_config"), new TranslationTextComponent("text.cloth-config.quit_config_sure"), new TranslationTextComponent("text.cloth-config.quit_discard"), new TranslationTextComponent("gui.cancel")));
      } else {
         this.minecraft.displayGuiScreen(this.parent);
      }

      return true;
   }

   public void tick() {
      super.tick();
      boolean edited = this.isEdited();
      Optional.ofNullable(this.getQuitButton()).ifPresent((button) -> {
         button.setMessage(edited ? new TranslationTextComponent("text.cloth-config.cancel_discard") : new TranslationTextComponent("gui.cancel"));
      });
   
      for (IGuiEventListener child : this.getEventListeners()) {
         if (child instanceof ITickableTileEntity) {
            ((ITickableTileEntity) child).tick();
         }
      }

   }

   @Nullable
   protected Widget getQuitButton() {
      return null;
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      super.render(matrices, mouseX, mouseY, delta);
   
      for (Tooltip tooltip : this.tooltips) {
         this.renderTooltip(matrices, tooltip.getText(), tooltip.getX(), tooltip.getY());
      }

      this.tooltips.clear();
   }

   public void addTooltip(Tooltip tooltip) {
      this.tooltips.add(tooltip);
   }

   protected void overlayBackground(MatrixStack matrices, Rectangle rect, int red, int green, int blue, int startAlpha, int endAlpha) {
      this.overlayBackground(matrices.getLast().getMatrix(), rect, red, green, blue, startAlpha, endAlpha);
   }

   protected void overlayBackground(Matrix4f matrix, Rectangle rect, int red, int green, int blue, int startAlpha, int endAlpha) {
      if (!this.isTransparentBackground()) {
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder buffer = tessellator.getBuffer();
         this.minecraft.getTextureManager().bindTexture(this.getBackgroundLocation());
         RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
         buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         buffer.pos(matrix, (float)rect.getMinX(), (float)rect.getMaxY(), 0.0F).tex((float)rect.getMinX() / 32.0F, (float)rect.getMaxY() / 32.0F).color(red, green, blue, endAlpha).endVertex();
         buffer.pos(matrix, (float)rect.getMaxX(), (float)rect.getMaxY(), 0.0F).tex((float)rect.getMaxX() / 32.0F, (float)rect.getMaxY() / 32.0F).color(red, green, blue, endAlpha).endVertex();
         buffer.pos(matrix, (float)rect.getMaxX(), (float)rect.getMinY(), 0.0F).tex((float)rect.getMaxX() / 32.0F, (float)rect.getMinY() / 32.0F).color(red, green, blue, startAlpha).endVertex();
         buffer.pos(matrix, (float)rect.getMinX(), (float)rect.getMinY(), 0.0F).tex((float)rect.getMinX() / 32.0F, (float)rect.getMinY() / 32.0F).color(red, green, blue, startAlpha).endVertex();
         tessellator.draw();
      }
   }

   public void renderComponentHoverEffect(MatrixStack matrices, Style style, int x, int y) {
      super.renderComponentHoverEffect(matrices, style, x, y);
   }

   public boolean handleComponentClicked(@Nullable Style style) {
      if (style == null) {
         return false;
      } else {
         ClickEvent clickEvent = style.getClickEvent();
         if (clickEvent != null && clickEvent.getAction() == Action.OPEN_URL) {
            try {
               URI uri = new URI(clickEvent.getValue());
               String string = uri.getScheme();
               if (string == null) {
                  throw new URISyntaxException(clickEvent.getValue(), "Missing protocol");
               }

               if (!string.equalsIgnoreCase("http") && !string.equalsIgnoreCase("https")) {
                  throw new URISyntaxException(clickEvent.getValue(), "Unsupported protocol: " + string.toLowerCase(Locale.ROOT));
               }

               Minecraft.getInstance().displayGuiScreen(new ConfirmOpenLinkScreen((openInBrowser) -> {
                  if (openInBrowser) {
                     Util.getOSType().openURI(uri);
                  }

                  Minecraft.getInstance().displayGuiScreen(this);
               }, clickEvent.getValue(), true));
            } catch (URISyntaxException var5) {
               ClothConfigInitializer.LOGGER.error("Can't open url for {}", clickEvent, var5);
            }

            return true;
         } else {
            return super.handleComponentClicked(style);
         }
      }
   }

   private class QuitSaveConsumer implements BooleanConsumer {
      private QuitSaveConsumer() {
      }

      public void accept(boolean t) {
         if (!t) {
            AbstractConfigScreen.this.minecraft.displayGuiScreen(AbstractConfigScreen.this);
         } else {
            AbstractConfigScreen.this.minecraft.displayGuiScreen(AbstractConfigScreen.this.parent);
         }

      }

      // $FF: synthetic method
      QuitSaveConsumer(Object x1) {
         this();
      }
   }
}
