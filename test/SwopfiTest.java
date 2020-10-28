import com.wavesplatform.wavesj.*;
import im.mak.paddle.Account;
import im.mak.paddle.actions.WriteData;
import im.mak.paddle.exceptions.NodeError;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static im.mak.paddle.Async.async;
import static im.mak.paddle.Node.node;
import static im.mak.paddle.actions.invoke.Arg.arg;
import static im.mak.paddle.util.Script.fromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.MethodOrderer.Alphanumeric;

@TestMethodOrder(Alphanumeric.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SwopfiTest {

    private Account firstExchanger, secondExchanger, thirdExchanger, firstCaller, secondCaller;
    private String tokenA;
    private String tokenB;
    private int aDecimal = 8;// 4ok  3ok  8ok 2shareTokenSuply diff300 1longOverflow
    private int bDecimal = 6;// 5ok  3ok  8ok 3shareTokenSuply diff300 1longOverflow
    private int wavesDecimal = 8;
    private int comission = 300;
    private int commisionGovernance = 200;
    private int comissionScaleDelimiter = 1000000;
    private String version = "1.0.0";
    private String shareTokenIdAB;
    //3MP9d7iovdAZtsPeRcq97skdsQH5MPEsfgm govAddress

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
                    firstCaller = new Account(1000_00000000L);
                    tokenA = firstCaller.issues(a -> a.quantity(20000000_00000000L).name("tokenA").decimals(aDecimal)).getId().toString();
                    tokenB = firstCaller.issues(a -> a.quantity(20000000_00000000L).name("tokenB").decimals(bDecimal)).getId().toString();
                }

        );
    }

    @Test
    void a_canFundAB() {
        node().waitForTransaction(tokenA);
        node().waitForTransaction(tokenB);

        long fundAmountA = 10000000000000L;
        long fundAmountB = 1000000000000L;

        int digitsInShareToken = (aDecimal + bDecimal) / 2;//316227.7660168379 * 100000.0 / 100.0
        double a = new BigDecimal(Math.pow(fundAmountA / Math.pow(10, aDecimal), 0.5)).setScale(aDecimal, RoundingMode.HALF_DOWN).doubleValue();//.setScale(aDecimal, RoundingMode.HALF_DOWN)
        double b = new BigDecimal(Math.pow(fundAmountB / Math.pow(10, bDecimal), 0.5)).setScale(bDecimal, RoundingMode.HALF_DOWN).doubleValue();//.setScale(bDecimal, RoundingMode.HALF_DOWN)


        System.out.printf("%s * %s / %s", a, b, Math.pow(10, digitsInShareToken));

        String invokeId = firstCaller.invokes(i -> i.dApp(firstExchanger).function("fund").payment(fundAmountA, tokenA).payment(fundAmountB, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);
        node().waitNBlocks(1);

        shareTokenIdAB = firstExchanger.dataStr("share_token_id");
        long shareTokenSupply = (long) (((new BigDecimal(Math.pow(fundAmountA / Math.pow(10, aDecimal), 0.5)).setScale(aDecimal, RoundingMode.HALF_DOWN).movePointRight(aDecimal).doubleValue() *
                new BigDecimal(Math.pow(fundAmountB / Math.pow(10, bDecimal), 0.5)).setScale(bDecimal, RoundingMode.HALF_DOWN).movePointRight(bDecimal).doubleValue()) / Math.pow(10, digitsInShareToken)));

                assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(fundAmountA),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(fundAmountB),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(firstExchanger.dataStr("share_token_id")).isNotNull(),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSupply),
                () -> assertThat(firstCaller.balance(shareTokenIdAB)).isEqualTo(shareTokenSupply)

        );
    }

    static Stream<Arguments> amountAndCountProvider() {
        return Stream.of(Arguments.of(100), Arguments.of(10000), Arguments.of(1000));
    }
    @ParameterizedTest(name = "firstCaller exchanges {0} tokens")
    @MethodSource("amountAndCountProvider")
    void b_canExchangeAB(int exchTokenAmount) {

        long tokenReceiveAmount = exchTokenAmount * (long) Math.pow(10, aDecimal);
        System.out.println(tokenReceiveAmount);
        long amountTokenA = firstExchanger.dataInt("amountTokenA");
        long amountTokenB = firstExchanger.dataInt("amountTokenB");
        long shareTokenSuplyBefore = firstExchanger.dataInt("share_token_supply");
        long tokenSendAmountWithoutFee =
                BigInteger.valueOf(tokenReceiveAmount)
                        .multiply(BigInteger.valueOf(amountTokenB))
                        .divide(BigInteger.valueOf(tokenReceiveAmount + amountTokenA))
                        .longValue();

        long tokenSendAmountWithFee = (tokenSendAmountWithoutFee * (comissionScaleDelimiter - comission)) / comissionScaleDelimiter;
        long tokenSendGovernance = tokenSendAmountWithoutFee * commisionGovernance / comissionScaleDelimiter;

        String invokeId = firstCaller.invokes(i -> i.dApp(firstExchanger).function("exchanger", arg(tokenSendAmountWithFee)).payment(tokenReceiveAmount, tokenA).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        for (DataEntry data : firstExchanger.data()) System.out.printf("%s: %s%n", data.getKey(), data.getValue());

        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(amountTokenA + tokenReceiveAmount),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(amountTokenB - tokenSendAmountWithFee - tokenSendGovernance),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(firstExchanger.dataStr("share_token_id")).isEqualTo(shareTokenIdAB),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSuplyBefore)

        );

    }

    @Test
    void c_secondCallerReplenishAB() {
        secondCaller = new Account(1000_00000000L);
        String transfer1 = firstCaller.transfers(t -> t
                .to(secondCaller)
                .amount(10000000_00000000L)
                .asset(tokenA)).getId().getBase58String();
        String transfer2 = firstCaller.transfers(t -> t
                .to(secondCaller)
                .amount(10000000_00000000L)
                .asset(tokenB)).getId().getBase58String();
        node().waitForTransaction(transfer1);
        node().waitForTransaction(transfer2);
        long amountTokenABefore = firstExchanger.dataInt("amountTokenA");
        long amountTokenBBefore = firstExchanger.dataInt("amountTokenB");
        long shareTokenSupplyBefore = firstExchanger.dataInt("share_token_supply");
//        long shareTokenAmountBefore = secondCaller.balance(shareTokenId);

        String invokeId = secondCaller.invokes(i -> i.dApp(firstExchanger).function("replenishment").payment(amountTokenABefore, tokenA).payment(amountTokenBBefore, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        long shareTokenToPay = (BigInteger.valueOf(amountTokenABefore).multiply(BigInteger.valueOf(shareTokenSupplyBefore)).divide(BigInteger.valueOf(amountTokenABefore))).longValue();


        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(amountTokenABefore + amountTokenABefore),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(amountTokenBBefore + amountTokenBBefore),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(firstExchanger.dataStr("share_token_id")).isEqualTo(shareTokenIdAB),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSupplyBefore + shareTokenToPay),
                () -> assertThat(secondCaller.balance(shareTokenIdAB)).isEqualTo(shareTokenToPay)

        );
    }

