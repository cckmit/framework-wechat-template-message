package com.alvin.framework.wechat.template.message.pusher;

import com.alvin.framework.wechat.template.message.annotation.NotNull;
import com.alvin.framework.wechat.template.message.response.Response;
import com.alvin.framework.wechat.template.message.template.TemplateMessage;

/**
 * datetime 2019/4/24 15:50
 *
 * @author sin5
 */
public interface TemplatePusher {

    /**
     * push template message to user
     *
     * @param templateMessage template message
     * @param business business of message for push control
     * @param instant push instantly, discard message if push failed
     */
    Response push(@NotNull TemplateMessage templateMessage, String business, boolean instant);

    /**
     * run something when application initialized
     */
    void onInit();
}
