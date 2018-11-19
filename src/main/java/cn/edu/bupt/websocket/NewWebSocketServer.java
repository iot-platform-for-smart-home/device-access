package cn.edu.bupt.websocket;

import cn.edu.bupt.dao.page.TextPageData;
import cn.edu.bupt.dao.page.TextPageLink;
import cn.edu.bupt.pojo.Device;
import cn.edu.bupt.service.DeviceService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.org.apache.xpath.internal.operations.String;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
@ServerEndpoint(value = "/api/v1/deviceaccess/websocket/device")
@Component
public class NewWebSocketServer{
    @Autowired
    private DeviceService deviceService;

    private static NewWebSocketServer newWebSocketServer ;

    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static Integer onlineCount = 0;

    public static Map<Integer,Set<Session>>customerSessionMap = new ConcurrentHashMap<>();  // 以上都是单例成员

    public List<Integer> customerIdList = new ArrayList<>();

    private static Logger logger = LoggerFactory.getLogger(NewWebSocketServer.class);

    private Session session;

    public static synchronized NewWebSocketServer getInstance(){
        if(newWebSocketServer == null){
            return new NewWebSocketServer();
        }
        return newWebSocketServer;
    }

    /**
     * 建立连接
     * @param session
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        addOnlineCount();           //在线数加1
        log.info("有新连接加入！当前在线人数为" + getOnlineCount());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        for(Integer customerId: customerIdList)
            customerSessionMap.get(customerId).remove(this.session);
        subOnlineCount();           //在线数减1
        log.info("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /***
     *
     * @param message 客户端发送过来的消息
     * @param session
     */
    @OnMessage
    public void onMessage(java.lang.String message, Session session)throws Exception{
        logger.info("客户端消息: " + message);

        // 获取 customerId
        JsonObject jsonObj = (JsonObject)new JsonParser().parse(message);
        JsonArray customerIdArray = jsonObj.get("customerId").getAsJsonArray();
        for(int i = 0; i < customerIdArray.size(); i++){
            customerIdList.add(customerIdArray.get(i).getAsInt());
        }

        // 查数据库设备信息
        List<TextPageData<Device>> deviceInfoList = getAllDeviceInfo(customerIdList);
        sendMessage(deviceInfoList.toArray().toString(), session);

        // 保存 Session
        for(Integer customerId: customerIdList){
            if(getInstance().customerSessionMap.containsKey(customerId)){
                getInstance().customerSessionMap.get(customerId).add(session);
            } else {
                Set<Session> s = new HashSet<>();
                s.add(this.session);
                getInstance().customerSessionMap.put(customerId,s);
            }
        }
    }

    /**
     * 查找数据库中设备的最新信息
     * @param customerIdList
     * @return
     */
    public List<TextPageData<Device>> getAllDeviceInfo(List<Integer> customerIdList) throws Exception{
        List<TextPageData<Device>> deviceInfoList = new ArrayList<>();
        for (Integer customerId : customerIdList) {
            TextPageData<Device> deviceInfo = getInstance().deviceService.findDevicesByTenantIdAndCustomerId(2, customerId, new TextPageLink(1000));
            deviceInfoList.add(deviceInfo);
        }
        return deviceInfoList;
    }

    public void sendMessage(java.lang.String message, Session session) throws IOException {
        System.out.println(message);
        session.getBasicRemote().sendText(message);
    }

    public static synchronized int getOnlineCount() {
        return getInstance().onlineCount;
    }

    public static synchronized void addOnlineCount() {
        getInstance().onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        getInstance().onlineCount--;
    }
}
