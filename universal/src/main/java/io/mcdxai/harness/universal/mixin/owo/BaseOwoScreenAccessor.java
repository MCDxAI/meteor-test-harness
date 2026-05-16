package io.mcdxai.harness.universal.mixin.owo;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Compile-safe access to {@code BaseOwoScreen.uiAdapter} (a {@code protected} field).
 *
 * Marked {@link Pseudo} so the Mixin processor tolerates owo-lib being absent at runtime —
 * if the target class isn't loaded, the mixin is silently skipped instead of erroring.
 * The {@link OwoScreenEngine} only casts to this interface inside an
 * {@code instanceof BaseOwoScreen<?>} guard, so the cast can never fire when owo isn't loaded.
 */
@Pseudo
@Mixin(BaseOwoScreen.class)
public interface BaseOwoScreenAccessor {
    @Accessor("uiAdapter")
    OwoUIAdapter<?> universalHarness$getUiAdapter();
}
