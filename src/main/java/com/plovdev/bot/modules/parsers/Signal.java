package com.plovdev.bot.modules.parsers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Signal {
    private String symbol;
    private String direction;
    private String stopLoss;
    private String type;
    private BigDecimal entryPrice;
    private String src;
    private BigDecimal limitEntryPrice;

    public BigDecimal getLimitEntryPrice() {
        return limitEntryPrice;
    }

    public void setLimitEntryPrice(BigDecimal limitEntryPrice) {
        this.limitEntryPrice = limitEntryPrice;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    private List<String> typeOreder;

    public List<String> getTypeOreder() {
        return typeOreder;
    }

    public void setTypeOreder(List<String> typeOreder) {
        this.typeOreder = typeOreder;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTargets(List<BigDecimal> targets) {
        this.targets = targets;
    }

    private List<BigDecimal> targets = new ArrayList<>();

    public Signal() {

    }

    public Signal(String type, String symbol, BigDecimal price, String direct, List<String> typeOrder, String stopLoss, List<BigDecimal> list, String s, BigDecimal lprice) {
        this.type = type;
        this.symbol = symbol;
        entryPrice = price;
        this.direction = direct;
        this.stopLoss = stopLoss;
        this.typeOreder = typeOrder;
        targets.addAll(list);
        src = s;
        limitEntryPrice = lprice;
    }

    public List<BigDecimal> getTargets() {
        return targets;
    }
    public void addTarget(BigDecimal target) {
        targets.add(target);
    }
    public void removeTarget(BigDecimal target) {
        targets.remove(target);
    }

    public void addTargets(List<BigDecimal> list) {
        targets.addAll(list);
    }
    public void removeTargets(List<BigDecimal> list) {
        targets.removeAll(list);
    }

    public String getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(String stopLoss) {
        this.stopLoss = stopLoss;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }


    @Override
    public String toString() {
        return String.format("""
                        {
                            "TYPE": "%2s",
                            "SYMBOL": "%2s",
                            "ENTRY_PRICE": "%2s",
                            "DIRECTION": "%2s",
                            "ORDER_TYPE": "%2s",
                            "TARGETS": "%2s",
                            "STOP_LOSS": "%2s",
                            "SOURCE": "%2s"
                        }""",
                type, symbol, entryPrice, direction, typeOreder, targets, stopLoss, src);
    }
}