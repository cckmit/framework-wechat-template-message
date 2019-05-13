package com.alvin.framework.wechat.template.message.pusher;

import com.alibaba.fastjson.JSONObject;
import com.alvin.framework.wechat.template.message.response.Response;
import com.alvin.framework.wechat.template.message.response.ResponseCode;
import com.alvin.framework.wechat.template.message.rule.*;
import com.alvin.framework.wechat.template.message.service.AppletRepository;
import com.alvin.framework.wechat.template.message.service.HttpPoster;
import com.alvin.framework.wechat.template.message.service.PushRecorder;
import com.alvin.framework.wechat.template.message.service.TemplateMessageItemRepository;
import com.alvin.framework.wechat.template.message.template.TemplateMessage;
import com.alvin.framework.wechat.template.message.template.TemplateMessageItem;
import com.alvin.framework.wechat.template.message.template.TemplateMessageStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * datetime 2019/4/26 16:59
 *
 * @author sin5
 */
public class StandardTemplatePusher implements TemplatePusher {
    private static final String APPLET_TEMPLATE_SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s";

    private int maxRetryTimes = 1000;
    private ExecutorService executorService;
    private List<PushRuleExecutor> ruleExecutors = new ArrayList<>();
    private AppletRepository appletRepository;
    private HttpPoster httpPoster;
    private PushRecorder pushRecoder;
    private TemplateMessageItemRepository templateMessageItemRepository;

    StandardTemplatePusher(ExecutorService executorService,
                           AppletRepository appletRepository,
                           HttpPoster httpPoster,
                           PushRecorder pushRecoder,
                           TemplateMessageItemRepository templateMessageItemRepository) {
        this.executorService = executorService;
        this.appletRepository = appletRepository;
        this.httpPoster = httpPoster;
        this.pushRecoder = pushRecoder;
        this.templateMessageItemRepository = templateMessageItemRepository;
    }

    void setMaxRetryTimes(int maxRetryTimes) {
        this.maxRetryTimes = maxRetryTimes;
    }

    void addPushRuleExecutor(PushRuleExecutor ruleExecutor) {
        this.ruleExecutors.add(ruleExecutor);
    }

    @Override
    public Response push(TemplateMessage templateMessage, String business, boolean instant) {
        Response response = doPush(templateMessage, business);
        long createTime = System.currentTimeMillis();
        String openId = templateMessage.getTouser();
        String messageId = templateMessageItemRepository
                .add(new TemplateMessageItem(null,
                        openId,
                        business,
                        templateMessage,
                        TemplateMessageStatus.pushing,
                        createTime,
                        instant));
        int errcode = response.getErrcode();
        if (errcode == ResponseCode.ok.getCode()) {
            templateMessageItemRepository.updateStatus(messageId, TemplateMessageStatus.pushed);
            pushRecoder.record(messageId, openId, business, true, null, System.currentTimeMillis());
            return response;
        } else {
            if (instant) {
                templateMessageItemRepository.updateStatus(messageId, TemplateMessageStatus.failed);
                pushRecoder.record(messageId, openId, business, false, response.getErrmsg(), System.currentTimeMillis());
                return response;
            } else {
                if (errcode == ResponseCode.http_error.getCode()) {
                    templateMessageItemRepository.updateStatus(messageId, TemplateMessageStatus.failed);
                } else if (errcode == ResponseCode.break_push_time_rule.getCode()) {
                    executorService.execute(() -> {
                        for (int i = 0; i < maxRetryTimes; i++) {
                            Response threadResponse = doPush(templateMessage, business);
                            int threadErrcode = threadResponse.getErrcode();
                            if (threadErrcode == ResponseCode.break_push_time_rule.getCode()) {
                                long sleep = (Long) threadResponse.getData();
                                if (sleep > 0) {
                                    try {
                                        Thread.sleep(sleep);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else if (threadErrcode == ResponseCode.http_error.getCode()) {
                                templateMessageItemRepository.updateStatus(messageId, TemplateMessageStatus.failed);
                                pushRecoder.record(messageId, openId, business, false, threadResponse.getErrmsg(), System.currentTimeMillis());
                                break;
                            } else if (threadErrcode == ResponseCode.ok.getCode()) {
                                templateMessageItemRepository.updateStatus(messageId, TemplateMessageStatus.pushed);
                                pushRecoder.record(messageId, openId, business, true, null, System.currentTimeMillis());
                                break;
                            }
                        }
                    });
                }
                return response;
            }
        }
    }

    @Override
    public void onInit() {
        List<TemplateMessageItem> items = templateMessageItemRepository.findAllByStatus(TemplateMessageStatus.pushing);
        items.forEach(item -> push(item.getTemplateMessage(), item.getBusiness(), item.isInstant()));
    }

    private Response doPush(TemplateMessage templateMessage, String business) {
        String openId = templateMessage.getTouser();
        Response response = new Response();
        Response pushableResponse = pushable(openId, business);
        int pushableCode = pushableResponse.getErrcode();
        if (pushableCode == ResponseCode.ok.getCode()) {
            String accessToken = appletRepository.getAccessToken();
            if (accessToken != null) {
                String url = String.format(APPLET_TEMPLATE_SEND_URL, accessToken);
                try {
                    String httpResponse = httpPoster.post(url, JSONObject.toJSONString(templateMessage));
                    if (httpResponse != null) {
                        JSONObject responseJson = JSONObject.parseObject(httpResponse);
                        Integer code = responseJson.getInteger("errcode");
                        if (code != null) {
                            response.setErrcode(code);
                            response.setErrmsg(responseJson.getString("errmsg"));
                        } else {
                            response.setMsgCode(ResponseCode.http_error);
                        }
                    } else {
                        response.setMsgCode(ResponseCode.http_error);
                    }
                } catch (Exception e) {
                    response.setMsgCode(ResponseCode.http_error);
                }
            } else {
                throw new IllegalArgumentException("access_token error");
            }
        } else {
            return pushableResponse;
        }
        return response;
    }

    private Response pushable(String openId, String business) {
        Response response = new Response();
        for (PushRuleExecutor ruleExecutor : ruleExecutors) {
            RuleResult ruleResult = ruleExecutor.pushable(openId, business);
            if (!ruleResult.isPushable()) {
                if (ruleResult.getPushAfterMillis() != null) {
                    response.setMsgCode(ResponseCode.break_push_time_rule);
                    response.setData(ruleResult.getPushAfterMillis());
                }
                return response;
            }
        }
        response.setMsgCode(ResponseCode.ok);
        return response;
    }
}
