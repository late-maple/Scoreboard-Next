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

package top.sctserver.scoreboard_next.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import top.sctserver.scoreboard_next.ScoreboardNext;
import top.sctserver.scoreboard_next.objective.Objectives;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SidebarStorage extends JsonFileStorage<Map<UUID, String>>
{
	private static final int FORMAT_VERSION = 1;
	private static final String FILE_NAME = "sidebar_subscriptions.json";
	private static final Gson GSON = new Gson();
	private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

	public SidebarStorage()
	{
		super(FILE_NAME);
	}

	public synchronized Optional<String> get(UUID uuid)
	{
		return Optional.ofNullable(this.payload().get(uuid));
	}

	public synchronized void put(UUID uuid, String key)
	{
		if (!key.equals(this.payload().put(uuid, key)))
		{
			this.save();
		}
	}

	public synchronized boolean remove(UUID uuid)
	{
		if (this.payload().remove(uuid) != null)
		{
			this.save();
			return true;
		}
		return false;
	}

	@Override
	protected Map<UUID, String> readPayload(Reader reader)
	{
		Data data = GSON.fromJson(reader, Data.class);
		if (data == null)
		{
			return null;
		}
		if (data.format != FORMAT_VERSION)
		{
			ScoreboardNext.LOGGER.error("Unknown sidebar subscription storage format version {}, expected {}; ignoring file", data.format, FORMAT_VERSION);
			return null;
		}
		if (data.subs == null)
		{
			return null;
		}
		Map<UUID, String> subscriptions = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : data.subs.entrySet())
		{
			String uuidString = entry.getKey();
			String key = entry.getValue();
			UUID uuid;
			try
			{
				uuid = UUID.fromString(uuidString);
			}
			catch (IllegalArgumentException e)
			{
				ScoreboardNext.LOGGER.warn("Invalid UUID '{}' in sidebar subscription storage, skipped", uuidString);
				continue;
			}
			if (Objectives.byKey(key).isEmpty())
			{
				ScoreboardNext.LOGGER.warn("Unknown objective key '{}' in sidebar subscription storage, skipped", key);
				continue;
			}
			subscriptions.put(uuid, key);
		}
		return subscriptions;
	}

	@Override
	protected Map<UUID, String> emptyPayload()
	{
		return new LinkedHashMap<>();
	}

	@Override
	protected String serialize(Map<UUID, String> subscriptions)
	{
		return PRETTY_GSON.toJson(new Data(subscriptions));
	}

	private static final class Data
	{
		int format;
		Map<String, String> subs;

		Data()
		{
			this.subs = Map.of();
		}

		Data(Map<UUID, String> subs)
		{
			this.format = FORMAT_VERSION;
			Map<String, String> packed = new LinkedHashMap<>();
			subs.forEach((uuid, key) -> packed.put(uuid.toString(), key));
			this.subs = packed;
		}
	}
}
