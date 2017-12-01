package com.oanda.bot.actor;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;

/**
 * Сообщения взаимодействия между акторами
 */
public enum Messages {
    WORK, SAVE_STATE;

    @Data
    @AllArgsConstructor
    public static class WorkTime {
        Boolean is;
    }

    @Data
    @AllArgsConstructor
    public static class LearnModel {
        File model;
        double openMin;
        double openMax;
        double lowMin;
        double lowMax;
        double highMin;
        double highMax;
        double closeMin;
        double closeMax;
        double volumeMin;
        double volumeMax;
    }

    @Data
    @AllArgsConstructor
    public static class Predict {
        private double price;
    }
}
