/*
 * (C) Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     ohun@live.cn (夜色)
 */

package com.shinemo.mpush.alloc;

import com.mpush.api.Constants;
import com.mpush.api.push.*;
import com.mpush.api.utils.SetUtil;
import com.mpush.tools.Jsons;
import com.mpush.tools.common.Strings;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Created by ohun on 16/9/7.
 *
 * 推送处理器
 *
 * @author ohun@live.cn (夜色)
 */
/*package*/ final class PushHandler implements HttpHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PushSender pushSender = PushSender.create();

    public void start() {
        pushSender.start();
    }

    public void stop() {
        pushSender.stop();
    }

    /**
     * 处理推送请求
     * @param httpExchange
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String body = new String(readBody(httpExchange), Constants.UTF_8);
        Map<String, Object> params = Jsons.fromJson(body, Map.class);

        boolean isOk = sendPush(params);

        byte[] data = isOk ? "服务已经开始推送,请注意查收消息".getBytes(Constants.UTF_8)
                : "userId、alias、tags三者必须有且只能有一种，多个用英文逗号分隔".getBytes(Constants.UTF_8) ;
        httpExchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        httpExchange.sendResponseHeaders(200, data.length);//200, content-length
        OutputStream out = httpExchange.getResponseBody();
        out.write(data);
        out.close();
        httpExchange.close();
    }

    private boolean sendPush(Map<String, Object> params) {
        String userId = params.containsKey("userId") ? (String) params.get("userId") : null;
        String alias = params.containsKey("alias") ? (String) params.get("alias") : null;
        String tags = params.containsKey("tags") ? (String) params.get("tags") : null;

        int d = 0;
        if(userId != null){
            d++;
        }
        if(alias != null){
            d++;
        }
        if(tags != null){
            d++;
        }
        if(d != 1){
            return false;
        }

        String title = (String) params.get("title");
        String content = (String) params.get("content");
        Boolean broadcast = (Boolean) params.get("broadcast");
        String condition = (String) params.get("condition");
        String ticker = params.containsKey("ticker") ? (String) params.get("ticker") : title;

        Map<String, String> extras = params.containsKey("extras") ? (Map<String, String>) params.get("extras") : null;

        MsgType msgType = params.containsKey("msgType") ? MsgType.getMsgType((int) params.get("msgType")) : MsgType.NOTIFICATION_AND_MESSAGE;
        if(msgType == null){
            msgType = MsgType.NOTIFICATION_AND_MESSAGE;
        }

        Set<String> tagsSet = tags!=null? SetUtil.toSet(tags.split(",")) : null;
        Set<String> aliasSet = alias!=null ? SetUtil.toSet(alias.split(",")) : null;
        Set<String> userIdSet = null;
        if(userId!=null && userId.indexOf(",")>0){
            userIdSet = SetUtil.toSet(userId.split(","));
        }

        Notification notification = new Notification();
        notification.content = content;
        notification.title = title;
        notification.ticker = ticker;
        PushMsg pushMsg = PushMsg.build(msgType, Jsons.toJson(notification));

        PushContext context = PushContext
                .build(pushMsg)
                .setUserId(userIdSet!=null ? null : Strings.isBlank(userId) ? null : userId)
                .setUserIds(userIdSet)
                .setAliasSet(aliasSet)
                .setTags(tagsSet)
                .setExtras(extras)
                .setBroadcast(broadcast != null && broadcast)
                .setCondition(Strings.isBlank(condition) ? null : condition)
                .setCallback(new PushCallback() {
                    @Override
                    public void onResult(PushResult result) {
                        logger.info(result.toString());
                    }
                });

        if(context.getUserIds() != null ){
            pushSender.sendByUserIds(context);
        } else if(context.getUserId() != null ){
            pushSender.sendByUserId(context);
        } else if(context.getAliasSet() != null ){
            pushSender.sendByAlias(context);
        } else if(context.getTags() != null ){
            pushSender.sendByTags(context);
        } else {
            return false;
        }
        return true;
    }

    private byte[] readBody(HttpExchange httpExchange) throws IOException {
        InputStream in = httpExchange.getRequestBody();
        String length = httpExchange.getRequestHeaders().getFirst("content-length");
        if (length != null && !length.equals("0")) {
            byte[] buffer = new byte[Integer.parseInt(length)];
            in.read(buffer);
            in.close();
            return buffer;
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            in.close();
            return out.toByteArray();
        }
    }
}
