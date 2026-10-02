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

package top.sctserver.scoreboard_next;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.slf4j.Logger;
import top.sctserver.scoreboard_next.objective.Objectives;
import top.sctserver.scoreboard_next.objective.ScoreboardManager;
import top.sctserver.scoreboard_next.tracker.Trackers;

public class ScoreboardNext implements ModInitializer
{
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final String MOD_ID = "scoreboard_next";
	public static String MOD_VERSION = "unknown";
	public static String MOD_NAME = "unknown";

	private static ScoreboardManager scoreboardManager;


	public static ScoreboardManager scoreboardManager()
	{
		ScoreboardManager manager = scoreboardManager;
		if (manager == null)
		{
			throw new IllegalStateException("ScoreboardManager is only available while a server is running");
		}
		return manager;
	}

	@Override
	public void onInitialize()
	{
		ModMetadata metadata = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(RuntimeException::new).getMetadata();
		MOD_NAME = metadata.getName();
		MOD_VERSION = metadata.getVersion().getFriendlyString();
		LOGGER.info("{} initialized, version {}", MOD_NAME, MOD_VERSION);

		Trackers.registerAll();

		ServerLifecycleEvents.SERVER_STARTING.register(server ->
		{
			ScoreboardManager manager = new ScoreboardManager(server);
			Objectives.all().forEach(manager::register);
			scoreboardManager = manager;
			LOGGER.info("{} manager bound to server ({} objectives registered)", MOD_NAME, Objectives.all().size());
		});
		ServerLifecycleEvents.SERVER_STARTED.register(server ->
		{
			// worlds and their scoreboards are loaded here, before the first tick
			scoreboardManager().reconcile();
			LOGGER.info("{} scoreboard objectives reconciled", MOD_NAME);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server ->
		{
			scoreboardManager = null;
		});
	}
}
