package dev.stardust.mixin;

import org.joml.Quaternionf;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import dev.stardust.mixin.interfaces.IItemEntityMixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Adds item model tumbling to Meteor's Item Physics module.
 *     See also: Meteor's ItemPhysics.java && my ItemPhysicsMixin.java (&& IItemEntityMixin.java)
 **/
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements IItemEntityMixin {
    @Unique private boolean seeded;
    @Unique private boolean landed;
    @Unique private boolean tumbling;
    @Unique private final Random rng = Random.createLocal();

    @Unique private int ticksAirborne;
    @Unique private float tumbleAngle;
    @Unique private float tumbleIntensity;
    @Unique private float baseAngularSpeed;

    @Unique private float tumbleAxisX = 0f;
    @Unique private float tumbleAxisY = 1f;
    @Unique private float tumbleAxisZ = 0f;

    @Unique private int settleTicks;
    @Unique private boolean settling;
    @Unique private float restingRotation;
    @Unique private static final int SETTLE_DURATION = 6;

    @Inject(method = "tick", at = @At("HEAD"))
    private void updateTumble(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;

        if (!seeded) {
            seeded = true;
            long seed = self.getUuid().getLeastSignificantBits() ^ ((long) self.getId() << 32);

            rng.setSeed(seed);
            restingRotation = rng.nextFloat() * 360f;
            tumbleIntensity = 0.25f + (rng.nextFloat() * rng.nextFloat());
        }

        if (!self.isOnGround()) {
            ++ticksAirborne;
            if (landed) {
                landed = false;
                tumbling = false;
                restingRotation = rng.nextFloat() * 360f;
                tumbleIntensity = 0.25f + (rng.nextFloat() * rng.nextFloat());
            }
        } else {
            landed = true;
            ticksAirborne = 0;
        }

        if (tumbling && self.isOnGround()) {
            tumbling = false;
            settling = true;
            settleTicks = 0;

            return;
        }

        if (settling) {
            ++settleTicks;
            if (settleTicks >= SETTLE_DURATION) {
                settling = false;
            }

            return;
        }

        Vec3d velocity = self.getVelocity();
        if (velocity.lengthSquared() < 0.0001 || self.isOnGround()) return;

        if (!tumbling) {
            tumbling = true;
            Vec3d motion = velocity.normalize();

            Vec3d fallback = new Vec3d(
                rng.nextFloat() * 2f - 1f,
                rng.nextFloat() * 2f - 1f,
                rng.nextFloat() * 2f - 1f
            );

            Vec3d axis = motion.crossProduct(fallback);

            if (axis.lengthSquared() < 0.0001) {
                axis = motion.crossProduct(new Vec3d(0, 1, 0));
            }

            if (axis.lengthSquared() < 0.0001) {
                axis = new Vec3d(0, 1, 0);
            }

            axis = axis.normalize();
            axis = new Vec3d(
                axis.x * tumbleIntensity + 0.2f * (1f - tumbleIntensity),
                axis.y,
                axis.z * tumbleIntensity + 0.2f * (1f - tumbleIntensity)
            ).normalize();

            tumbleAxisX = (float) axis.x;
            tumbleAxisY = (float) axis.y;
            tumbleAxisZ = (float) axis.z;
            float speed = (float) velocity.length();
            baseAngularSpeed = 18f + speed * 140f + rng.nextFloat() * 20f;
        }

        float timeFactor = 5f;
        float airTimeFactor = Math.min(1f, ticksAirborne / timeFactor);

        float effectiveIntensity = tumbleIntensity * airTimeFactor;

        tumbleAngle += baseAngularSpeed * effectiveIntensity;

        if (tumbleAngle >= 360f) tumbleAngle -= 360f;
    }

    @Override
    public Quaternionf stardust$getRenderQuaternion(float tickDelta) {
        Quaternionf tumble = new Quaternionf().rotateAxis(
            (float) Math.toRadians(tumbleAngle),
            tumbleAxisX,
            tumbleAxisY,
            tumbleAxisZ
        );

        Quaternionf rest = RotationAxis.POSITIVE_Z.rotationDegrees(restingRotation);

        if (tumbling)
            return tumble;

        if (settling) {
            float t = (settleTicks + tickDelta) / (float) SETTLE_DURATION;

            t = 1f - (1f - t) * (1f - t); // ease-out
            return new Quaternionf(tumble).slerp(rest, t);
        }

        return new Quaternionf(rest);
    }
}
