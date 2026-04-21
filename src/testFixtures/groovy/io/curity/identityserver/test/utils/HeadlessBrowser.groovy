/*
 * Copyright 2026 Curity AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.curity.identityserver.test.utils

import org.htmlunit.BrowserVersion
import org.htmlunit.NicelyResynchronizingAjaxController
import org.htmlunit.Page
import org.htmlunit.WebClient
import org.htmlunit.html.DomElement
import org.htmlunit.html.HtmlElement
import org.htmlunit.html.HtmlInput
import org.htmlunit.html.HtmlPage
import org.htmlunit.html.HtmlTextArea
import org.slf4j.LoggerFactory

import java.time.Duration

/**
 * Simple HtmlUnit-based headless browser utility for integration tests.
 */
final class HeadlessBrowser implements Closeable {

    private static final def logger = LoggerFactory.getLogger(HeadlessBrowser.class)
    private final WebClient webClient
    private final Duration defaultTimeout
    private final Duration backgroundJsTimeout
    private HtmlPage currentPage
    private int lastStatusCode = -1

    private HeadlessBrowser(WebClient webClient, Duration defaultTimeout, Duration backgroundJsTimeout) {
        this.webClient = webClient
        this.defaultTimeout = defaultTimeout
        this.backgroundJsTimeout = backgroundJsTimeout
    }

    static HeadlessBrowser create() {
        def config = BrowserConfig.fromEnvironment()
        def client = createClient(config)
        return new HeadlessBrowser(client, config.defaultTimeout, config.backgroundJsTimeout)
    }

    static HeadlessBrowser create(URL keystoreUrl, String keystorePassword, String keystoreType = "PKCS12") {
        def config = BrowserConfig.fromEnvironment(keystoreUrl, keystorePassword, keystoreType)
        def client = createClient(config)
        return new HeadlessBrowser(client, config.defaultTimeout, config.backgroundJsTimeout)
    }

    private static WebClient createClient(BrowserConfig config) {
        def client = new WebClient(config.browserVersion)
        client.options.setUseInsecureSSL(true)
        client.options.setJavaScriptEnabled(config.javaScriptEnabled)
        client.options.setCssEnabled(config.cssEnabled)
        client.cookieManager.setCookiesEnabled(true)
        client.options.setThrowExceptionOnScriptError(false)
        client.options.setThrowExceptionOnFailingStatusCode(false)
        client.options.setTimeout((int) config.pageLoadTimeout.toMillis())
        client.ajaxController = new NicelyResynchronizingAjaxController()
        if (config.keystoreUrl != null) {
            client.options.setSSLClientCertificateKeyStore(config.keystoreUrl, config.keystorePassword, config.keystoreType)
        }
        return client
    }

    void setClientCertificate(URL keystoreUrl, String keystorePassword, String keystoreType = "PKCS12") {
        webClient.options.setSSLClientCertificateKeyStore(keystoreUrl, keystorePassword, keystoreType)
    }

    WebClient getClient() {
        return webClient
    }

    HtmlPage getCurrentPage() {
        return currentPage
    }

    int getLastStatusCode() {
        return lastStatusCode
    }

    String getCurrentUrl() {
        return currentPage?.url?.toString()
    }

    HtmlPage navigate(String url) {
        logger.info("Navigating to: $url")
        Page page = webClient.getPage(url)
        if (page instanceof HtmlPage) {
            currentPage = page
            updateStatusFromPage(currentPage)
        } else {
            lastStatusCode = page.webResponse?.statusCode ?: -1
        }
        webClient.waitForBackgroundJavaScript(backgroundJsTimeout.toMillis())
        return currentPage
    }

    HtmlPage startCodeFlow(String url, String clientId, String scope = null, String redirectUri = null, String acrValues = null, Map<String, String> extraParameters = [:]) {
        def queryParams = []
        queryParams << "client_id=${URLEncoder.encode(clientId, 'UTF-8')}"
        queryParams << "response_type=code"
        if (scope) {
            queryParams << "scope=${URLEncoder.encode(scope, 'UTF-8')}"
        }
        if (redirectUri) {
            queryParams << "redirect_uri=${URLEncoder.encode(redirectUri, 'UTF-8')}"
        }
        if (acrValues) {
            queryParams << "acr_values=${URLEncoder.encode(acrValues, 'UTF-8')}"
        }
        extraParameters.each { key, value ->
            queryParams << "${URLEncoder.encode(key, 'UTF-8')}=${URLEncoder.encode(value, 'UTF-8')}"
        }
        def queryString = "?" + queryParams.join("&")
        navigate("$url$queryString")
    }

    HtmlElement waitForElement(String cssSelector) {
        return waitUntil {
            elementByCss(cssSelector)
        }
    }

    boolean waitForTitle(String expectedTitle) {
        return waitUntil {
            currentPage?.titleText == expectedTitle
        } as boolean
    }

    String textByCss(String cssSelector) {
        return waitForElement(cssSelector).textContent
    }

