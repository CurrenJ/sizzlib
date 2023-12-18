package grill24.sizzlib.mixin.client;

import grill24.sizzlib.IDefaultPosArgumentMixin;
import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.command.argument.DefaultPosArgument;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DefaultPosArgument.class)
public class DefaultPosArgumentMixin implements IDefaultPosArgumentMixin {
    @Shadow
    CoordinateArgument x;

    @Shadow
    CoordinateArgument y;

    @Shadow
    CoordinateArgument z;

    @Override
    public Vec3d toAbsolutePos(Vec3d source) {
        return new Vec3d(this.x.toAbsoluteCoordinate(source.x), this.y.toAbsoluteCoordinate(source.y), this.z.toAbsoluteCoordinate(source.z));
    }
}
