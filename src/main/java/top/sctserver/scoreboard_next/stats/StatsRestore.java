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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import top.sctserver.scoreboard_next.objective.Objectives;
import top.sctserver.scoreboard_next.tracker.StatBoardTracker;
import top.sctserver.scoreboard_next.tracker.Trackers;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class StatsRestore
{
	private static final Identifier ARMOR_STAND_TYPE = Identifier.withDefaultNamespace("armor_stand");

	private StatsRestore()
	{
	}

	public static Map<String, Long> compute(StatsCounter stats)
	{
		Map<String, Long> values = new LinkedHashMap<>();
		values.put(Objectives.DEATHS.key(), (long) stats.getValue(Stats.CUSTOM.get(Stats.DEATHS)));
		for (StatBoardTracker tracker : Trackers.statBoards())
		{
			values.put(tracker.board().key(), stats.getValue(tracker.stat()) / (long) tracker.divisor());
		}
		values.put(Objectives.MINED.key(), sumBlockMined(stats));
		values.put(Objectives.PLACED.key(), sumBlocksUsed(stats));
		values.put(Objectives.KILLS.key(), sumKills(stats));
		return values;
	}

	public static Set<String> restorableKeys()
	{
		Set<String> keys = new LinkedHashSet<>();
		keys.add(Objectives.DEATHS.key());
		Trackers.statBoards().forEach(tracker -> keys.add(tracker.board().key()));
		keys.add(Objectives.MINED.key());
		keys.add(Objectives.PLACED.key());
		keys.add(Objectives.KILLS.key());
		return keys;
	}

	private static long sumBlockMined(StatsCounter stats)
	{
		long sum = 0;
		for (Block block : BuiltInRegistries.BLOCK)
		{
			sum += stats.getValue(Stats.BLOCK_MINED, block);
		}
		return sum;
	}

	private static long sumBlocksUsed(StatsCounter stats)
	{
		long sum = 0;
		for (Item item : BuiltInRegistries.ITEM)
		{
			if (item instanceof BlockItem)
			{
				sum += stats.getValue(Stats.ITEM_USED, item);
			}
		}
		return sum;
	}

	private static long sumKills(StatsCounter stats)
	{
		long sum = 0;
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE)
		{
			if (!EntityType.getKey(type).equals(ARMOR_STAND_TYPE))
			{
				sum += stats.getValue(Stats.ENTITY_KILLED, type);
			}
		}
		return sum;
	}
}
