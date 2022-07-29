package org.lsst.fitsheaderextractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Use Jackson to write FITS header as json
 *
 * @author tonyj
 */
public class JsonWriter {

    public JsonWriter() {
    }

    void writeHeader(Path fileToWrite, Object data) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileToWrite.toFile(), data);
    }
}
