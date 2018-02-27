package com.oanda.bot.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class InstrumentRepository {

    private final Map<String, Instrument> instruments = Maps.newHashMap();

    @Autowired
    private AccountService accountService;

    public Instrument getInstrument(String instrument) {
        Instrument result = instruments.get(instrument);
        if (result == null) {
            result = updateInstrument(instrument);
        }
        return result;
    }

    public List<Instrument> getAllInstruments() {
        return Lists.newArrayList(instruments.values());
    }

    private Instrument updateInstrument(String instrument) {
        Instrument result = accountService.getInstrument(instrument);
        instruments.put(instrument, result);
        return result;
    }
}
