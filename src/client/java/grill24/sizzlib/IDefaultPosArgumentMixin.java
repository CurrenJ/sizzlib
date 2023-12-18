package grill24.sizzlib;

import net.minecraft.util.math.Vec3d;

public interface IDefaultPosArgumentMixin {
    default Vec3d toAbsolutePos(Vec3d source) {
        return new Vec3d(0, 0, 0);
    }
}
