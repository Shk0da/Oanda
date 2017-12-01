package com.oanda.bot.actor;

import akka.actor.UntypedAbstractActor;
import com.oanda.bot.config.ActorConfig;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import com.oanda.bot.repository.CandleRepository;
import com.oanda.bot.util.learning.StockDataSetIterator;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Scope("prototype")
@Component("PredicatorActor")
public class PredicatorActor extends UntypedAbstractActor {

    private final Instrument instrument;
    private final Step step;

    private Double lastPredict = 0D;

    @Autowired
    private CandleRepository candleRepository;

    public PredicatorActor(Instrument instrument, Step step) {
        this.instrument = instrument;
        this.step = step;
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof Messages.LearnModel) {
            Messages.LearnModel learnModel = (Messages.LearnModel) message;

            Candle lastCandle = candleRepository.getLastCandle(instrument, step);
            INDArray input = Nd4j.create(new int[]{1, StockDataSetIterator.VECTOR_SIZE}, 'f');
            input.putScalar(new int[]{0, 0}, (lastCandle.getOpenMid() - learnModel.getOpenMin()) / (learnModel.getOpenMax() - learnModel.getOpenMin()));
            input.putScalar(new int[]{0, 1}, (lastCandle.getCloseMid() - learnModel.getCloseMin()) / (learnModel.getCloseMax() - learnModel.getCloseMin()));
            input.putScalar(new int[]{0, 2}, (lastCandle.getLowMid() - learnModel.getLowMin()) / (learnModel.getLowMax() - learnModel.getLowMin()));
            input.putScalar(new int[]{0, 3}, (lastCandle.getHighMid() - learnModel.getHighMin()) / (learnModel.getHighMax() - learnModel.getHighMin()));
            input.putScalar(new int[]{0, 4}, (lastCandle.getVolume() - learnModel.getVolumeMin()) / (learnModel.getVolumeMax() - learnModel.getVolumeMin()));

            MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork(learnModel.getModel(), true);
            double closePrice = net.rnnTimeStep(input).getDouble(0) * (learnModel.getCloseMax() - learnModel.getCloseMin()) + learnModel.getCloseMin();

            if (closePrice != lastPredict) {
                lastPredict = closePrice;
                Messages.Predict predict = new Messages.Predict(closePrice);
                getContext().actorSelection(ActorConfig.ACTOR_PATH_HEAD + "TradeActor_" + instrument.getInstrument() + "_" + step.name()).tell(predict, sender());
            }
        }
    }
}
