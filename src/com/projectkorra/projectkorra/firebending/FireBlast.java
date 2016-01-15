package com.projectkorra.projectkorra.firebending;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.avatar.AvatarState;
import com.projectkorra.projectkorra.earthbending.EarthBlast;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.waterbending.PlantRegrowth;
import com.projectkorra.projectkorra.waterbending.WaterManipulation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FireBlast extends FireAbility {
	
	private static final int MAX_TICKS = 10000;
	
	private boolean powerFurnace;
	private boolean showParticles;
	private boolean dissipate;
	private boolean isFireBurst;
	private int ticks;
	private long cooldown;
	private double speedFactor;
	private double range;
	private double damage;
	private double speed;
	private double radius;
	private double fireTicks;
	private double pushFactor;
	private Random random;
	private Location location;
	private Location origin;
	private Vector direction;
	private List<Block> safeBlocks;
	
	public FireBlast(Location location, Vector direction, Player player, int damage, List<Block> safeBlocks) {
		super(player);
		
		if (location.getBlock().isLiquid()) {
			return;
		}
		
		this.isFireBurst = true;
		this.safeBlocks = safeBlocks;
		this.damage = damage;
		this.powerFurnace = true;
		this.showParticles = true;
		this.radius = 2;
		this.dissipate = getConfig().getBoolean("Abilities.Fire.FireBlast.Dissipate");
		this.cooldown = getConfig().getLong("Abilities.Fire.FireBlast.Cooldown");
		this.range = getConfig().getDouble("Abilities.Fire.FireBlast.Range");
		this.speed = getConfig().getDouble("Abilities.Fire.FireBlast.Speed");
		this.fireTicks = getConfig().getDouble("Abilities.Fire.FireBlast.FireTicks");
		this.pushFactor = getConfig().getDouble("Abilities.Fire.FireBlast.Push");
		this.random = new Random();

		this.location = location.clone();
		this.origin = location.clone();
		this.direction = direction.clone().normalize();
		
		this.range = getDayFactor(range);
		this.damage *= 1.5;

		start();
	}
	
	public FireBlast(Player player) {
		super(player);
		
		if (bPlayer.isOnCooldown(this)) {
			return;
		} else if (player.getEyeLocation().getBlock().isLiquid() || FireBlastCharged.isCharging(player)) {
			return;
		}
		
		this.isFireBurst = false;
		this.powerFurnace = true;
		this.showParticles = true;
		this.radius = 2;
		this.dissipate = getConfig().getBoolean("Abilities.Fire.FireBlast.Dissipate");
		this.cooldown = getConfig().getLong("Abilities.Fire.FireBlast.Cooldown");
		this.range = getConfig().getDouble("Abilities.Fire.FireBlast.Range");
		this.damage = getConfig().getInt("Abilities.Fire.FireBlast.Damage");
		this.speed = getConfig().getDouble("Abilities.Fire.FireBlast.Speed");
		this.fireTicks = getConfig().getDouble("Abilities.Fire.FireBlast.FireTicks");
		this.pushFactor = getConfig().getDouble("Abilities.Fire.FireBlast.Push");
		this.random = new Random();
		this.safeBlocks = new ArrayList<>();
		
		this.range = getDayFactor(this.range);
		
		this.location = player.getEyeLocation();
		this.origin = player.getEyeLocation();
		this.direction = player.getEyeLocation().getDirection().normalize();
		this.location = location.add(direction.clone());
		
		start();
		bPlayer.addCooldown(this);
	}	

	private void advanceLocation() {
		if (showParticles) {
			ParticleEffect.FLAME.display(location, 0.275F, 0.275F, 0.275F, 0, 6);
			ParticleEffect.SMOKE.display(location, 0.3F, 0.3F, 0.3F, 0, 3);
		}
		location = location.add(direction.clone().multiply(speedFactor));
		if (random.nextInt(4) == 0) {
			playFirebendingSound(location);
		}
	}

	private void affect(Entity entity) {
		if (entity.getUniqueId() != player.getUniqueId()) {
			if (bPlayer.isAvatarState()) {
				GeneralMethods.setVelocity(entity, direction.clone().multiply(AvatarState.getValue(pushFactor)));
			} else {
				GeneralMethods.setVelocity(entity, direction.clone().multiply(pushFactor));
			}
			if (entity instanceof LivingEntity) {
				entity.setFireTicks((int) (fireTicks * 20));
				GeneralMethods.damageEntity(this, entity, (int) getDayFactor(damage));
				AirAbility.breakBreathbendingHold(entity);
				new FireDamageTimer(entity, player);
				remove();
			}
		}
	}

	private void ignite(Location location) {
		for (Block block : GeneralMethods.getBlocksAroundPoint(location, radius)) {
			if (BlazeArc.isIgnitable(player, block) 
					&& !safeBlocks.contains(block)
					&& !GeneralMethods.isRegionProtectedFromBuild(this, block.getLocation())) {
				if (canFireGrief()) {
					if (WaterAbility.isPlantbendable(block)) {
						new PlantRegrowth(player, block);
					}
					block.setType(Material.FIRE);
				} else {
					createTempFire(block.getLocation());
				}
				
				if (dissipate) {
					BlazeArc.getIgnitedBlocks().put(block, player);
					BlazeArc.getIgnitedTimes().put(block, System.currentTimeMillis());
				}
			}
		}
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this)
				|| GeneralMethods.isRegionProtectedFromBuild(this, location)) {
			remove();
			return;
		}
		
		speedFactor = speed * (ProjectKorra.time_step / 1000.0);
		ticks++;

		if (ticks > MAX_TICKS) {
			remove();
			return;
		}

		Block block = location.getBlock();
		if (GeneralMethods.isSolid(block) || block.isLiquid()) {
			if (block.getType() == Material.FURNACE && powerFurnace) {
				Furnace furnace = (Furnace) block.getState();
				furnace.setBurnTime((short) 800);
				furnace.setCookTime((short) 800);
				furnace.update();
			} else if (BlazeArc.isIgnitable(player, block.getRelative(BlockFace.UP))) {
				ignite(location);
			}
			remove();
			return;
		}

		if (location.distanceSquared(origin) > range * range) {
			remove();
			return;
		}

		WaterAbility.removeWaterSpouts(location, player);
		AirAbility.removeAirSpouts(location, player);

		Player source = player;
		if (EarthBlast.annihilateBlasts(location, radius, source) 
				|| WaterManipulation.annihilateBlasts(location, radius, source)
				|| FireBlast.annihilateBlasts(location, radius, source)) {
			remove();
			return;
		}

		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, radius)) {
			affect(entity);
			if (entity instanceof LivingEntity) {
				break;
			}
		}

		advanceLocation();
	}

	public static boolean annihilateBlasts(Location location, double radius, Player source) {
		boolean broke = false;
		for (FireBlast blast : getAbilities(FireBlast.class)) {
			Location fireBlastLocation = blast.location;
			if (location.getWorld().equals(fireBlastLocation.getWorld()) && !blast.player.equals(source)) {
				if (location.distanceSquared(fireBlastLocation) <= radius * radius) {
					blast.remove();
					broke = true;
				}
			}
		}
		if (FireBlastCharged.annihilateBlasts(location, radius, source)) {
			broke = true;
		}
		return broke;
	}

	public static ArrayList<FireBlast> getAroundPoint(Location location, double radius) {
		ArrayList<FireBlast> list = new ArrayList<FireBlast>();
		for (FireBlast fireBlast : getAbilities(FireBlast.class)) {
			Location fireblastlocation = fireBlast.location;
			if (location.getWorld().equals(fireblastlocation.getWorld())) {
				if (location.distanceSquared(fireblastlocation) <= radius * radius) {
					list.add(fireBlast);
				}
			}
		}
		return list;
	}

	public static void removeFireBlastsAroundPoint(Location location, double radius) {
		for (FireBlast fireBlast : getAbilities(FireBlast.class)) {
			Location fireBlastLocation = fireBlast.location;
			if (location.getWorld().equals(fireBlastLocation.getWorld())) {
				if (location.distanceSquared(fireBlastLocation) <= radius * radius) {
					fireBlast.remove();
				}
			}
		}
		FireBlastCharged.removeFireballsAroundPoint(location, radius);
	}

	@Override
	public String getName() {
		return isFireBurst ? "FireBurst" : "FireBlast";
	}

	@Override
	public Location getLocation() {
		return location != null ? location : origin;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}
	
	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	public boolean isPowerFurnace() {
		return powerFurnace;
	}

	public void setPowerFurnace(boolean powerFurnace) {
		this.powerFurnace = powerFurnace;
	}

	public boolean isShowParticles() {
		return showParticles;
	}

	public void setShowParticles(boolean showParticles) {
		this.showParticles = showParticles;
	}

	public boolean isDissipate() {
		return dissipate;
	}

	public void setDissipate(boolean dissipate) {
		this.dissipate = dissipate;
	}

	public int getTicks() {
		return ticks;
	}

	public void setTicks(int ticks) {
		this.ticks = ticks;
	}

	public double getSpeedFactor() {
		return speedFactor;
	}

	public void setSpeedFactor(double speedFactor) {
		this.speedFactor = speedFactor;
	}

	public double getRange() {
		return range;
	}

	public void setRange(double range) {
		this.range = range;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public double getFireTicks() {
		return fireTicks;
	}

	public void setFireTicks(double fireTicks) {
		this.fireTicks = fireTicks;
	}

	public double getPushFactor() {
		return pushFactor;
	}

	public void setPushFactor(double pushFactor) {
		this.pushFactor = pushFactor;
	}

	public Random getRandom() {
		return random;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	public Location getOrigin() {
		return origin;
	}

	public void setOrigin(Location origin) {
		this.origin = origin;
	}

	public Vector getDirection() {
		return direction;
	}

	public void setDirection(Vector direction) {
		this.direction = direction;
	}

	public static int getMaxTicks() {
		return MAX_TICKS;
	}

	public List<Block> getSafeBlocks() {
		return safeBlocks;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public boolean isFireBurst() {
		return isFireBurst;
	}

	public void setFireBurst(boolean isFireBurst) {
		this.isFireBurst = isFireBurst;
	}
	
}
