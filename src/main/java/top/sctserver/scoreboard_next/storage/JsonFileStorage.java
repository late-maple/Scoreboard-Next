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

import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import top.sctserver.scoreboard_next.ScoreboardNext;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class JsonFileStorage<P>
{
	private static final String DIRECTORY = "scoreboard_next";
	private static final ThreadPoolExecutor IO_POOL = new ThreadPoolExecutor(
			0, 1,
			10L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(1),
			runnable ->
			{
				Thread thread = new Thread(runnable, "ScoreboardNext Storage IO");
				thread.setDaemon(true);
				return thread;
			},
			new ThreadPoolExecutor.DiscardOldestPolicy()
	);

	private final Path file;
	private P payload;

	protected JsonFileStorage(String fileName)
	{
		this.file = FabricLoader.getInstance().getConfigDir().resolve(DIRECTORY).resolve(Objects.requireNonNull(fileName, "fileName must not be null"));
	}

	protected final synchronized P payload()
	{
		if (this.payload == null)
		{
			this.payload = this.load();
		}
		return this.payload;
	}

	private P load()
	{
		if (!Files.isRegularFile(this.file))
		{
			return this.emptyPayload();
		}
		try (Reader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8))
		{
			P payload = this.readPayload(reader);
			return payload != null ? payload : this.emptyPayload();
		}
		catch (IOException | JsonParseException e)
		{
			ScoreboardNext.LOGGER.error("Failed to read storage file {}", this.file, e);
			return this.emptyPayload();
		}
	}

	protected abstract P readPayload(Reader reader) throws IOException;

	protected abstract P emptyPayload();

	protected abstract String serialize(P payload);

	protected final synchronized void save()
	{
		if (this.payload == null)
		{
			return;
		}
		String content = this.serialize(this.payload);
		IO_POOL.submit(() -> JsonFileStorage.writeToFile(this.file, content));
	}

	private static void writeToFile(Path file, String content)
	{
		try
		{
			Files.createDirectories(file.getParent());
			Files.write(file, content.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			ScoreboardNext.LOGGER.error("Failed to write storage file {}", file, e);
		}
	}
}
