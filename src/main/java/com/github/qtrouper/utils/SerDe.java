package com.github.qtrouper.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

/**
 * @author koushik
 */
public class SerDe {

    private static ObjectMapper mapper;

    public static void init(ObjectMapper objectMapper) {
        mapper = objectMapper;
    }

    public static ObjectMapper mapper() {
        Preconditions.checkNotNull(mapper, "Please call SerDe.init(mapper) to set mapper");
        return mapper;
    }

}
