package com.oanda.bot.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class Candle {
    @JsonDeserialize(using = StringDateTimeDeserializer.class)
    private DateTime time;
    private Mid mid = new Mid();
    private Bid bid = new Bid();
    private Ask ask = new Ask();
    private int volume;
    private boolean complete;
    private boolean broken;
    private DateTime brokenTime;
    private int direction;

    public double getOpenMid() {
        return mid.o;
    }

    public void setOpenMid(double val) {
        this.mid.o = val;
    }

    public double getHighMid() {
        return mid.h;
    }

    public void setHighMid(double val) {
        this.mid.h = val;
    }

    public double getLowMid() {
        return mid.l;
    }

    public void setLowMid(double val) {
        this.mid.l = val;
    }

    public double getCloseMid() {
        return mid.c;
    }

    public void setCloseMid(double val) {
        this.mid.c = val;
    }

    public Date getBrokenDateTime() {
        return time.toDate();
    }

    public void setBrokenDateTime(Date date) {
        time = new DateTime(date.getTime(), DateTimeZone.getDefault());
    }

    public Date getDateTime() {
        return time.toDate();
    }

    public void setDateTime(Date date) {
        time = new DateTime(date.getTime(), DateTimeZone.getDefault());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candles {
        private String instrument;

        @JsonDeserialize(using = CustomGranularityDeserializer.class)
        private Step granularity;

        private List<Candle> candles = new ArrayList<>();

        @Override
        public String toString() {
            if (instrument == null) {
                return "No candles";
            }
            StringBuilder string = new StringBuilder("Candles [instrument=" + instrument + ", granularity=" + granularity + ", candles=\n");
            if (candles != null) {
                for (Candle c : candles) {
                    string.append("\t");
                    string.append(c.toString());
                    string.append("\n");
                }
            }
            string.append("]");
            return string.toString();
        }
    }

    public static class CustomGranularityDeserializer extends JsonDeserializer<Step> {
        @Override
        public Step deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
                throws IOException {
            return Step.valueOf(jsonparser.getText());
        }

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Mid {
        private double o;
        private double h;
        private double l;
        private double c;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ask {
        private double o;
        private double h;
        private double l;
        private double c;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bid {
        private double o;
        private double h;
        private double l;
        private double c;
    }
}
