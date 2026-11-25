package com.resurrect;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side logic: remembers where players died, validates requests, runs the cutscene. */
@Mod.EventBusSubscriber(modid = ResurrectMod.MODID)
public class ResurrectManager {
    private static final long WAIT_MS = ResurrectMod.WAIT_SECONDS * 1000L;
    private static final long TOLERANCE_MS = 750L;

    private record DeathRecord(long millis, ResourceKey<Level> dim, double x, double y, double z, float yRot, float xRot) { }
    private record Pending(DeathRecord death, long requestedMillis) { }

    private static final Map<UUID, DeathRecord> DEATHS = new ConcurrentHashMap<>();
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            DEATHS.put(sp.getUUID(), new DeathRecord(System.currentTimeMillis(), sp.level().dimension(),
                    sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot()));
            PENDING.remove(sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DEATHS.remove(event.getEntity().getUUID());
        PENDING.remove(event.getEntity().getUUID());
    }

    /** Called when the client presses Resurrect (just before it sends the normal respawn request). */
    public static void handleRequest(ServerPlayer player) {
        if (!player.isDeadOrDying() || player.server.isHardcore()) return;
        DeathRecord death = DEATHS.get(player.getUUID());
        if (death == null) return;
        long now = System.currentTimeMillis();
        if (now - death.millis() < WAIT_MS - TOLERANCE_MS) return; // too early
        PENDING.put(player.getUUID(), new Pending(death, now));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) return;

        Pending pending = PENDING.get(sp.getUUID());
        if (pending != null) {
            if (System.currentTimeMillis() - pending.requestedMillis() > 15_000L) {
                PENDING.remove(sp.getUUID());
            } else if (!sp.isDeadOrDying()) {
                // The vanilla respawn has completed and this is the fresh player entity.
                PENDING.remove(sp.getUUID());
                startResurrection(sp, pending.death());
            }
        }

        MobEffectInstance inst = sp.getEffect(ModEffects.RESURRECTING.get());
        if (inst != null) {
            tickCutscene(sp, inst);
        }
    }

    private static void startResurrection(ServerPlayer sp, DeathRecord d) {
        ServerLevel level = sp.server.getLevel(d.dim());
        if (level != null && d.y() > level.getMinBuildHeight()) {
            sp.teleportTo(level, d.x(), d.y(), d.z(), d.yRot(), d.xRot());
        }
        sp.setDeltaMovement(Vec3.ZERO);
        sp.fallDistance = 0.0F;
        sp.clearFire();

        int total = ResurrectMod.CUTSCENE_TICKS;
        sp.addEffect(new MobEffectInstance(ModEffects.RESURRECTING.get(), total, 0, false, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, total + 60, 4, false, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, total + 100, 0, false, false, false));
    }

    private static void tickCutscene(ServerPlayer sp, MobEffectInstance inst) {
        int remaining = inst.getDuration();
        int elapsed = ResurrectMod.CUTSCENE_TICKS - remaining;
        ServerLevel level = sp.serverLevel();

        if (elapsed % 20 == 1) {
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.PLAYERS, 1.2F, 0.8F);
        }
        if (elapsed % 5 == 0) {
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.SLIME_SQUISH,
                    SoundSource.PLAYERS, 0.6F, 0.5F + sp.getRandom().nextFloat() * 0.4F);
        }
        if (remaining == 1) {
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE,
                    SoundSource.PLAYERS, 1.0F, 0.9F);
        }
    }
}
