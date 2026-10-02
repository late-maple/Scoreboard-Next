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

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import top.sctserver.scoreboard_next.ScoreboardNext;
import top.sctserver.scoreboard_next.objective.Objectives;

public final class KillTracker
{
	private KillTracker()
	{
	}

	public static void register()
	{
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed, damageSource) ->
		{
			if (killed instanceof ArmorStand)
			{
				return;
			}
			if (killer instanceof ServerPlayer player && killer != killed)
			{
				ScoreboardNext.scoreboardManager().addScore(Objectives.KILLS.key(), player, 1);
			}
		});
	}
}