//    @Disabled
    @Test
    void d_secondCallerWithdrawAB() {
        long dAppTokensAmountA = firstExchanger.balance(tokenA);
        long dAppTokensAmountB = firstExchanger.balance(tokenB);
        long secondCallerAmountA = secondCaller.balance(tokenA);
        long secondCallerAmountB = secondCaller.balance(tokenB);
        long shareTokenSupply = firstExchanger.dataInt("share_token_supply");
        long secondCallerShareBalance = secondCaller.balance(shareTokenIdAB);
        long tokensToPayA =
                BigDecimal.valueOf(secondCallerShareBalance)
                        .multiply(BigDecimal.valueOf(dAppTokensAmountA))
                        .divide(BigDecimal.valueOf(shareTokenSupply), 8, RoundingMode.HALF_DOWN).longValue();

        long tokensToPayB =
                BigDecimal.valueOf(secondCallerShareBalance)
                        .multiply(BigDecimal.valueOf(dAppTokensAmountB))
                        .divide(BigDecimal.valueOf(shareTokenSupply), 8, RoundingMode.HALF_DOWN).longValue();

        System.out.println("tokensToPayB: " + tokensToPayB);
        System.out.println("dAppTokensAmountB: " + dAppTokensAmountB);
        System.out.println("secondCallerAmountB: " + secondCallerAmountB);
        System.out.println("tokensToPayA: " + tokensToPayA);
        System.out.println("dAppTokensAmountA: " + dAppTokensAmountA);
        System.out.println("secondCallerAmountA: " + secondCallerAmountA);

        String invokeId = secondCaller.invokes(i -> i.dApp(firstExchanger).function("withdraw").payment(secondCallerShareBalance, shareTokenIdAB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);
        node().waitNBlocks(2);
        //900099908452
        //

        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(dAppTokensAmountA - tokensToPayA),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(dAppTokensAmountB - tokensToPayB),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(firstExchanger.dataStr("share_token_id")).isEqualTo(shareTokenIdAB),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSupply - secondCallerShareBalance),
                () -> assertThat(secondCaller.balance(tokenA)).isEqualTo(secondCallerAmountA + tokensToPayA),
                () -> assertThat(secondCaller.balance(tokenB)).isEqualTo(secondCallerAmountB + tokensToPayB)

        );
    }

    @Disabled
    @Test
    void b_canFundAWaves() {
        long fundAmount = 10000000000L;
        String invokeId = firstCaller.invokes(i -> i.dApp(secondExchanger).function("fund").payment(fundAmount, tokenA).wavesPayment(fundAmount).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        assertAll("data and balances",
                () -> assertThat(secondExchanger.dataInt("amountTokenA")).isEqualTo(fundAmount),
                () -> assertThat(secondExchanger.dataInt("amountTokenB")).isEqualTo(fundAmount),
                () -> assertThat(secondExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(secondExchanger.dataStr("assetIdTokenB")).isEqualTo("WAVES"),
                () -> assertThat(secondExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(secondExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(secondExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(secondExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(secondExchanger.dataBin("share_token_id")).isNotNull(),
                () -> assertThat(secondExchanger.dataInt("share_token_supply")).isEqualTo(
                        //pow(tokenReceiveAmountA,digitTokenA,5,1,4,HALFDOWN)*pow(tokenReceiveAmountB,digitTokenB,5,1,4,HALFDOWN)
                        (long) ((new BigDecimal(Math.pow(fundAmount / Math.pow(10, aDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue() *
                                new BigDecimal(Math.pow(fundAmount / Math.pow(10, wavesDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue()) * 100000000L)
                )

        );
    }

    @Disabled
    @Test
    void c_canFundWavesB() {
        aDecimal = 8;
        long fundAmount = 1000000000L;
        String invokeId = firstCaller.invokes(i -> i.dApp(thirdExchanger).function("fund").wavesPayment(fundAmount).payment(fundAmount, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        assertAll("data and balances",
                () -> assertThat(thirdExchanger.dataInt("amountTokenA")).isEqualTo(fundAmount),
                () -> assertThat(thirdExchanger.dataInt("amountTokenB")).isEqualTo(fundAmount),
                () -> assertThat(thirdExchanger.dataStr("assetIdTokenA")).isEqualTo("WAVES"),
                () -> assertThat(thirdExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(thirdExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(thirdExchanger.dataInt("comission")).isEqualTo(2000),
                () -> assertThat(thirdExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(10000),
                () -> assertThat(thirdExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(thirdExchanger.dataBin("share_token_id")).isNotNull(),
                () -> assertThat(thirdExchanger.dataInt("share_token_supply")).isEqualTo(
                        //pow(tokenReceiveAmountA,digitTokenA,5,1,4,HALFDOWN)*pow(tokenReceiveAmountB,digitTokenB,5,1,4,HALFDOWN)
                        (long) ((new BigDecimal(Math.pow(fundAmount / Math.pow(10, wavesDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue() *
                                new BigDecimal(Math.pow(fundAmount / Math.pow(10, bDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue()) * 100000000L)
                )

        );
    }

    @Disabled
    @Test
    void e_minTokenReceiveFail() {
        long exchAmount = 10000000000L;
        long minTokenReceive = 10000000000L;

        NodeError error = assertThrows(NodeError.class, () ->
                firstCaller.invokes(i -> i.dApp(firstExchanger).function("exchanger", arg(minTokenReceive)).payment(exchAmount, tokenA).fee(1_00500000L))
        );

        assertTrue(error.getMessage().contains("Price has changed dramaticaly. Receiving token amount don't satisfy specified price level"));
    }

    @Disabled
    @Test
    void f_canReplenishAB() {
        long amountTokenABefore = firstExchanger.dataInt("amountTokenA");
        long amountTokenBBefore = firstExchanger.dataInt("amountTokenB");
        long bReplenishAmount = 10000000000L;
        long shareTokenSupplyBefore = firstExchanger.dataInt("share_token_supply");
        String shareTokenId = Base58.encode(firstExchanger.dataBin("share_token_id"));
        long shareTokenAmountBefore = firstCaller.balance(shareTokenId);

        Map<String, Long> insufficientTokenRatioAmounts = new HashMap<>();
        insufficientTokenRatioAmounts.put("aReplenishAmount", aReplenishAmount(990, bReplenishAmount, amountTokenABefore, amountTokenBBefore));
        insufficientTokenRatioAmounts.put("bReplenishAmount", bReplenishAmount);

        NodeError error = assertThrows(NodeError.class, () ->
                firstCaller.invokes(i -> i.dApp(firstExchanger).function("replenishment").payment(insufficientTokenRatioAmounts.get("aReplenishAmount"), tokenA).payment(insufficientTokenRatioAmounts.get("bReplenishAmount"), tokenB).fee(1_00500000L))
        );
        assertTrue(error.getMessage().contains("incorrect assets amount"));

        Map<String, Long> tooBigTokenRatioAmounts = new HashMap<>();
        tooBigTokenRatioAmounts.put("aReplenishAmount", aReplenishAmount(1010, bReplenishAmount, amountTokenABefore, amountTokenBBefore));
        tooBigTokenRatioAmounts.put("bReplenishAmount", bReplenishAmount);

        NodeError error2 = assertThrows(NodeError.class, () ->
                firstCaller.invokes(i -> i.dApp(firstExchanger).function("replenishment").payment(tooBigTokenRatioAmounts.get("aReplenishAmount"), tokenA).payment(tooBigTokenRatioAmounts.get("bReplenishAmount"), tokenB).fee(1_00500000L))
        );
        assertTrue(error2.getMessage().contains("incorrect assets amount"));

        Map<String, Long> replenishAmounts = new HashMap<>();
        replenishAmounts.put("aReplenishAmount", aReplenishAmount(991, bReplenishAmount, amountTokenABefore, amountTokenBBefore));
        replenishAmounts.put("bReplenishAmount", bReplenishAmount);

        String invokeId = firstCaller.invokes(i -> i.dApp(firstExchanger).function("replenishment").payment(replenishAmounts.get("aReplenishAmount"), tokenA).payment(replenishAmounts.get("bReplenishAmount"), tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        long shareTokenToPay = (BigInteger.valueOf(replenishAmounts.get("aReplenishAmount")).multiply(BigInteger.valueOf(shareTokenSupplyBefore)).divide(BigInteger.valueOf(amountTokenABefore))).longValue();


        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(amountTokenABefore + replenishAmounts.get("aReplenishAmount")),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(amountTokenBBefore + bReplenishAmount),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(2000),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(10000),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(firstExchanger.dataBin("share_token_id")).isNotNull(),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSupplyBefore + shareTokenToPay),
                () -> assertThat(firstCaller.balance(shareTokenId)).isEqualTo(shareTokenAmountBefore + shareTokenToPay)

        );
    }

    @Disabled
    @Test
    void g_canReplenishAWaves() {
        long replenishAmount = 10000000000L;
        long amountTokenABefore = firstExchanger.dataInt("amountTokenA");
        System.out.println(amountTokenABefore);
        long amountTokenBBefore = firstExchanger.dataInt("amountTokenB");
        System.out.println(amountTokenBBefore);
        long shareTokenSupplyBefore = firstExchanger.dataInt("share_token_supply");
        String shareTokenId = Base58.encode(firstExchanger.dataBin("share_token_id"));

        Map<String, Long> insufficientTokenRatio = new HashMap<>();
        insufficientTokenRatio.put("aReplenishAmount", 9900000000L);//tokenRatio = 990
        insufficientTokenRatio.put("wReplenishAmount", 10000000000L);

        NodeError error = assertThrows(NodeError.class, () ->
                firstCaller.invokes(i -> i.dApp(firstExchanger).function("replenishment").payment(insufficientTokenRatio.get("aReplenishAmount"), tokenA).wavesPayment(insufficientTokenRatio.get("wReplenishAmount")).fee(1_00500000L))
        );
        assertTrue(error.getMessage().contains("incorrect assets amount"));

        Map<String, Long> tooBigTokenRatio = new HashMap<>();
        tooBigTokenRatio.put("aReplenishAmount", 10100000000L);//tokenRatio = 1010
        tooBigTokenRatio.put("wReplenishAmount", 10000000000L);

        NodeError error2 = assertThrows(NodeError.class, () ->
                firstCaller.invokes(i -> i.dApp(firstExchanger).function("replenishment").payment(tooBigTokenRatio.get("aReplenishAmount"), tokenA).wavesPayment(tooBigTokenRatio.get("bReplenishAmount")).fee(1_00500000L))
        );
        assertTrue(error2.getMessage().contains("incorrect assets amount"));

        long aReplenishAmount = 992000000L;
        long bReplenishAmount = 1000000000L;

        String invokeId = firstCaller.invokes(i -> i.dApp(firstExchanger).function("replenishment").payment(aReplenishAmount, tokenA).payment(bReplenishAmount, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        long shareTokenToPay = (BigInteger.valueOf(aReplenishAmount).multiply(BigInteger.valueOf(shareTokenSupplyBefore)).divide(BigInteger.valueOf(amountTokenABefore))).longValue();


        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(amountTokenABefore + aReplenishAmount),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(amountTokenBBefore + bReplenishAmount),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(2000),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(10000),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(firstExchanger.dataBin("share_token_id")).isNotNull(),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSupplyBefore + shareTokenToPay),
                () -> assertThat(firstCaller.balance(shareTokenId)).isEqualTo(shareTokenToPay)

        );
//        String invokeId  = firstCaller.invokes(i -> i.dApp(secondExchanger).function("fund").payment(fundAmount,tokenA).wavesPayment(fundAmount).fee(1_00500000L)).getId().getBase58String();
//        node().waitForTransaction(invokeId);
//
//        assertAll("data and balances",
//                () -> assertThat(secondExchanger.dataInt("amountTokenA")).isEqualTo(fundAmount),
//                () -> assertThat(secondExchanger.dataInt("amountTokenB")).isEqualTo(ReplenishAmount),
//                () -> assertThat(secondExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
//                () -> assertThat(secondExchanger.dataStr("assetIdTokenB")).isEqualTo("WAVES"),
//                () -> assertThat(secondExchanger.dataInt("exchange_count")).isEqualTo(0),
//                () -> assertThat(secondExchanger.dataBool("status")).isEqualTo(true),
//                () -> assertThat(secondExchanger.dataInt("comission")).isEqualTo(2000),
//                () -> assertThat(secondExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(10000),
//                () -> assertThat(secondExchanger.dataStr("version")).isEqualTo("0.0.2"),
//                () -> assertThat(secondExchanger.dataBin("share_token_id")).isNotNull(),
//                () -> assertThat(secondExchanger.dataInt("share_token_supply")).isEqualTo(
//                        //pow(tokenReceiveAmountA,digitTokenA,5,1,4,HALFDOWN)*pow(tokenReceiveAmountB,digitTokenB,5,1,4,HALFDOWN)
//                        (long)((new BigDecimal(Math.pow(ReplenishAmount / Math.pow(10, aDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue() *
//                                new BigDecimal(Math.pow(ReplenishAmount / Math.pow(10, wavesDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue()) * 100000000L)
//                )
//
//        );
    }

    @Disabled
    @Test
    void h_canReplenishWavesB() {
        aDecimal = 8;
        long fundAmount = 1000000000L;
        String invokeId = firstCaller.invokes(i -> i.dApp(thirdExchanger).function("fund").wavesPayment(fundAmount).payment(fundAmount, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        assertAll("data and balances",
                () -> assertThat(thirdExchanger.dataInt("amountTokenA")).isEqualTo(fundAmount),
                () -> assertThat(thirdExchanger.dataInt("amountTokenB")).isEqualTo(fundAmount),
                () -> assertThat(thirdExchanger.dataStr("assetIdTokenA")).isEqualTo("WAVES"),
                () -> assertThat(thirdExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(thirdExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(thirdExchanger.dataInt("comission")).isEqualTo(2000),
                () -> assertThat(thirdExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(10000),
                () -> assertThat(thirdExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(thirdExchanger.dataBin("share_token_id")).isNotNull(),
                () -> assertThat(thirdExchanger.dataInt("share_token_supply")).isEqualTo(
                        //pow(tokenReceiveAmountA,digitTokenA,5,1,4,HALFDOWN)*pow(tokenReceiveAmountB,digitTokenB,5,1,4,HALFDOWN)
                        (long) ((new BigDecimal(Math.pow(fundAmount / Math.pow(10, wavesDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue() *
                                new BigDecimal(Math.pow(fundAmount / Math.pow(10, bDecimal), 0.5)).setScale(8, RoundingMode.HALF_DOWN).doubleValue()) * 100000000L)
                )

        );
    }

//    private long calculateReplShareTokenToPay() {
//        BigInteger firstProd = BigInteger.valueOf(tokenReceiveAmountA).multiply(BigInteger.valueOf(1000_000_000_000L * 1000L));
//
//        return firstProd.divide(BigInteger.valueOf(dAppTokensAmountA));
//    }

    private int calculateReplTokenRatio(long tokenReceiveAmountA, long tokenReceiveAmountB, long dAppTokensAmountA, long dAppTokensAmountB) {
        BigInteger firstProd = BigInteger.valueOf(tokenReceiveAmountA).multiply(BigInteger.valueOf(1000_000_000_000L * 1000L));
        BigInteger firstFraction = firstProd.divide(BigInteger.valueOf(dAppTokensAmountA));

        BigInteger secondProd = BigInteger.valueOf(tokenReceiveAmountB).multiply(BigInteger.valueOf(1000_000_000_000L));
        BigInteger secondFraction = secondProd.divide(BigInteger.valueOf(dAppTokensAmountB));

        return firstFraction.divide(secondFraction).intValue();
    }

    private long aReplenishAmount(int tokenRatio, long bReplenishAmount, long amountTokenABefore, long amountTokenBBefore) {
        return ((BigInteger.valueOf(tokenRatio)
                .multiply(BigInteger.valueOf(amountTokenABefore))
                .multiply(BigInteger.valueOf(1000000000000L))
                .multiply(BigInteger.valueOf(amountTokenBBefore)))
                .divide(
                        BigInteger.valueOf(bReplenishAmount)
                                .multiply(BigInteger.valueOf(1000000000000L * 1000L)))).longValue();

    }


}
