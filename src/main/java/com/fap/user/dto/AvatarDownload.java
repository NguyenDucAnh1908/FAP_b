package com.fap.user.dto;

/**
 * Carrier for avatar bytes returned by the download endpoint — avoids loading
 * the BLOB inside a view layer or a lazy-load boundary.
 */
public record AvatarDownload(String contentType, byte[] data) {
}
