package frontend;


import base.GameMechanic;
import base.MessageSystem;
import base.UserData;
import base.WebSocket;
import dbService.UserDataSet;
import gameClasses.Field;
import gameClasses.Snapshot;
import gameClasses.Stroke;
import gameMechanic.GameMechanicImpl;
import messageSystem.MessageSystemImpl;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.json.simple.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class WebSocketImplTest {

    public static final String SESSION_ID = "sessionId";
    public static final String START_SERVER_TIME = "startServerTime";
    public static final String FAKE_MESSAGE = "FAKE_MESSAGE";
    public static final String FAKE_SESSION_ID = "fake_sessionId";
    public static final String FAKE_NICK_NAME = "Fake_Nick_Name";
    public static final int FAKE_USER_ID = 100;

    private WebSocketImpl webSocket;
    private GameMechanic gameMechanic;
    private MessageSystem messageSystem;

    @BeforeMethod
    public void setUp() throws Exception {
        messageSystem = new MessageSystemImpl();

        gameMechanic = new GameMechanicImpl(messageSystem);
        webSocket = spy(new WebSocketImpl(messageSystem));
    }

    @Test
    public void testIsNotConnected() throws Exception {
        webSocket.onWebSocketText(FAKE_MESSAGE);
        when(webSocket.isNotConnected()).thenReturn(true);
    }

    //{"sessionId":"d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","startServerTime":"8fd40bd9930a6d2e4e2c606ea5380ad1b81665c6eb21d57975f1585bf87e1225"}
    //{"sessionId":"6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b","startServerTime":"8fd40bd9930a6d2e4e2c606ea5380ad1b81665c6eb21d57975f1585bf87e1225"}
    @Test
    public void testSendInitializeMessage() throws Exception {
        when(webSocket.isNotConnected()).thenReturn(false);

        String serverTime = UserDataImpl.getStartServerTime();
        webSocket.onWebSocketText(getMessage(FAKE_SESSION_ID, serverTime)); //userSession == null

        UserDataImpl.putLogInUser(FAKE_SESSION_ID, new UserDataSet());

        Session session = mock(Session.class);
        RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);
        when(webSocket.getSession()).thenReturn(session);
        when(session.getRemote()).thenReturn(remoteEndpoint);

        webSocket.onWebSocketText(getMessage(FAKE_SESSION_ID, serverTime)); // userSession != null
        Assert.assertEquals(remoteEndpoint, UserDataImpl.getWSBySessionId(FAKE_SESSION_ID));
    }

    //{"from_x":4,"from_y":5,"to_x":5,"to_y":4,"status":"","sessionId":"6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b","startServerTime":"caf09bef4f44531a3bedb4c2780696376fb1bf0668e81e3bfae7732391dd77a2"}
    @Test
    public void testSendLocation() throws Exception {
        when(webSocket.isNotConnected()).thenReturn(false);

        UserDataImpl.putLogInUser(FAKE_SESSION_ID, new UserDataSet());
        String serverTime = UserDataImpl.getStartServerTime();
        webSocket.onWebSocketText(getMessage(FAKE_SESSION_ID, serverTime, 4, 5, 5, 4, ""));
        Assert.assertTrue(messageSystem.getMessages().get(gameMechanic.getAddress()).size() > 0);
    }

    @Test
    public void testSendStroke() throws Exception {
        UserDataImpl.putLogInUser(FAKE_SESSION_ID, new UserDataSet(FAKE_USER_ID, FAKE_NICK_NAME, 0, 0, 0));

        Session session = mock(Session.class);
        RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);
        when(webSocket.getSession()).thenReturn(session);
        when(session.getRemote()).thenReturn(remoteEndpoint);

        UserDataImpl.putSessionIdAndWS(FAKE_SESSION_ID, webSocket);
        webSocket.sendStroke(getUsersStroke());
        verify(remoteEndpoint, times(1)).sendString("{\"to_y\":5,\"status\":\"\",\"to_x\":4,\"color\":null,\"next\":0,\"from_y\":4,\"from_x\":5}");
    }

    @Test
    public void testUpdateUserColor() throws Exception {
        Session session = mock(Session.class);
        RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);
        when(webSocket.getSession()).thenReturn(session);
        when(session.getRemote()).thenReturn(remoteEndpoint);

        UserDataImpl.putSessionIdAndWS(FAKE_SESSION_ID, webSocket);
        UserDataImpl.putLogInUser(FAKE_SESSION_ID, new UserDataSet(FAKE_USER_ID, FAKE_NICK_NAME, 0, 0, 0));

        webSocket.updateUsersColor(getSessionIdToColor(WebSocketImpl.COLOR_WHITE));
        verify(remoteEndpoint, times(1)).sendString(WebSocketImpl.JSON_COLOR_WHITE);

        webSocket.updateUsersColor(getSessionIdToColor(WebSocketImpl.COLOR_BLACK));
        verify(remoteEndpoint, times(1)).sendString(WebSocketImpl.JSON_COLOR_BLACK);
    }

    @Test
    public void testDoneSnapshot() throws Exception {
        Session session = mock(Session.class);
        RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);
        when(webSocket.getSession()).thenReturn(session);
        when(session.getRemote()).thenReturn(remoteEndpoint);

        UserDataImpl.putLogInUser(FAKE_SESSION_ID, new UserDataSet(FAKE_USER_ID, FAKE_NICK_NAME, 0, 0, 0));

        webSocket.doneSnapshot(FAKE_USER_ID, new Snapshot(new Field[10][10], 'w', 10, 'b'));
        verify(remoteEndpoint, times(1)).sendString(WebSocketImpl.JSON_COLOR_BLACK);
    }

    public Map<String, String> getSessionIdToColor(String color) {
        Map<String, String> sessionIdToColor = new HashMap<String, String>();
        sessionIdToColor.put(FAKE_SESSION_ID, color);
        return sessionIdToColor;
    }

    public Map<Integer, Stroke> getUsersStroke() {
        Map<Integer, Stroke> strokeMap = new HashMap<Integer, Stroke>();
        strokeMap.put(FAKE_USER_ID, new Stroke(4, 5, 5, 4, ""));
        return strokeMap;
    }

    public String getMessage(String sessionId, String serverTime) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(SESSION_ID, sessionId);
        jsonObject.put(START_SERVER_TIME, serverTime);
        return jsonObject.toJSONString();
    }

   public String getMessage(String sessionId, String serverTime, int fromX, int fromY, int toX, int toY, String status) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(SESSION_ID, sessionId);
        jsonObject.put(START_SERVER_TIME, serverTime);
        jsonObject.put(WebSocketImpl.FROM_X, fromX);
        jsonObject.put(WebSocketImpl.FROM_Y, fromY);
        jsonObject.put(WebSocketImpl.TO_X, toX);
        jsonObject.put(WebSocketImpl.TO_Y, toY);
        jsonObject.put(WebSocketImpl.STATUS, status);
        return jsonObject.toJSONString();
    }
}