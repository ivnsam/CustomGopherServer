import java.nio.file.Paths;

public class ServerConfig {
    private Boolean isDebug = false;
    private static String serverHost = "0.0.0.0";
    private static int serverPort = 7070;
    private static String serverHostname = "localhost";
    private static String serverMessage = "hello_text_web!";
    private static String serverHomeDir = Paths.get("").toAbsolutePath().toString();
    private static int serverDirMaxDepth = 3;

    public Boolean isDebug() {
        return isDebug;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String host) {
        serverHost = host;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int port) {
        serverPort = port;
    }

    public String getServerHostname() {
        return serverHostname;
    }

    public void setServerHostname(String hostname) {
        serverHostname = hostname;
    }

    public String getServerMessage() {
        return serverMessage;
    }

    public void setServerMessage(String message) {
        serverMessage = message;
    }

    public String getServerHomeDir() {
        return serverHomeDir;
    }

    public void setServerHomeDir(String homeDir) {
        serverHomeDir = Paths.get(homeDir).toAbsolutePath().toString();
    }

    public int getServerDirMaxDepth() {
        return serverDirMaxDepth;
    }

    public void setServerDirMaxDepth(int maxDepth) {
        serverDirMaxDepth = maxDepth;
    }

    ServerConfig() {
        Object isDebugRaw = System.getProperties().get("debug");
        isDebug = isDebugRaw != null && (Boolean.valueOf(isDebugRaw.toString()) || isDebugRaw.toString().isEmpty());
        if (isDebug)
            System.out.println("DEBUG MODE IS HERE!!!");
    }
}
