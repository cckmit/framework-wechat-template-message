package com.alvin.framework.wechat.template.message.pusher;

import com.alvin.framework.wechat.template.message.annotation.NotNull;
import com.alvin.framework.wechat.template.message.rule.PushRuleExecutor;
import com.alvin.framework.wechat.template.message.rule.TimeGapPushRuleExecutor;
import com.alvin.framework.wechat.template.message.rule.TimeWindowPushRuleExecutor;
import com.alvin.framework.wechat.template.message.service.AppletRepository;
import com.alvin.framework.wechat.template.message.service.HttpPoster;
import com.alvin.framework.wechat.template.message.service.PushRecorder;
import com.alvin.framework.wechat.template.message.service.TemplateMessageItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * datetime 2019/4/26 18:05
 *
 * @author sin5
 */
public abstract class TemplatePusherBuilder {

    protected ExecutorService executorService;
    protected List<PushRuleExecutor> ruleExecutors = new ArrayList<>();
    protected AppletRepository appletRepository;
    protected HttpPoster httpPoster;
    protected PushRecorder pushRecorder;
    protected TemplateMessageItemRepository templateMessageItemRepository;

    public abstract StandardTemplatePusher build();

    public TemplatePusherBuilder withExecutorService(@NotNull ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    public TemplatePusherBuilder withAppletRepository(@NotNull AppletRepository appletRepository) {
        this.appletRepository = appletRepository;
        return this;
    }

    public TemplatePusherBuilder withHttpPoster(@NotNull HttpPoster httpPoster) {
        this.httpPoster = httpPoster;
        return this;
    }

    public TemplatePusherBuilder withPushRecorder(@NotNull PushRecorder pushRecorder) {
        this.pushRecorder = pushRecorder;
        return this;
    }

    public TemplatePusherBuilder withTemplateMessageItemRepository(@NotNull TemplateMessageItemRepository repository) {
        this.templateMessageItemRepository = repository;
        return this;
    }

    public void addPushRuleExecutor(@NotNull PushRuleExecutor pushRuleExecutor) {
        this.ruleExecutors.add(pushRuleExecutor);
        if (pushRuleExecutor instanceof TimeWindowPushRuleExecutor && this.pushRecorder != null) {
            ((TimeWindowPushRuleExecutor) pushRuleExecutor).setPushRecorder(this.pushRecorder);
        } else if (pushRuleExecutor instanceof TimeGapPushRuleExecutor && this.pushRecorder != null) {
            ((TimeGapPushRuleExecutor) pushRuleExecutor).setPushRecorder(this.pushRecorder);
        }
    }
}
