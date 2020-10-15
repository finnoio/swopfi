import com.wavesplatform.wavesj.ByteString;
import com.wavesplatform.wavesj.DataEntry;
import com.wavesplatform.wavesj.Transaction;
import im.mak.paddle.Account;
import im.mak.paddle.actions.WriteData;
import im.mak.paddle.exceptions.NodeError;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static im.mak.paddle.Async.async;
import static im.mak.paddle.Node.node;
import static im.mak.paddle.actions.invoke.Arg.arg;
import static im.mak.paddle.util.Script.fromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.MethodOrderer.Alphanumeric;

@TestMethodOrder(Alphanumeric.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SwopfiTest {

    private Account firstExchanger, secondExchanger, thirdExchanger, caller;
    private String tokenA;
    private String tokenB;
    private int aDecimal = 2;
    private int bDecimal = 3;
    private int wavesDecimal = 8;
    private int comission = 2000;
    private int comissionScaleDelimiter = 10000;

    @BeforeAll
    void before() {
        async(
                () -> {
                    firstExchanger = new Account(1000_00000000L);
                    firstExchanger.setsScript(s -> s.script(fromFile("dApps/exchanger.ride")));
                },
                () -> {
                    secondExchanger = new Account(1000_00000000L);
                    secondExchanger.setsScript(s -> s.script(fromFile("dApps/exchanger.ride")));
                },
                () -> {
                    thirdExchanger = new Account(1000_00000000L);
                    thirdExchanger.setsScript(s -> s.script(fromFile("dApps/exchanger.ride")));
                },
                () -> {
                    caller = new Account(1000_00000000L);
                    tokenA = caller.issues(a -> a.quantity(10000_00000000L).name("tokenA").decimals(aDecimal)).getId().toString();
                    tokenB = caller.issues(a -> a.quantity(10000_00000000L).name("tokenB").decimals(bDecimal)).getId().toString();
                }
        );
    }

    @Test
    void a_canFundAB(){
        node().waitForTransaction(tokenA);
        node().waitForTransaction(tokenB);

        long fundAmount = 10000000000L;
        String invokeId  = caller.invokes(i -> i.dApp(firstExchanger).function("fund").payment(fundAmount,tokenA).payment(fundAmount, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(fundAmount),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(fundAmount),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataInt("exchange_count")).isEqualTo(0),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(2000),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(10000),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(firstExchanger.dataBin("share_token_id")).isNotNull(),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(
                        //pow(tokenReceiveAmountA,digitTokenA,5,1,4,HALFDOWN)*pow(tokenReceiveAmountB,digitTokenB,5,1,4,HALFDOWN)
                        (long)((new BigDecimal(Math.pow(fundAmount / Math.pow(10, aDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue() *
                                new BigDecimal(Math.pow(fundAmount / Math.pow(10, bDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue()) * 100000000L)
                )

        );
    }

    @Test
    void b_canFundAWaves(){
        long fundAmount = 10000000000L;
        String invokeId  = caller.invokes(i -> i.dApp(secondExchanger).function("fund").payment(fundAmount,tokenA).wavesPayment(fundAmount).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        assertAll("data and balances",
                () -> assertThat(secondExchanger.dataInt("amountTokenA")).isEqualTo(fundAmount),
                () -> assertThat(secondExchanger.dataInt("amountTokenB")).isEqualTo(fundAmount),
                () -> assertThat(secondExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(secondExchanger.dataStr("assetIdTokenB")).isEqualTo("WAVES"),
                () -> assertThat(secondExchanger.dataInt("exchange_count")).isEqualTo(0),
                () -> assertThat(secondExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(secondExchanger.dataInt("comission")).isEqualTo(2000),
                () -> assertThat(secondExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(10000),
                () -> assertThat(secondExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(secondExchanger.dataBin("share_token_id")).isNotNull(),
                () -> assertThat(secondExchanger.dataInt("share_token_supply")).isEqualTo(
                        //pow(tokenReceiveAmountA,digitTokenA,5,1,4,HALFDOWN)*pow(tokenReceiveAmountB,digitTokenB,5,1,4,HALFDOWN)
                        (long)((new BigDecimal(Math.pow(fundAmount / Math.pow(10, aDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue() *
                                new BigDecimal(Math.pow(fundAmount / Math.pow(10, wavesDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue()) * 100000000L)
                )

        );
    }

    @Test
    void c_canFundWavesB(){
        aDecimal = 8;
        long fundAmount = 10000000000L;
        String invokeId = caller.invokes(i -> i.dApp(thirdExchanger).function("fund").wavesPayment(fundAmount).payment(fundAmount, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        assertAll("data and balances",
                () -> assertThat(thirdExchanger.dataInt("amountTokenA")).isEqualTo(fundAmount),
                () -> assertThat(thirdExchanger.dataInt("amountTokenB")).isEqualTo(fundAmount),
                () -> assertThat(thirdExchanger.dataStr("assetIdTokenA")).isEqualTo("WAVES"),
                () -> assertThat(thirdExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(thirdExchanger.dataInt("exchange_count")).isEqualTo(0),
                () -> assertThat(thirdExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(thirdExchanger.dataInt("comission")).isEqualTo(2000),
                () -> assertThat(thirdExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(10000),
                () -> assertThat(thirdExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(thirdExchanger.dataBin("share_token_id")).isNotNull(),
                () -> assertThat(thirdExchanger.dataInt("share_token_supply")).isEqualTo(
                        //pow(tokenReceiveAmountA,digitTokenA,5,1,4,HALFDOWN)*pow(tokenReceiveAmountB,digitTokenB,5,1,4,HALFDOWN)
                        (long)((new BigDecimal(Math.pow(fundAmount / Math.pow(10, wavesDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue() *
                                new BigDecimal(Math.pow(fundAmount / Math.pow(10, bDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue()) * 100000000L)
                )

        );
    }

    @Test
    void d_canExchangeAB() {

        long exchAmount = 10000000000L;
        long amountTokenABefore = firstExchanger.dataInt("amountTokenA");
        long amountTokenBBefore = firstExchanger.dataInt("amountTokenB");
        long minTokenReceive = 10000000000L;
        long shareTokenSuplyBefore = firstExchanger.dataInt("share_token_supply");
        BigInteger prodAmount = BigInteger.valueOf(exchAmount).multiply(BigInteger.valueOf(amountTokenBBefore));
        long tokenBSendExpected = amountTokenBBefore - ((prodAmount.divide(BigInteger.valueOf(exchAmount + amountTokenABefore)).longValue() * (comissionScaleDelimiter - comission))/ comissionScaleDelimiter);
        caller.invokes(i -> i.dApp(firstExchanger).function("exchanger", arg()).payment(exchAmount, tokenA).fee(1_00500000L));
        for (DataEntry data : firstExchanger.data()) System.out.println(data.getKey() + ": " + data.getValue());

        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(amountTokenABefore + exchAmount),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(tokenBSendExpected),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataInt("exchange_count")).isEqualTo(1),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(firstExchanger.dataBin("share_token_id")).isNotNull(),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSuplyBefore)

        );

    }

    @Test
    void e_canReplenishAB() {

    }


}
