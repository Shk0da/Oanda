package com.oanda.bot.actor;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Сообщения взаимодействия между акторами
 */
public enum Messages {
    WORK, PREDICT, LEARN;

    @Data
    @AllArgsConstructor
    public static class WorkTime {
        Boolean is;
    }

    @Data
    @AllArgsConstructor
    public static class Predict {
        public enum Signal {UP, DOWN, NONE}

        private Signal trend;
    }
}
