package com.mobileplatform.creator.mpk;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MPK 应用间通信
 * 用于应用之间的消息传递和事件通知
 */
public class MpkInterAppCommunication {
    private static final String TAG = "MpkInterAppComm";
    
    /**
     * 消息回调接口
     */
    public interface MessageCallback {
        void onMessage(Message message);
    }
    
    /**
     * 事件回调接口
     */
    public interface EventCallback {
        void onEvent(Event event);
    }
    
    /**
     * 连接状态回调接口
     */
    public interface ConnectionCallback {
        void onConnected(String appId);
        void onDisconnected(String appId);
    }
    
    /**
     * 消息类
     */
    public static class Message {
        private String id;              // 消息 ID
        private String from;            // 发送者 ID
        private String to;              // 接收者 ID
        private String type;            // 消息类型
        private JSONObject data;        // 消息数据
        private long timestamp;         // 消息时间戳
        private boolean received;       // 是否已接收
        private long receivedTime;      // 接收时间
        private boolean needsResponse;  // 是否需要回复
        private String responseId;      // 回复 ID
        private Message response;       // 回复消息
        
        /**
         * 构造函数
         */
        public Message() {
            this.id = UUID.randomUUID().toString();
            this.timestamp = System.currentTimeMillis();
            this.received = false;
            this.needsResponse = false;
        }
        
        /**
         * 获取消息 ID
         * 
         * @return 消息 ID
         */
        public String getId() {
            return id;
        }
        
        /**
         * 设置消息 ID
         * 
         * @param id 消息 ID
         */
        public void setId(String id) {
            this.id = id;
        }
        
        /**
         * 获取发送者 ID
         * 
         * @return 发送者 ID
         */
        public String getFrom() {
            return from;
        }
        
        /**
         * 设置发送者 ID
         * 
         * @param from 发送者 ID
         */
        public void setFrom(String from) {
            this.from = from;
        }
        
        /**
         * 获取接收者 ID
         * 
         * @return 接收者 ID
         */
        public String getTo() {
            return to;
        }
        
        /**
         * 设置接收者 ID
         * 
         * @param to 接收者 ID
         */
        public void setTo(String to) {
            this.to = to;
        }
        
        /**
         * 获取消息类型
         * 
         * @return 消息类型
         */
        public String getType() {
            return type;
        }
        
        /**
         * 设置消息类型
         * 
         * @param type 消息类型
         */
        public void setType(String type) {
            this.type = type;
        }
        
        /**
         * 获取消息数据
         * 
         * @return 消息数据
         */
        public JSONObject getData() {
            return data;
        }
        
        /**
         * 设置消息数据
         * 
         * @param data 消息数据
         */
        public void setData(JSONObject data) {
            this.data = data;
        }
        
        /**
         * 获取时间戳
         * 
         * @return 时间戳
         */
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * 是否已接收
         * 
         * @return 是否已接收
         */
        public boolean isReceived() {
            return received;
        }
        
        /**
         * 设置已接收
         * 
         * @param received 是否已接收
         */
        public void setReceived(boolean received) {
            this.received = received;
            if (received) {
                this.receivedTime = System.currentTimeMillis();
            }
        }
        
        /**
         * 获取接收时间
         * 
         * @return 接收时间
         */
        public long getReceivedTime() {
            return receivedTime;
        }
        
        /**
         * 是否需要回复
         * 
         * @return 是否需要回复
         */
        public boolean isNeedsResponse() {
            return needsResponse;
        }
        
        /**
         * 设置是否需要回复
         * 
         * @param needsResponse 是否需要回复
         */
        public void setNeedsResponse(boolean needsResponse) {
            this.needsResponse = needsResponse;
        }
        
        /**
         * 获取回复 ID
         * 
         * @return 回复 ID
         */
        public String getResponseId() {
            return responseId;
        }
        
        /**
         * 设置回复 ID
         * 
         * @param responseId 回复 ID
         */
        public void setResponseId(String responseId) {
            this.responseId = responseId;
        }
        
        /**
         * 获取回复消息
         * 
         * @return 回复消息
         */
        public Message getResponse() {
            return response;
        }
        
        /**
         * 设置回复消息
         * 
         * @param response 回复消息
         */
        public void setResponse(Message response) {
            this.response = response;
        }
        
