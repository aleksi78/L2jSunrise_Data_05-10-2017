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
package handlers.skillhandlers;

import l2r.gameserver.enums.ShotType;
import l2r.gameserver.handler.ISkillHandler;
import l2r.gameserver.model.L2Object;
import l2r.gameserver.model.actor.L2Character;
import l2r.gameserver.model.effects.L2Effect;
import l2r.gameserver.model.skills.L2Skill;
import l2r.gameserver.model.skills.L2SkillType;
import l2r.gameserver.model.stats.Formulas;
import l2r.gameserver.network.SystemMessageId;
import l2r.gameserver.network.serverpackets.SystemMessage;

/**
 * Class handling the Mana damage skill
 * @author slyce
 */
public class Manadam implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.MANADAM
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		boolean sps = skill.useSpiritShot() && activeChar.isChargedShot(ShotType.SPIRITSHOTS);
		boolean bss = skill.useSpiritShot() && activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		
		for (L2Character target : (L2Character[]) targets)
		{
			if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
			{
				target = activeChar;
			}
			
			if (target.isInvul())
			{
				continue;
			}
			
			if (!Formulas.calcMagicAffected(activeChar, target, skill))
			{
				if (activeChar.isPlayer())
				{
					activeChar.sendPacket(SystemMessageId.ATTACK_FAILED);
				}
				if (target.isPlayer())
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_C2_DRAIN2);
					sm.addCharName(target);
					sm.addCharName(activeChar);
					target.sendPacket(sm);
				}
				continue;
			}
			
			final byte shld = Formulas.calcShldUse(activeChar, target, skill);
			final boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, skill));
			double damage = skill.isStaticDamage() ? skill.getPower() : Formulas.calcManaDam(activeChar, target, skill, shld, sps, bss, mcrit);
			double mp = (damage > target.getCurrentMp() ? target.getCurrentMp() : damage);
			
			// Maybe launch chance skills on us
			if (activeChar.getChanceSkills() != null)
			{
				activeChar.getChanceSkills().onSkillHit(target, skill, false, damage);
			}
			// Maybe launch chance skills on target
			if (target.getChanceSkills() != null)
			{
				target.getChanceSkills().onSkillHit(activeChar, skill, true, damage);
			}
			
			if (damage > 0)
			{
				target.stopEffectsOnDamage(true);
				target.setCurrentMp(target.getCurrentMp() - mp);
			}
			
			if (target.isPlayer())
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_MP_HAS_BEEN_DRAINED_BY_C1);
				sm.addCharName(activeChar);
				sm.addInt((int) mp);
				target.sendPacket(sm);
			}
			
			if (activeChar.isPlayer())
			{
				SystemMessage sm2 = SystemMessage.getSystemMessage(SystemMessageId.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1);
				sm2.addInt((int) mp);
				activeChar.sendPacket(sm2);
			}
		}
		
		if (skill.hasSelfEffects())
		{
			L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if ((effect != null) && effect.isSelfEffect())
			{
				// Replace old effect with new one.
				effect.exit();
			}
			// cast self effect if any
			skill.getEffectsSelf(activeChar);
		}
		
		activeChar.setChargedShot(bss ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS, false);
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}