    void clickByCss(String cssSelector) {
        def element = waitForElement(cssSelector)
        def result = element.click()
        if (result instanceof HtmlPage) {
            currentPage = (HtmlPage) result
            updateStatusFromPage(currentPage)
            webClient.waitForBackgroundJavaScript(backgroundJsTimeout.toMillis())
        }
    }

    void typeByCss(String cssSelector, String value) {
        def element = waitForElement(cssSelector)
        if (element instanceof HtmlInput) {
            element.setValueAttribute(value)
        } else if (element instanceof HtmlTextArea) {
            element.setText(value)
        } else {
            throw new IllegalStateException("Element '${cssSelector}' is not a text input")
        }
    }

    /**
     * Clear all cookies from the browser's cookie manager.
     */
    void clearCookies() {
        webClient.cookieManager.clearCookies()
    }

    @Override
    void close() {
        webClient.close()
    }

    private HtmlElement elementByCss(String cssSelector) {
        if (currentPage == null) {
            throw new IllegalStateException("No page has been loaded yet")
        }
        DomElement node = currentPage.querySelector(cssSelector)
        return node instanceof HtmlElement ? (HtmlElement) node : null
    }

    private <T> T waitUntil(Closure<T> condition) {
        def deadline = System.currentTimeMillis() + defaultTimeout.toMillis()
        T result = null
        while (System.currentTimeMillis() < deadline) {
            result = condition.call()
            if (result) {
                return result
            }
            Thread.sleep(100)
        }
        return result
    }

    private void updateStatusFromPage(HtmlPage page) {
        if (page != null) {
            lastStatusCode = page.webResponse?.statusCode ?: -1
        }
    }

    private static final class BrowserConfig {
        final BrowserVersion browserVersion
        final boolean javaScriptEnabled
        final boolean cssEnabled
        final Duration defaultTimeout
        final Duration pageLoadTimeout
        final Duration backgroundJsTimeout
        final URL keystoreUrl
        final String keystorePassword
        final String keystoreType

        private BrowserConfig(BrowserVersion browserVersion,
                              boolean javaScriptEnabled,
                              boolean cssEnabled,
                              Duration defaultTimeout,
                              Duration pageLoadTimeout,
                              Duration backgroundJsTimeout,
                              URL keystoreUrl,
                              String keystorePassword,
                              String keystoreType) {
            this.browserVersion = browserVersion
            this.javaScriptEnabled = javaScriptEnabled
            this.cssEnabled = cssEnabled
            this.defaultTimeout = defaultTimeout
            this.pageLoadTimeout = pageLoadTimeout
            this.backgroundJsTimeout = backgroundJsTimeout
            this.keystoreUrl = keystoreUrl
            this.keystorePassword = keystorePassword
            this.keystoreType = keystoreType
        }

        static BrowserConfig fromEnvironment(URL keystoreUrl = null, String keystorePassword = null, String keystoreType = "PKCS12") {
            def browserName = (System.getenv("HTMLUNIT_BROWSER") ?: "chrome").toLowerCase(Locale.ROOT)
            def browserVersion = browserName == "firefox" ? BrowserVersion.FIREFOX : BrowserVersion.CHROME
            def javaScriptEnabled = (System.getenv("HTMLUNIT_JS_ENABLED") ?: "true").toBoolean()
            def cssEnabled = (System.getenv("HTMLUNIT_CSS_ENABLED") ?: "true").toBoolean()
            def timeoutSeconds = (System.getenv("HTMLUNIT_TIMEOUT_SECONDS") ?: "30") as int
            def pageLoadSeconds = (System.getenv("HTMLUNIT_PAGELOAD_SECONDS") ?: "60") as int
            def jsWaitSeconds = (System.getenv("HTMLUNIT_JS_WAIT_SECONDS") ?: "5") as int

            def resolvedKeystoreUrl = keystoreUrl
            def resolvedKeystorePassword = keystorePassword
            def resolvedKeystoreType = keystoreType

            def envKeystorePath = System.getenv("HTMLUNIT_SSL_KEYSTORE_PATH")
            if (resolvedKeystoreUrl == null && envKeystorePath != null) {
                resolvedKeystoreUrl = new File(envKeystorePath).toURI().toURL()
                resolvedKeystorePassword = System.getenv("HTMLUNIT_SSL_KEYSTORE_PASSWORD") ?: ""
                resolvedKeystoreType = System.getenv("HTMLUNIT_SSL_KEYSTORE_TYPE") ?: "PKCS12"
            }

            return new BrowserConfig(
                    browserVersion,
                    javaScriptEnabled,
                    cssEnabled,
                    Duration.ofSeconds(timeoutSeconds),
                    Duration.ofSeconds(pageLoadSeconds),
                    Duration.ofSeconds(jsWaitSeconds),
                    resolvedKeystoreUrl,
                    resolvedKeystorePassword,
                    resolvedKeystoreType
            )
        }
    }
}
