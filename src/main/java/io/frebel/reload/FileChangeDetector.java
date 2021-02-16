package io.frebel.reload;

import io.frebel.ClassInner;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileChangeDetector {
    private String pathToListen;
    private long interval;
    private Map<String, List<FileEntry>> classFileMap = new ConcurrentHashMap<>();

    public FileChangeDetector(String pathToListen) {
        this.pathToListen = pathToListen;
    }

    public void start() {
        try {
            final WatchService watchService = FileSystems.getDefault().newWatchService();
            final Path path = Paths.get(pathToListen);
            Files.walkFileTree(path, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toFile().isDirectory() || !file.toFile().getName().endsWith(".class")) {
                        return FileVisitResult.CONTINUE;
                    }
                    String parentPath = file.toAbsolutePath().getParent().toString();
                    List<FileEntry> fileSet = classFileMap.get(parentPath);
                    if (fileSet == null) {
                        classFileMap.put(parentPath, new ArrayList<FileEntry>());
                        fileSet = classFileMap.get(parentPath);
                    }
                    fileSet.add(new FileEntry(file));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
            path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            WatchKey watchKey = watchService.take();
                            List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
                            for (WatchEvent<?> watchEvent : watchEvents) {
                                System.out.println("[" + path.toAbsolutePath() + "/" + watchEvent.context() + "] fire [" + watchEvent.kind() + "] event");
                                String parentPath = path.toAbsolutePath() + "/" + watchEvent.context();
                                List<FileEntry> oldFileSet = classFileMap.get(parentPath);
                                String[] newFileArr = Paths.get(parentPath).toFile().list();
                                if (newFileArr == null) {
                                    classFileMap.put(parentPath, new ArrayList<FileEntry>());
                                } else {
                                    // modified
                                    List<FileEntry> modified = new ArrayList<>();
                                    List<FileEntry> created = new ArrayList<>();
                                    List<FileEntry> newFileSet = new ArrayList<>();
                                    for (String newFile : newFileArr) {
                                        Path file = Paths.get(parentPath + "/" + newFile);
                                        FileEntry newFileEntry = new FileEntry(file);
                                        newFileSet.add(newFileEntry);
                                        boolean oldFinded = false;
                                        for (FileEntry oldFileEntry : oldFileSet) {
                                            if (Objects.equals(oldFileEntry.fileName, newFile)) {
                                                oldFinded = true;
                                            }
                                            if (Objects.equals(oldFileEntry.fileName, newFile)
                                                    && !Objects.equals(oldFileEntry.md5, newFileEntry.md5)) {
                                                System.out.println("file: " + file.toAbsolutePath() + " has changed," +
                                                        "oldMd5：" + oldFileEntry.md5 + ",newMd5:" + newFileEntry.md5);
                                                modified.add(newFileEntry);
                                            }
                                        }

                                        if (!oldFinded) {
                                            // 在旧文件记录中没有找到，是新增的文件
                                            created.add(newFileEntry);
                                        }
                                    }

                                    classFileMap.put(parentPath, newFileSet);
                                    ClassInner[] modifiedClassInners = new ClassInner[modified.size()];
                                    for (int i = 0; i < modified.size(); i++) {
                                        modifiedClassInners[i] = new ClassInner(Files.readAllBytes(Paths.get(parentPath + "/" + modified.get(i).fileName)));
                                    }
                                    for (FileEntry fileEntry : created) {
                                        ReloadManager.INSTANCE.loadNewClass(new ClassInner(Files.readAllBytes(Paths.get(parentPath + "/" + fileEntry.fileName))));
                                    }
                                    ReloadManager.INSTANCE.batchReload(modifiedClassInners);
                                }
                            }
                            watchKey.reset();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        watchService.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class FileEntry {
        private String fileName;
        private String md5;

        public FileEntry(Path file) {
            fileName = file.toFile().getName();
            md5 = getMD5Two(file);
        }



        public String getMD5Two(Path path) {
            StringBuffer sb = new StringBuffer("");
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(FileUtils.readFileToByteArray(path.toFile()));
                byte b[] = md.digest();
                int d;
                for (int i = 0; i < b.length; i++) {
                    d = b[i];
                    if (d < 0) {
                        d = b[i] & 0xff;
                        // 与上一行效果等同
                        // i += 256;
                    }
                    if (d < 16)
                        sb.append("0");
                    sb.append(Integer.toHexString(d));
                }
            } catch (NoSuchAlgorithmException | IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }
    }
}
