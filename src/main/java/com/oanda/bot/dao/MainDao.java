package com.oanda.bot.dao;

import com.oanda.bot.model.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.oanda.bot.StrategySteps;
import com.oanda.bot.constants.Step;
import com.oanda.bot.model.*;
import com.oanda.bot.model.Candle.Candles;
import com.oanda.bot.util.ModelUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

@Repository("mainDao")
public class MainDao {

	private static final String PH_INSTRUMENT = "%INSTRUMENT%";
	private static final String PH_STEP = "%STEP%";
	private static final String PH_CANDLES = "%CANDLES%";
	private static final String PH_PIVOT = "%PIVOT%";
	private static final String PH_ORDERS = "%ORDERS%";
	private static final String PH_FRACTALS = "%FRACTALS%";
	private static final String PARAM_DIRECTION = "dir";
	private static final String PARAM_BREAKING_TIME = "breakingTime";
	private static final String PARAM_FRACTAL_TIME = "fractalTime";
	private static final String CANDLES_PREFIX = "CANDLES";
	private static final String PIVOT_PREFIX = "PIVOT";
	private static final String FRACTALS_PREFIX = "FRACTALS";
	private static final String ORDERS_PREFIX = "ORDERS";
	@Autowired
    Logger logger;
    @Autowired
    private StrategySteps steps;
	private JdbcTemplate jdbcTemplate;
	private NamedParameterJdbcTemplate npJdbcTemplate;
	private String createCandlesTableScript;
	private String insertCandleScript;
	private String getLastCandleScript;
	private String createPivotTableScript;
	private String getLastPivotScript;
	private String insertPivotScript;
	private String createFractalsTableScript;
	private String insertFractalsScript;
	private String updateFractalsScript;
	private String updateFractalsScript2;
	private String getLastFractalScript;
	private String getLastFractalScript2;
	private String getLastFractalScript3;
	private String getLastFractalScript4;
	private String createOrdersTableScript;
	private String insertOrderScript;
	private String closeOrderScript;

