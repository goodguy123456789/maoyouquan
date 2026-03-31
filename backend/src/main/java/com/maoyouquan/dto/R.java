package com.maoyouquan.dto;

import lombok.Data;

@Data
public class R<T> {
    private boolean success;
    private T data;
    private String message;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.success = true;
        r.data = data;
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(String message) {
        R<T> r = new R<>();
        r.success = false;
        r.message = message;
        return r;
    }
}