        /**
         * 创建回复消息
         * 
         * @return 回复消息
         */
        public Message createResponse() {
            Message response = new Message();
            response.setFrom(this.to);
            response.setTo(this.from);
            response.setType(this.type + "_response");
            response.setResponseId(this.id);
            
            return response;
        }
        
        /**
         * 转换为 JSON 对象
         * 
         * @return JSON 对象
         * @throws JSONException 如果转换失败
         */
        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("from", from);
            json.put("to", to);
            json.put("type", type);
            json.put("data", data);
            json.put("timestamp", timestamp);
            json.put("received", received);
            json.put("receivedTime", receivedTime);
            json.put("needsResponse", needsResponse);
            
            if (responseId != null) {
                json.put("responseId", responseId);
            }
            
            return json;
        }
        
        /**
         * 从 JSON 对象创建消息
         * 
         * @param json JSON 对象
         * @return 消息对象
         * @throws JSONException 如果创建失败
         */
        public static Message fromJson(JSONObject json) throws JSONException {
            Message message = new Message();
            
            if (json.has("id")) {
                message.setId(json.getString("id"));
            }
            
            if (json.has("from")) {
                message.setFrom(json.getString("from"));
            }
            
            if (json.has("to")) {
                message.setTo(json.getString("to"));
            }
            
            if (json.has("type")) {
                message.setType(json.getString("type"));
            }
            
            if (json.has("data")) {
                message.setData(json.getJSONObject("data"));
            }
            
            if (json.has("timestamp")) {
                message.timestamp = json.getLong("timestamp");
            }
            
            if (json.has("received")) {
                message.setReceived(json.getBoolean("received"));
            }
            
            if (json.has("receivedTime")) {
                message.receivedTime = json.getLong("receivedTime");
            }
            
            if (json.has("needsResponse")) {
                message.setNeedsResponse(json.getBoolean("needsResponse"));
            }
            
            if (json.has("responseId")) {
                message.setResponseId(json.getString("responseId"));
            }
            
            return message;
        }
        
