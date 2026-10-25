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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import top.sctserver.scoreboard_next.ScoreboardNext;
import top.sctserver.scoreboard_next.objective.ObjectiveDefinition;
import top.sctserver.scoreboard_next.objective.Objectives;
import top.sctserver.scoreboard_next.storage.MenuGestureStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SidebarMenu
{
	private SidebarMenu()
	{
	}

	public static void sendMenu(ServerPlayer player)
	{
		Optional<String> current = ScoreboardNext.sidebarManager().subscriptionOf(player);

		MutableComponent message = Component.literal("");
		message.append(Component.literal("—— 计分板侧边栏订阅 ——").withStyle(ChatFormatting.BOLD));
		message.append(Component.literal("\n"));
		message.append(Component.literal("当前订阅："));
		message.append(Component.literal(current.flatMap(Objectives::byKey).map(definition -> definition.displayName().getString()).orElse("无(跟随全局)")).withStyle(ChatFormatting.YELLOW));
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
		message.append(Component.literal("\n"));
		message.append(Component.literal("动作打开(抬头蹲起)："));
		message.append(gestureChip(player.getUUID()));
		player.sendSystemMessage(message);
	}

	private static Component gestureChip(UUID uuid)
	{
		boolean enabled = MenuGestureStorage.getInstance().isEnabled(uuid);
		return Component.literal(enabled ? "[已启用]" : "[已关闭]").withStyle(style -> style
				.withColor(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)
				.withClickEvent(new ClickEvent.RunCommand("/scoreboardnext gesture"))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(enabled ? "点击关闭" : "点击开启"))));
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
}
