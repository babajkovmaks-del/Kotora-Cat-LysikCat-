package com.example.kotoracat.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import java.util.EnumSet;
import java.util.List;

/**
 * Kotora Cat compatibility mob for Cracker's Wither Storm Mod 4.2.1.
 *
 * The integration intentionally uses only the public Minecraft entity registry id
 * "witherstormmod:wither_storm". No Wither Storm classes are bundled here.
 */
public class KotoraCatEntity extends Cat {
    private static final String STORM_ID = "witherstormmod:wither_storm";
    private static final double HUMILIATION_RANGE_SQR = 36.0D;
    private static final double ATTACK_RANGE_SQR = 256.0D;
    private static final int ATTACK_INTERVAL = 20;
    private static final int HITS_BEFORE_FINISHER = 20;

    private int attackCooldown;
    private int humiliationTicks;
    private int rageHits;
    private boolean enraged;

    public KotoraCatEntity(EntityType<? extends Cat> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new StormAttackGoal(this, 1.55D, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (attackCooldown > 0) attackCooldown--;
        if (humiliationTicks > 0) humiliationTicks--;

        LivingEntity storm = findNearestStorm(96.0D);
        if (storm == null || !storm.isAlive()) return;

        // Phase 1: the cat has to get close enough to "humiliate" the storm.
        if (!enraged && distanceToSqr(storm) <= HUMILIATION_RANGE_SQR) {
            enraged = true;
            humiliationTicks = 120;
            rageHits = 0;
            setTarget(storm);
            if (!level().isClientSide) {
                storm.setCustomName(Component.literal("Wither Storm — humiliated by Kotora Cat"));
                level().broadcastEntityEvent(this, (byte) 60);
                level().broadcastEntityEvent(storm, (byte) 61);
            }
        }

        // Phase 2: permanent rage. The cat keeps attacking until the storm is gone.
        if (enraged && !level().isClientSide && distanceToSqr(storm) <= ATTACK_RANGE_SQR
                && attackCooldown == 0) {
            attackCooldown = ATTACK_INTERVAL;
            rageHits++;

            DamageSource source = level().damageSources().mobAttack(this);
            // Strong normal hit. This is deliberately separate from the finalizer below.
            storm.hurt(source, 100000.0F);

            // Visual feedback for the special attack.
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        storm.getX(), storm.getY() + storm.getBbHeight() * 0.5D, storm.getZ(),
                        18, 2.0D, 2.0D, 2.0D, 0.1D);
            }

            // Phase 3: after enough rage strikes, Kotora gets a guaranteed finishing move.
            // This is what makes the requested "eventually can kill the storm" behavior
            // independent of the storm's custom health/damage rules.
            if (rageHits >= HITS_BEFORE_FINISHER && storm.isAlive()) {
                storm.setCustomName(Component.literal("Wither Storm — defeated by Kotora Cat"));
                storm.kill();
                level().broadcastEntityEvent(this, (byte) 62);
            }
        }
    }

    private LivingEntity findNearestStorm(double range) {
        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(range),
                e -> e.isAlive()
                        && STORM_ID.equals(e.getType().builtInRegistryHolder().key().location().toString()));

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity entity : candidates) {
            double distance = distanceToSqr(entity);
            if (distance < bestDist) {
                bestDist = distance;
                best = entity;
            }
        }
        return best;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("KotoraEnraged", enraged);
        tag.putInt("KotoraRageHits", rageHits);
        tag.putInt("KotoraHumiliationTicks", humiliationTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        enraged = tag.getBoolean("KotoraEnraged");
        rageHits = tag.getInt("KotoraRageHits");
        humiliationTicks = tag.getInt("KotoraHumiliationTicks");
    }

    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 60) {
            playSound(SoundEvents.CAT_HISS, 1.4F, 0.7F);
        } else if (id == 62) {
            playSound(SoundEvents.CAT_HISS, 2.0F, 0.45F);
            for (int i = 0; i < 12; i++) {
                level().addParticle(ParticleTypes.CLOUD,
                        getX(), getY() + 0.3D, getZ(), 0.0D, 0.08D, 0.0D);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    private static final class StormAttackGoal extends Goal {
        private final KotoraCatEntity cat;
        private final double speed;
        private final boolean longMemory;
        private LivingEntity storm;

        StormAttackGoal(KotoraCatEntity cat, double speed, boolean longMemory) {
            this.cat = cat;
            this.speed = speed;
            this.longMemory = longMemory;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!cat.isEnraged()) return false;
            storm = cat.findNearestStorm(128.0D);
            return storm != null && storm.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return cat.isEnraged() && storm != null && storm.isAlive();
        }

        @Override
        public void tick() {
            if (storm == null || !storm.isAlive()) return;
            cat.getLookControl().setLookAt(storm, 30.0F, 30.0F);
            cat.getNavigation().moveTo(storm, speed);
            if (cat.distanceToSqr(storm) < 16.0D && longMemory) {
                cat.setTarget(storm);
            }
        }
    }
}
