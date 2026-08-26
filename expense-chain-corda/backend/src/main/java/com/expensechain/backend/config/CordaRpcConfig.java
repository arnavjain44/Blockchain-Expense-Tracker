package com.expensechain.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "corda")
public class CordaRpcConfig {

    private Map<String, NodeProperties> nodes = new HashMap<>();
    private NotaryProperties notary = new NotaryProperties();

    public static class NodeProperties {
        private String host = "localhost";
        private int rpcPort;
        private String username = "user1";
        private String password = "test";
        private String x500Name;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getRpcPort() { return rpcPort; }
        public void setRpcPort(int rpcPort) { this.rpcPort = rpcPort; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getX500Name() { return x500Name; }
        public void setX500Name(String x500Name) { this.x500Name = x500Name; }
    }

    public static class NotaryProperties {
        private String x500Name = "O=Notary,L=London,C=GB";

        public String getX500Name() { return x500Name; }
        public void setX500Name(String x500Name) { this.x500Name = x500Name; }
    }

    public Map<String, NodeProperties> getNodes() { return nodes; }
    public void setNodes(Map<String, NodeProperties> nodes) { this.nodes = nodes; }

    public NotaryProperties getNotary() { return notary; }
    public void setNotary(NotaryProperties notary) { this.notary = notary; }
}
