package com.plovdev.bot.modules.parsers;

import com.plovdev.bot.modules.beerjes.utils.BeerjUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class SignalCorrector {
    public static Signal correct(Signal toCorrect, String beerj) {
        String symbol = toCorrect.getSymbol();
        String retSymbol = BeerjUtils.getExchangeCoin(symbol, beerj);

        if (!symbol.equals(retSymbol)) {
            BigDecimal stop = new BigDecimal(toCorrect.getStopLoss());
            BigDecimal thous = new BigDecimal("1000");
            toCorrect.setSymbol(retSymbol);
            if (retSymbol.startsWith("1000")) {
                toCorrect.setStopLoss(stop.multiply(thous).toPlainString());
                List<BigDecimal> newTargets = new ArrayList<>();
                for (BigDecimal target : toCorrect.getTargets()) {
                    newTargets.add(target.multiply(thous));
                }
                toCorrect.setTargets(newTargets);

                List<String> tvhs = toCorrect.getTypeOreder();
                List<String> tvh = new ArrayList<>(tvhs.contains("market") ? List.of("market") : new ArrayList<>());
                for (String tv : tvhs) {
                    if (!tv.equals("market")) {
                        BigDecimal newTvh = new BigDecimal(tv).multiply(thous);
                        tvh.add(newTvh.toPlainString());
                    }
                }
                toCorrect.setTypeOreder(tvh);
            } else {
                toCorrect.setStopLoss(stop.divide(thous, 15, RoundingMode.HALF_EVEN).toPlainString());
                List<BigDecimal> newTargets = new ArrayList<>();
                for (BigDecimal target : toCorrect.getTargets()) {
                    newTargets.add(target.divide(thous, 15,RoundingMode.HALF_EVEN));
                }
                toCorrect.setTargets(newTargets);

                List<String> tvhs = toCorrect.getTypeOreder();
                List<String> tvh = new ArrayList<>(tvhs.contains("market") ? List.of("market") : new ArrayList<>());
                for (String tv : tvhs) {
                    if (!tv.equals("market")) {
                        BigDecimal newTvh = new BigDecimal(tv).divide(thous, 10, RoundingMode.HALF_EVEN);
                        tvh.add(newTvh.toPlainString());
                    }
                }
                toCorrect.setTypeOreder(tvh);
            }
        }

        return toCorrect;
    }
}