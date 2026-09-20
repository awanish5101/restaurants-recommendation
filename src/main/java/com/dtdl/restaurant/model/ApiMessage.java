package com.dtdl.restaurant.model;

/** Small JSON envelope for simple acknowledgement responses. */
public record ApiMessage(String status, String message) {

    public static ApiMessage ok(String message) {
        return new ApiMessage("ok", message);
    }
}
