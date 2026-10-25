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

package top.sctserver.scoreboard_next.stats;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.ScoreHolder;
import top.sctserver.scoreboard_next.ScoreboardNext;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ScoreRestorer
{
	public enum Status
	{
		RESTORED,
		MISSING_STATS,
		CORRUPTED_STATS
	}

	public record Outcome(Status status, Map<String, Integer> values)
	{
	}

	public record WhitelistResult(int total, int restored, List<String> missingStats, List<String> corruptedStats)
	{
	}

	private ScoreRestorer()
	{
	}

	public static Outcome restorePlayer(MinecraftServer server, NameAndId target, String boardKey)
	{
		ServerPlayer online = server.getPlayerList().getPlayer(target.id());
		StatsCounter stats;
		if (online != null)
		{
			stats = online.getStats();
		}
		else
		{
			try
			{
				stats = readStatsFile(server, target);
			}
			catch (IOException | JsonParseException e)
			{
				ScoreboardNext.LOGGER.error("Failed to read statistics file of {}", target.name(), e);
				return new Outcome(Status.CORRUPTED_STATS, Map.of());
			}
			if (stats == null)
			{
				return new Outcome(Status.MISSING_STATS, Map.of());
			}
		}
		Map<String, Long> values = computeValues(stats, boardKey);
		applyValues(server, target, values);
		return new Outcome(Status.RESTORED, clamped(values));
	}

	public static CompletableFuture<WhitelistResult> restoreWhitelist(MinecraftServer server, String boardKey)
	{
		List<NameAndId> targets = new ArrayList<>();
		for (UserWhiteListEntry entry : server.getPlayerList().getWhiteList().getEntries())
		{
			targets.add(entry.getUser());
		}
		if (targets.isEmpty())
		{
			return CompletableFuture.completedFuture(new WhitelistResult(0, 0, List.of(), List.of()));
		}
		Map<UUID, Map<String, Long>> computed = new HashMap<>();
		List<NameAndId> offlineTargets = new ArrayList<>();
		for (NameAndId target : targets)
		{
			ServerPlayer online = server.getPlayerList().getPlayer(target.id());
			if (online != null)
			{
				computed.put(target.id(), computeValues(online.getStats(), boardKey));
			}
			else
			{
				offlineTargets.add(target);
			}
		}
		CompletableFuture<WhitelistResult> future = new CompletableFuture<>();
		CompletableFuture.runAsync(() ->
		{
			List<String> missingStats = new ArrayList<>();
			List<String> corruptedStats = new ArrayList<>();
			for (NameAndId target : offlineTargets)
			{
				StatsCounter stats;
				try
				{
					stats = readStatsFile(server, target);
				}
				catch (IOException | JsonParseException e)
				{
					ScoreboardNext.LOGGER.error("Failed to read statistics file of {}", target.name(), e);
					corruptedStats.add(target.name());
					continue;
				}
				if (stats == null)
				{
					missingStats.add(target.name());
					continue;
				}
				computed.put(target.id(), computeValues(stats, boardKey));
			}
			server.execute(() -> future.complete(applyWhitelist(server, targets, computed, missingStats, corruptedStats)));
		});
		return future;
	}

	private static WhitelistResult applyWhitelist(MinecraftServer server, List<NameAndId> targets, Map<UUID, Map<String, Long>> computed, List<String> missingStats, List<String> corruptedStats)
	{
		if (ScoreboardNext.scoreboardManagerOrNull() == null)
		{
			return new WhitelistResult(targets.size(), 0, missingStats, corruptedStats);
		}
		int restored = 0;
		for (NameAndId target : targets)
		{
			Map<String, Long> values = computed.get(target.id());
			if (values == null)
			{
				continue;
			}
			applyValues(server, target, values);
			restored++;
		}
		return new WhitelistResult(targets.size(), restored, missingStats, corruptedStats);
	}

	private static void applyValues(MinecraftServer server, NameAndId target, Map<String, Long> values)
	{
		ScoreHolder holder = resolveHolder(server, target);
		for (Map.Entry<String, Long> entry : values.entrySet())
		{
			ScoreboardNext.scoreboardManager().setScore(entry.getKey(), holder, clampToInt(entry.getValue()));
		}
	}

	private static Map<String, Long> computeValues(StatsCounter stats, String boardKey)
	{
		Map<String, Long> values = StatsRestore.compute(stats);
		if (boardKey != null)
		{
			values.keySet().retainAll(Set.of(boardKey));
		}
		return values;
	}

	private static StatsCounter readStatsFile(MinecraftServer server, NameAndId target) throws IOException, JsonParseException
	{
		Path file = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(target.id() + ".json");
		if (!Files.isRegularFile(file))
		{
			return null;
		}
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
		{
			JsonParser.parseReader(reader);
		}
		return new ServerStatsCounter(server, file);
	}

	private static ScoreHolder resolveHolder(MinecraftServer server, NameAndId target)
	{
		ServerPlayer online = server.getPlayerList().getPlayer(target.id());
		return online != null ? online : ScoreHolder.forNameOnly(target.name());
	}

	private static Map<String, Integer> clamped(Map<String, Long> values)
	{
		Map<String, Integer> clamped = new LinkedHashMap<>();
		values.forEach((key, value) -> clamped.put(key, clampToInt(value)));
		return clamped;
	}

	private static int clampToInt(long value)
	{
		return (int) Math.max(0, Math.min(value, Integer.MAX_VALUE));
	}
}
