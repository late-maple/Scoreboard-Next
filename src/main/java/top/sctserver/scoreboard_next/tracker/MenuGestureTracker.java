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

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import top.sctserver.scoreboard_next.commands.SidebarCommand;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class MenuGestureTracker
{
	public static final float PITCH_THRESHOLD = -85.0F;
	public static final int COOLDOWN_TICKS = 60;

	private static final MenuGestureTracker INSTANCE = new MenuGestureTracker();

	private final Set<ServerPlayer> sneakingPlayers = Collections.newSetFromMap(new WeakHashMap<>());
	private final Map<ServerPlayer, Integer> menuCooldownEnds = new WeakHashMap<>();

	private MenuGestureTracker()
	{
	}

	public static void register()
	{
		ServerTickEvents.END_SERVER_TICK.register(INSTANCE::tick);
	}

	private void tick(MinecraftServer server)
	{
		int currentTick = server.getTickCount();
		for (ServerPlayer player : server.getPlayerList().getPlayers())
		{
			boolean sneaking = player.isShiftKeyDown();
			boolean standUp = detectStandUp(this.sneakingPlayers, player, sneaking, player.getXRot());
			if (standUp && player.isAlive() && !player.isSpectator() && MenuGestureStorage.getInstance().isEnabled(player.getUUID())
					&& !isMenuOnCooldown(this.menuCooldownEnds, player, currentTick))
			{
				markMenuCooldown(this.menuCooldownEnds, player, currentTick);
				SidebarCommand.sendMenu(player);
			}
		}
	}

	public static boolean detectStandUp(Set<ServerPlayer> sneakingPlayers, ServerPlayer player, boolean sneaking, float pitch)
	{
		boolean wasSneaking = sneakingPlayers.contains(player);
		if (sneaking)
		{
			sneakingPlayers.add(player);
		}
		else
		{
			sneakingPlayers.remove(player);
		}
		return wasSneaking && !sneaking && pitch <= PITCH_THRESHOLD;
	}

	public static boolean isMenuOnCooldown(Map<ServerPlayer, Integer> menuCooldownEnds, ServerPlayer player, int currentTick)
	{
		return currentTick < menuCooldownEnds.getOrDefault(player, Integer.MIN_VALUE);
	}

	public static void markMenuCooldown(Map<ServerPlayer, Integer> menuCooldownEnds, ServerPlayer player, int currentTick)
	{
		menuCooldownEnds.put(player, currentTick + COOLDOWN_TICKS);
	}
}
