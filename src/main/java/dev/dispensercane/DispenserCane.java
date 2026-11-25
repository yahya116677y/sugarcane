package dev.dispensercane;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;

public class DispenserCane implements ModInitializer {

	public static final int MAX_HEIGHT = 3;
	public static final int BLOCKS_PER_BONE_MEAL = 1;

	@Override
	public void onInitialize() {
		DispenseItemBehavior vanilla = DispenserBlock.DISPENSER_REGISTRY.get(Items.BONE_MEAL);
		if (vanilla == null) {
			vanilla = new DefaultDispenseItemBehavior();
		}
		DispenserBlock.registerBehavior(Items.BONE_MEAL, new CaneBoneMealBehavior(vanilla));
	}

	private static final class CaneBoneMealBehavior implements DispenseItemBehavior {
		private final DispenseItemBehavior fallback;

		private CaneBoneMealBehavior(DispenseItemBehavior fallback) {
			this.fallback = fallback;
		}

		@Override
		public ItemStack dispense(BlockSource source, ItemStack stack) {
			ServerLevel level = source.level();
			Direction facing = source.state().getValue(DispenserBlock.FACING);
			BlockPos target = source.pos().relative(facing);

			if (level.getBlockState(target).is(Blocks.SUGAR_CANE) && growCane(level, target)) {
				stack.shrink(1);
				level.levelEvent(1000, source.pos(), 0);
				level.levelEvent(2000, source.pos(), facing.get3DDataValue());
				level.levelEvent(1505, target, 15);
				return stack;
			}
			return fallback.dispense(source, stack);
		}
	}

	private static boolean growCane(ServerLevel level, BlockPos pos) {
		BlockPos top = pos;
		while (level.getBlockState(top.above()).is(Blocks.SUGAR_CANE)) {
			top = top.above();
		}
		BlockPos bottom = pos;
		while (level.getBlockState(bottom.below()).is(Blocks.SUGAR_CANE)) {
			bottom = bottom.below();
		}

		int height = top.getY() - bottom.getY() + 1;
		boolean grew = false;

		for (int i = 0; i < BLOCKS_PER_BONE_MEAL && height < MAX_HEIGHT; i++) {
			BlockPos next = top.above();
			if (!level.getBlockState(next).isAir()) {
				break;
			}
			level.setBlockAndUpdate(next, Blocks.SUGAR_CANE.defaultBlockState());
			top = next;
			height++;
			grew = true;
		}
		return grew;
	}
}
