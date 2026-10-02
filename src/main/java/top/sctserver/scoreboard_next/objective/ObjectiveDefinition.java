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

import java.util.Objects;
import java.util.regex.Pattern;

public record ObjectiveDefinition(String key, Component displayName, ObjectiveCriteria criteria)
{
	public static final String NAMESPACE = "scoreboardnext.";
	private static final Pattern KEY = Pattern.compile("[a-z0-9_]+");

	public ObjectiveDefinition(String key, String displayName, ObjectiveCriteria criteria)
	{
		this(key, Component.literal(displayName), criteria);
	}

	public ObjectiveDefinition
	{
		Objects.requireNonNull(key, "key must not be null");
		Objects.requireNonNull(displayName, "displayName must not be null");
		Objects.requireNonNull(criteria, "criteria must not be null");
		if (!KEY.matcher(key).matches())
		{
			throw new IllegalArgumentException(
					"Invalid objective key '" + key + "': only lowercase letters, digits and underscores are allowed"
			);
		}
	}

	public String objectiveName()
	{
		return NAMESPACE + key;
	}
}
