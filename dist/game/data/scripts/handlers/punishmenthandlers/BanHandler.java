/*
 * Copyright (C) 2004-2013 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.punishmenthandlers;

import l2r.gameserver.LoginServerThread;
import l2r.gameserver.handler.IPunishmentHandler;
import l2r.gameserver.model.L2World;
import l2r.gameserver.model.actor.instance.L2PcInstance;
import l2r.gameserver.model.punishment.PunishmentTask;
import l2r.gameserver.model.punishment.PunishmentType;
import l2r.gameserver.network.L2GameClient;

/**
 * This class handles ban punishment.
 * @author UnAfraid
 */
public class BanHandler implements IPunishmentHandler
{
	@Override
	public void onStart(PunishmentTask task)
	{
		try
		{
			switch (task.getAffect())
			{
				case CHARACTER:
				{
					int objectId = Integer.parseInt(String.valueOf(task.getKey()));
					final L2PcInstance player = L2World.getInstance().getPlayer(objectId);
					if (player != null)
					{
						applyToPlayer(player);
					}
					break;
				}
				case ACCOUNT:
				{
					String account = String.valueOf(task.getKey());
					final L2GameClient client = LoginServerThread.getInstance().getClient(account);
					if (client != null)
					{
						final L2PcInstance player = client.getActiveChar();
						if (player != null)
						{
							applyToPlayer(player);
						}
						else
						{
							client.closeNow();
						}
					}
					break;
				}
				case IP:
				{
					String ip = String.valueOf(task.getKey());
					for (L2PcInstance player : L2World.getInstance().getPlayers())
					{
						if (player.getIPAddress().equals(ip))
						{
							applyToPlayer(player);
						}
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
		
		}
	}
	
	@Override
	public void onEnd(PunishmentTask task)
	{
	
	}
	
	/**
	 * Applies all punishment effects from the player.
	 * @param player
	 */
	private static void applyToPlayer(L2PcInstance player)
	{
		player.logout();
	}
	
	@Override
	public PunishmentType getType()
	{
		return PunishmentType.BAN;
	}
}
