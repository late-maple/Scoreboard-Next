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

package top.sctserver.scoreboard_next.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import top.sctserver.scoreboard_next.ScoreboardNext;
import top.sctserver.scoreboard_next.objective.ObjectiveDefinition;
import top.sctserver.scoreboard_next.objective.Objectives;
import top.sctserver.scoreboard_next.sidebar.SidebarManager;

import java.util.Optional;

public final class SidebarCommand
{
	private SidebarCommand()
	{
	}

	public static void register()
	{
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("scoreboardnext")
						.executes(context -> SidebarCommand.usage(context.getSource()))
						.then(Commands.literal("sidebar")
								.then(Commands.argument("board", StringArgumentType.word())
										.suggests((context, builder) ->
										{
											Objectives.all().forEach(definition -> builder.suggest(definition.key()));
											return builder.buildFuture();
										})
										.executes(context -> SidebarCommand.toggle(context.getSource(), StringArgumentType.getString(context, "board"))))
								.then(Commands.literal("off")
										.executes(context -> SidebarCommand.off(context.getSource()))))));
	}

	private static int usage(CommandSourceStack source)
	{
		source.sendSuccess(() -> Component.literal("用法：/scoreboardnext sidebar <榜名|off>"), false);
		return 1;
	}

	private static int toggle(CommandSourceStack source, String key) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		ObjectiveDefinition definition = Objectives.byKey(key).orElse(null);
		if (definition == null)
		{
			source.sendFailure(Component.literal("未知的计分板：" + key).withStyle(ChatFormatting.RED));
			return 0;
		}
		SidebarManager manager = ScoreboardNext.sidebarManager();
		Optional<String> current = manager.subscriptionOf(player);
		if (current.filter(key::equals).isPresent())
		{
			manager.unsubscribe(player);
			source.sendSuccess(() -> Component.literal("已取消订阅，侧边栏恢复为全局设置"), false);
			return 1;
		}
		boolean switching = current.isPresent();
		manager.subscribe(player, key);
		if (switching)
		{
			source.sendSuccess(() -> Component.literal("侧边栏已切换为：").append(definition.displayName()), false);
		}
		else
		{
			source.sendSuccess(() -> Component.literal("侧边栏已订阅：").append(definition.displayName()), false);
		}
		return 1;
	}

	private static int off(CommandSourceStack source) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
		SidebarManager manager = ScoreboardNext.sidebarManager();
		if (manager.subscriptionOf(player).isEmpty())
		{
			source.sendSuccess(() -> Component.literal("当前没有订阅任何计分板"), false);
			return 1;
		}
		manager.unsubscribe(player);
		source.sendSuccess(() -> Component.literal("已取消订阅，侧边栏恢复为全局设置"), false);
		return 1;
	}
}
