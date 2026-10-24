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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import top.sctserver.scoreboard_next.ScoreboardNext;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MenuGestureStorage
{
	private static final int FORMAT_VERSION = 1;
	private static final String FILE_NAME = "menu_gesture.json";
	private static final MenuGestureStorage INSTANCE = new MenuGestureStorage();
	private static final ThreadPoolExecutor IO_POOL = new ThreadPoolExecutor(
			0, 1,
			10L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(1),
			runnable ->
			{
				Thread thread = new Thread(runnable, "MenuGestureStorage IO");
				thread.setDaemon(true);
				return thread;
			},
			new ThreadPoolExecutor.DiscardOldestPolicy()
	);

	private final Path file;
	private Set<UUID> disabled;

	private MenuGestureStorage()
	{
		this.file = FabricLoader.getInstance().getConfigDir().resolve("scoreboard_next").resolve(FILE_NAME);
	}

	public static MenuGestureStorage getInstance()
	{
		return INSTANCE;
	}

	public synchronized boolean isEnabled(UUID uuid)
	{
		this.ensureLoaded();
		return !this.disabled.contains(uuid);
	}

	public synchronized void setEnabled(UUID uuid, boolean enabled)
	{
		this.ensureLoaded();
		boolean changed = enabled ? this.disabled.remove(uuid) : this.disabled.add(uuid);
		if (changed)
		{
			this.save();
		}
	}

	private void ensureLoaded()
	{
		if (this.disabled == null)
		{
			this.load();
		}
	}

	private void load()
	{
		this.disabled = new HashSet<>();
		if (!Files.isRegularFile(this.file))
		{
			return;
		}
		try (Reader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8))
		{
			Data data = new Gson().fromJson(reader, Data.class);
			if (data == null)
			{
				return;
			}
			if (data.format != FORMAT_VERSION)
			{
				ScoreboardNext.LOGGER.error("Unknown menu gesture storage format version {}, expected {}; ignoring file", data.format, FORMAT_VERSION);
				return;
			}
			if (data.disabled == null)
			{
				return;
			}
			for (String uuidString : data.disabled)
			{
				try
				{
					this.disabled.add(UUID.fromString(uuidString));
				}
				catch (IllegalArgumentException e)
				{
					ScoreboardNext.LOGGER.warn("Invalid UUID '{}' in menu gesture storage, skipped", uuidString);
				}
			}
		}
		catch (IOException | JsonParseException e)
		{
			ScoreboardNext.LOGGER.error("Failed to read menu gesture storage file {}", this.file, e);
		}
	}

	private void save()
	{
		Data data = new Data(this.disabled);
		String content = new GsonBuilder().setPrettyPrinting().create().toJson(data);
		IO_POOL.submit(() -> this.writeToFile(content));
	}

	private void writeToFile(String content)
	{
		try
		{
			Files.createDirectories(this.file.getParent());
			Files.write(this.file, content.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			ScoreboardNext.LOGGER.error("Failed to write menu gesture storage file {}", this.file, e);
		}
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
			this.disabled = new LinkedHashSet<>();
			disabled.forEach(uuid -> this.disabled.add(uuid.toString()));
		}
	}
}
