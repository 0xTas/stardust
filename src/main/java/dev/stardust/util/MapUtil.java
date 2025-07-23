package dev.stardust.util;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
public class MapUtil {
    public static void addWaypoint(BlockPos pos, String name, String initials, Purpose purpose, WpColor color, boolean temp) {
        if (!StardustUtil.XAERO_AVAILABLE) return;
        XaeroIntegration.addWaypoint(pos, name, initials, purpose, color, temp);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static void removeWaypoints(String name, Predicate<BlockPos> posPredicate, Optional<Integer> yOverride) {
        if (!StardustUtil.XAERO_AVAILABLE) return;
        XaeroIntegration.removeWaypoints(name, posPredicate, yOverride);
    }

    private static class XaeroIntegration {
        static void addWaypoint(BlockPos pos, String name, String initials, Purpose purpose, WpColor color, boolean temp) {
            try {
                if (getWaypointByCoordinate(pos.getX(), pos.getZ()) != null) {
                    LogUtil.warn("Cancelling duplicate waypoint with name: \"" + name + "\"..!", "MapUtil");
                    return;
                }

                xaero.hud.minimap.waypoint.set.WaypointSet set = getWaypointSet();

                if (set == null) {
                    LogUtil.warn("Cancelling waypoint with name \"" + name + "\" because the waypoint set is null..!", "MapUtil");
                    return;
                }

                xaero.common.minimap.waypoints.Waypoint waypoint = new xaero.common.minimap.waypoints.Waypoint(
                    pos.getX(), pos.getY(), pos.getZ(),
                    name, initials, getColor(color), getPurpose(purpose), temp
                );

                set.add(waypoint);
                xaero.map.mods.SupportMods.xaeroMinimap.requestWaypointsRefresh();
                saveWaypoints();
            } catch (Exception err) {
                LogUtil.error("Error while trying to add waypoint to Xaero map! Why - " + err, "MapUtil");
            }
        }

        static @Nullable xaero.hud.minimap.module.MinimapSession getMinimapSession() {
            return xaero.hud.minimap.BuiltInHudModules.MINIMAP.getCurrentSession();
        }

        static @Nullable xaero.hud.minimap.world.MinimapWorld getWaypointWorld() {
            xaero.hud.minimap.module.MinimapSession session = getMinimapSession();
            if (session == null) return null;
            return session.getWorldManager().getCurrentWorld();
        }

        static @Nullable xaero.hud.minimap.waypoint.set.WaypointSet getWaypointSet() {
            xaero.hud.minimap.world.MinimapWorld currentWorld = getWaypointWorld();
            if (currentWorld == null) return null;
            return currentWorld.getCurrentWaypointSet();
        }

        static @Nullable xaero.common.minimap.waypoints.Waypoint getWaypointByCoordinate(int x, int z) {
            xaero.hud.minimap.waypoint.set.WaypointSet waypointSet = getWaypointSet();
            if (waypointSet == null) return null;
            for (xaero.common.minimap.waypoints.Waypoint waypoint : waypointSet.getWaypoints()) {
                if (waypoint.getX() == x && waypoint.getZ() == z) {
                    return waypoint;
                }
            }
            return null;
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        static void removeWaypoints(String name, Predicate<BlockPos> posPredicate, Optional<Integer> yOverride) {
            try {
                xaero.hud.minimap.waypoint.set.WaypointSet set = getWaypointSet();
                if (set == null) return;
                BlockPos.Mutable mPos = new BlockPos.Mutable();
                List<xaero.common.minimap.waypoints.Waypoint> toRemove = new ObjectArrayList<>();
                for (xaero.common.minimap.waypoints.Waypoint wp : set.getWaypoints()) {
                    if (wp.getName().trim().startsWith(name.trim())) {
                        int y = yOverride.orElseGet(wp::getY);
                        mPos.set(wp.getX(), y, wp.getZ());
                        if (posPredicate.test(mPos)) {
                            toRemove.add(wp);
                        }
                    }
                }
                for (xaero.common.minimap.waypoints.Waypoint wp : toRemove) {
                    set.remove(wp);
                }
                XaeroIntegration.saveWaypoints();
            } catch (Exception err) {
                LogUtil.error("Error while trying to remove waypoints from Xaero map! Why - " + err, "MapUtil");
            }
        }

        static void saveWaypoints() {
            try {
                xaero.hud.minimap.module.MinimapSession session = getMinimapSession();
                xaero.hud.minimap.world.MinimapWorld world = getWaypointWorld();
                if (world != null) session.getWorldManagerIO().saveWorld(world);
            } catch (Exception err) {
                LogUtil.error("Failed saving minimap waypoints! Why: " + err, "MapUtil");
            }
        }

        static xaero.hud.minimap.waypoint.WaypointPurpose getPurpose(Purpose purpose) {
            return switch (purpose) {
                case Normal -> xaero.hud.minimap.waypoint.WaypointPurpose.NORMAL;
                case Destination -> xaero.hud.minimap.waypoint.WaypointPurpose.DESTINATION;
            };
        }

        static xaero.hud.minimap.waypoint.WaypointColor getColor(WpColor color) {
            return switch (color) {
                case Random -> xaero.hud.minimap.waypoint.WaypointColor.getRandom();
                case Black -> xaero.hud.minimap.waypoint.WaypointColor.BLACK;
                case Dark_Blue -> xaero.hud.minimap.waypoint.WaypointColor.DARK_BLUE;
                case Dark_Green -> xaero.hud.minimap.waypoint.WaypointColor.DARK_GREEN;
                case Dark_Aqua -> xaero.hud.minimap.waypoint.WaypointColor.DARK_AQUA;
                case Dark_Red -> xaero.hud.minimap.waypoint.WaypointColor.DARK_RED;
                case Dark_Purple -> xaero.hud.minimap.waypoint.WaypointColor.DARK_PURPLE;
                case Gold -> xaero.hud.minimap.waypoint.WaypointColor.GOLD;
                case Gray -> xaero.hud.minimap.waypoint.WaypointColor.GRAY;
                case Dark_Gray -> xaero.hud.minimap.waypoint.WaypointColor.DARK_GRAY;
                case Blue -> xaero.hud.minimap.waypoint.WaypointColor.BLUE;
                case Green -> xaero.hud.minimap.waypoint.WaypointColor.GREEN;
                case Aqua -> xaero.hud.minimap.waypoint.WaypointColor.AQUA;
                case Red -> xaero.hud.minimap.waypoint.WaypointColor.RED;
                case Purple -> xaero.hud.minimap.waypoint.WaypointColor.PURPLE;
                case Yellow -> xaero.hud.minimap.waypoint.WaypointColor.YELLOW;
                case White -> xaero.hud.minimap.waypoint.WaypointColor.WHITE;
            };
        }
    }

    public enum Purpose {
        Normal, Destination
    }

    public enum WpColor {
        Black, Dark_Blue, Dark_Green, Dark_Aqua, Dark_Red, Dark_Purple,
        Gold, Gray, Dark_Gray, Blue, Green, Aqua, Red, Purple, Yellow, White, Random
    }
}
