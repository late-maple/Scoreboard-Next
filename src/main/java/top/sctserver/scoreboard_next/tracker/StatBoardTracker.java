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

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import top.sctserver.scoreboard_next.ScoreboardNext;
import top.sctserver.scoreboard_next.objective.ObjectiveDefinition;
import top.sctserver.scoreboard_next.objective.ScoreboardManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public abstract class StatBoardTracker
{
	private final ObjectiveDefinition board;
	private final Stat<?> stat;
	private final int divisor;
	private final int intervalTicks;
	private final Map<UUID, Long> baselines = new HashMap<>();
	private int countdown;

	protected StatBoardTracker(ObjectiveDefinition board, Stat<?> stat, int divisor, int intervalTicks)
	{
		this.board = Objects.requireNonNull(board, "board must not be null");
		this.stat = Objects.requireNonNull(stat, "stat must not be null");
		if (divisor <= 0)
		{
			throw new IllegalArgumentException("divisor must be positive, got " + divisor);
		}
		if (intervalTicks <= 0)
		{
			throw new IllegalArgumentException("intervalTicks must be positive, got " + intervalTicks);
		}
		this.divisor = divisor;
		this.intervalTicks = intervalTicks;
	}

	public final void attach()
	{
		ServerTickEvents.END_SERVER_TICK.register(this::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> baselines.clear());
	}

	private void tick(MinecraftServer server)
	{
		if (++countdown < intervalTicks)
		{
			return;
		}
		countdown = 0;

		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		Set<UUID> online = new HashSet<>();
		for (ServerPlayer player : players)
		{
			online.add(player.getUUID());
			poll(player);
		}
		baselines.keySet().retainAll(online);
	}

	private void poll(ServerPlayer player)
	{
		long baseline = baselines.computeIfAbsent(player.getUUID(), uuid -> statValue(player));
		topUp(player, statValue(player) - baseline);
	}

	private long statValue(ServerPlayer player)
	{
		return player.getStats().getValue(stat);
	}

	private void topUp(ServerPlayer player, long gained)
	{
		long earned = gained / divisor;
		if (earned <= 0)
		{
			return;
		}
		ScoreboardManager manager = ScoreboardNext.scoreboardManager();
		int current = manager.getScore(board.key(), player);
		if (earned > current)
		{
			manager.addScore(board.key(), player, (int) (earned - current));
		}
	}
}
