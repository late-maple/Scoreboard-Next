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

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import top.sctserver.scoreboard_next.ScoreboardNext;
import top.sctserver.scoreboard_next.objective.ObjectiveDefinition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SidebarManager
{
	private final MinecraftServer server;
	private final SidebarStorage storage;
	private final Set<String> trackedBoards = new HashSet<>();
	private final Map<UUID, Set<String>> syncedObjectives = new HashMap<>();

	public SidebarManager(MinecraftServer server)
	{
		this.server = Objects.requireNonNull(server, "server must not be null");
		this.storage = new SidebarStorage();
	}

	public void onPlayerJoined(ServerPlayer player)
	{
		this.syncedObjectives.remove(player.getUUID());
		try
		{
			this.apply(player);
		}
		catch (Exception e)
		{
			ScoreboardNext.LOGGER.error("Failed to apply sidebar subscription for player {}", player.getGameProfile().name(), e);
		}
	}

	public void subscribe(ServerPlayer player, String key)
	{
		this.storage.put(player.getUUID(), key);
		this.apply(player);
	}

	public void unsubscribe(ServerPlayer player)
	{
		this.storage.remove(player.getUUID());
		player.connection.send(new ClientboundSetDisplayObjectivePacket(
				DisplaySlot.SIDEBAR,
				this.server.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR)
		));
	}

	public void repushSubscribersOf(String key)
	{
		this.trackedBoards.remove(key);
		this.syncedObjectives.values().forEach(keys -> keys.remove(key));
		for (ServerPlayer player : this.server.getPlayerList().getPlayers())
		{
			if (this.storage.get(player.getUUID()).filter(key::equals).isPresent())
			{
				this.apply(player);
			}
		}
	}

	public boolean shouldBlockDisplay(ServerPlayer player, String objectiveName)
	{
		return this.storage.get(player.getUUID()).
				map(key -> !ObjectiveDefinition.NAMESPACE.concat(key).equals(objectiveName)).
				orElse(false);
	}

	public boolean shouldBlockDisplayFor(Object listener, String objectiveName)
	{
		for (ServerPlayer player : this.server.getPlayerList().getPlayers())
		{
			if (player.connection == listener)
			{
				return this.shouldBlockDisplay(player, objectiveName);
			}
		}
		return false;
	}

	public Optional<String> subscriptionOf(ServerPlayer player)
	{
		return this.storage.get(player.getUUID());
	}

	private void apply(ServerPlayer player)
	{
		String key = this.storage.get(player.getUUID()).orElse(null);
		if (key == null)
		{
			return;
		}
		Objective objective = ScoreboardNext.scoreboardManager().ensureObjective(key);
		if (this.isDisplayed(objective))
		{
			this.trackedBoards.remove(key);
		}
		else
		{
			this.ensureTracked(objective, key);
			if (!this.isSynced(player.getUUID(), key))
			{
				for (Packet<?> packet : this.server.getScoreboard().getStartTrackingPackets(objective))
				{
					player.connection.send(packet);
				}
				this.markSynced(player.getUUID(), key);
			}
		}
		player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
	}

	private void ensureTracked(Objective objective, String key)
	{
		if (this.isDisplayed(objective))
		{
			this.trackedBoards.remove(key);
			return;
		}
		if (this.trackedBoards.add(key))
		{
			this.server.getScoreboard().startTrackingObjective(objective);
			for (ServerPlayer online : this.server.getPlayerList().getPlayers())
			{
				this.markSynced(online.getUUID(), key);
			}
		}
	}

	private boolean isDisplayed(Objective objective)
	{
		return this.server.getScoreboard().getObjectiveDisplaySlotCount(objective) > 0;
	}

	private boolean isSynced(UUID uuid, String key)
	{
		return this.syncedObjectives.getOrDefault(uuid, Set.of()).contains(key);
	}

	private void markSynced(UUID uuid, String key)
	{
		this.syncedObjectives.computeIfAbsent(uuid, u -> new HashSet<>()).add(key);
	}
}
