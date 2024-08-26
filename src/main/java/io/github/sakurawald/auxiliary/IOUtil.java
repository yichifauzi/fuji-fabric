package io.github.sakurawald.auxiliary;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class IOUtil {

    @SneakyThrows
    public static void compressFiles(@NotNull List<File> input, @NotNull File output) {
        if (!output.exists()) {
            output.createNewFile();
        }

        try (FileOutputStream fos = new FileOutputStream(output);
             ArchiveOutputStream<ZipArchiveEntry> archiveOut = new ZipArchiveOutputStream(fos)) {
            for (File file : input) {
                if (file.isFile() && file.exists()) {
                    ZipArchiveEntry entry = new ZipArchiveEntry(file, getEntryName(file));
                    archiveOut.putArchiveEntry(entry);
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            archiveOut.write(buffer, 0, len);
                        }
                    }
                    archiveOut.closeArchiveEntry();
                }
            }
        }
    }

    private static @NotNull String getEntryName(@NotNull File file) {
        return file.getParentFile().getName() + File.separator + file.getName();
    }

    public static @NotNull List<Path> getLatestFiles(@NotNull Path path) {
        try (Stream<Path> files = Files.list(path)) {
            return files
                    .filter(Files::isRegularFile)
                    .sorted((o1, o2) -> {
                        try {
                            FileTime t1 = Files.readAttributes(o1, BasicFileAttributes.class).creationTime();
                            FileTime t2 = Files.readAttributes(o2, BasicFileAttributes.class).creationTime();
                            return t1.compareTo(t2);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String post(@NotNull URI uri, @NotNull String param) throws IOException {
        LogUtil.debug("post() -> uri = {}, param = {}", uri, param);

        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Fuji");
        connection.setDoOutput(true);
        connection.setDoInput(true);

        IOUtils.write(param.getBytes(StandardCharsets.UTF_8), connection.getOutputStream());
        return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
    }

    public static String get(@NotNull URI uri) throws IOException {
        LogUtil.debug("get() -> uri = {}", uri);

        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);

        return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
    }
}