package top.saltwood.everythingAtHome;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

public class FileWatcher {
    private final String FILE_PATH = "handshake"; // 这里替换为你的文件路径
    String handshakeString = "";
    private long lastModifiedTime = 0;

    public FileWatcher() {
        File file = new File(FILE_PATH);

        // 初始化文件的最后修改时间
        lastModifiedTime = file.lastModified();

        // 创建一个新的线程来监控文件变化
        new Thread(() -> {
            while (true) {
                try {
                    // 休眠一段时间，检查文件是否更改
                    Thread.sleep(5000); // 每5秒检查一次

                    // 检查文件是否被更改
                    long currentModifiedTime = file.lastModified();
                    if (currentModifiedTime != lastModifiedTime) {
                        lastModifiedTime = currentModifiedTime;

                        // 读取文件内容并更新 handshakeString
                        readFileContent();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace(System.out);
                }
            }
        }).start();
    }

    private void readFileContent() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            synchronized (handshakeString) {
                handshakeString = content.toString();
            }
            System.out.println("File changed. Updated handshakeString:");
            System.out.println(handshakeString);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
}
