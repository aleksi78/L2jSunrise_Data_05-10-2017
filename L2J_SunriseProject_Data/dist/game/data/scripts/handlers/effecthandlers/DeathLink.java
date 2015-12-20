/*
 * Copyright (C) 2004-2015 L2J DataPack
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
package handlers.effecthandlers;

import l2r.gameserver.enums.ShotType;
import l2r.gameserver.model.actor.L2Character;
import l2r.gameserver.model.effects.EffectTemplate;
import l2r.gameserver.model.effects.L2Effect;
import l2r.gameserver.model.effects.L2EffectType;
import l2r.gameserver.model.stats.Env;
import l2r.gameserver.model.stats.Formulas;
import l2r.gameserver.model.stats.Stats;
import l2r.util.Rnd;

/**
 * Death Link effect implementation.
 * @author Adry_85
 */
public final class DeathLink extends L2Effect
{
	public DeathLink(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.DEATH_LINK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public boolean onStart()
	{
		L2Character target = getEffected();
		L2Character activeChar = getEffector();
		
		if (activeChar.isAlikeDead())
		{
			return false;
		}
		
		boolean sps = getSkill().useSpiritShot() && activeChar.isChargedShot(ShotType.SPIRITSHOTS);
		boolean bss = getSkill().useSpiritShot() && activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		
		if (target.isPlayer() && target.getActingPlayer().isFakeDeath())
		{
			target.stopFakeDeath(true);
		}
		
		final boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, getSkill()));
		final byte shld = Formulas.calcShldUse(activeChar, target, getSkill());
		int damage = (int) Formulas.calcMagicDam(activeChar, target, getSkill(), shld, sps, bss, mcrit);
		
		if (damage > 0)
		{
			// Manage attack or cast break of the target (calculating rate, sending message...)
			if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
			{
				target.breakAttack();
				target.breakCast();
			}
			
			// Shield Deflect Magic: Reflect all damage on caster.
			if (target.getStat().calcStat(Stats.VENGEANCE_SKILL_MAGIC_DAMAGE, 0, target, getSkill()) > Rnd.get(100))
			{
				activeChar.reduceCurrentHp(damage, target, getSkill());
				activeChar.notifyDamageReceived(damage, target, getSkill(), mcrit, false);
			}
			else
			{
				target.reduceCurrentHp(damage, activeChar, getSkill());
				target.notifyDamageReceived(damage, activeChar, getSkill(), mcrit, false);
				activeChar.sendDamageMessage(target, damage, mcrit, false, false);
			}
			
			// Maybe launch chance skills on us
			if (activeChar.getChanceSkills() != null)
			{
				activeChar.getChanceSkills().onSkillHit(target, getSkill(), false, damage);
			}
			// Maybe launch chance skills on target
			if (target.getChanceSkills() != null)
			{
				target.getChanceSkills().onSkillHit(activeChar, getSkill(), true, damage);
			}
		}
		
		if (getSkill().isSuicideAttack())
		{
			activeChar.doDie(activeChar);
		}
		
		return true;
	}
}