package com.alvin.framework.wechat.template.message.service;

import com.alvin.framework.wechat.template.message.annotation.NotNull;
import com.alvin.framework.wechat.template.message.template.TemplateMessageItem;
import com.alvin.framework.wechat.template.message.template.TemplateMessageStatus;

import java.util.List;

/**
 * datetime 2019/4/26 17:25
 *
 * @author sin5
 */
public interface TemplateMessageItemRepository {

    String add(@NotNull TemplateMessageItem item);

    void updateStatus(@NotNull String messageId, @NotNull TemplateMessageStatus status);

    List<TemplateMessageItem> findAllByStatus(@NotNull TemplateMessageStatus status);

    List<TemplateMessageItem> findTopNByOpenIdAndStatusOrderByCreateTimeAsc(@NotNull String openId,
                                                                            @NotNull TemplateMessageStatus status,
                                                                            int limit);
}
