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

import java.io.Reader;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class MenuGestureStorage extends JsonFileStorage<Set<UUID>>
{
	private static final int FORMAT_VERSION = 1;
	private static final String FILE_NAME = "menu_gesture.json";
	private static final Gson GSON = new Gson();
	private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final MenuGestureStorage INSTANCE = new MenuGestureStorage();

	private MenuGestureStorage()
	{
		super(FILE_NAME);
	}

	public static MenuGestureStorage getInstance()
	{
		return INSTANCE;
	}

	public synchronized boolean isEnabled(UUID uuid)
	{
		return !this.payload().contains(uuid);
	}

	public synchronized void setEnabled(UUID uuid, boolean enabled)
	{
		boolean changed = enabled ? this.payload().remove(uuid) : this.payload().add(uuid);
		if (changed)
		{
			this.save();
		}
	}

	@Override
	protected Set<UUID> readPayload(Reader reader)
	{
		Data data = GSON.fromJson(reader, Data.class);
		if (data == null)
		{
			return null;
		}
		if (data.format != FORMAT_VERSION)
		{
			ScoreboardNext.LOGGER.error("Unknown menu gesture storage format version {}, expected {}; ignoring file", data.format, FORMAT_VERSION);
			return null;
		}
		if (data.disabled == null)
		{
			return null;
		}
		Set<UUID> disabled = new HashSet<>();
		for (String uuidString : data.disabled)
		{
			try
			{
				disabled.add(UUID.fromString(uuidString));
			}
			catch (IllegalArgumentException e)
			{
				ScoreboardNext.LOGGER.warn("Invalid UUID '{}' in menu gesture storage, skipped", uuidString);
			}
		}
		return disabled;
	}

	@Override
	protected Set<UUID> emptyPayload()
	{
		return new HashSet<>();
	}

	@Override
	protected String serialize(Set<UUID> disabled)
	{
		return PRETTY_GSON.toJson(new Data(disabled));
	}

	private static final class Data
	{
		int format;
		Set<String> disabled;

		Data()
		{
			this.disabled = Set.of();
		}

		Data(Set<UUID> disabled)
		{
			this.format = FORMAT_VERSION;
			Set<String> packed = new LinkedHashSet<>();
			disabled.forEach(uuid -> packed.add(uuid.toString()));
			this.disabled = packed;
		}
	}
}
