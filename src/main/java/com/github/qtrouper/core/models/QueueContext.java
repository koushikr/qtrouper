package com.github.qtrouper.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.qtrouper.utils.SerDe;
import com.google.common.base.Strings;
import lombok.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author koushik
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class QueueContext implements Serializable{

    private String referenceId;

    private Map<String, Object> data = new HashMap<>();

    @Builder
    public QueueContext(String serviceReference) {
        this.referenceId = serviceReference;
    }

    public <T> void addContext(Class<T> klass, T value) {
        addContext(klass.getSimpleName().toUpperCase(), value);
    }

    @JsonIgnore
    public <T> T getContext(Class<T> tClass) {
        return getContext(tClass.getSimpleName().toUpperCase(), tClass);
    }

    public <T> void addContext(String key, T value) {
        if (this.data == null) this.data = new HashMap<>();

        if (Strings.isNullOrEmpty(key.toUpperCase()))
            throw new RuntimeException("Invalid key for context data. Key cannot be null/empty");

        this.data.put(key.toUpperCase(), value);
    }

    @JsonIgnore
    public <T> T getContext(String key, Class<T> tClass) {
        Object value = this.data.get(key.toUpperCase());
        return null == value ? null : SerDe.mapper().convertValue(value, tClass);
    }
}
