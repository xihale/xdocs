package top.xihale.xdocs.websocket;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * WebSocket Configurator，在握手阶段将 Origin header 存入 userProperties，
 * 供 BaseWebSocket.checkOrigin() 校验。
 */
public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

    public static final String ORIGIN_KEY = "ws.origin";

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        String origin = null;
        if (request.getHeaders().containsKey("origin")) {
            var origins = request.getHeaders().get("origin");
            if (!origins.isEmpty()) {
                origin = origins.get(0);
            }
        }
        if (origin != null) {
            sec.getUserProperties().put(ORIGIN_KEY, origin);
        }
    }
}
