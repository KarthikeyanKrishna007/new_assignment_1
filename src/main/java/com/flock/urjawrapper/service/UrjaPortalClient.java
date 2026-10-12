package com.flock.urjawrapper.service;

import com.flock.urjawrapper.dto.MeterResponse;
import com.flock.urjawrapper.dto.ConsumptionResponse;
import com.flock.urjawrapper.dto.NetworkNodeResponse;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class UrjaPortalClient {

    @Value("${urja.portal.base-url}")
    private String baseUrl;

    @Value("${urja.portal.username}")
    private String username;

    @Value("${urja.portal.password}")
    private String password;

    private Map<String, String> sessionCookies = new HashMap<>();
    private volatile boolean authenticated = false;
    private final DateTimeFormatter legacyDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public synchronized void authenticate() {
        try {
            Connection.Response loginPage = Jsoup.connect(baseUrl + "/login")
                    .method(Connection.Method.GET)
                    .timeout(10000)
                    .execute();

            String csrfToken = loginPage.parse().select("input[name=_csrf]").val();

            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("password", password);
            if (csrfToken != null && !csrfToken.isEmpty()) {
                payload.put("_csrf", csrfToken);
            }

            Connection.Response authAction = Jsoup.connect(baseUrl + "/login")
                    .data(payload)
                    .cookies(loginPage.cookies())
                    .method(Connection.Method.POST)
                    .timeout(10000)
                    .execute();

            if (authAction.statusCode() == 200 && authAction.parse().html().toLowerCase().contains("logout")) {
                this.sessionCookies = authAction.cookies();
                this.authenticated = true;
            } else {
                throw new IllegalStateException("Credentials rejected by legacy portal.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Upstream legacy infrastructure is down: " + e.getMessage(), e);
        }
    }

    private void verifySessionContext() {
        if (!authenticated || sessionCookies.isEmpty()) {
            authenticate();
        }
    }

    public List<MeterResponse> scrapeAllMeters() {
        verifySessionContext();
        try {
            Document document = Jsoup.connect(baseUrl + "/meters")
                    .cookies(sessionCookies)
                    .timeout(10000)
                    .get();

            Elements structuralRows = document.select("table#meter-list-table tr, table tr:has(td)");

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                return executor.submit(() ->
                        structuralRows.stream()
                                .filter(row -> row.select("th").isEmpty())
                                .map(row -> row.select("td"))
                                .filter(nodes -> nodes.size() >= 3)
                                .map(nodes -> new MeterResponse(
                                        nodes.get(0).text().trim(),
                                        nodes.get(1).text().trim(),
                                        nodes.get(2).text().trim().toLowerCase(),
                                        nodes.size() > 3 ? nodes.get(3).text().trim() : "Unassigned Grid Node"
                                ))
                                .toList()
                ).get();
            }
        } catch (Exception e) {
            this.authenticated = false;
            throw new RuntimeException("Error executing high-throughput meters extraction stream.", e);
        }
    }

    public List<ConsumptionResponse> scrapeConsumptionHistory(String meterId) {
        verifySessionContext();
        try {
            Document doc = Jsoup.connect(baseUrl + "/meters/" + meterId + "/consumption")
                    .cookies(sessionCookies)
                    .timeout(10000)
                    .get();

            Elements rows = doc.select("table.consumption-history tr, table tr:has(td)");

            return rows.stream()
                    .filter(row -> row.select("th").isEmpty())
                    .map(row -> row.select("td"))
                    .filter(dataset -> dataset.size() >= 2)
                    .map(dataset -> {
                        try {
                            String rawDateStr = dataset.get(0).text().trim();
                            String rawKwhStr = dataset.get(1).text().trim().replaceAll("[^0-9.]", "");

                            ZonedDateTime normalizedDate = rawDateStr.contains("/") ?
                                    LocalDate.parse(rawDateStr, legacyDateFormatter).atStartOfDay(ZoneId.of("UTC")) :
                                    ZonedDateTime.parse(rawDateStr);

                            return Optional.of(new ConsumptionResponse(normalizedDate, Double.parseDouble(rawKwhStr)));
                        } catch (Exception e) {
                            return Optional.<ConsumptionResponse>empty();
                        }
                    })
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException e) {
            this.authenticated = false;
            throw new RuntimeException("Upstream network error targeting logs for meter: " + meterId, e);
        }
    }

    public List<NetworkNodeResponse> reconstructGridHierarchy() {
        var groupings = scrapeAllMeters().stream()
                .collect(Collectors.groupingBy(MeterResponse::structuralNode));

        return groupings.entrySet().stream()
                .map(entry -> new NetworkNodeResponse(entry.getKey(), "DISTRIBUTION_TRANSFORMER", List.of(), entry.getValue()))
                .toList();
    }
}
