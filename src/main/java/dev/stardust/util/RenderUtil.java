package dev.stardust.util;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;

/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
public class RenderUtil {
    public static boolean shouldRenderBox(ESPBlockData esp) {
        return switch (esp.shapeMode) {
            case Both -> esp.lineColor.a > 0 || esp.sideColor.a > 0;
            case Lines -> esp.lineColor.a > 0;
            case Sides -> esp.sideColor.a > 0;
        };
    }

    public static boolean shouldRenderTracer(ESPBlockData esp) {
        return esp.tracer && esp.tracerColor.a > 0;
    }

    public static void renderTracerTo(Render3DEvent event, @NotNull BlockPos pos, Color tracerColor) {
        Vec3d tracerPos = pos.toCenterPos();
        event.renderer.line(
            RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
            tracerPos.x, tracerPos.y, tracerPos.z, tracerColor
        );
    }

    public static void renderBlock(Render3DEvent event, BlockPos pos, Color lineColor, Color sideColor, ShapeMode mode) {
        event.renderer.box(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, sideColor, lineColor, mode, 0
        );
    }

    public static void renderBlock(Render3DEvent event, BlockPos pos, Color color) {
        event.renderer.box(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, color, color, ShapeMode.Lines, 0
        );
    }
}
