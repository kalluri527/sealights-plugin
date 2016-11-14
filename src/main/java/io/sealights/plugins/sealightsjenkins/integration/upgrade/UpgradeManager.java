package io.sealights.plugins.sealightsjenkins.integration.upgrade;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sealights.plugins.sealightsjenkins.integration.SeaLightsPluginInfo;
import io.sealights.plugins.sealightsjenkins.integration.upgrade.entities.UpgradeResponse;
import io.sealights.plugins.sealightsjenkins.utils.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by shahar on 8/16/2016.
 */
public class UpgradeManager {

    private SeaLightsPluginInfo slInfo;
    private Logger logger;

    public UpgradeManager(SeaLightsPluginInfo slInfo, Logger logger) {
        this.slInfo = slInfo;
        this.logger = logger;
    }

    public String queryServerForMavenPluginVersion() throws IOException {
        URL url = createUrlToGetRecommendedVersion();
        ObjectMapper mapper = new ObjectMapper();
        logger.info("Sending request to get recommended version: '"+url+"'.");
        UpgradeResponse upgradeResponse = mapper.readValue(url, UpgradeResponse.class);
        return upgradeResponse.getAgent().getVersion();
    }

    private URL createUrlToGetRecommendedVersion() throws MalformedURLException, UnsupportedEncodingException {
        String urlStr = slInfo.getServerUrl()+"/"+getBaseUrl()+"/"+getQueryString();
        return new URL(urlStr);
    }

    private String getBaseUrl() {
        return "v1/agents/sl-maven-plugin/recommended";
    }

    private String getQueryString() throws UnsupportedEncodingException {
        String customerId = encodeValue(slInfo.getCustomerId());
        String appName = encodeValue(slInfo.getAppName());
        String branch = encodeValue(slInfo.getBranchName());
        String envName = encodeValue(slInfo.getEnvironment());

        StringBuilder queryString = new StringBuilder();
        addQueryStringValue(queryString, "customerId", customerId);
        addQueryStringValue(queryString, "appName", appName);
        addQueryStringValue(queryString, "branch", branch);
        addQueryStringValue(queryString, "envName", envName);

        String qs = queryString.toString();
        if ("".equals(qs))
            return "";

        qs = "?" + qs;
        qs = qs.substring(0, qs.length() - 1); //Remove the last &.
        return qs;
    }

    private void addQueryStringValue(StringBuilder queryString, String key, String value) {
        if (isNotNullOrEmpty(value) && isNotNullOrEmpty(key)) {
            queryString.append(key);
            queryString.append("=");
            queryString.append(value);
            queryString.append("&");
        }
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    private boolean isNotNullOrEmpty(String s) {
        return !isNullOrEmpty(s);
    }

    private String encodeValue(String value) throws UnsupportedEncodingException {
        if (isNotNullOrEmpty(value)) {
            return URLEncoder.encode(value, "UTF-8");
        }
        return null;
    }
}