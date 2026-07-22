package com.yyn.labor.util;

import com.yyn.labor.Config;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * 东方女仆（TLM）聊天气泡兼容工具类。
 * 使用反射调用 TLM 的 ChatBubble API，避免硬依赖。
 *
 * 用法：当女仆在工位上时，周期性调用 showWorkingBubble(maid) 显示随机测试文本。
 * 使用 create() 方法自定义气泡持续时间，使其短于发送间隔，避免气泡叠加。
 */
public final class MaidChatBubbleUtil {

    private MaidChatBubbleUtil() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("CreateLabor/TLMChatBubble");
    private static final Random RANDOM = new Random();

    // 测试文本 1-10
    private static final String[] TEST_TEXTS = {
        "chat_bubble.create_labor.working.1",
        "chat_bubble.create_labor.working.2",
        "chat_bubble.create_labor.working.3",
        "chat_bubble.create_labor.working.4",
        "chat_bubble.create_labor.working.5",
        "chat_bubble.create_labor.working.6",
        "chat_bubble.create_labor.working.7",
        "chat_bubble.create_labor.working.8",
        "chat_bubble.create_labor.working.9",
        "chat_bubble.create_labor.working.10"
    };

    // 反射缓存
    private static Class<?> textChatBubbleDataClass;
    private static Method createMethod;
    private static Method getChatBubbleManagerMethod;
    private static Object type2Bg; // IChatBubbleData.TYPE_2
    private static boolean initialized = false;
    private static boolean available = false;

    /**
     * 初始化反射缓存。仅在首次调用时执行。
     * @return true 表示 TLM 聊天气泡 API 可用
     */
    private static boolean init() {
        if (initialized) return available;
        initialized = true;
        try {
            // TextChatBubbleData 包名候选（1.5.x 实际路径：entity.chatbubble.implement）
            String[] dataClassCandidates = {
                "com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData",
                "com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.data.TextChatBubbleData",
                "com.github.tartaricacid.touhoulittlemaid.client.chatbubble.data.TextChatBubbleData"
            };
            for (String cls : dataClassCandidates) {
                try {
                    textChatBubbleDataClass = Class.forName(cls);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }
            if (textChatBubbleDataClass == null) {
                LOGGER.warn("TLM TextChatBubbleData class not found, chat bubble disabled");
                return false;
            }

            // 获取 IChatBubbleData.TYPE_2 静态字段（ResourceLocation）
            String[] interfaceCandidates = {
                "com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData",
                "com.github.tartaricacid.touhoulittlemaid.client.chatbubble.IChatBubbleData"
            };
            Class<?> interfaceClass = null;
            for (String cls : interfaceCandidates) {
                try {
                    interfaceClass = Class.forName(cls);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }
            if (interfaceClass == null) {
                LOGGER.warn("TLM IChatBubbleData class not found, chat bubble disabled");
                return false;
            }
            type2Bg = interfaceClass.getField("TYPE_2").get(null);

            // 获取 TextChatBubbleData.create(int, Component, ResourceLocation, int) 静态方法
            createMethod = textChatBubbleDataClass.getMethod(
                "create", int.class, Component.class, ResourceLocation.class, int.class);

            // 获取 EntityMaid.getChatBubbleManager() 方法
            Class<?> maidClass = getMaidClass();
            if (maidClass == null) {
                LOGGER.warn("TLM EntityMaid class not found, chat bubble disabled");
                return false;
            }
            getChatBubbleManagerMethod = maidClass.getMethod("getChatBubbleManager");

            available = true;
            LOGGER.info("TLM chat bubble compatibility initialized successfully");
        } catch (Exception e) {
            available = false;
            LOGGER.warn("TLM chat bubble init failed, bubble disabled: {}", e.toString());
        }
        return available;
    }

    private static Class<?> getMaidClass() {
        try {
            return Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid");
        } catch (ClassNotFoundException e1) {
            try {
                return Class.forName("touhou_little_maid.entity.passive.MaidEntity");
            } catch (ClassNotFoundException e2) {
                return null;
            }
        }
    }

    /**
     * 为工位上的女仆显示随机聊天气泡。
     * 使用 create() 方法自定义持续时间，确保气泡在下次发送前消失，避免叠加。
     *
     * @param maid 女仆实体
     * @return true 表示成功发送气泡
     */
    public static boolean showWorkingBubble(Entity maid) {
        if (!Config.ENABLE_TLM_CHAT_BUBBLE.get()) return false;
        if (!init()) return false;
        if (!WorkerUtil.isMaidEntity(maid)) return false;

        try {
            String key = TEST_TEXTS[RANDOM.nextInt(TEST_TEXTS.length)];
            Component text = Component.translatable(key);

            // 气泡持续时间 = 发送间隔 - 20 tick，确保下次发送前气泡已消失
            // 最小 20 tick（1秒），避免间隔过小时气泡瞬间消失
            int existTick = Math.max(20, Config.TLM_CHAT_BUBBLE_INTERVAL.get() - 20);

            // 调用 TextChatBubbleData.create(existTick, text, TYPE_2, priority)
            Object bubbleData = createMethod.invoke(null, existTick, text, type2Bg, 0);

            // 调用 maid.getChatBubbleManager()
            Object manager = getChatBubbleManagerMethod.invoke(maid);
            if (manager == null) return false;

            // 调用 manager.addChatBubble(bubbleData)
            Method addMethod = null;
            for (Method m : manager.getClass().getMethods()) {
                if ("addChatBubble".equals(m.getName()) && m.getParameterCount() == 1) {
                    Class<?> paramType = m.getParameterTypes()[0];
                    if (paramType.isInstance(bubbleData) || paramType.isAssignableFrom(bubbleData.getClass())) {
                        addMethod = m;
                        break;
                    }
                }
            }
            if (addMethod == null) return false;

            addMethod.invoke(manager, bubbleData);
            return true;
        } catch (Exception e) {
            LOGGER.warn("TLM chat bubble show failed: {}", e.toString());
            return false;
        }
    }
}
