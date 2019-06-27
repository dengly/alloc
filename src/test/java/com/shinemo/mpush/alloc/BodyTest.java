package com.shinemo.mpush.alloc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mpush.api.push.*;
import com.mpush.api.utils.SetUtil;
import com.mpush.tools.Jsons;
import com.mpush.tools.common.Strings;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @description:
 * @author: dengliaoyan
 * @create: 2019-06-27 19:56
 **/
public class BodyTest {
    @Test
    public void test(){
        String body = "{\"msgType\":\"2\",\"alias\":\"13631276694\",\"extras\":{\"expire\":30},\"title\":\"回复\",\"content\":\"{'result':[{'pin':7,'state':0}],'a':'relay','i':'8383','m':'opt_R'}\"}";

        JSONObject json = JSONObject.parseObject(body);

        String userId = json.containsKey("userId") ? json.getString("userId") : null;
        String alias = json.containsKey("alias") ? json.getString("alias") : null;
        String tags = json.containsKey("tags") ? json.getString("tags") : null;

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
            return ;
        }

        String title = json.getString("title");
        String content = json.getString("content");
        boolean broadcast = json.containsKey("broadcast") ? json.getBooleanValue("broadcast") : false;
        String condition = json.getString("condition");
        String ticker = json.containsKey("ticker") ? json.getString("ticker") : title;

        JSONObject extras = json.containsKey("extras") ? json.getJSONObject("extras") : null;
        Map<String, String> extrasMap = null;
        if(extras!=null){
            Set<Map.Entry<String, Object>> set = extras.entrySet();
            if(set!=null && set.size()>0){
                extrasMap = new HashMap<>();
                for(Map.Entry<String, Object> entry : set){
                    extrasMap.put(entry.getKey(), JSON.toJSONString(entry.getValue()));
                }
            }
        }

        MsgType msgType = json.containsKey("msgType") ? MsgType.getMsgType(json.getIntValue("msgType")) : MsgType.NOTIFICATION_AND_MESSAGE;
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
                .setExtras(extrasMap)
                .setBroadcast(broadcast)
                .setCondition(Strings.isBlank(condition) ? null : condition);
    }
}
