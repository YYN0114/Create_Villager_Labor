package com.yyn.labor.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

/**
 * 工人实体检测工具类：统一判断哪些实体可以作为工位工人。
 * 支持原版村民、玩家、东方女仆、千年村庄村民，以及通过 create_labor:workers 标签的意外兼容。
 */
public final class WorkerUtil {

    private WorkerUtil() {}

    // 自定义实体类型标签：声明哪些实体可以作为工位工人
    public static final TagKey<net.minecraft.world.entity.EntityType<?>> WORKER_TAG =
        TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("create_labor", "workers"));

    // 东方女仆反射缓存
    private static Class<?> maidClass;
    private static boolean maidChecked = false;

    // 千年村庄村民反射缓存
    private static Class<?> millenaireVillagerClass;
    private static boolean millenaireChecked = false;

    /**
     * 判断实体是否为有效工人（村民/玩家/女仆/千年村庄村民/标签兼容）。
     */
    public static boolean isWorkerEntity(Entity entity) {
        return entity instanceof Villager
            || entity instanceof Player
            || isMaidEntity(entity)
            || isMillenaireVillager(entity)
            || isTaggedWorker(entity);
    }

    /**
     * 判断实体是否为非玩家的工人（用于排除玩家）。
     */
    public static boolean isNonPlayerWorker(Entity entity) {
        return entity instanceof Villager
            || isMaidEntity(entity)
            || isMillenaireVillager(entity)
            || (isTaggedWorker(entity) && !(entity instanceof Player));
    }

    // 东方女仆兼容：通过反射检测 EntityMaid / MaidEntity
    public static boolean isMaidEntity(Entity entity) {
        if (!maidChecked) {
            try {
                maidClass = Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid");
            } catch (ClassNotFoundException e1) {
                try {
                    maidClass = Class.forName("touhou_little_maid.entity.passive.MaidEntity");
                } catch (ClassNotFoundException e2) {
                    maidClass = null;
                }
            }
            maidChecked = true;
        }
        return maidClass != null && maidClass.isInstance(entity);
    }

    // 千年村庄村民兼容：通过反射检测 MillVillager 类
<<<<<<< Updated upstream
    // 实际发布版包名：org.millenaire.entity.MillVillager
    // port 源码包名：org.dizzymii.millenaire2.entity.MillVillager
    // 1.20.1 rewrite 预留包名：com.jasoncian.millenaire_rewrite.entity.MillVillager
    public static boolean isMillenaireVillager(Entity entity) {
        if (!millenaireChecked) {
            try {
                millenaireVillagerClass = Class.forName("org.millenaire.entity.MillVillager");
            } catch (ClassNotFoundException e1) {
                try {
                    millenaireVillagerClass = Class.forName("org.dizzymii.millenaire2.entity.MillVillager");
                } catch (ClassNotFoundException e2) {
                    try {
                        millenaireVillagerClass = Class.forName("com.jasoncian.millenaire_rewrite.entity.MillVillager");
                    } catch (ClassNotFoundException e3) {
                        millenaireVillagerClass = null;
                    }
                }
            }
            millenaireChecked = true;
        }
        return millenaireVillagerClass != null && millenaireVillagerClass.isInstance(entity);
=======
    // 已知包名候选（按可能性递减）
    private static final String[] MILLENAIRE_CLASS_CANDIDATES = {
        "org.millenaire.entity.MillVillager",
        "org.millenaire.common.entity.MillVillager",
        "org.dizzymii.millenaire2.entity.MillVillager",
        "com.jasoncian.millenaire_rewrite.entity.MillVillager",
        "net.millenaire.entity.MillVillager",
        "millenaire.entity.MillVillager",
    };
    public static boolean isMillenaireVillager(Entity entity) {
        // 1. 精确类名反射匹配（仅首次执行一次，找到后缓存）
        if (!millenaireChecked) {
            for (String className : MILLENAIRE_CLASS_CANDIDATES) {
                try {
                    millenaireVillagerClass = Class.forName(className);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }
            millenaireChecked = true;
        }
        // 如果精确类名匹配成功，走快速路径
        if (millenaireVillagerClass != null && millenaireVillagerClass.isInstance(entity)) {
            return true;
        }
        // 2. 逐实体扫描类继承链中是否含 "millenaire" 包名（不缓存，兼容未知版本的包名变动）
        for (Class<?> cls = entity.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            Package pkg = cls.getPackage();
            if (pkg != null && pkg.getName().contains("millenaire")) {
                return true;
            }
        }
        return false;
>>>>>>> Stashed changes
    }

    // 标签兼容：检测实体类型是否在 create_labor:workers 标签中
    public static boolean isTaggedWorker(Entity entity) {
        return entity.getType().is(WORKER_TAG);
    }
}
