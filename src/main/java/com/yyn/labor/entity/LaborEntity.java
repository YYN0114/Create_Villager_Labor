package com.yyn.labor.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 村民工人实体 - 仅使用原版村民模型，无AI，不可交易，不可移动。
 * 由性能升级(performance_upgrade)在工位上生成，作为永久工人。
 */
public class LaborEntity extends Villager {

    public LaborEntity(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        // 不注册任何 AI 目标，使其完全静止
    }

    @Override
    public void aiStep() {
        // 阻止父类的 AI 行为，不做任何更新
    }

    @Override
    public void tick() {
        // 仅执行基本实体 tick，不执行 AI
        super.tick();
        // 防止游荡
        if (!level().isClientSide) {
            setDeltaMovement(0, 0, 0);
            xo = getX();
            yo = getY();
            zo = getZ();
        }
    }

    @Override
    public void refreshDimensions() {
        // 阻止尺寸刷新
    }

    // 禁止交易
    @Override
    public MerchantOffers getOffers() {
        return new MerchantOffers();
    }

    @Override
    public void setTradingPlayer(@Nullable net.minecraft.world.entity.player.Player player) {
        // 禁止打开交易 GUI
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Nullable
    @Override
    public Villager getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.FOLLOW_RANGE, 0.0);
    }
}
