package com.plovdev.bot.modules.parsers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TradingViewSignalParser {
    public static Signal parse(String signalText) {
        String symbol = "";
        String direction = "";
        List<BigDecimal> targets = new ArrayList<>();
        String stopLoss = "0";


        // Парсинг символа
        Pattern symbolPattern = Pattern.compile("\uD83D\uDCE9\\s*#(\\w+[.P]?)");
        Matcher symbolMatcher = symbolPattern.matcher(signalText);
        if (symbolMatcher.find()) {
            symbol = symbolMatcher.group(1);
        }

        // Парсинг направления (SHORT/LONG)
        Pattern directionPattern = Pattern.compile("(?i)(BUY|SELL)");
        Matcher directionMatcher = directionPattern.matcher(signalText);
        if (directionMatcher.find()) {
            String dir = directionMatcher.group(1);
            direction = dir.contains("SELL") ? "SHORT" : "LONG";
        }


        // Парсинг целей (targets)
        Pattern targetPattern = Pattern.compile("Target\\s*\\d+\\s*:\\s*(\\d+.\\d+)");
        Matcher targetMatcher = targetPattern.matcher(signalText);
        while (targetMatcher.find()) {
            System.err.println(targetMatcher.group(1));
            targets.add(new BigDecimal(targetMatcher.group(1)));
        }

        // Парсинг стоп-лосса
        Pattern stopLossPattern = Pattern.compile("Stop-Loss:\\s*([0-9.]+)");
        Matcher stopLossMatcher = stopLossPattern.matcher(signalText);
        if (stopLossMatcher.find()) {
            stopLoss = stopLossMatcher.group(1);
        }
        Collections.sort(targets);

        return new Signal("tv", symbol.replaceAll("\\.P|\\.", ""), null, direction, List.of("market"), stopLoss, targets, "tv", null);
    }

    public static boolean validate(String signalText) {
        boolean symbol;
        boolean direction;
        boolean stopLoss;


        // Парсинг символа
        Pattern symbolPattern = Pattern.compile("\uD83D\uDCE9\\s*#(\\w+[.P]?)");
        Matcher symbolMatcher = symbolPattern.matcher(signalText);
        symbol = symbolMatcher.find();

        // Парсинг направления (SHORT/LONG)
        Pattern directionPattern = Pattern.compile("(?i)(BUY|SELL)");
        Matcher directionMatcher = directionPattern.matcher(signalText);
        direction = directionMatcher.find();

        // Парсинг стоп-лосса
        Pattern stopLossPattern = Pattern.compile("Stop-Loss:\\s*([0-9.]+)");
        Matcher stopLossMatcher = stopLossPattern.matcher(signalText);
        stopLoss = stopLossMatcher.find();

        System.err.println("Valid symbol? - " + symbol);
        System.err.println("Valid direction? - " + direction);
        System.err.println("Valid stop loss? - " + stopLoss);

        return symbol && direction && stopLoss;
    }
}