import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

public class App {

    private static final String HTTP_HTML_HEADER = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n";
    private static final String HTTP_RAW_FILE_HEADER = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream;\r\n\r\n";
    private static final String HTML_PREFIX = "<html><body>";
    private static final String HTML_POSTFIX = "</body></html>";
    private static ServerConfig configs;

    public static void main(String[] args) {
        System.out.println("CustomGopherServer.");
        configs = new ServerConfig();

        try {
            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(configs.getServerHost(), configs.getServerPort()));
            serverSocket.configureBlocking(false);

            Selector selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server started on port " + configs.getServerPort());

            while (true) {
                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isValid()) {
                        if (key.isAcceptable())
                            accept(selector, serverSocket);
                        if (key.isReadable())
                            readRequest(key);
                        if (key.isWritable())
                            sendResponse(key);
                    }
                }
                selector.selectedKeys().clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (CancelledKeyException e) {
            e.printStackTrace();
        }
    }

    private static void accept(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        if (configs.isDebug())
            System.out.println("New connect from " + client.getRemoteAddress());
    }

    private static void readRequest(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            client.close();
            return;
        }

        buffer.flip();

        String request = new String(buffer.array(), 0, bytesRead);
        if (configs.isDebug())
            System.out.println("Request: " + request.split("\n")[0]);

        ByteBuffer response;
        try {
            response = generateResponse(request);
        } catch (java.nio.file.FileSystemNotFoundException | java.nio.file.NoSuchFileException e) {
            if (configs.isDebug())
                System.out.println(e.getClass() + ": " + e.getMessage());
            response = ByteBuffer.wrap("3Resource not found.".getBytes());
        }
        if (configs.isDebug())
            System.out.println("Response: \n" + StandardCharsets.UTF_8.decode(response.duplicate()) + "Response.");
        key.attach(response);
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private static void sendResponse(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        if (buffer != null && buffer.hasRemaining()) {
            int sent = client.write(buffer);
            if (configs.isDebug())
                System.out.println("Sent: " + sent + "\nBuf size: " + buffer.capacity());
        } else {
            client.close();
        }
    }

    private static ByteBuffer generateResponse(String request) throws IOException {
        String[] requestByLines = request.split("\n");
        String responseString;
        if (requestByLines.length > 1 && requestByLines[0].contains("HTTP/")) {
            // responseString = HTTP_HTML_HEADER + configs.getServerMessage();
            Path requestPath = Paths.get(
                    configs.getServerHomeDir()
                            + (requestByLines[0].split(" ")[1].startsWith("/") ? requestByLines[0].split(" ")[1]
                                    : "/" + requestByLines[0].split(" ")[1]));
            if (Files.isDirectory(requestPath)) {
                responseString = Files.list(requestPath).map(p -> {
                    p = Paths.get(configs.getServerHomeDir()).relativize(p);
                    return "<a href=\"" + p.toString() + "\">" + p.getFileName() + "</a>";
                }).collect(Collectors.joining("<br>"));
                responseString = HTTP_HTML_HEADER + HTML_PREFIX + responseString + HTML_POSTFIX;
            } else {
                if (Files.size(requestPath) == 0)
                    return ByteBuffer.wrap((HTTP_RAW_FILE_HEADER + " ").getBytes());
                ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                responseStream.write(HTTP_RAW_FILE_HEADER.getBytes());
                responseStream.write(Files.readAllBytes(requestPath));
                return ByteBuffer.wrap(responseStream.toByteArray());
            }
        } else {
            Path requestPath = Paths.get(
                    configs.getServerHomeDir() + (requestByLines[0].trim().startsWith("/") ? requestByLines[0].trim()
                            : "/" + requestByLines[0].trim()));
            if (Files.isDirectory(requestPath)) {
                responseString = Files.list(requestPath)
                        .map(p -> {
                            String prefix = "0";
                            if (Files.isDirectory(p)) {
                                prefix = "1";
                            } else {
                                try {
                                    Optional<String> pType = Optional.ofNullable(Files.probeContentType(p));
                                    if (pType.orElse("").startsWith("text/"))
                                        prefix = "0";
                                    else if (pType.orElse("").contains("x-zip-compressed"))
                                        prefix = "5";
                                    else if (pType.orElse("").equals("image/gif"))
                                        prefix = "g";
                                    else if (pType.orElse("").startsWith("image/"))
                                        prefix = "I";
                                    else if (pType.orElse("").startsWith("application/"))
                                        prefix = "9";
                                } catch (IOException e) {

                                }
                            }
                            p = Paths.get(configs.getServerHomeDir()).relativize(p);
                            String pFileName = p.getFileName().toString();
                            return prefix + pFileName + '\t' + '/' + p.toString().replace('\\', '/') + "\t"
                                    + configs.getServerHost() + "\t" + configs.getServerPort();
                        }).collect(Collectors.joining("\n")) + "\n.";
            } else {
                if (Files.size(requestPath) == 0)
                    return ByteBuffer.wrap(" ".getBytes());
                return ByteBuffer.wrap(Files.readAllBytes(requestPath));
            }
        }
        return ByteBuffer.wrap(responseString.getBytes());
    }
}
