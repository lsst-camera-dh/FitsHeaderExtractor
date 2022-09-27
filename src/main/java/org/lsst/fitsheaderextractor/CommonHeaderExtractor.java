package org.lsst.fitsheaderextractor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Extract common headers from a set of headers.
 * @author tonyj
 */
public class CommonHeaderExtractor {

    private final Map<String, Object> common;

    CommonHeaderExtractor(List<Map<String, Object>> headers) {
        common = new LinkedHashMap<>();
        if (!headers.isEmpty()) {
            common.putAll(headers.get(0));
            // First pass, remove any items not in common
            for (Map<String, Object> header : headers.subList(1, headers.size())) {
                for (Map.Entry<String, Object> entry : header.entrySet()) {
                    Object commonValue = common.get(entry.getKey());
                    if (!Objects.equals(commonValue, entry.getValue())) {
                        common.remove(entry.getKey());
                    }
                }
            }
            // Second pass, remove commeon entries from original headers
            for (String key : common.keySet()) {
                for (Map<String, Object> header : headers) {
                    header.remove(key);
                }
            }
        }
    }

    public Map<String, Object> getCommon() {
        return common;
    }

}
