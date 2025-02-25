package moriyashiine.aylyth.common.entity.mob;

import moriyashiine.aylyth.common.block.LargeWoodyGrowthBlock;
import moriyashiine.aylyth.common.registry.ModBlocks;
import moriyashiine.aylyth.common.registry.ModSoundEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class AylythianEntity extends HostileEntity implements IAnimatable {
	private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
	
	public AylythianEntity(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
		this.setCanPickUpLoot(true);
	}
	
	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 35)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5)
				.add(EntityAttributes.GENERIC_ARMOR, 2)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
	}
	
	@Override
	public void registerControllers(AnimationData animationData) {
		animationData.addAnimationController(new AnimationController<>(this, "controller", 10, animationEvent -> {
			float limbSwingAmount = Math.abs(animationEvent.getLimbSwingAmount());
			AnimationBuilder builder = new AnimationBuilder();
			if (limbSwingAmount > 0.01F) {
				MoveState state = limbSwingAmount > 0.6F ? MoveState.RUN : limbSwingAmount > 0.3F ? MoveState.WALK : MoveState.STALK;
				builder = switch (state) {
					case RUN -> builder.addAnimation("run", ILoopType.EDefaultLoopTypes.LOOP);
					case WALK -> builder.addAnimation("walk", ILoopType.EDefaultLoopTypes.LOOP);
					case STALK -> builder.addAnimation("stalk", ILoopType.EDefaultLoopTypes.LOOP);
				};
			}
			else {
				builder.addAnimation("idle", ILoopType.EDefaultLoopTypes.LOOP);
			}
			animationEvent.getController().setAnimation(builder);
			return PlayState.CONTINUE;
		}));
		animationData.addAnimationController(new AnimationController<>(this, "arms", 0, animationEvent -> {
			AnimationBuilder builder = new AnimationBuilder();
			if (handSwingTicks > 0 && !isDead()) {
				animationEvent.getController().setAnimation(builder.addAnimation(getMainArm() == Arm.RIGHT ? "clawswipe_right" : "clawswipe_left", ILoopType.EDefaultLoopTypes.LOOP));
				return PlayState.CONTINUE;
			}
			return PlayState.STOP;
		}));
	}
	
	@Override
	public AnimationFactory getFactory() {
		return factory;
	}
	
	@Override
	public void tick() {
		super.tick();
		if (age % 200 == 0) {
			heal(1);
		}
	}
	
	@Nullable
	@Override
	protected SoundEvent getAmbientSound() {
		return ModSoundEvents.ENTITY_AYLYTHIAN_AMBIENT;
	}
	
	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSoundEvents.ENTITY_AYLYTHIAN_HURT;
	}
	
	@Override
	protected SoundEvent getDeathSound() {
		return ModSoundEvents.ENTITY_AYLYTHIAN_DEATH;
	}
	
	@Override
	public boolean spawnsTooManyForEachTry(int count) {
		return count > 3;
	}
	
	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return 0.5F;
	}
	
	@Override
	public int getLimitPerChunk() {
		return 3;
	}
	
	@Override
	public boolean damage(DamageSource source, float amount) {
		return super.damage(source, source.isFire() ? amount * 2 : amount);
	}
	
	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (isTargetInBush(target)) {
			target = null;
		}
		super.setTarget(target);
	}
	
	@Override
	protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
		super.dropEquipment(source, lootingMultiplier, allowDrops);
		double random = this.random.nextDouble();
		if (random <= 0.20 && !world.isClient && world.getBlockState(getBlockPos()).getMaterial().isReplaceable() && ModBlocks.LARGE_WOODY_GROWTH.getDefaultState().canPlaceAt(world, getBlockPos())) {
			placeWoodyGrowths(world, getBlockPos());
		} else if (random <= 0.30 && !world.isClient && world.getBlockState(getBlockPos()).getMaterial().isReplaceable() && ModBlocks.YMPE_BLOCKS.sapling.getDefaultState().canPlaceAt(world, getBlockPos())) {
			BlockState state = ModBlocks.YMPE_BLOCKS.sapling.getDefaultState();
			world.setBlockState(getBlockPos(), state);
			playSound(state.getSoundGroup().getPlaceSound(), getSoundVolume(), getSoundPitch());
		}
	}

	public void placeWoodyGrowths(World world, BlockPos blockPos){
		List<BlockPos> possiblePositions = new ArrayList<>();
		for(int x = -1; x <= 1; x++){
			for(int z = -1; z <= 1; z++){
				for(int y = -1; y <= 1; y++){
					BlockPos offsetPos = blockPos.add(x,y,z);
					if(!world.isClient && world.getBlockState(offsetPos).getMaterial().isReplaceable() && world.getBlockState(offsetPos.down()).isIn(BlockTags.DIRT) ){
						possiblePositions.add(offsetPos);
					}
				}

			}
		}
		if (possiblePositions.size() != 0) {
			int random = this.random.nextBetween(1, 3);
			for(int i = 0; i < random; i++){
				if(possiblePositions.size() >= i){
					BlockPos placePos = Util.getRandom(possiblePositions, this.random);
					BlockState placementState = this.random.nextBoolean() ? ModBlocks.LARGE_WOODY_GROWTH.getDefaultState() : ModBlocks.SMALL_WOODY_GROWTH.getDefaultState();
					if(placementState.canPlaceAt(world, placePos)){
						LargeWoodyGrowthBlock.placeInWorld(placementState, world, placePos);
						playSound(placementState.getSoundGroup().getPlaceSound(), getSoundVolume(), getSoundPitch());
					}
				}
			}
		}
	}
	
	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new MeleeAttackGoal(this, 1.2F, false));
		goalSelector.add(2, new WanderAroundFarGoal(this, 0.5F));
		goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8));
		goalSelector.add(3, new LookAroundGoal(this));
		targetSelector.add(0, new RevengeGoal(this));
		targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	public static boolean canSpawn(EntityType<? extends MobEntity> aylythianEntityEntityType, ServerWorldAccess serverWorldAccess, SpawnReason spawnReason, BlockPos blockPos, Random random) {
		return canMobSpawn(aylythianEntityEntityType, serverWorldAccess, spawnReason, blockPos, random) && serverWorldAccess.getDifficulty() != Difficulty.PEACEFUL && random.nextBoolean();
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	public boolean isUndead() {
		return true;
	}

	public static boolean isTargetInBush(LivingEntity target) {
		if (target != null && target.isSneaking()) {
			for (int i = 0; i <= target.getHeight(); i++) {
				if (target.world.getBlockState(target.getBlockPos().up(i)).getBlock() != ModBlocks.AYLYTH_BUSH) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	enum MoveState {
		WALK, RUN, STALK
	}
}