        @Override
        public String toString() {
            return "Message{" +
                    "id='" + id + '\'' +
                    ", from='" + from + '\'' +
                    ", to='" + to + '\'' +
                    ", type='" + type + '\'' +
                    ", timestamp=" + timestamp +
                    ", received=" + received +
                    '}';
        }
    }
    
    /**
     * 事件类
     */
    public static class Event {
        private String name;           // 事件名
        private String source;         // 事件源
        private JSONObject data;       // 事件数据
        private long timestamp;        // 事件时间戳
        private String topic;          // 事件主题
        private Set<String> tags;      // 事件标签
        
        /**
         * 构造函数
         */
        public Event() {
            this.timestamp = System.currentTimeMillis();
            this.tags = new HashSet<>();
        }
        
        /**
         * 构造函数
         * 
         * @param name 事件名
         * @param source 事件源
         * @param data 事件数据
         */
        public Event(String name, String source, JSONObject data) {
            this();
            this.name = name;
            this.source = source;
            this.data = data;
        }
        
        /**
         * 构造函数
         * 
         * @param name 事件名
         * @param source 事件源
         * @param topic 事件主题
         * @param data 事件数据
         */
        public Event(String name, String source, String topic, JSONObject data) {
            this(name, source, data);
            this.topic = topic;
        }
        
        /**
         * 获取事件名
         * 
         * @return 事件名
         */
        public String getName() {
            return name;
        }
        
        /**
         * 设置事件名
         * 
         * @param name 事件名
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * 获取事件源
         * 
         * @return 事件源
         */
        public String getSource() {
            return source;
        }
        
        /**
         * 设置事件源
         * 
         * @param source 事件源
         */
        public void setSource(String source) {
            this.source = source;
        }
        
        /**
         * 获取事件数据
         * 
         * @return 事件数据
         */
        public JSONObject getData() {
            return data;
        }
        
        /**
         * 设置事件数据
         * 
         * @param data 事件数据
         */
        public void setData(JSONObject data) {
            this.data = data;
        }
        
        /**
         * 获取事件时间戳
         * 
         * @return 事件时间戳
         */
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * 获取事件主题
         * 
         * @return 事件主题
         */
        public String getTopic() {
            return topic;
        }
        
        /**
         * 设置事件主题
         * 
         * @param topic 事件主题
         */
        public void setTopic(String topic) {
            this.topic = topic;
        }
        
        /**
         * 获取事件标签
         * 
         * @return 事件标签
         */
        public Set<String> getTags() {
            return Collections.unmodifiableSet(tags);
        }
        
        /**
         * 添加事件标签
         * 
         * @param tag 事件标签
         */
        public void addTag(String tag) {
            this.tags.add(tag);
        }
        
        /**
         * 移除事件标签
         * 
         * @param tag 事件标签
         * @return 是否成功移除
         */
        public boolean removeTag(String tag) {
            return this.tags.remove(tag);
        }
        
        /**
         * 转换为 JSON 对象
         * 
         * @return JSON 对象
         * @throws JSONException 如果转换失败
         */
        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("source", source);
            json.put("data", data);
            json.put("timestamp", timestamp);
            
            if (topic != null) {
                json.put("topic", topic);
            }
            
            if (!tags.isEmpty()) {
                json.put("tags", new org.json.JSONArray(tags));
            }
            
            return json;
        }
        
        /**
         * 从 JSON 对象创建事件
         * 
         * @param json JSON 对象
         * @return 事件对象
         * @throws JSONException 如果创建失败
         */
        public static Event fromJson(JSONObject json) throws JSONException {
            Event event = new Event();
            
            if (json.has("name")) {
                event.setName(json.getString("name"));
            }
            
            if (json.has("source")) {
                event.setSource(json.getString("source"));
            }
            
            if (json.has("data")) {
                event.setData(json.getJSONObject("data"));
            }
            
            if (json.has("timestamp")) {
                event.timestamp = json.getLong("timestamp");
            }
            
            if (json.has("topic")) {
                event.setTopic(json.getString("topic"));
            }
            
            if (json.has("tags")) {
                org.json.JSONArray tagsArray = json.getJSONArray("tags");
                for (int i = 0; i < tagsArray.length(); i++) {
                    event.addTag(tagsArray.getString(i));
                }
            }
            
            return event;
        }
        
        @Override
        public String toString() {
            return "Event{" +
                    "name='" + name + '\'' +
                    ", source='" + source + '\'' +
                    ", topic='" + topic + '\'' +
                    ", timestamp=" + timestamp +
                    ", tags=" + tags +
                    '}';
        }
    }
    
    // 已注册的应用
    private Set<String> registeredApps;
    
    // 消息回调
    private Map<String, List<MessageCallback>> messageCallbacks;
    
    // 事件回调
    private Map<String, List<EventCallback>> eventCallbacks;
    
    // 主题订阅
    private Map<String, Set<String>> topicSubscriptions;
    
    // 连接回调
    private List<ConnectionCallback> connectionCallbacks;
    
    // 消息历史
    private Map<String, List<Message>> messageHistory;
    
    // 事件历史
    private Map<String, List<Event>> eventHistory;
    
    // 历史记录大小限制
    private static final int HISTORY_SIZE_LIMIT = 100;
    
    // 主线程处理器
    private Handler mainHandler;
    
    /**
     * 构造函数
     */
    public MpkInterAppCommunication() {
        this.registeredApps = new HashSet<>();
        this.messageCallbacks = new HashMap<>();
        this.eventCallbacks = new HashMap<>();
        this.topicSubscriptions = new HashMap<>();
        this.connectionCallbacks = new ArrayList<>();
        this.messageHistory = new HashMap<>();
        this.eventHistory = new HashMap<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 注册应用
     * 
     * @param appId 应用 ID
     * @return 是否成功注册
     */
    public boolean registerApp(String appId) {
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "应用 ID 不能为空");
            return false;
        }
        
        if (registeredApps.contains(appId)) {
            Log.w(TAG, "应用已注册: " + appId);
            return true;
        }
        
        registeredApps.add(appId);
        messageCallbacks.put(appId, new CopyOnWriteArrayList<>());
        messageHistory.put(appId, new ArrayList<>());
        eventHistory.put(appId, new ArrayList<>());
        
        // 通知连接回调
        for (ConnectionCallback callback : connectionCallbacks) {
            mainHandler.post(() -> callback.onConnected(appId));
        }
        
        Log.i(TAG, "应用已注册: " + appId);
        return true;
    }
    
    /**
     * 注销应用
     * 
     * @param appId 应用 ID
     * @return 是否成功注销
     */
    public boolean unregisterApp(String appId) {
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "应用 ID 不能为空");
            return false;
        }
        
        if (!registeredApps.contains(appId)) {
            Log.w(TAG, "应用未注册: " + appId);
            return true;
        }
        
        registeredApps.remove(appId);
        messageCallbacks.remove(appId);
        
        // 移除主题订阅
        for (Map.Entry<String, Set<String>> entry : topicSubscriptions.entrySet()) {
            entry.getValue().remove(appId);
        }
        
        // 清理空主题
        topicSubscriptions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // 通知连接回调
        for (ConnectionCallback callback : connectionCallbacks) {
            mainHandler.post(() -> callback.onDisconnected(appId));
        }
        
        Log.i(TAG, "应用已注销: " + appId);
        return true;
    }
    
    /**
     * 应用是否已注册
     * 
     * @param appId 应用 ID
     * @return 是否已注册
     */
    public boolean isAppRegistered(String appId) {
        return registeredApps.contains(appId);
    }
    
    /**
     * 获取已注册的应用
     * 
     * @return 已注册的应用
     */
    public Set<String> getRegisteredApps() {
        return Collections.unmodifiableSet(registeredApps);
    }
    
    /**
     * 添加消息回调
     * 
     * @param appId 应用 ID
     * @param callback 回调
     * @return 是否成功添加
     */
    public boolean addMessageCallback(String appId, MessageCallback callback) {
        if (!registeredApps.contains(appId)) {
            Log.e(TAG, "应用未注册: " + appId);
            return false;
        }
        
        List<MessageCallback> callbacks = messageCallbacks.get(appId);
        callbacks.add(callback);
        
        Log.d(TAG, "添加消息回调: " + appId);
        return true;
    }
    
    /**
     * 移除消息回调
     * 
     * @param appId 应用 ID
     * @param callback 回调
     * @return 是否成功移除
     */
    public boolean removeMessageCallback(String appId, MessageCallback callback) {
        if (!registeredApps.contains(appId)) {
            Log.e(TAG, "应用未注册: " + appId);
            return false;
        }
        
        List<MessageCallback> callbacks = messageCallbacks.get(appId);
        boolean result = callbacks.remove(callback);
        
        if (result) {
            Log.d(TAG, "移除消息回调: " + appId);
        }
        
        return result;
    }
    
    /**
     * 添加事件回调
     * 
     * @param appId 应用 ID
     * @param callback 回调
     * @return 是否成功添加
     */
    public boolean addEventListener(String appId, EventCallback callback) {
        if (!registeredApps.contains(appId)) {
            Log.e(TAG, "应用未注册: " + appId);
            return false;
        }
        
        List<EventCallback> callbacks = eventCallbacks.computeIfAbsent(appId, k -> new CopyOnWriteArrayList<>());
        callbacks.add(callback);
        
        Log.d(TAG, "添加事件回调: " + appId);
        return true;
    }
    
    /**
     * 移除事件回调
     * 
     * @param appId 应用 ID
     * @param callback 回调
     * @return 是否成功移除
     */
    public boolean removeEventListener(String appId, EventCallback callback) {
        if (!registeredApps.contains(appId)) {
            Log.e(TAG, "应用未注册: " + appId);
            return false;
        }
        
        List<EventCallback> callbacks = eventCallbacks.get(appId);
        if (callbacks == null) {
            return false;
        }
        
        boolean result = callbacks.remove(callback);
        
        if (result) {
            Log.d(TAG, "移除事件回调: " + appId);
            
            if (callbacks.isEmpty()) {
                eventCallbacks.remove(appId);
            }
        }
        
        return result;
    }
    
    /**
     * 订阅主题
     * 
     * @param appId 应用 ID
     * @param topic 主题
     * @return 是否成功订阅
     */
    public boolean subscribeTopic(String appId, String topic) {
        if (!registeredApps.contains(appId)) {
            Log.e(TAG, "应用未注册: " + appId);
            return false;
        }
        
        Set<String> subscribers = topicSubscriptions.computeIfAbsent(topic, k -> new HashSet<>());
        subscribers.add(appId);
        
        Log.d(TAG, "订阅主题: " + appId + " -> " + topic);
        return true;
    }
    
    /**
     * 取消订阅主题
     * 
     * @param appId 应用 ID
     * @param topic 主题
     * @return 是否成功取消订阅
     */
    public boolean unsubscribeTopic(String appId, String topic) {
        if (!registeredApps.contains(appId)) {
            Log.e(TAG, "应用未注册: " + appId);
            return false;
        }
        
        Set<String> subscribers = topicSubscriptions.get(topic);
        if (subscribers == null) {
            return false;
        }
        
        boolean result = subscribers.remove(appId);
        
        if (result) {
            Log.d(TAG, "取消订阅主题: " + appId + " -> " + topic);
            
            if (subscribers.isEmpty()) {
                topicSubscriptions.remove(topic);
            }
        }
        
        return result;
    }
    
    /**
     * 获取主题订阅者
     * 
     * @param topic 主题
     * @return 订阅者
     */
    public Set<String> getTopicSubscribers(String topic) {
        Set<String> subscribers = topicSubscriptions.get(topic);
        return subscribers != null ? Collections.unmodifiableSet(subscribers) : Collections.emptySet();
    }
    
    /**
     * 添加连接回调
     * 
     * @param callback 回调
     */
    public void addConnectionCallback(ConnectionCallback callback) {
        connectionCallbacks.add(callback);
    }
    
    /**
     * 移除连接回调
     * 
     * @param callback 回调
     * @return 是否成功移除
     */
    public boolean removeConnectionCallback(ConnectionCallback callback) {
        return connectionCallbacks.remove(callback);
    }
    
    /**
     * 发送消息
     * 
     * @param message 消息
     * @return 是否成功发送
     */
    public boolean sendMessage(Message message) {
        String from = message.getFrom();
        String to = message.getTo();
        
        if (!registeredApps.contains(from)) {
            Log.e(TAG, "发送者未注册: " + from);
            return false;
        }
        
        if (!registeredApps.contains(to)) {
            Log.e(TAG, "接收者未注册: " + to);
            return false;
        }
        
        // 更新消息历史
        List<Message> fromHistory = messageHistory.get(from);
        if (fromHistory.size() >= HISTORY_SIZE_LIMIT) {
            fromHistory.remove(0);
        }
        fromHistory.add(message);
        
        List<Message> toHistory = messageHistory.get(to);
        if (toHistory.size() >= HISTORY_SIZE_LIMIT) {
            toHistory.remove(0);
        }
        toHistory.add(message);
        
        // 通知接收者
        List<MessageCallback> callbacks = messageCallbacks.get(to);
        if (callbacks != null && !callbacks.isEmpty()) {
            // 标记消息为已接收
            message.setReceived(true);
            
            // 在主线程中回调
            mainHandler.post(() -> {
                for (MessageCallback callback : callbacks) {
                    try {
                        callback.onMessage(message);
                    } catch (Exception e) {
                        Log.e(TAG, "消息回调异常", e);
                    }
                }
            });
        }
        
        Log.d(TAG, "发送消息: " + from + " -> " + to + ", type=" + message.getType());
        return true;
    }
    
    /**
     * 发送消息
     * 
     * @param from 发送者 ID
     * @param to 接收者 ID
     * @param type 消息类型
     * @param data 消息数据
     * @return 是否发送成功
     */
    public boolean sendMessage(String from, String to, String type, JSONObject data) {
        Message message = new Message();
        message.setFrom(from);
        message.setTo(to);
        message.setType(type);
        message.setData(data);
        
        return sendMessage(message);
    }
    
    /**
     * 广播消息
     * 
     * @param from 发送者 ID
     * @param type 消息类型
     * @param data 消息数据
     * @return 发送的消息数量
     */
    public int broadcastMessage(String from, String type, JSONObject data) {
        if (!registeredApps.contains(from)) {
            Log.e(TAG, "发送者未注册: " + from);
            return 0;
        }
        
        int count = 0;
        for (String appId : registeredApps) {
            if (!appId.equals(from)) {
                Message message = new Message();
                message.setFrom(from);
                message.setTo(appId);
                message.setType(type);
                message.setData(data);
                
                if (sendMessage(message)) {
                    count++;
                }
            }
        }
        
        Log.d(TAG, "广播消息: " + from + ", type=" + type + ", count=" + count);
        return count;
    }
    
    /**
     * 发布事件
     * 
     * @param event 事件
     * @return 是否成功发布
     */
    public boolean publishEvent(Event event) {
        String source = event.getSource();
        String topic = event.getTopic();
        
        if (!registeredApps.contains(source)) {
            Log.e(TAG, "事件源未注册: " + source);
            return false;
        }
        
        // 更新事件历史
        List<Event> sourceHistory = eventHistory.get(source);
        if (sourceHistory.size() >= HISTORY_SIZE_LIMIT) {
            sourceHistory.remove(0);
        }
        sourceHistory.add(event);
        
        // 如果有主题，通知订阅者
        if (topic != null && !topic.isEmpty()) {
            Set<String> subscribers = topicSubscriptions.get(topic);
            if (subscribers != null && !subscribers.isEmpty()) {
                for (String appId : subscribers) {
                    if (!appId.equals(source)) {
                        // 更新事件历史
                        List<Event> subscriberHistory = eventHistory.get(appId);
                        if (subscriberHistory != null) {
                            if (subscriberHistory.size() >= HISTORY_SIZE_LIMIT) {
                                subscriberHistory.remove(0);
                            }
                            subscriberHistory.add(event);
                        }
                        
                        // 通知订阅者
                        List<EventCallback> callbacks = eventCallbacks.get(appId);
                        if (callbacks != null && !callbacks.isEmpty()) {
                            final String finalAppId = appId;
                            mainHandler.post(() -> {
                                for (EventCallback callback : callbacks) {
                                    try {
                                        callback.onEvent(event);
                                    } catch (Exception e) {
                                        Log.e(TAG, "事件回调异常: " + finalAppId, e);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }
        
        Log.d(TAG, "发布事件: " + source + ", name=" + event.getName() + ", topic=" + topic);
        return true;
    }
    
    /**
     * 发布事件
     * 
     * @param source 事件源
     * @param name 事件名
     * @param data 事件数据
     * @return 事件对象，如果发布失败则返回 null
     */
    public Event publishEvent(String source, String name, JSONObject data) {
        Event event = new Event(name, source, data);
        
        if (publishEvent(event)) {
            return event;
        } else {
            return null;
        }
    }
    
    /**
     * 发布主题事件
     * 
     * @param source 事件源
     * @param name 事件名
     * @param topic 事件主题
     * @param data 事件数据
     * @return 事件对象，如果发布失败则返回 null
     */
    public Event publishEvent(String source, String name, String topic, JSONObject data) {
        Event event = new Event(name, source, topic, data);
        
        if (publishEvent(event)) {
            return event;
        } else {
            return null;
        }
    }
    
    /**
     * 获取应用的消息历史
     * 
     * @param appId 应用 ID
     * @return 消息历史
     */
    public List<Message> getMessageHistory(String appId) {
        if (!registeredApps.contains(appId)) {
            Log.e(TAG, "应用未注册: " + appId);
            return Collections.emptyList();
        }
        
        List<Message> history = messageHistory.get(appId);
        return Collections.unmodifiableList(history);
    }
    
    /**
     * 获取应用的事件历史
     * 
     * @param appId 应用 ID
     * @return 事件历史
     */
    public List<Event> getEventHistory(String appId) {
        if (!registeredApps.contains(appId)) {
            Log.e(TAG, "应用未注册: " + appId);
            return Collections.emptyList();
        }
        
        List<Event> history = eventHistory.get(appId);
        return Collections.unmodifiableList(history);
    }
    
    /**
     * 清理应用的消息历史
     * 
     * @param appId 应用 ID
     * @return 是否成功清理
     */
    public boolean clearMessageHistory(String appId) {
        if (!registeredApps.contains(appId)) {
            Log.e(TAG, "应用未注册: " + appId);
            return false;
        }
        
        List<Message> history = messageHistory.get(appId);
        history.clear();
        
        Log.d(TAG, "清理消息历史: " + appId);
        return true;
    }
    
    /**
     * 清理应用的事件历史
     * 
     * @param appId 应用 ID
     * @return 是否成功清理
     */
    public boolean clearEventHistory(String appId) {
        if (!registeredApps.contains(appId)) {
            Log.e(TAG, "应用未注册: " + appId);
            return false;
        }
        
        List<Event> history = eventHistory.get(appId);
        history.clear();
        
        Log.d(TAG, "清理事件历史: " + appId);
        return true;
    }
    
    /**
     * 关闭应用间通信
     */
    public void shutdown() {
        registeredApps.clear();
        messageCallbacks.clear();
        eventCallbacks.clear();
        topicSubscriptions.clear();
        connectionCallbacks.clear();
        messageHistory.clear();
        eventHistory.clear();
        
        Log.i(TAG, "应用间通信已关闭");
    }
} 