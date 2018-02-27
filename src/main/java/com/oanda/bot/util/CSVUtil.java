package com.oanda.bot.util;

import com.google.common.collect.Lists;
import com.oanda.bot.domain.Candle;
import com.opencsv.CSVReader;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
public class CSVUtil {

    @SneakyThrows
    public static void saveCandles(List<Candle> candles, String fileName) {
        if (candles.isEmpty()) return;

        String separator = ",";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSSZ");
        StringBuilder stringBuilder = new StringBuilder();
        candles.forEach(candle -> {
            stringBuilder.append(simpleDateFormat.format(candle.getTime()));
            stringBuilder.append(separator);
            stringBuilder.append(candle.getOpenMid());
            stringBuilder.append(separator);
            stringBuilder.append(candle.getHighMid());
            stringBuilder.append(separator);
            stringBuilder.append(candle.getLowMid());
            stringBuilder.append(separator);
            stringBuilder.append(candle.getCloseMid());
            stringBuilder.append(separator);
            stringBuilder.append(candle.getVolume());
            stringBuilder.append("\n");
        });

        File csvDataFile = new File(fileName + ".csv");
        if (csvDataFile.exists() || !csvDataFile.exists() && csvDataFile.createNewFile()) {
            FileCopyUtils.copy(
                    stringBuilder.toString(),
                    new OutputStreamWriter(new FileOutputStream(csvDataFile), "UTF-8")
            );
        } else {
            log.error("Something wrong. File {} not save.", csvDataFile.getName());
        }
    }

    @SneakyThrows
    public static List<Candle> getCandles(String fileName, int size) {
        File csvDataFile = new File(fileName);
        if (!csvDataFile.exists()) return Lists.newArrayList();

        List<Candle> data = Lists.newArrayList();
        CSVReader reader = new CSVReader(new FileReader(csvDataFile));
        String[] line;
        while ((line = reader.readNext()) != null) {
            Candle candle = new Candle();
            candle.setOpenMid(Double.valueOf(line[1]));
            candle.setHighMid(Double.valueOf(line[2]));
            candle.setLowMid(Double.valueOf(line[3]));
            candle.setCloseMid(Double.valueOf(line[4]));
            candle.setVolume(Double.valueOf(line[5]).intValue());
            data.add(candle);
        }

        return data.subList(0, data.size() > size ? data.size() : size);
    }
}
