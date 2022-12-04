package com.afunproject.dawncraft;

import java.util.List;
import java.util.UUID;

import com.afunproject.dawncraft.capability.CapabilitiesRegister;
import com.afunproject.dawncraft.capability.RestrictBlock;
import com.google.common.collect.Lists;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventListener {

	//private static final List<EntityType<?>> BOSS_ENTITIES = Lists.newArrayList();

	private static final UUID BOSS_MODIFIER = UUID.fromString("dd686c7a-e2c7-479c-96d5-3e193b35c7b8");

	List<String> bosses = Lists.newArrayList(
			"entity.minecraft.ender_dragon","entity.minecraft.wither","entity.simple_mobs.ogre",
			"entity.simple_mobs.martian","entity.simple_mobs.sentinel_knight","entity.simple_mobs.fire_giant",
			"entity.simple_mobs.nine_tails","entity.simple_mobs.skeletonlord","entity.simple_mobs.knight_4",
			"entity.bloodandmadness.father_gascoigne","entity.bloodandmadness.gascoigne_beast",
			"entity.bloodandmadness.micolash","entity.ob_aquamirae.captain_cornelia","entity.conjurer_illager.conjurer",
			"entity.mowziesmobs.ferrous_wroughtnaut","entity.mowziesmobs.barako","entity.mowziesmobs.frostmaw",
			"entity.mowziesmobs.naga","entity.meetyourfight.projectile_line","entity.meetyourfight.swamp_mine",
			"entity.meetyourfight.swampjaw","entity.meetyourfight.dame_fortuna","entity.meetyourfight.bellringer",
			"entity.alexsmobs.warped_mosco","entity.alexsmobs.void_worm","entity.cataclysm.ender_golem",
			"entity.cataclysm.netherite_monstrosity","entity.ba_bt.land_golem","entity.ba_bt.ocean_golem",
			"entity.ba_bt.core_golem","entity.a_bt.nether_golem","entity.ba_bt.end_golem","entity.ba_bt.sky_golem",
			"entity.goblinsanddungeons.goblin_king","entity.illageandspillage.magispeller",
			"entity.illageandspillage.illashooter","entity.illageandspillage.twittollager","entity.illageandspillage.spiritcaller"
			);

	@SubscribeEvent
	public void entityJoinWorld(EntityJoinWorldEvent event) {
		if (bosses.contains(event.getEntity().getType().getDescriptionId()) && event.getEntity() instanceof Mob) {
			Mob boss = (Mob) event.getEntity();
			int players = 0;
			for (Player player : boss.level.players()) {
				if (player.distanceTo(boss)<=100) players++;
			}
			if (players > 1) {
				double damage_multiplier = Math.max(1.3, Math.pow(1.05, players-1));
				AttributeInstance damage = boss.getAttribute(Attributes.ATTACK_DAMAGE);
				damage.removeModifier(BOSS_MODIFIER);
				damage.addPermanentModifier(new AttributeModifier(BOSS_MODIFIER, "dawncraft_multiplayer_scaling", damage_multiplier, Operation.MULTIPLY_TOTAL));

				double health_multiplier = Math.max(2, Math.pow(1.5, players-1));
				AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
				health.removeModifier(BOSS_MODIFIER);
				health.addPermanentModifier(new AttributeModifier(BOSS_MODIFIER, "dawncraft_multiplayer_scaling", health_multiplier, Operation.MULTIPLY_TOTAL));
			}
		}
		if (event.getEntity() instanceof PathfinderMob) {
			PathfinderMob entity = (PathfinderMob) event.getEntity();
			entity.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(entity, 1.0D));
			LazyOptional<RestrictBlock> optional = entity.getCapability(CapabilitiesRegister.RESTRICT_BLOCK);
			if (optional.isPresent()) {
				RestrictBlock cap = optional.resolve().get();
				if (cap.canRestrict(entity)) cap.applyRestriction(entity);
			}
		}
	}

	@SubscribeEvent
	public void playerRespawn(PlayerRespawnEvent event) {
		if (event.getPlayer() instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) event.getPlayer();
			if (!player.getAbilities().mayBuild) {
				player.setGameMode(GameType.SURVIVAL);
			}
		}
	}

}