	public int breakFractal(Instrument instrument, Step step, Candle fractal) {
		String script = updateFractalsScript2.replaceAll(PH_STEP, step.toString())
				.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_FRACTALS, FRACTALS_PREFIX);
		Map<String, Object> params = new HashMap<>();
		params.put(PARAM_FRACTAL_TIME, fractal.getDateTime());
		params.put(PARAM_DIRECTION, Integer.valueOf(fractal.getDirection()));
		params.put(PARAM_BREAKING_TIME, fractal.getBrokenDateTime());
		int count = npJdbcTemplate.update(script, params);
		return count;
	}

	public int closeOrder(Order order, Instrument instrument) {
		String script = closeOrderScript.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_ORDERS,
				ORDERS_PREFIX);
		Map<String, Object> map = new HashMap<>();
		map.put(":id", order.getId());
		map.put(":closedTime", DateTime.now());
		return npJdbcTemplate.update(script, map);
	}

	public void createCandlesTable(Step step, Instrument instrument) throws IOException {
		String script = createCandlesTableScript.replaceAll(PH_STEP, step.toString())
				.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_CANDLES, CANDLES_PREFIX);
		jdbcTemplate.execute(script);
	}

	public void createFractalsTable(Step step, Instrument instrument) {
		String script = createFractalsTableScript.replaceAll(PH_STEP, step.toString())
				.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_FRACTALS, FRACTALS_PREFIX);
		jdbcTemplate.execute(script);
	}

	public void createOrdersTable(Instrument instrument) throws IOException {
		String script = createOrdersTableScript.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_ORDERS,
				ORDERS_PREFIX);
		jdbcTemplate.execute(script);
	}

	public void createPivotTable(Instrument instrument) throws IOException {
		String script = createPivotTableScript.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_PIVOT,
				PIVOT_PREFIX);
		jdbcTemplate.execute(script);
	}

	public void createTables(Instrument instrument) throws IOException {
		createCandlesTable(steps.tradingStep(), instrument);
		createCandlesTable(steps.trendStep(), instrument);
		createFractalsTable(steps.tradingStep(), instrument);
		createFractalsTable(steps.trendStep(), instrument);
		createPivotTable(instrument);
		createOrdersTable(instrument);
	}

	public int findBrokenFractals(Step step, Instrument instrument) {
		String script = updateFractalsScript.replaceAll(PH_STEP, step.toString())
				.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_CANDLES, CANDLES_PREFIX)
				.replaceAll(PH_FRACTALS, FRACTALS_PREFIX);
		return jdbcTemplate.update(script);
	}

	public int findFractals(Step step, Instrument instrument) {
		String script = insertFractalsScript.replaceAll(PH_STEP, step.toString())
				.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_CANDLES, CANDLES_PREFIX)
				.replaceAll(PH_FRACTALS, FRACTALS_PREFIX);
		return jdbcTemplate.update(script);
	}

	public Candle getLastBrokenFractal(Step step, Instrument instrument) {
		findBrokenFractals(step, instrument);
		String script = getLastFractalScript3.replaceAll(PH_STEP, step.toString())
				.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_CANDLES, CANDLES_PREFIX)
				.replaceAll(PH_FRACTALS, FRACTALS_PREFIX);
		try {
			Map<String, Object> params = new HashMap<>();
			Candle candle = npJdbcTemplate.queryForObject(script, params, new FractalRowMapper());
			return candle;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Candle getLastBrokenFractal(Step step, Instrument instrument, int direction) {
		String script = getLastFractalScript4.replaceAll(PH_STEP, step.toString())
				.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_CANDLES, CANDLES_PREFIX)
				.replaceAll(PH_FRACTALS, FRACTALS_PREFIX);
		try {
			Map<String, Object> params = new HashMap<>();
			params.put(PARAM_DIRECTION, Integer.valueOf(direction));
			Candle candle = npJdbcTemplate.queryForObject(script, params, new FractalRowMapper());
			return candle;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Candle getLastCandle(Step step, Instrument instrument) {
		String script = getLastCandleScript.replaceAll(PH_STEP, step.toString())
				.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_CANDLES, CANDLES_PREFIX);
		try {
			Candle candle = jdbcTemplate.queryForObject(script, new CandleRowMapper());
			return candle;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Candle getLastFractal(Step step, Instrument instrument) {
		String script = getLastFractalScript2.replaceAll(PH_STEP, step.toString())
				.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_CANDLES, CANDLES_PREFIX)
				.replaceAll(PH_FRACTALS, FRACTALS_PREFIX);
		try {
			Map<String, Object> params = new HashMap<>();
			Candle candle = npJdbcTemplate.queryForObject(script, params, new FractalRowMapper());
			return candle;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Candle getLastFractal(Step step, Instrument instrument, int direction) {
		String script = getLastFractalScript.replaceAll(PH_STEP, step.toString())
				.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_CANDLES, CANDLES_PREFIX)
				.replaceAll(PH_FRACTALS, FRACTALS_PREFIX);
		try {
			Map<String, Object> params = new HashMap<>();
			params.put(PARAM_DIRECTION, Integer.valueOf(direction));
			Candle candle = npJdbcTemplate.queryForObject(script, params, new FractalRowMapper());
			return candle;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Pivot getLastPivot(Instrument instrument) {
		String script = getLastPivotScript.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_PIVOT,
				PIVOT_PREFIX);
		Pivot pivot = new Pivot();
		pivot.setInstrument(instrument);
		try {
			pivot = jdbcTemplate.queryForObject(script, new RowMapper<Pivot>() {

				@Override
				public Pivot mapRow(ResultSet rs, int rowNum) throws SQLException {
					Pivot pivot = new Pivot();
					pivot.setTime(new DateTime(rs.getTimestamp(1), DateTimeZone.getDefault()));
					pivot.setInstrument(instrument);
					pivot.setR3(rs.getFloat(2));
					pivot.setR2(rs.getFloat(3));
					pivot.setR1(rs.getFloat(4));
					pivot.setPp(rs.getFloat(5));
					pivot.setS1(rs.getFloat(6));
					pivot.setS2(rs.getFloat(7));
					pivot.setS3(rs.getFloat(8));
					pivot.setM0(rs.getFloat(9));
					pivot.setM1(rs.getFloat(10));
					pivot.setM2(rs.getFloat(11));
					pivot.setM3(rs.getFloat(12));
					pivot.setM4(rs.getFloat(13));
					pivot.setM5(rs.getFloat(14));
					return pivot;
				}
			});
		} catch (EmptyResultDataAccessException e) {
			logger.error("Could not find pivot.");
		}
		return pivot;
	}

	@PostConstruct
	public void init() throws IOException {
		BufferedReader in = new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream("/sql/candles_create.sql")));
		LineNumberReader fileReader = new LineNumberReader(in);
		createCandlesTableScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/sql/candles_insert.sql")));
		fileReader = new LineNumberReader(in);
		insertCandleScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/sql/candles_getlast.sql")));
		fileReader = new LineNumberReader(in);
		getLastCandleScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/sql/pivot_create.sql")));
		fileReader = new LineNumberReader(in);
		createPivotTableScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/sql/pivot_insert.sql")));
		fileReader = new LineNumberReader(in);
		insertPivotScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/sql/pivot_getlast.sql")));
		fileReader = new LineNumberReader(in);
		getLastPivotScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/sql/fractals_create.sql")));
		fileReader = new LineNumberReader(in);
		createFractalsTableScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/sql/fractals_insert.sql")));
		fileReader = new LineNumberReader(in);
		insertFractalsScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/sql/fractals_update.sql")));
		fileReader = new LineNumberReader(in);
		updateFractalsScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream("/sql/fractals_update2.sql")));
		fileReader = new LineNumberReader(in);
		updateFractalsScript2 = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream("/sql/fractals_getlast.sql")));
		fileReader = new LineNumberReader(in);
		getLastFractalScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream("/sql/fractals_getlast_all.sql")));
		fileReader = new LineNumberReader(in);
		getLastFractalScript2 = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream("/sql/fractals_getlastbroken.sql")));
		fileReader = new LineNumberReader(in);
		getLastFractalScript3 = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream("/sql/fractals_getlastbroken2.sql")));
		fileReader = new LineNumberReader(in);
		getLastFractalScript4 = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/sql/orders_create.sql")));
		fileReader = new LineNumberReader(in);
		createOrdersTableScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/sql/orders_insert.sql")));
		fileReader = new LineNumberReader(in);
		insertOrderScript = ScriptUtils.readScript(fileReader, "--", ";");
		in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/sql/orders_close.sql")));
		fileReader = new LineNumberReader(in);
		closeOrderScript = ScriptUtils.readScript(fileReader, "--", ";");
	}

	public Candle insertCandles(Candles candles) {
		String script = insertCandleScript.replaceAll(PH_STEP, candles.getGranularity().toString())
				.replaceAll(PH_INSTRUMENT, candles.getInstrument().toString()).replaceAll(PH_CANDLES, CANDLES_PREFIX);
		Candle[] array = ModelUtil.getCompletedCandles(candles).toArray(new Candle[] {});
		if (array.length > 0) {
			SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(array);
			int[] counts = npJdbcTemplate.batchUpdate(script, batch);
			int linesAffected = Arrays.stream(counts).sum();
			if (linesAffected == array.length) {
				logger.debug("inserted " + linesAffected + " candles " + candles.getGranularity().toString());
				Optional<Candle> lastCompletedCandle = Stream.of(array).max(Comparator.comparing(c -> c.getTime()));
				if (lastCompletedCandle.isPresent()) {
					return lastCompletedCandle.get();
				} else {
					return null;
				}
			}
		}
		return null;
	}

	public int insertOrder(PostOrderResponse order, Instrument instrument) {
		String script = insertOrderScript.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_ORDERS,
				ORDERS_PREFIX);
		SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(new PostOrderResponse[] { order });
		int[] counts = npJdbcTemplate.batchUpdate(script, batch);
		return Arrays.stream(counts).sum();
	}

	public int insertPivot(Pivot pivot, Instrument instrument) {
		String script = insertPivotScript.replaceAll(PH_INSTRUMENT, instrument.toString()).replaceAll(PH_PIVOT,
				PIVOT_PREFIX);
		SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(new Pivot[] { pivot });
		int[] counts = npJdbcTemplate.batchUpdate(script, batch);
		return Arrays.stream(counts).sum();

	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.npJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

	}

    private class CandleRowMapper implements RowMapper<Candle> {

        @Override
        public Candle mapRow(ResultSet rs, int rowNum) throws SQLException {
            Candle candle = new Candle();
            candle.setTime(new DateTime(rs.getTimestamp(1), DateTimeZone.getDefault()));
            candle.setOpenMid(rs.getFloat(2));
            candle.setHighMid(rs.getFloat(3));
            candle.setLowMid(rs.getFloat(4));
            candle.setCloseMid(rs.getFloat(5));
            candle.setVolume(rs.getInt(6));
            candle.setComplete(rs.getBoolean(7));
            return candle;
        }
    }

    private class FractalRowMapper implements RowMapper<Candle> {

        @Override
        public Candle mapRow(ResultSet rs, int rowNum) throws SQLException {
            Candle candle = new Candle();
            candle.setTime(new DateTime(rs.getTimestamp(1), DateTimeZone.getDefault()));
            candle.setOpenMid(rs.getFloat(2));
            candle.setHighMid(rs.getFloat(3));
            candle.setLowMid(rs.getFloat(4));
            candle.setCloseMid(rs.getFloat(5));
            candle.setVolume(rs.getInt(6));
            candle.setComplete(rs.getBoolean(7));
            candle.setBroken(rs.getBoolean(8));
            candle.setBrokenTime(new DateTime(rs.getTimestamp(9), DateTimeZone.getDefault()));
            candle.setDirection(rs.getInt(10));
            return candle;
        }
    }

}
