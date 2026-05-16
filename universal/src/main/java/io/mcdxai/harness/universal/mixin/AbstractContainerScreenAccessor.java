package io.mcdxai.harness.universal.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Compile-safe access to the protected layout origin fields on AbstractContainerScreen,
 * needed to translate slot-relative coordinates into screen-absolute pixels.
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int universalHarness$getLeftPos();

    @Accessor("topPos")
    int universalHarness$getTopPos();
}
