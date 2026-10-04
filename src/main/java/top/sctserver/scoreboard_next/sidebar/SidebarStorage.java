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

package top.sctserver.scoreboard_next.sidebar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import top.sctserver.scoreboard_next.ScoreboardNext;
import top.sctserver.scoreboard_next.objective.Objectives;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SidebarStorage
{
	private static final int FORMAT_VERSION = 1;
	private static final String FILE_NAME = "sidebar_subscriptions.json";
	private static final ThreadPoolExecutor IO_POOL = new ThreadPoolExecutor(
			0, 1,
			10L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(1),
			runnable ->
			{
				Thread thread = new Thread(runnable, "SidebarStorage IO");
				thread.setDaemon(true);
				return thread;
			},
			new ThreadPoolExecutor.DiscardOldestPolicy()
	);

	private final Path file;
	private Map<UUID, String> subscriptions;

	public SidebarStorage()
	{
		this.file = FabricLoader.getInstance().getConfigDir().resolve("scoreboard_next").resolve(FILE_NAME);
	}

	public synchronized Optional<String> get(UUID uuid)
	{
		this.ensureLoaded();
		return Optional.ofNullable(this.subscriptions.get(uuid));
	}

	public synchronized void put(UUID uuid, String key)
	{
		this.ensureLoaded();
		if (!key.equals(this.subscriptions.put(uuid, key)))
		{
			this.save();
		}
	}

	public synchronized boolean remove(UUID uuid)
	{
		this.ensureLoaded();
		if (this.subscriptions.remove(uuid) != null)
		{
			this.save();
			return true;
		}
		return false;
	}

	private void ensureLoaded()
	{
		if (this.subscriptions == null)
		{
			this.load();
		}
	}

	private void load()
	{
		this.subscriptions = new LinkedHashMap<>();
		boolean fileUsable = false;
		if (Files.isRegularFile(this.file))
		{
			try (Reader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8))
			{
				Data data = new Gson().fromJson(reader, Data.class);
				if (data != null)
				{
					if (data.format != FORMAT_VERSION)
					{
						ScoreboardNext.LOGGER.error("Unknown sidebar subscription storage format version {}, expected {}; ignoring file", data.format, FORMAT_VERSION);
					}
					else if (data.subs != null)
					{
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
							this.subscriptions.put(uuid, key);
							fileUsable = true;
						}
					}
				}
			}
			catch (IOException | JsonParseException e)
			{
				ScoreboardNext.LOGGER.error("Failed to read sidebar subscription storage file {}", this.file, e);
			}
		}
		if (!fileUsable)
		{
			this.save();
		}
	}

	private void save()
	{
		Map<String, String> packed = new LinkedHashMap<>();
		this.subscriptions.forEach((uuid, key) -> packed.put(uuid.toString(), key));
		String content = new GsonBuilder().setPrettyPrinting().create().toJson(new Data(packed));
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
			ScoreboardNext.LOGGER.error("Failed to write sidebar subscription storage file {}", this.file, e);
			return;
		}
		try
		{
			Thread.sleep(3);
		}
		catch (InterruptedException ignored)
		{
			Thread.currentThread().interrupt();
		}
	}

	private static final class Data
	{
		int format;
		Map<String, String> subs;

		Data()
		{
			this.subs = Map.of();
		}

		Data(Map<String, String> subs)
		{
			this.format = FORMAT_VERSION;
			this.subs = subs;
		}
	}
}
