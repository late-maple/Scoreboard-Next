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

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreHolder;
import top.sctserver.scoreboard_next.ScoreboardNext;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class ScoreboardManager
{
	private final MinecraftServer server;
	private final Map<String, ObjectiveDefinition> definitions = new LinkedHashMap<>();
	private final Set<String> loggedCriteriaMismatches = new HashSet<>();
	private boolean loggedEmptyReconcile;
	private Consumer<ObjectiveDefinition> objectiveCreatedCallback = definition ->
	{
	};

	public ScoreboardManager(MinecraftServer server)
	{
		this.server = Objects.requireNonNull(server, "server must not be null");
	}

	public void register(ObjectiveDefinition definition)
	{
		Objects.requireNonNull(definition, "definition must not be null");
		definitions.put(definition.key(), definition);
	}

	public void unregister(String key)
	{
		definitions.remove(Objects.requireNonNull(key, "key must not be null"));
	}

	public Collection<ObjectiveDefinition> definitions()
	{
		return List.copyOf(definitions.values());
	}

	public void setObjectiveCreatedCallback(Consumer<ObjectiveDefinition> callback)
	{
		this.objectiveCreatedCallback = Objects.requireNonNull(callback, "callback must not be null");
	}

	public void reconcile()
	{
		Scoreboard scoreboard = server.getScoreboard();
		if (definitions.isEmpty())
		{
			if (!loggedEmptyReconcile)
			{
				loggedEmptyReconcile = true;
				ScoreboardNext.LOGGER.warn("reconcile() called with no registered definitions; nothing to do");
			}
			return;
		}
		Map<String, Objective> managed = managedObjectives(scoreboard);
		for (ObjectiveDefinition definition : definitions.values())
		{
			Objective current = managed.get(definition.objectiveName());
			if (current == null)
			{
				createObjective(scoreboard, definition);
			}
			else
			{
				if (!current.getCriteria().equals(definition.criteria()) && loggedCriteriaMismatches.add(definition.key()))
				{
					ScoreboardNext.LOGGER.warn(
							"Objective '{}' has criteria '{}' but definition '{}' expects '{}'; leaving it untouched",
							definition.objectiveName(),
							current.getCriteria().getName(),
							definition.key(),
							definition.criteria().getName()
					);
				}
				if (!current.getDisplayName().equals(definition.displayName()))
				{
					current.setDisplayName(definition.displayName());
				}
			}
		}
	}

	public void addScore(String key, ScoreHolder holder, int amount)
	{
		Objects.requireNonNull(holder, "holder must not be null");
		ObjectiveDefinition definition = definitions.get(Objects.requireNonNull(key, "key must not be null"));
		if (definition == null)
		{
			throw new IllegalArgumentException("Unknown objective key '" + key + "'");
		}
		if (amount == 0)
		{
			return;
		}
		Scoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective(definition.objectiveName());
		if (objective == null)
		{
			objective = createObjective(scoreboard, definition);
		}
		scoreboard.getOrCreatePlayerScore(holder, objective).add(amount);
	}

	public int getScore(String key, ScoreHolder holder)
	{
		Objects.requireNonNull(holder, "holder must not be null");
		ObjectiveDefinition definition = definitions.get(Objects.requireNonNull(key, "key must not be null"));
		if (definition == null)
		{
			throw new IllegalArgumentException("Unknown objective key '" + key + "'");
		}
		Scoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective(definition.objectiveName());
		if (objective == null)
		{
			return 0;
		}
		ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(holder, objective);
		return info == null ? 0 : info.value();
	}

	public Objective ensureObjective(String key)
	{
		ObjectiveDefinition definition = definitions.get(Objects.requireNonNull(key, "key must not be null"));
		if (definition == null)
		{
			throw new IllegalArgumentException("Unknown objective key '" + key + "'");
		}
		Scoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective(definition.objectiveName());
		if (objective == null)
		{
			objective = createObjective(scoreboard, definition);
		}
		return objective;
	}

	private static Map<String, Objective> managedObjectives(Scoreboard scoreboard)
	{
		Map<String, Objective> managed = new HashMap<>();
		for (Objective objective : scoreboard.getObjectives())
		{
			String name = objective.getName();
			if (name.startsWith(ObjectiveDefinition.NAMESPACE))
			{
				managed.put(name, objective);
			}
		}
		return managed;
	}

	private Objective createObjective(Scoreboard scoreboard, ObjectiveDefinition definition)
	{
		Objective objective = scoreboard.addObjective(
				definition.objectiveName(),
				definition.criteria(),
				definition.displayName(),
				definition.criteria().getDefaultRenderType(),
				true,
				null
		);
		this.objectiveCreatedCallback.accept(definition);
		return objective;
	}
}
