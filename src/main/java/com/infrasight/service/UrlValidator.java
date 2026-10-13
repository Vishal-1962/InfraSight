package com.infrasight.service;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.infrasight.config.InfraSightProperties;

/**
 * Validates monitored URLs and blocks destinations that should not be reachable from a monitor
 * (loopback, private ranges, link-local and cloud metadata addresses) unless explicitly allowed.
 * The same check runs when saving an endpoint, before every check, and for every redirect.
 */
@Component
public class UrlValidator {

    private final InfraSightProperties properties;

    public UrlValidator(InfraSightProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns the parsed URI or throws {@link UnsafeUrlException} with a message that is safe to show.
     * A host name that does not resolve is accepted: the check itself will report the DNS failure.
     */
    public URI validate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UnsafeUrlException("Enter a URL.");
        }
        String value = raw.trim();
        if (value.length() > 2000) {
            throw new UnsafeUrlException("The URL is too long.");
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (Exception ex) {
            throw new UnsafeUrlException("Enter a valid URL, for example https://example.com/health.");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new UnsafeUrlException("Only http:// and https:// URLs are supported.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeUrlException("The URL must include a host name, for example https://example.com.");
        }
        if (uri.getUserInfo() != null) {
            throw new UnsafeUrlException("URLs that contain a user name or password are not allowed.");
        }
        if (uri.getPort() == 0 || uri.getPort() > 65535) {
            throw new UnsafeUrlException("The port number is not valid.");
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            return uri;
        }
        for (InetAddress address : addresses) {
            if (isBlocked(address)) {
                throw new UnsafeUrlException("This address is not allowed. Private, loopback and link-local "
                        + "network destinations cannot be monitored.");
            }
        }
        return uri;
    }

    private boolean isBlocked(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLinkLocalAddress() || a.isMulticastAddress()) {
            return true;
        }
        if (properties.isAllowPrivateTargets()) {
            return false;
        }
        if (a.isLoopbackAddress() || a.isSiteLocalAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        if (a instanceof Inet4Address) {
            int b0 = b[0] & 0xff;
            int b1 = b[1] & 0xff;
            return b0 == 0                                   // 0.0.0.0/8
                    || (b0 == 100 && b1 >= 64 && b1 <= 127)  // 100.64.0.0/10 carrier-grade NAT
                    || (b0 == 192 && b1 == 0 && (b[2] & 0xff) == 0) // 192.0.0.0/24
                    || (b0 == 198 && (b1 == 18 || b1 == 19)) // 198.18.0.0/15 benchmarking
                    || b0 >= 240;                            // reserved
        }
        if (a instanceof Inet6Address) {
            return (b[0] & 0xfe) == 0xfc;                    // fc00::/7 unique local
        }
        return false;
    }
}
