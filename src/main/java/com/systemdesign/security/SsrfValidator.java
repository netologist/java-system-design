package com.systemdesign.security;

import java.net.InetAddress;
import java.net.URI;

/**
 * Server-Side Request Forgery (SSRF) Protection Validator (Category 19).
 * <p>
 * Inspects outbound destination URLs before initiating network calls.
 * Blocks private IP ranges, loopback interfaces (127.0.0.1, localhost),
 * link-local addresses, and cloud provider metadata IPs (169.254.169.254).
 */
public final class SsrfValidator {

    private SsrfValidator() {}

    public static boolean isSafeUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(urlString);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false; // Only allow HTTP/HTTPS
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }

            InetAddress address = InetAddress.getByName(host);

            // Block loopback, link-local, site-local, any-local (private clouds)
            if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress()) {
                return false;
            }

            // Block AWS / GCP / Azure metadata endpoint
            String ip = address.getHostAddress();
            if (ip.startsWith("169.254.") || ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("192.168.")) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false; // Malformed or unresolvable URL is deemed unsafe
        }
    }
}
