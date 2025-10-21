package com.plovdev.bot.modules.parsers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TradingViewSignalParser {
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("\uD83D\uDCE9\\s*#(\\w+[.P]?)");
    private static final Pattern DIRECTION_PATTERN = Pattern.compile("(?i)(BUY|SELL)");
    private static final Pattern TARGET_PATTERN = Pattern.compile("Target\\s*\\d+\\s*:\\s*(\\d+.\\d+)");
    private static final Pattern STOP_LOSS_PATTERN = Pattern.compile("Stop-Loss:\\s*([0-9.]+)");

    public static Signal parse(String signalText) {
        if (!validate(signalText)) {
            throw new IllegalArgumentException("Invalid signal format");
        }
        String symbol = parseString(signalText, SYMBOL_PATTERN);
        String direction = parseDirection(signalText);
        List<BigDecimal> targets = parseTargets(signalText);
        String stopLoss = parseString(signalText, STOP_LOSS_PATTERN);
        Collections.sort(targets);
        return new Signal("tv", symbol.replaceAll("\\.P|\\.", ""), null, direction, List.of("market"), stopLoss, targets, "tv", null);
    }
    private static String parseString(String signalText, Pattern pattern) {
        Matcher matcher = pattern.matcher(signalText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
    private static String parseDirection(String signalText) {
        Matcher matcher = DIRECTION_PATTERN.matcher(signalText);
        if (matcher.find()) {
            String dir = matcher.group(1);
            return dir.contains("SELL") ? "SHORT" : "LONG";
        }
        return "";
    }
    private static List<BigDecimal> parseTargets(String signalText) {
        List<BigDecimal> targets = new ArrayList<>();
        Matcher matcher = TARGET_PATTERN.matcher(signalText);
        while (matcher.find()) {
            targets.add(new BigDecimal(matcher.group(1)));
        }
        return targets;
    }
    public static boolean validate(String signalText) {
        if (signalText == null || signalText.trim().isEmpty()) {
            return false;
        }
        boolean symbolFound = SYMBOL_PATTERN.matcher(signalText).find();
        boolean directionFound = DIRECTION_PATTERN.matcher(signalText).find();
        boolean stopLossFound = STOP_LOSS_PATTERN.matcher(signalText).find();
        return symbolFound && directionFound && stopLossFound;
    }
}