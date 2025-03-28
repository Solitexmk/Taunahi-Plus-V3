package net.taunahi.ezskyblockscripts.feature.impl;

import net.taunahi.ezskyblockscripts.Taunahi;
import net.taunahi.ezskyblockscripts.config.TaunahiConfig;
import io.netty.bootstrap.ChannelFactory;
import io.netty.channel.socket.oio.OioSocketChannel;

import java.net.*;
import java.util.Objects;

public class Proxy {
    private static Proxy instance;

    public static Proxy getInstance() {
        if (instance == null) {
            instance = new Proxy();
        }
        return instance;
    }

    public enum ProxyType {
        SOCKS,
        HTTP,
    }

    public void setProxy(boolean enabled, String host, ProxyType type, String username, String password) {
        TaunahiConfig.proxyEnabled = enabled;
        TaunahiConfig.proxyAddress = host;
        TaunahiConfig.proxyType = type;
        if (!Objects.equals(username, "Username"))
            TaunahiConfig.proxyUsername = username;
        if (!Objects.equals(password, "Password"))
            TaunahiConfig.proxyPassword = password;
        Taunahi.config.save();
    }

    public java.net.Proxy getProxy() {
        if (!TaunahiConfig.proxyEnabled) return null;
        String addressStr = TaunahiConfig.proxyAddress.split(":")[0];
        int port = Integer.parseInt(TaunahiConfig.proxyAddress.split(":")[1]);
        InetSocketAddress address;
        try {
            address = new InetSocketAddress(InetAddress.getByName(addressStr), port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }

        if (TaunahiConfig.proxyUsername != null && TaunahiConfig.proxyPassword != null)
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(TaunahiConfig.proxyUsername, TaunahiConfig.proxyPassword.toCharArray());
                }
            });

        switch (TaunahiConfig.proxyType) {
            case SOCKS:
                return new java.net.Proxy(java.net.Proxy.Type.SOCKS, address);
            case HTTP:
                return new java.net.Proxy(java.net.Proxy.Type.HTTP, address);
            default:
                return null;
        }
    }

    public static class ProxyOioChannelFactory implements ChannelFactory<OioSocketChannel> {

        private final java.net.Proxy proxy;

        public ProxyOioChannelFactory(java.net.Proxy proxy) {
            this.proxy = proxy;
        }

        @Override
        public OioSocketChannel newChannel() {
            return new OioSocketChannel(new Socket(proxy));
        }
    }
}
