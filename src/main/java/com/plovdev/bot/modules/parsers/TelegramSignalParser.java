package com.plovdev.bot.modules.parsers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelegramSignalParser {
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("ℹ️\\s*(\\w+/?\\w+)");
    private static final Pattern DIRECTION_PATTERN = Pattern.compile("(🟢\\s*LONG|🔴\\s*SHORT)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TYPE_PATTERN = Pattern.compile(".(\uD83D\uDD31\\s*market|\uD83D\uDD31\\s*\\d+\\.\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TARGET_PATTERN = Pattern.compile("🎯\\s*(\\d+\\.\\d+)");
    private static final Pattern STOP_LOSS_PATTERN = Pattern.compile("[⛔|⛔️]\\s*(\\d+\\.*\\d+)");
    public static Signal parse(String signalText) {
        if (!validate(signalText)) {
            throw new IllegalArgumentException("Invalid signal format");
        }
        String symbol = parseString(signalText, SYMBOL_PATTERN);
        String direction = parseDirection(signalText);
        List<String> types = parseTypes(signalText);
        List<BigDecimal> targets = parseTargets(signalText);
        String stopLoss = parseString(signalText, STOP_LOSS_PATTERN);
        Collections.sort(targets);
        return new Signal("tg", symbol.replace("/", ""), null, direction, types, stopLoss, targets, "tg", null);
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
            return dir.contains("LONG") ? "LONG" : "SHORT";
        }
        return "";
    }
    private static List<String> parseTypes(String signalText) {
        List<String> types = new ArrayList<>();
        Matcher matcher = TYPE_PATTERN.matcher(signalText);
        while (matcher.find()) {
            types.add(matcher.group(1).substring(2).trim().toLowerCase());
        }
        return types;
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