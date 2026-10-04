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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import top.sctserver.scoreboard_next.ScoreboardNext;
import top.sctserver.scoreboard_next.objective.ObjectiveDefinition;
import top.sctserver.scoreboard_next.objective.Objectives;
import top.sctserver.scoreboard_next.sidebar.SidebarManager;

import java.util.ArrayList;
import java.util.List;
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
						.executes(context -> SidebarCommand.menu(context.getSource()))
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

	private static int menu(CommandSourceStack source)
	{
		ServerPlayer player;
		try
		{
			player = source.getPlayerOrException();
		}
		catch (CommandSyntaxException e)
		{
			return usage(source);
		}
		Optional<String> current = ScoreboardNext.sidebarManager().subscriptionOf(player);

		MutableComponent message = Component.literal("");
		message.append(Component.literal("—— 计分板侧边栏订阅 ——").withStyle(ChatFormatting.BOLD));
		message.append(Component.literal("\n"));
		message.append(Component.literal("当前订阅："));
		message.append(Component.literal(current.flatMap(Objectives::byKey).map(definition -> definition.displayName().getString()).orElse("无（跟随全局）")).withStyle(ChatFormatting.YELLOW));
		message.append(Component.literal("\n"));

		List<Component> parts = new ArrayList<>();
		for (ObjectiveDefinition definition : Objectives.all())
		{
			parts.add(boardChip(definition, current));
			parts.add(Component.literal(" "));
		}
		if (current.isPresent())
		{
			parts.add(resetChip());
		}
		else
		{
			parts.remove(parts.size() - 1);
		}
		for (Component part : parts)
		{
			message.append(part);
		}
		source.sendSuccess(() -> message, false);
		return 1;
	}

	private static Component boardChip(ObjectiveDefinition definition, Optional<String> current)
	{
		String key = definition.key();
		String name = definition.displayName().getString();
		boolean subscribedToThis = current.filter(key::equals).isPresent();
		String hover;
		if (subscribedToThis)
		{
			hover = "点击取消订阅";
		}
		else if (current.isPresent())
		{
			hover = "切换为" + name;
		}
		else
		{
			hover = "订阅" + name;
		}
		return Component.literal("[" + name + "]").withStyle(style -> style
				.withColor(subscribedToThis ? ChatFormatting.AQUA : ChatFormatting.GRAY)
				.withBold(subscribedToThis)
				.withUnderlined(subscribedToThis)
				.withClickEvent(new ClickEvent.RunCommand("/scoreboardnext sidebar " + key))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
	}

	private static Component resetChip()
	{
		return Component.literal("[恢复默认]").withStyle(style -> style
				.withColor(ChatFormatting.GRAY)
				.withClickEvent(new ClickEvent.RunCommand("/scoreboardnext sidebar off"))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("恢复为全局设置"))));
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
