package vn.dangquangdat.javabegin.learning.day05;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Ngay 5: checked/unchecked exception, Optional, java.time va try-with-resources. */
public class ExceptionsAndIoDemo {

    public static void main(String[] args) throws IOException {
        Path file = Files.createTempFile("data-sources-", ".txt");
        Files.writeString(file, "github\n\npostgres\nnotion\n");

        DataSourceImporter importer = new DataSourceImporter();
        List<DataSource> sources = importer.importFrom(file);
        Optional<DataSource> postgres = importer.findByName(sources, "postgres");

        System.out.println("Imported at " + Instant.now() + ": " + sources);
        System.out.println("Found: " + postgres.orElseThrow());
        Files.deleteIfExists(file);
    }

    static final class DataSourceImporter {
        List<DataSource> importFrom(Path path) {
            if (!Files.isReadable(path)) {
                throw new IllegalArgumentException("File is not readable: " + path);
            }

            List<DataSource> result = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                long id = 1;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        result.add(new DataSource(id++, line.trim()));
                    }
                }
                return List.copyOf(result);
            } catch (IOException exception) {
                throw new DataSourceImportException("Cannot import " + path, exception);
            }
        }

        Optional<DataSource> findByName(List<DataSource> sources, String name) {
            return sources.stream().filter(source -> source.name().equalsIgnoreCase(name)).findFirst();
        }
    }

    static final class DataSourceImportException extends RuntimeException {
        DataSourceImportException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    record DataSource(long id, String name) {
    }
}

