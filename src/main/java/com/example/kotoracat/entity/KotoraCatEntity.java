package com.example.kotoracat.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Kotora is intentionally NOT a subclass of the vanilla Cat.
 * Its movement, combat, taming and personality are implemented here.
 * Vanilla-like cat sounds are kept for familiarity.
 */
public class KotoraCatEntity extends TamableAnimal {
    private static final String STORM_ID = "witherstormmod:wither_storm";
    private static final double TARGET_RANGE_SQR = 32.0D * 32.0D;
    private static final double STORM_RANGE_SQR = 128.0D * 128.0D;
    private static final int ATTACK_INTERVAL = 12;
    private static final int STORM_FINISH_HITS = 16;

    public enum Personality {
        KIND,
        EVIL
    }

    private Personality personality;
    private int attackCooldown;
    private int maneuverCooldown;
    private int stormHits;
    private boolean stormEnraged;
    private long personalitySeed;

    public KotoraCatEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        personalitySeed = level.random.nextLong();
        personality = level.random.nextBoolean() ? Personality.KIND : Personality.EVIL;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    @Override
    protected void registerGoals() {
        // This is our own AI stack. There is no Cat.registerGoals() call here.
        goalSelector.addGoal(1, new KotoraCombatGoal(this));
        goalSelector.addGoal(2, new KotoraFollowOwnerGoal(this));
        goalSelector.addGoal(8, new KotoraWanderGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (attackCooldown > 0) attackCooldown--;
        if (maneuverCooldown > 0) maneuverCooldown--;

        if (!level().isClientSide) {
            LivingEntity target = getTarget();
            if (target != null && (!target.isAlive() || !canFight(target))) {
                setTarget(null);
                target = null;
            }

            if (target != null) {
                getLookControl().setLookAt(target, 40.0F, 40.0F);
                if (maneuverCooldown == 0) {
                    doCombatManeuver(target);
                }
                if (distanceToSqr(target) <= 3.2D * 3.2D && attackCooldown == 0) {
                    attackCooldown = ATTACK_INTERVAL;
                    performAttack(target);
                }
            } else if (isTame() && getOwner() != null && distanceToSqr(getOwner()) > 18.0D * 18.0D) {
                teleportNearOwnerIfNeeded();
            }
        }
    }

    private boolean canFight(LivingEntity target) {
        if (target == this) return false;
        if (isOwnedBy(target)) return false;
        if (target instanceof Player player && player.isCreative()) return false;
        return target instanceof Enemy || isStorm(target);
    }

    private LivingEntity findBestTarget() {
        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(32.0D),
                this::canFight);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double d = distanceToSqr(candidate);
            if (d < bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        return best;
    }

    private LivingEntity findNearestStorm() {
        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(128.0D),
                e -> e.isAlive() && isStorm(e));
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double d = distanceToSqr(candidate);
            if (d < bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        return best;
    }

    private boolean isStorm(Entity entity) {
        return STORM_ID.equals(entity.getType().builtInRegistryHolder().key().location().toString());
    }

    private void performAttack(LivingEntity target) {
        if (isStorm(target)) {
            attackStorm(target);
            return;
        }
        swing(InteractionHand.MAIN_HAND);
        doHurtTarget(target);
        if (personality == Personality.EVIL && random.nextFloat() < 0.22F) {
            target.setSecondsOnFire(2);
        }
    }

    private void attackStorm(LivingEntity storm) {
        if (!stormEnraged && distanceToSqr(storm) <= 6.0D * 6.0D) {
            stormEnraged = true;
            stormHits = 0;
            storm.setCustomName(Component.literal("Wither Storm — Kotora target"));
            level().broadcastEntityEvent(this, (byte) 60);
        }

        if (!stormEnraged) return;
        stormHits++;
        swing(InteractionHand.MAIN_HAND);

        // Normal damage is attempted for visual/gameplay consistency, but the
        // finishing move uses Cracker's own command because the Storm has custom defenses.
        storm.hurt(level().damageSources().mobAttack(this), 100000.0F);

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    storm.getX(), storm.getY() + storm.getBbHeight() * 0.5D, storm.getZ(),
                    24, 2.5D, 2.5D, 2.5D, 0.08D);
        }

