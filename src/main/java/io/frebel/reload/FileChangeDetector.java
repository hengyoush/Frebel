package io.frebel.reload;

import io.frebel.ClassInner;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileChangeDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileChangeDetector.class);

    private final String pathToListen;
    private final Map<String, List<FileEntry>> classFileMap = new ConcurrentHashMap<>();

    public FileChangeDetector(String pathToListen) {
        this.pathToListen = pathToListen;
        LOGGER.debug("FileChangeDetector pathToListen is initialized to {}.", pathToListen);
    }

    public void start() {
        try {
            final WatchService watchService = FileSystems.getDefault().newWatchService();
            final Path path = Paths.get(pathToListen);
            LOGGER.debug("FileChangeDetector starts walking file tree to initialize classFileMap.");
            Files.walkFileTree(path, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toFile().isDirectory() || !file.toFile().getName().endsWith(".class")) {
                        return FileVisitResult.CONTINUE;
                    }
                    String parentPath = file.toAbsolutePath().getParent().toString();
                    List<FileEntry> fileSet = classFileMap.get(parentPath);
                    if (fileSet == null) {
                        classFileMap.put(parentPath, new ArrayList<>());
                        fileSet = classFileMap.get(parentPath);
                    }
                    fileSet.add(new FileEntry(file));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("FileChangeDetector walk file tree finished.");
                LOGGER.debug("FileChangeDetector classFileMap is: {}.", classFileMap);
                LOGGER.debug("FileChangeDetector starts thread to watch the directory.");
            }
            path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        WatchKey watchKey = watchService.take();
                        List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
                        LOGGER.info("FileChangeDetector has detected file change events, events num: {}", watchEvents.size());
                        for (WatchEvent<?> watchEvent : watchEvents) {
                            LOGGER.info("[" + path.toAbsolutePath() + "/" + watchEvent.context() + "] fire [" + watchEvent.kind() + "] event");
                            String parentPath = path.toAbsolutePath() + "/" + watchEvent.context();
                            List<FileEntry> oldFileSet = classFileMap.get(parentPath);
                            String[] newFileArr = Paths.get(parentPath).toFile().list();
                            if (newFileArr == null) {
                                classFileMap.put(parentPath, new ArrayList<>());
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
                                            LOGGER.info("file: " + file.toAbsolutePath() + " has changed," +
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
                                LOGGER.info("Modified classes is: {}.", Arrays.stream(modifiedClassInners).map(ClassInner::getOriginClassName).collect(Collectors.toList()));
                                LOGGER.info("New classes is: {}.", created.stream().map(FileEntry::getFileName).collect(Collectors.toList()));
                                for (FileEntry fileEntry : created) {
                                    ReloadManager.INSTANCE.loadNewClass(new ClassInner(Files.readAllBytes(Paths.get(parentPath + "/" + fileEntry.fileName))));
                                }
                                ReloadManager.INSTANCE.batchReload(modifiedClassInners);
                                LOGGER.info("Reload finished.");
                            }
                        }
                        watchKey.reset();
                        LOGGER.info("FileChangeDetector continues to listen.");
                    } catch (ClosedWatchServiceException e) {
                        LOGGER.warn("FileChangeDetector file watch service has closed!");
                        break;
                    } catch (Exception e) {
                        LOGGER.error("FileChangeDetector handle file change events encounters error.", e);
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    watchService.close();
                } catch (IOException e) {
                    LOGGER.error("FileChangeDetector fails to close watch service.", e);
                }
            }));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private static class FileEntry {
        private final String fileName;
        private final String md5;

        public FileEntry(Path file) {
            fileName = file.toFile().getName();
            md5 = getMD5Two(file);
        }



        public String getMD5Two(Path path) {
            StringBuilder sb = new StringBuilder();
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(FileUtils.readFileToByteArray(path.toFile()));
                byte[] b = md.digest();
                int d;
                for (byte value : b) {
                    d = value;
                    if (d < 0) {
                        d = value & 0xff;
                        // 与上一行效果等同
                        // i += 256;
                    }
                    if (d < 16)
                        sb.append("0");
                    sb.append(Integer.toHexString(d));
                }
            } catch (NoSuchAlgorithmException | IOException e) {
                LOGGER.error("Calculate file's md5 encounters error.", e);
            }
            return sb.toString();
        }

        public String getFileName() {
            return fileName;
        }
    }
}
