package com.penguinehis.ultrasshservice.tunnel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import android.annotation.SuppressLint;
import android.content.Context;

import com.penguinehis.ultrasshservice.config.Settings;
import com.penguinehis.ultrasshservice.tunnel.vpn.SocketProtector;

import org.conscrypt.Conscrypt;

import java.security.SecureRandom;
import java.util.Objects;


public class TLSSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory internalSSLSocketFactory;



    static {
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1);

        } catch (NoClassDefFoundError e) {
            e.printStackTrace();
        }}

    public SSLContext sslctx;
    private Settings mConfig;
    public TLSSocketFactory(Context context) throws KeyManagementException, NoSuchAlgorithmException {
        mConfig = new Settings(context);
        // For easier debugging purpose, trust all certificates

        //dsp = ApplicationBase.getDefSharedPreferences();
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @SuppressLint({"TrustAllX509TrustManager"})
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @SuppressLint({"TrustAllX509TrustManager"})
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
        sslctx = SSLContext.getInstance("TLS");
        sslctx.init(null, trustAllCerts, new SecureRandom());
        internalSSLSocketFactory = sslctx.getSocketFactory();

    }

    @Override
    public String[] getDefaultCipherSuites() {
        return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		Socket socket = internalSSLSocketFactory.createSocket();
		SocketProtector.protect(socket);
		socket.connect(new InetSocketAddress(host, port));
		return enableTLSOnSocket(socket);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
		Socket socket = internalSSLSocketFactory.createSocket();
		socket.bind(new InetSocketAddress(localHost, localPort));
		SocketProtector.protect(socket);
		socket.connect(new InetSocketAddress(host, port));
		return enableTLSOnSocket(socket);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
		Socket socket = internalSSLSocketFactory.createSocket();
		SocketProtector.protect(socket);
		socket.connect(new InetSocketAddress(host, port));
		return enableTLSOnSocket(socket);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
		Socket socket = internalSSLSocketFactory.createSocket();
		socket.bind(new InetSocketAddress(localAddress, localPort));
		SocketProtector.protect(socket);
		socket.connect(new InetSocketAddress(address, port));
		return enableTLSOnSocket(socket);
    }

    private Socket enableTLSOnSocket(Socket socket) {

        if (socket instanceof SSLSocket) {
            if (Objects.equals(mConfig.getPrivString(Settings.TLS12), "1")){
                ((SSLSocket) socket).setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"});

            }else{
                ((SSLSocket) socket).setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"});
            }

           // ((SSLSocket) socket).setEnabledProtocols(((SSLSocket) socket).getSupportedProtocols());

        }
        return socket;
    }

}