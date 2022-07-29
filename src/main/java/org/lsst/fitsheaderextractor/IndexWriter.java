package org.lsst.fitsheaderextractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.lsst.fitsheaderextractor.FitsHeaderReader.Mode;

/**
 *
 * @author tonyj
 */
class IndexWriter implements AutoCloseable {

    private final Map<String, Object> fileHeaders = new LinkedHashMap<>();
    private final Mode mode;
    private final int verbosity;
    private final Path jsonPath;
    private final boolean singleFileMode;

    IndexWriter(Path jsonPath, Mode mode, int verbosity) {
        this.mode = mode;
        this.verbosity = verbosity;
        this.jsonPath = jsonPath;
        this.singleFileMode = true;
    }

    IndexWriter(Path directory, Mode mode, Path outputDirectory, int verbosity) {
        this.mode = mode;
        this.verbosity = verbosity;
        this.singleFileMode = false;
        if (outputDirectory == null) {
            jsonPath = directory.resolve("_index.json");
        } else {
            jsonPath = outputDirectory.resolve(directory.getFileName().toString() + ".json");
        }
    }

    @Override
    public void close() throws IOException {
        if (fileHeaders.isEmpty()) {
            if (verbosity > 1) {
                System.out.println("Skipped " + jsonPath);
            }
        } else if (singleFileMode) {
            Map<String, Object> headersToWrite = new LinkedHashMap<>();
            headersToWrite.put("__CONTENT__", "metadata");
            headersToWrite.putAll(fileHeaders);
            JsonWriter writer = new JsonWriter();

            writer.writeHeader(jsonPath, headersToWrite);
            if (verbosity > 0) {
                System.out.println("Wrote " + jsonPath);
            }
        } else {
            CommonHeaderExtractor che;
            if (mode == Mode.CRAZY || mode == Mode.PRIMARY) {
                che = new CommonHeaderExtractor(new ArrayList(fileHeaders.values()));
            } else {
                ArrayList primaryHeaders = new ArrayList<>();
                for (Map.Entry<String, Object> entry : fileHeaders.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> map = (Map) entry.getValue();
                        for (Map.Entry<String, Object> mapEntry : map.entrySet()) {
                            if ("PRIMARY".equals(mapEntry.getKey())) {
                                primaryHeaders.add(mapEntry.getValue());
                            }
                        }
                    }
                }
                che = new CommonHeaderExtractor(primaryHeaders);
            }
            Map<String, Object> headersToWrite = new LinkedHashMap<>();
            headersToWrite.put("__CONTENT__", "metadata");
            headersToWrite.put("__COMMON__", che.getCommon());
            headersToWrite.putAll(fileHeaders);
            JsonWriter writer = new JsonWriter();

            writer.writeHeader(jsonPath, headersToWrite);
            if (verbosity > 0) {
                System.out.println("Wrote " + jsonPath);
            }
        }
    }

    void addFile(Path file) throws IOException {
        if (verbosity > 2) {
            System.out.println("Reading " + file);
        }
        FitsHeaderReader reader = new FitsHeaderReader(file, mode);
        switch (mode) {
            case CRAZY -> {
                LinkedHashMap<String, Object> combined = new LinkedHashMap<>();
                for (var header : reader.getHeaderData().values()) {
                    combined.putAll(header);
                }
                fileHeaders.put(file.getFileName().toString(), combined);
            }
            case PRIMARY ->
                fileHeaders.put(file.getFileName().toString(), reader.getHeaderData().get("PRIMARY"));
            default ->
                fileHeaders.put(file.getFileName().toString(), reader.getHeaderData());
        }
    }

    boolean outputFileExists() {
        return Files.exists(jsonPath);
    }

}
