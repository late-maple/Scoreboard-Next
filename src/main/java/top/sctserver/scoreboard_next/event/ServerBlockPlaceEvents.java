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

package top.sctserver.scoreboard_next.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ServerBlockPlaceEvents
{
	@FunctionalInterface
	public interface After
	{
		void afterBlockPlace(ServerPlayer player, BlockPos pos, BlockState state, ItemStack stack, InteractionHand hand);
	}

	public static final Event<After> AFTER = EventFactory.createArrayBacked(After.class,
			listeners -> (player, pos, state, stack, hand) ->
			{
				for (After listener : listeners)
				{
					listener.afterBlockPlace(player, pos, state, stack, hand);
				}
			});

	private ServerBlockPlaceEvents()
	{
	}
}
