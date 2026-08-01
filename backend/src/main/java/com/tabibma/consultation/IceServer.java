package com.tabibma.consultation;

/** Mirrors the browser RTCIceServer shape (username/credential null for STUN-only entries). */
public record IceServer(String urls, String username, String credential) {

    public static IceServer stun(String urls) {
        return new IceServer(urls, null, null);
    }
}
