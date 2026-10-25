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

import java.util.List;

public final class Trackers
{
	private static final List<StatBoardTracker> STAT_BOARDS = List.of(
			new OnlineTimeTracker(),
			new AviatingTracker(),
			new FishCaughtTracker(),
			new TradeTracker(),
			new DamageTakenTracker()
	);

	private Trackers()
	{
	}

	public static void registerAll()
	{
		BlockBreakTracker.register();
		BlockPlaceTracker.register();
		KillTracker.register();
		STAT_BOARDS.forEach(StatBoardTracker::attach);
	}

	public static List<StatBoardTracker> statBoards()
	{
		return STAT_BOARDS;
	}
}