        if (stormHits >= STORM_FINISH_HITS && storm.isAlive()) {
            finishStorm(storm);
        }
    }

    private void finishStorm(LivingEntity storm) {
        storm.setCustomName(Component.literal("Wither Storm — defeated by Kotora"));
        if (!(storm.level() instanceof ServerLevel serverLevel)) return;

        MinecraftServer server = serverLevel.getServer();
        if (server != null) {
            // Cracker's documented command is used instead of vanilla /kill so the
            // Wither Storm's own death sequence can run.
            var source = server.createCommandSourceStack()
                    .withLevel(serverLevel)
                    .withPosition(storm.position())
                    .withPermission(4)
                    .withSuppressedOutput();
            server.getCommands().performPrefixedCommand(
                    source,
                    "witherstormmod kill @e[type=witherstormmod:wither_storm,sort=nearest,limit=1,distance=..8]");
        }

        if (storm.isAlive()) {
            storm.kill();
        }
        stormEnraged = false;
        stormHits = 0;
    }

    private void doCombatManeuver(LivingEntity target) {
        int chance = personality == Personality.EVIL ? 18 : 30;
        if (random.nextInt(100) >= chance) {
            maneuverCooldown = 10;
            return;
        }

        double dx = getX() - target.getX();
        double dz = getZ() - target.getZ();
        double length = Math.max(0.01D, Math.sqrt(dx * dx + dz * dz));
        double sideX = -dz / length;
        double sideZ = dx / length;
        double direction = random.nextBoolean() ? 1.0D : -1.0D;
        double strength = personality == Personality.EVIL ? 0.34D : 0.26D;
        setDeltaMovement(getDeltaMovement().add(sideX * strength * direction, 0.18D, sideZ * strength * direction));
        hasImpulse = true;
        maneuverCooldown = personality == Personality.EVIL ? 22 : 32;

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.15D, getZ(),
                    4, 0.15D, 0.05D, 0.15D, 0.02D);
        }
    }

    private void teleportNearOwnerIfNeeded() {
        LivingEntity owner = getOwner();
        if (owner == null || distanceToSqr(owner) < 18.0D * 18.0D) return;
        teleportTo(owner.getX() + Mth.nextInt(random, -2, 2), owner.getY(), owner.getZ() + Mth.nextInt(random, -2, 2));
        level().broadcastEntityEvent(this, (byte) 62);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (isTame() && isOwnedBy(player) && stack.isEmpty()) {
            setOrderedToSit(!isOrderedToSit());
            setInSittingPose(isOrderedToSit());
            playSound(SoundEvents.CAT_AMBIENT, 0.9F, isOrderedToSit() ? 0.7F : 1.1F);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Wild Kotora can only be tamed with fish. Each attempt has a 50% chance.
        // Empty-hand interaction no longer tames the cat.
        if (!isTame() && stack.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!isTame() && isFish(stack)) {
            if (!level().isClientSide) {
                stack.shrink(1);
                if (random.nextBoolean()) {
                    tame(player);
                    setOrderedToSit(false);
                    setInSittingPose(false);
                    heal(8.0F);
                    setCustomName(Component.literal("Kotora • " + getPersonalityName()));
                    level().broadcastEntityEvent(this, (byte) 61);
                } else {
                    // Failed taming attempt: Kotora keeps its wild state.
                    playSound(SoundEvents.CAT_HISS, 1.0F, 0.9F + random.nextFloat() * 0.2F);
                    setTarget(null);
                    setDeltaMovement(getDeltaMovement().add(
                            (getX() - player.getX()) * 0.08D,
                            0.12D,
                            (getZ() - player.getZ()) * 0.08D));
                    hasImpulse = true;
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (isTame() && isOwnedBy(player) && isFish(stack)) {
            if (!level().isClientSide) {
                stack.shrink(1);
                heal(6.0F);
                playSound(SoundEvents.CAT_EAT, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    private boolean isFish(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON)
                || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH);
    }

    public Personality getPersonality() {
        return personality;
    }

    public String getPersonalityName() {
        return personality == Personality.KIND ? "Добрый" : "Злой";
    }

    public boolean isEvil() {
        return personality == Personality.EVIL;
    }

    public boolean isEnraged() {
        return stormEnraged;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("KotoraPersonality", personality.name());
        tag.putLong("KotoraPersonalitySeed", personalitySeed);
        tag.putInt("KotoraStormHits", stormHits);
        tag.putBoolean("KotoraStormEnraged", stormEnraged);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        String saved = tag.getString("KotoraPersonality");
        try {
            personality = Personality.valueOf(saved);
        } catch (IllegalArgumentException ignored) {
            personality = random.nextBoolean() ? Personality.KIND : Personality.EVIL;
        }
        personalitySeed = tag.getLong("KotoraPersonalitySeed");
        stormHits = tag.getInt("KotoraStormHits");
        stormEnraged = tag.getBoolean("KotoraStormEnraged");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.CAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAT_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        KotoraCatEntity child = com.example.kotoracat.registry.KotoraCatEntities.KOTORA_CAT.get().create(level);
        if (child != null) {
            child.personality = level.random.nextBoolean() ? Personality.KIND : Personality.EVIL;
            child.personalitySeed = level.random.nextLong();
        }
        return child;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !isTame() && distanceToClosestPlayer > 48.0D;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 60) {
            playSound(SoundEvents.CAT_HISS, 1.5F, 0.55F);
        } else if (id == 61) {
            for (int i = 0; i < 12; i++) {
                level().addParticle(ParticleTypes.HEART, getX(), getY() + 0.5D, getZ(),
                        random.nextGaussian() * 0.03D, 0.08D, random.nextGaussian() * 0.03D);
            }
        } else if (id == 62) {
            playSound(SoundEvents.CAT_HISS, 1.2F, 1.3F);
        } else if (id == 63) {
            playSound(SoundEvents.CAT_HISS, 1.0F, 0.7F);
            for (int i = 0; i < 6; i++) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.4D, getZ(), 0, 0.05D, 0);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    private static final class KotoraCombatGoal extends Goal {
        private final KotoraCatEntity cat;

        KotoraCombatGoal(KotoraCatEntity cat) {
            this.cat = cat;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cat.isOrderedToSit()) return false;
            LivingEntity current = cat.getTarget();
            if (current != null && current.isAlive() && cat.canFight(current)) return true;
            LivingEntity found = cat.findNearestStorm();
            if (found == null) found = cat.findBestTarget();
            if (found != null) cat.setTarget(found);
            return found != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = cat.getTarget();
            return !cat.isOrderedToSit() && target != null && target.isAlive() && cat.canFight(target);
        }

        @Override
        public void tick() {
            LivingEntity target = cat.getTarget();
            if (target == null) return;
            cat.getLookControl().setLookAt(target, 45.0F, 45.0F);
            cat.getNavigation().moveTo(target, cat.personality == Personality.EVIL ? 1.45D : 1.25D);
        }
    }

    private static final class KotoraFollowOwnerGoal extends Goal {
        private final KotoraCatEntity cat;

        KotoraFollowOwnerGoal(KotoraCatEntity cat) {
            this.cat = cat;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = cat.getOwner();
            return !cat.isOrderedToSit() && cat.isTame() && owner != null
                    && cat.getTarget() == null && cat.distanceToSqr(owner) > 9.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = cat.getOwner();
            return !cat.isOrderedToSit() && cat.isTame() && owner != null
                    && cat.getTarget() == null && cat.distanceToSqr(owner) > 4.0D;
        }

        @Override
        public void tick() {
            LivingEntity owner = cat.getOwner();
            if (owner != null) {
                cat.getNavigation().moveTo(owner, 1.2D);
                cat.getLookControl().setLookAt(owner, 25.0F, 25.0F);
            }
        }
    }

    private static final class KotoraWanderGoal extends Goal {
        private final KotoraCatEntity cat;

        KotoraWanderGoal(KotoraCatEntity cat) {
            this.cat = cat;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !cat.isOrderedToSit() && cat.getTarget() == null && cat.random.nextInt(80) == 0;
        }

        @Override
        public void start() {
            double x = cat.getX() + (cat.random.nextDouble() - 0.5D) * 10.0D;
            double y = cat.getY();
            double z = cat.getZ() + (cat.random.nextDouble() - 0.5D) * 10.0D;
            cat.getNavigation().moveTo(x, y, z, 1.0D);
        }
    }
}
