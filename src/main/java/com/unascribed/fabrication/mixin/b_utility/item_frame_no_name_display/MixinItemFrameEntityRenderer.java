package com.unascribed.fabrication.mixin.b_utility.item_frame_no_name_display;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.unascribed.fabrication.support.EligibleIf;
import com.unascribed.fabrication.support.Env;
import com.unascribed.fabrication.support.MixinConfigPlugin;

import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.entity.decoration.ItemFrameEntity;

@Mixin(value=ItemFrameEntityRenderer.class, priority=1004)
@EligibleIf(configAvailable="*.item_frame_no_name_display", envMatches = Env.CLIENT)
public class MixinItemFrameEntityRenderer {

	@Inject(at=@At("HEAD"), method= "hasLabel(Lnet/minecraft/entity/decoration/ItemFrameEntity;)Z", cancellable=true)
	public void hasLabel(ItemFrameEntity itemFrameEntity, CallbackInfoReturnable<Boolean> cir) {
		if (MixinConfigPlugin.isEnabled("*.item_frame_no_name_display")) {
			cir.setReturnValue(false);
		}
	}

}
