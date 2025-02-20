package com.mrcrayfish.backpacked.mixin.common;

import com.mrcrayfish.backpacked.core.ModEnchantments;
import com.mrcrayfish.backpacked.core.ModItems;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Author: MrCrayfish
 */
@Mixin(ItemEntity.class)
public class ItemEntityMixin
{
    @Inject(method = "fireImmune", at = @At(value = "HEAD"), cancellable = true)
    public void fireImmuneHead(CallbackInfoReturnable<Boolean> cir)
    {
        ItemEntity entity = (ItemEntity) (Object) this;
        ItemStack stack = entity.getItem();
        if(stack.getItem() == ModItems.BACKPACK.get())
        {
            if(EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.IMBUED_HIDE.get(), stack) > 0)
            {
                cir.setReturnValue(true);
            }
        }
    }
}
