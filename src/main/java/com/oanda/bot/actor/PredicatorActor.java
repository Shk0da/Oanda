package com.oanda.bot.actor;

import akka.actor.UntypedAbstractActor;
import com.oanda.bot.config.ActorConfig;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import com.oanda.bot.repository.CandleRepository;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.oanda.bot.util.learning.StockDataSetIterator.*;

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
            INDArray input = Nd4j.create(new int[]{1, VECTOR_SIZE}, 'f');
            input.putScalar(new int[]{0, 0}, normalize(lastCandle.getOpenMid(), learnModel.getOpenMin(), learnModel.getOpenMax()));
            input.putScalar(new int[]{0, 1}, normalize(lastCandle.getCloseMid(), learnModel.getCloseMin(), learnModel.getCloseMax()));
            input.putScalar(new int[]{0, 2}, normalize(lastCandle.getLowMid(), learnModel.getLowMin(), learnModel.getLowMax()));
            input.putScalar(new int[]{0, 3}, normalize(lastCandle.getHighMid(), learnModel.getHighMin(), learnModel.getHighMax()));
            input.putScalar(new int[]{0, 4}, normalize(lastCandle.getVolume(), learnModel.getVolumeMin(), learnModel.getVolumeMax()));

            MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork(learnModel.getModel(), true);
            double closePrice = deNormalize(net.rnnTimeStep(input).getDouble(0), learnModel.getCloseMin(), learnModel.getCloseMax());

            if (closePrice != lastPredict) {
                lastPredict = closePrice;
                Messages.Predict predict = new Messages.Predict(closePrice);
                getContext().actorSelection(ActorConfig.ACTOR_PATH_HEAD + "TradeActor_" + instrument.getInstrument() + "_" + step.name()).tell(predict, sender());
            }
        }
    }
}
