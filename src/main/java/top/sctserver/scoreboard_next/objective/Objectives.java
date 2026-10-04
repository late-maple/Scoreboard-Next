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

package top.sctserver.scoreboard_next.objective;

import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Objectives
{
	private static final List<ObjectiveDefinition> ALL = new ArrayList<>();
	private static final Map<String, ObjectiveDefinition> BY_KEY = new LinkedHashMap<>();

	public static final ObjectiveDefinition DEATHS   = register(new ObjectiveDefinition("deaths", "死亡榜", ObjectiveCriteria.DEATH_COUNT));
	public static final ObjectiveDefinition TRADE    = register(new ObjectiveDefinition("trade", "交易榜", ObjectiveCriteria.DUMMY));
	public static final ObjectiveDefinition MINED    = register(new ObjectiveDefinition("mined", "挖掘榜", ObjectiveCriteria.DUMMY));
	public static final ObjectiveDefinition PLACED   = register(new ObjectiveDefinition("placed", "放置榜", ObjectiveCriteria.DUMMY));
	public static final ObjectiveDefinition KILLS    = register(new ObjectiveDefinition("kill_counts", "击杀榜", ObjectiveCriteria.DUMMY));
	public static final ObjectiveDefinition FISHING  = register(new ObjectiveDefinition("fishing_counts", "钓鱼榜", ObjectiveCriteria.DUMMY));
	public static final ObjectiveDefinition DAMAGE   = register(new ObjectiveDefinition("damage_taken", "抖M榜", ObjectiveCriteria.DUMMY));
	public static final ObjectiveDefinition AVIATING = register(new ObjectiveDefinition("aviating_distance", "鞘翅飞行距离", ObjectiveCriteria.DUMMY));
	public static final ObjectiveDefinition ONLINE   = register(new ObjectiveDefinition("activation", "在线时间", ObjectiveCriteria.DUMMY));

	private Objectives()
	{
	}

	private static ObjectiveDefinition register(ObjectiveDefinition definition)
	{
		ALL.add(definition);
		BY_KEY.put(definition.key(), definition);
		return definition;
	}

	public static List<ObjectiveDefinition> all()
	{
		return List.copyOf(ALL);
	}

	public static Optional<ObjectiveDefinition> byKey(String key)
	{
		return Optional.ofNullable(BY_KEY.get(key));
	}
}
