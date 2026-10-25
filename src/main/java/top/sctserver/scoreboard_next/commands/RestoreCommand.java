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
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import top.sctserver.scoreboard_next.objective.Objectives;
import top.sctserver.scoreboard_next.stats.ScoreRestorer;
import top.sctserver.scoreboard_next.stats.StatsRestore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class RestoreCommand
{
	private static final SuggestionProvider<CommandSourceStack> BOARD_SUGGESTIONS = (context, builder) ->
	{
		StatsRestore.restorableKeys().forEach(builder::suggest);
		return builder.buildFuture();
	};

	private RestoreCommand()
	{
	}

	public static void register()
	{
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("scoreboardnext")
						.then(Commands.literal("restore")
								.requires(source -> source.permissions() instanceof LevelBasedPermissionSet levels && levels.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS))
								.then(Commands.argument("player", GameProfileArgument.gameProfile())
										.executes(context -> restore(context, null))
										.then(Commands.argument("board", StringArgumentType.word())
												.suggests(BOARD_SUGGESTIONS)
												.executes(context -> restore(context, StringArgumentType.getString(context, "board")))))
								.then(Commands.literal("whitelist")
										.executes(context -> restoreWhitelist(context.getSource(), null))
										.then(Commands.argument("board", StringArgumentType.word())
												.suggests(BOARD_SUGGESTIONS)
												.executes(context -> restoreWhitelist(context.getSource(), StringArgumentType.getString(context, "board"))))))));
	}

	private static int restore(CommandContext<CommandSourceStack> context, String boardKey) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		if (!checkBoard(source, boardKey))
		{
			return 0;
		}
		Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(context, "player");
		MinecraftServer server = source.getServer();
		int restored = 0;
		for (NameAndId target : targets)
		{
			ScoreRestorer.Outcome outcome = ScoreRestorer.restorePlayer(server, target, boardKey);
			switch (outcome.status())
			{
				case RESTORED ->
				{
					source.sendSuccess(() -> feedback(target.name(), outcome.values()), false);
					restored++;
				}
				case MISSING_STATS -> source.sendFailure(Component.literal("未找到 " + target.name() + " 的统计数据文件").withStyle(ChatFormatting.RED));
				case CORRUPTED_STATS -> source.sendFailure(Component.literal(target.name() + " 的统计数据文件损坏，已跳过，未修改分数").withStyle(ChatFormatting.RED));
			}
		}
		return restored;
	}

	private static int restoreWhitelist(CommandSourceStack source, String boardKey)
	{
		if (!checkBoard(source, boardKey))
		{
			return 0;
		}
		source.sendSuccess(() -> Component.literal("正在后台恢复白名单玩家的榜单分数"), false);
		ScoreRestorer.restoreWhitelist(source.getServer(), boardKey).thenAccept(result ->
		{
			if (result.total() == 0)
			{
				source.sendFailure(Component.literal("白名单为空").withStyle(ChatFormatting.RED));
				return;
			}
			MutableComponent message = Component.literal("已恢复白名单中 " + result.restored() + "/" + result.total() + " 名玩家的榜单分数");
			if (!result.missingStats().isEmpty())
			{
				message.append(Component.literal("，未找到统计数据文件" + summarizeNames(result.missingStats())).withStyle(ChatFormatting.RED));
			}
			if (!result.corruptedStats().isEmpty())
			{
				message.append(Component.literal("，统计文件损坏已跳过" + summarizeNames(result.corruptedStats())).withStyle(ChatFormatting.RED));
			}
			source.sendSuccess(() -> message, false);
		});
		return 1;
	}

	private static boolean checkBoard(CommandSourceStack source, String boardKey)
	{
		if (boardKey == null)
		{
			return true;
		}
		if (Objectives.byKey(boardKey).isEmpty())
		{
			source.sendFailure(Component.literal("未知的计分板：" + boardKey).withStyle(ChatFormatting.RED));
			return false;
		}
		if (!StatsRestore.restorableKeys().contains(boardKey))
		{
			source.sendFailure(Component.literal("该榜不支持从统计数据恢复：" + boardKey).withStyle(ChatFormatting.RED));
			return false;
		}
		return true;
	}

	private static Component feedback(String name, Map<String, Integer> values)
	{
		MutableComponent message = Component.literal("已恢复 " + name + " 的榜单分数：");
		boolean first = true;
		for (Map.Entry<String, Integer> entry : values.entrySet())
		{
			if (!first)
			{
				message.append("，");
			}
			first = false;
			String boardName = Objectives.byKey(entry.getKey()).map(definition -> definition.displayName().getString()).orElse(entry.getKey());
			message.append(Component.literal(boardName + " " + entry.getValue()).withStyle(ChatFormatting.YELLOW));
		}
		return message;
	}

	private static String summarizeNames(List<String> names)
	{
		return names.size() <= 10 ? "：" + String.join("、", names) : "（共 " + names.size() + " 名）";
	}
}
