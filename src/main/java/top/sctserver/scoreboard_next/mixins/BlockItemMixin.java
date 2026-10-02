/*
 * This file is part of the Scoreboard Next project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  late-maple and contributors
 *
 * Scoreboard Next is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Scoreboard Next is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Scoreboard Next.  If not, see <https://www.gnu.org/licenses/>.
 */

package top.sctserver.scoreboard_next.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.sctserver.scoreboard_next.event.ServerBlockPlaceEvents;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin
{
	@Inject(method = "place", at = @At("RETURN"))
	private void scoreboardNext$afterPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir)
	{
		if (context.getLevel().isClientSide() || !cir.getReturnValue().consumesAction())
		{
			return;
		}
		if (context.getPlayer() instanceof ServerPlayer player)
		{
			BlockPos pos = context.getClickedPos();
			ServerBlockPlaceEvents.AFTER.invoker().afterBlockPlace(
					player,
					pos,
					context.getLevel().getBlockState(pos),
					context.getItemInHand(),
					context.getHand());
		}
	}
}
