/*
 *  Copyright 2023 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.plugins.utils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class UriUtils
{
    private static final Logger _logger = LoggerFactory.getLogger(UriUtils.class);
    private static final ImmutableSet<String> userAtHostSchemes = ImmutableSet.of("acct", "mailto");

    private UriUtils()
    {
    }

    public static String fullyQualify(String baseUrl, String... endpointParts)
    {
        if (baseUrl == null || StringUtils.isEmpty(baseUrl))
        {
            throw new IllegalArgumentException("baseUrl cannot be null or empty");
        }

        StringBuilder url = new StringBuilder(baseUrl);

        if (endpointParts != null && endpointParts.length > 0)
        {
            for (String endpointPart : endpointParts)
            {
                url.append("/").append(endpointPart);
            }
        }

        return URI.create(url.toString()).normalize().toString();
    }

    public static String addQueryParameters(String baseUrl, Map<String, String> queryParameters)
    {
        return addParameters(baseUrl, queryParameters, false);
    }

    public static String addFragmentParameters(String baseUrl, Map<String, String> fragmentParameters)
    {
        return addParameters(baseUrl, fragmentParameters, true);
    }

    public static String addParameters(String baseUrl, Map<String, String> parameters, boolean toFragment)
    {
        if (parameters.isEmpty())
        {
            return baseUrl;
        }

        return addMapParameters(baseUrl, parameters.entrySet(), toFragment);
    }

    public static String addQueryParameters(String baseUrl, Multimap<String, String> queryParameters)
    {
        return addParameters(baseUrl, queryParameters, false);
    }

    public static String addFragmentParameters(String baseUrl, Multimap<String, String> fragmentParameters)
    {
        return addParameters(baseUrl, fragmentParameters, true);
    }

    public static String addParameters(String baseUrl, Multimap<String, String> parameters, boolean toFragment)
    {
        if (parameters.isEmpty())
        {
            return baseUrl;
        }

        return addMapParameters(baseUrl, parameters.entries(), toFragment);
    }

    public static void appendQueryParameter(StringBuilder stringBuilder, Map.Entry<String, String> entry, boolean toFragment)
    {
        if (!stringBuilder.isEmpty())
        {
            stringBuilder.append("&");
        }
        else if (toFragment)
        {
            stringBuilder.append("#");
        }
        else
        {
            stringBuilder.append("?");
        }
        addUrlEncodedParameter(stringBuilder, entry);
    }

    private static void addUrlEncodedParameter(StringBuilder stringBuilder, Map.Entry<String, String> entry)
    {
        String key = entry.getKey();
        String value = entry.getValue();

        String encodedKey = urlEncodeString(key);
        stringBuilder.append(encodedKey);

        if (!Strings.isNullOrEmpty(value))
        {
            String encodedValue = urlEncodeString(value);
            stringBuilder.append("=").append(encodedValue);
        }
    }

    private static void appendParameter(StringBuilder stringBuilder, Map.Entry<String, String> entry)
    {
        addUrlEncodedParameter(stringBuilder, entry);
        stringBuilder.append("&");
    }

    private static String addMapParameters(String baseUrl, Collection<Map.Entry<String, String>> entries, boolean toFragment)
    {
        StringBuilder stringBuilder = new StringBuilder(baseUrl);

        String separator = (toFragment ? "#" : "?");

        if (baseUrl.contains(separator))
        {
            stringBuilder.append("&");
        }
        else
        {
            stringBuilder.append(separator);
        }

        for (Map.Entry<String, String> entry : entries)
        {
            appendParameter(stringBuilder, entry);
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        String url = stringBuilder.toString();

        return url;
    }

    public static boolean isValidCallbackUrl(String callbackUrl, Set<String> callbackWhiteListUrls)
    {
        boolean isValidCallbackUrl = false;

        if (StringUtils.isNotEmpty(callbackUrl))
        {
            for (String allowedCallbackPattern : callbackWhiteListUrls)
            {
                try
                {
                    if ("*".equals(allowedCallbackPattern) ||
                            exactMatch(callbackUrl, allowedCallbackPattern) ||
                            matchesWildcard(callbackUrl, allowedCallbackPattern))
                    {
                        isValidCallbackUrl = true;

                        break;
                    }
                }
                catch (IllegalArgumentException | NullPointerException e)
                {
                    _logger.debug("When comparing {} to {}, a {} occurred. " +
                                    "Considering this to be non-matching.", callbackUrl,
                            allowedCallbackPattern, e.getClass().getSimpleName());
                }
            }
        }

        return isValidCallbackUrl;
    }

    public static boolean exactMatch(String callbackUrl, String allowedCallbackPattern)
    {
        URI uri1 = URI.create(callbackUrl);
        URI uri2 = URI.create(allowedCallbackPattern);

        return uri1.equals(uri2);
    }

    private static boolean matchesWildcard(String callbackUrl, String allowedCallbackPattern)
    {
        boolean endsWithWildcard = allowedCallbackPattern.endsWith("*");
        int offset = allowedCallbackPattern.indexOf('*') == -1 ? 0 : 1;
        String beforeWildcard = allowedCallbackPattern.substring(0, allowedCallbackPattern.length() - offset);

        URI uri1 = URI.create(callbackUrl);
        URI uri2 = URI.create(beforeWildcard);

        boolean schemesMatch;
        boolean hostsMatch;
        boolean portsMatch;

        if (uri1.isAbsolute())
        {
            @Nullable String host1 = uri1.getHost();

            schemesMatch = StringUtils.equals(uri1.getScheme(), uri2.getScheme());
            hostsMatch = StringUtils.equals(host1, uri2.getHost());
            portsMatch = uri1.getPort() == uri2.getPort();

            if (!hostsMatch && uri2.getAuthority() != null && uri2.getAuthority().startsWith("*."))
            {
                @Nullable String domain1 = NullUtils.map(host1, (s) -> s.replaceFirst("[^.]*\\.", ""));
                String domain2 = uri2.getAuthority().replaceFirst("[^.]*\\.", "");

                domain1 = StringUtils.removeEnd(domain1, ".");
                domain2 = StringUtils.removeEnd(domain2, ".");

                hostsMatch = StringUtils.equals(domain1, domain2);
            }
        }
        else
        {
            schemesMatch = hostsMatch = portsMatch = !uri2.isAbsolute() && !callbackUrl.startsWith("//");
        }

        boolean pathsMatch = uri1.getPath().startsWith(uri2.getPath()) && endsWithWildcard;

        return schemesMatch && hostsMatch && portsMatch && pathsMatch;
    }

    /**
     * Safely decodes a URL-encoded string.
     *
     * <p>
     * If the input value is not a valid URL-encoded string, then an empty string will be returned. Likewise, if the
     * input is an empty string, that will be returned, making the two cases indistinguishable. Similarly, if the
     * value is null, then an empty string will be returned. If these difference are important, the client should
     * handle them in some way. For instance, these three example will all return an empty string:
     *
     * <pre>
     *     UriHelper.decodeSafely("%"); // Invalid URL-encoded value
     *     UriHelper.decodeSafely("")
     *     UriHelper.decodeSafely(null)
     *     </pre>
     *
     * @param encodedValue a (possibly invalid) URL-encoded string
     * @return an empty string if the input was an empty string, null, or a value that could not be decoded; otherwise,
     * the result is the URL-decoded representation of the input string.
     */
    public static String decodeSafely(@Nullable String encodedValue)
    {
        try
        {
            return encodedValue == null ? "" : URLDecoder.decode(encodedValue, StandardCharsets.UTF_8.name());
        }
        catch (IllegalArgumentException e)
        {
            _logger.debug("Could not decode value '{}' because it was not properly URL encoded.", encodedValue);

            return "";
        }
        catch (UnsupportedEncodingException e)
        {
            assert false; // Will never happen because UTF-8 exists in all JREs

            if (_logger.isDebugEnabled())
            {
                _logger.debug("Could not convert cached request body content because the encoding {} is " +
                        "not supported: {}", StandardCharsets.UTF_8.name(), e.getMessage());
            }

            return encodedValue;
        }
    }

    public static String urlEncodeString(String unencodedString)
    {
        return URLEncoder.encode(unencodedString, StandardCharsets.UTF_8);
    }

    public static boolean isAbsoluteUri(String uriString)
    {
        try
        {
            URI uri = new URI(uriString);
            return uri.isAbsolute();
        }
        catch (URISyntaxException ignored)
        {
            return false;
        }
    }

    public static Optional<String> getHost(String uriString)
    {
        try
        {
            URI uri = new URI(uriString);
            return getHost(uri);
        }
        catch (URISyntaxException ignored)
        {
            return Optional.empty();
        }
    }

    public static Optional<String> getHost(URI uri)
    {
        @Nullable String host = uri.getHost();
        if (host != null)
        {
            return cleanIpv6Host(Optional.of(host));
        }
        if (userAtHostSchemes.contains(uri.getScheme()))
        {
            return cleanIpv6Host(userAtHostExtractor(uri));
        }
        return Optional.empty();
    }

    private static Optional<String> cleanIpv6Host(Optional<String> host)
    {
        return host.map(h -> h.startsWith("[") && h.endsWith("]") ?
                h.substring(1, h.length() - 1) :
                h);
    }

    private static Optional<String> userAtHostExtractor(URI uri)
    {
        String opaquePart = uri.getRawSchemeSpecificPart();
        try
        {
            // Uses the URI parser to parse the "userinfo@authority"
            URI innerUri = new URI("https://" + opaquePart);
            return Optional.ofNullable(innerUri.getHost());
        }
        catch (URISyntaxException e)
        {
            return Optional.empty();
        }
    }

    public static boolean hasHost(String uri, String... hosts)
    {
        try
        {
            return hasHost(new URI(uri), hosts);
        }
        catch (URISyntaxException e)
        {
            _logger.trace("Malformed uri", e);
            return false;
        }
    }

    public static boolean hasHost(URI uri, String... hosts)
    {
        if (ArrayUtils.getLength(hosts) < 1)
        {
            return false;
        }

        @Nullable
        String uriHost = uri.getHost();

        if (uriHost == null || uriHost.isEmpty())
        {
            return false;
        }

        return Arrays.asList(hosts).contains(uriHost);
    }

    /**
     * Will return <code>false</code> if the passed <code>uri</code> is malformed.
     *
     * @see #isHostLoopbackInterface(URI)
     */
    public static boolean isHostLoopbackInterface(String uri)
    {
        try
        {
            return isHostLoopbackInterface(new URI(uri));
        }
        catch (URISyntaxException e)
        {
            _logger.trace("Malformed uri", e);
            return false;
        }
    }

    /**
     * Will perform a host lookup if the host of the passed URI is not an IP literal.
     */
    public static boolean isHostLoopbackInterface(URI uri)
    {
        try
        {
            // Will do a host lookup
            return InetAddress.getByName(uri.getHost()).isLoopbackAddress();
        }
        catch (UnknownHostException e)
        {
            _logger.trace("Unknown host", e);
            return false;
        }
    }

    /**
     * Create a new URL instance from the provided String representation of a URL.
     * It is assumed that the provided urlAsString is a valid URL, therefore no checked exception is used to indicate
     * an error. Instead, an (unchecked) IllegalArgumentException will be thrown when the urlAsString is invalid.
     *
     * @param urlAsString String representation of te URL; assumed to be a valid URL.
     * @return URL instance of the urlAsString
     */
    public static URL safeCreateUrl(String urlAsString)
    {
        try
        {
            return new URL(urlAsString);
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException(
                    "Provided URL was not a valid URL: " + NullUtils.rootCauseErrorMessage(e));
        }
    }

    /**
     * Remove the path- and query part of a URL, such that the URL no longer has any component after the host/port part.
     * <p>
     * The provided urlString must be a valid URL, otherwise an {@link IllegalArgumentException} will be thrown.
     *
     * @param urlString String representation of a URL to normalize
     * @return String representation with a possible path- or query component removed.
     */
    public static String urlWithoutPath(String urlString)
    {
        try
        {
            // Assert that urlString really is a URL
            // Using URL does require a supported scheme to be used
            URL url = new URL(urlString);

            int validUntil = urlString.length() - 1;

            if (StringUtils.isNotEmpty(url.getPath()) || StringUtils.isNotEmpty(url.getQuery()))
            {
                int index = validUntil;
                while (index > 1)
                {
                    if ('/' == urlString.charAt(index) || '?' == urlString.charAt(index))
                    {
                        if ('/' == urlString.charAt(index - 1))
                        {
                            // We've reached the 'scheme://' point in the url
                            index--;
                        }
                        else
                        {
                            // Strip off everything after *and* including the '/', keep going on
                            validUntil = index - 1;
                        }
                    }
                    index--;
                }
            }

            return urlString.substring(0, validUntil + 1);
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException(
                    String.format("Tried to process a URL-value '%s' that was not a URL: %s",
                            urlString, e.getMessage()));
        }
    }
}
