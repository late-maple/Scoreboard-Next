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

package top.sctserver.scoreboard_next.tracker;

import top.sctserver.scoreboard_next.ScoreboardNext;
import top.sctserver.scoreboard_next.event.ServerBlockPlaceEvents;
import top.sctserver.scoreboard_next.objective.Objectives;

public final class BlockPlaceTracker
{
	private BlockPlaceTracker()
	{
	}

	public static void register()
	{
		ServerBlockPlaceEvents.AFTER.register((player, pos, state, stack, hand) ->
		{
			if (!player.isCreative())
			{
				ScoreboardNext.scoreboardManager().addScore(Objectives.PLACED.key(), player, 1);
			}
		});
	}
}
