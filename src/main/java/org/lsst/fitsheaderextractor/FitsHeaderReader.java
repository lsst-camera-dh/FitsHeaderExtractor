package org.lsst.fitsheaderextractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.header.Standard;
import nom.tam.util.Cursor;

/**
 *
 * @author tonyj
 */
public class FitsHeaderReader {

    private final Map<String, Map<String, Object>> headerData = new LinkedHashMap<>();

    public enum Mode {
        PRIMARY, // Only read the primary header
        IMAGEEXT, // Read the primaryheader and all image extensions
        ALL, // Read all image extensions
        CRAZY // Read primary and first image extension only
    }  

    FitsHeaderReader(Path fitsPath, Mode mode) throws IOException {
        try (final Fits fits = new Fits(fitsPath.toFile())) {
            Header header = Header.readHeader(fits.getStream());
            processHeader("PRIMARY", header);
            if (mode == Mode.PRIMARY) {
                return;
            }
            fits.getStream().skip(header.getDataSize());
            for (;;) {
                header = Header.readHeader(fits.getStream());
                if (header == null) {
                    break;
                }
                String xtension = header.getStringValue(Standard.XTENSION);
                if (header.getBooleanValue("ZIMAGE")) {
                    xtension = header.getStringValue("ZTENSION");
                }
                if ("IMAGE".equals(xtension) || mode == Mode.ALL) {
                    processHeader(header.getStringValue(Standard.EXTNAME), header);
                    if (mode == Mode.CRAZY) {
                        return;
                    }
                }
                fits.getStream().skip(header.getDataSize());
            }
        } catch (IOException | FitsException x) {
            throw new IOException("Error reading FITS header for " + fitsPath, x);
        }
    }

    private void processHeader(String headerName, Header header) {
        Map<String, Object> headerMap = new LinkedHashMap<>();
        headerData.put(headerName, headerMap);
        for (Cursor<String, HeaderCard> iterator = header.iterator(); iterator.hasNext();) {
            HeaderCard card = iterator.next();
            if ("END".equals(card.getKey())) {
                break;
            }
            Class valueType = card.valueType();
            if (valueType == Boolean.class) {
                headerMap.put(card.getKey(), card.getValue(Boolean.class, false));
            } else if (valueType == Double.class) {
                headerMap.put(card.getKey(), card.getValue(Double.class, 0.0));
            } else if (valueType == Integer.class) {
                headerMap.put(card.getKey(), card.getValue(Integer.class, 0));
            } else {
                headerMap.put(card.getKey(), card.getValue());
            }
        }
    }

    public Map<String, Map<String, Object>> getHeaderData() {
        return headerData;
    }

    public static void main(String[] args) throws IOException {
        Path path = Path.of("/home/tonyj/Data/MC_C_20200905_000412/MC_C_20200905_000412_R23_S01.fits");
        FitsHeaderReader fitsHeaderReader = new FitsHeaderReader(path, Mode.CRAZY);
        JsonWriter writer = new JsonWriter();
        writer.writeHeader(Path.of("test.json"), fitsHeaderReader.getHeaderData());
    }

}
