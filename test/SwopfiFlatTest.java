import com.wavesplatform.wavesj.Base58;
import com.wavesplatform.wavesj.DataEntry;
import com.wavesplatform.wavesj.transactions.InvokeScriptTransaction;
import im.mak.paddle.Account;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.SQLOutput;
import java.util.stream.Stream;

import ch.obermuhlner.math.big.BigDecimalMath;

import static im.mak.paddle.Async.async;
import static im.mak.paddle.Node.node;
import static im.mak.paddle.actions.invoke.Arg.arg;
import static im.mak.paddle.util.Script.fromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.Alphanumeric.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SwopfiFlatTest {
    private Account firstExchanger, secondExchanger, thirdExchanger, firstCaller, secondCaller;
    private String tokenA;
    private String tokenB;
    private int aDecimal = 6;
    private int bDecimal = 6;
    private int wavesDecimal = 8;
    private int comission = 500;
    private int comissionGovernance = 200;
    private int comissionScaleDelimiter = 1000000;
    private int scaleValue8 = 100000000;
    private int scaleValue8Digits = 8;
    private long scaleValue12 = 1000000000000L;
    private int ratioThresholdMax = 100000000;
    private int ratioThresholdMin = 99999000;
    private double alpha = 0.5;
    private double betta = 0.46;
    private String version = "2.0.0";
    private String shareTokenId;


    @BeforeAll
    void before() {
        async(
                () -> {
                    firstExchanger = new Account(1000_00000000L);
                    firstExchanger.setsScript(s -> s.script(fromFile("dApps/exchangerFlat.ride")));
                },
                () -> {
                    secondExchanger = new Account(1000_00000000L);
                    secondExchanger.setsScript(s -> s.script(fromFile("dApps/exchangerFlat.ride")));
                },
                () -> {
                    thirdExchanger = new Account(1000_00000000L);
                    thirdExchanger.setsScript(s -> s.script(fromFile("dApps/exchangerFlat.ride")));
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

        //20051.94116
        //20092
        long fundAmountA = 6453897666L;
        long fundAmountB = 10000000000L;
//        long fundAmountA = 20077977934L;
//        long fundAmountB = 20077971232L;
        int digitsInShareToken = (aDecimal + bDecimal) / 2;
        String invokeId = firstCaller.invokes(i -> i.dApp(firstExchanger).function("fund").payment(fundAmountA, tokenA).payment(fundAmountB, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);
        node().waitNBlocks(1);
        long shareTokenSupply = (long) (((new BigDecimal(Math.pow(fundAmountA / Math.pow(10, aDecimal), 0.5)).setScale(aDecimal, RoundingMode.HALF_DOWN).movePointRight(aDecimal).doubleValue() *
                new BigDecimal(Math.pow(fundAmountB / Math.pow(10, bDecimal), 0.5)).setScale(bDecimal, RoundingMode.HALF_DOWN).movePointRight(bDecimal).doubleValue()) / Math.pow(10, digitsInShareToken)));
        shareTokenId = firstExchanger.dataStr("share_token_id");

        System.out.println();

        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(fundAmountA),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(fundAmountB),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataInt("invariant")).isEqualTo(invariantCalc(fundAmountA, fundAmountB).longValue()),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo("2.0.0"),
                () -> assertThat(firstExchanger.dataStr("share_token_id")).isNotNull(),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSupply),
                () -> assertThat(firstCaller.balance(shareTokenId)).isEqualTo(shareTokenSupply)

        );
    }

    private static Stream<Arguments> bExchangeProvider() {
        return Stream.of(Arguments.of(4010L));
    }

    @Disabled
    @ParameterizedTest(name = "firstCaller exchanges {0} tokenB")
    @MethodSource("bExchangeProvider")
    void b_canExchangeB(long exchTokenAmount) {
        shareTokenId = firstExchanger.dataStr("share_token_id");
        long tokenReceiveAmount = exchTokenAmount * (long) Math.pow(10, bDecimal);
        System.out.println(tokenReceiveAmount);
        long amountTokenA = firstExchanger.dataInt("amountTokenA");
        long amountTokenB = firstExchanger.dataInt("amountTokenB");
        long invariant = firstExchanger.dataInt("invariant");
        long shareTokenSuplyBefore = firstExchanger.dataInt("share_token_supply");
        long amountSendEstimated = amountToSendEstimated(amountTokenB, amountTokenA, amountTokenB + tokenReceiveAmount);
        long minTokenReceiveAmount = amountSendEstimated;
        long tokenSendAmountWithoutFee = calculateHowManySendTokenA(amountSendEstimated, minTokenReceiveAmount, amountTokenA, amountTokenB, tokenReceiveAmount, invariant);
        long tokenSendAmountWithFee = tokenSendAmountWithoutFee * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
        long tokenSendGovernance = tokenSendAmountWithoutFee * comissionGovernance / comissionScaleDelimiter;


        InvokeScriptTransaction invoke = firstCaller.invokes(i -> i.dApp(firstExchanger).function("exchanger", arg(amountSendEstimated), arg(minTokenReceiveAmount)).payment(tokenReceiveAmount, tokenB).fee(1_00500000L));//.getId().getBase58String();
        node().waitForTransaction(invoke.getId().getBase58String());
        long invariantAfter;
        if (invoke.getPayments().get(0).getAssetId() == tokenA) {
            invariantAfter = invariantCalc(amountTokenA + tokenReceiveAmount, amountTokenB - tokenSendAmountWithFee - tokenSendGovernance).longValue();
        } else {
            invariantAfter = invariantCalc(amountTokenA - tokenSendAmountWithFee - tokenSendGovernance, amountTokenB + tokenReceiveAmount).longValue();
        }

        for (DataEntry data : firstExchanger.data()) System.out.printf("%s: %s%n", data.getKey(), data.getValue());
        //34696943458793
        //34696943458793

        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(amountTokenA - tokenSendAmountWithFee - tokenSendGovernance),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(amountTokenB + tokenReceiveAmount),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataInt("invariant")).isEqualTo(invariantAfter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSuplyBefore)

        );

    }

    private static Stream<Arguments> aExchangeProvider() {
        return Stream.of(Arguments.of(4010L)/*, Arguments.of(10000L), Arguments.of(1000L)*/);
    }
    @Disabled
    @ParameterizedTest(name = "firstCaller exchanges {0} tokens A")
    @MethodSource("aExchangeProvider")
    void c_canExchangeA(long exchTokenAmount) {
        shareTokenId = firstExchanger.dataStr("share_token_id");
        long tokenReceiveAmount = exchTokenAmount * (long) Math.pow(10, bDecimal);
        System.out.println(tokenReceiveAmount);
        long amountTokenA = firstExchanger.dataInt("amountTokenA");
        long amountTokenB = firstExchanger.dataInt("amountTokenB");
        long invariant = firstExchanger.dataInt("invariant");
        long shareTokenSuplyBefore = firstExchanger.dataInt("share_token_supply");
        long amountSendEstimated = amountToSendEstimated(amountTokenA, amountTokenB, amountTokenA + tokenReceiveAmount);
        long minTokenReceiveAmount = amountSendEstimated;
        long tokenSendAmountWithoutFee = calculateHowManySendTokenB(amountSendEstimated, minTokenReceiveAmount, amountTokenA, amountTokenB, tokenReceiveAmount, invariant);
        long tokenSendAmountWithFee = tokenSendAmountWithoutFee * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
        long tokenSendGovernance = tokenSendAmountWithoutFee * comissionGovernance / comissionScaleDelimiter;


        InvokeScriptTransaction invoke = firstCaller.invokes(i -> i.dApp(firstExchanger).function("exchanger", arg(amountSendEstimated), arg(minTokenReceiveAmount)).payment(tokenReceiveAmount, tokenA).fee(1_00500000L));//.getId().getBase58String();
        node().waitForTransaction(invoke.getId().getBase58String());
        long invariantAfter;
        if (invoke.getPayments().get(0).getAssetId() == tokenA) {
            invariantAfter = invariantCalc(amountTokenA + tokenReceiveAmount, amountTokenB - tokenSendAmountWithFee - tokenSendGovernance).longValue();
        } else {
            invariantAfter = invariantCalc(amountTokenA - tokenSendAmountWithFee - tokenSendGovernance, amountTokenB + tokenReceiveAmount).longValue();
        }

        for (DataEntry data : firstExchanger.data()) System.out.printf("%s: %s%n", data.getKey(), data.getValue());
        //34696943458793
        //34696943458793

        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(amountTokenA + tokenReceiveAmount),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(amountTokenB - tokenSendAmountWithFee - tokenSendGovernance),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataInt("invariant")).isEqualTo(invariantAfter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSuplyBefore)

        );

    }

    static Stream<Arguments> replenishOneTokenAProvider() {
        //Arguments.of(19200000L, 9599851L, 9598306L) 200k
        //Arguments.of(19199999L, 9599851L, 9598304L) 200k neok
        //
        //Arguments.of(8000000L, 3999140L, 3999265L) 20077977934/20077971232 ok
        //Arguments.of(7999999L, 3999140L, 3999264L) 20077977934/20077971232 ok
        //Arguments.of(6000000L, 2999416L, 2999687L) 20077977934/20077971232 neok
        //Arguments.of(7000000L, 3499290L, 3499492L) 20077977934/20077971232 neok
        //Arguments.of(7750000L, 3874180L, 3874325L) 20077977934/20077971232 neok
        //Arguments.of(7900000L, 3949156L, 3949288L) 20077977934/20077971232 neok
        //Arguments.of(7950000L, 3974148L, 3974276L) 20077977934/20077971232 neok
        //Arguments.of(7975000L, 3986644L, 3986768L) 20077977934/20077971232 neok
        //Arguments.of(7985000L, 3991643L, 3991767L) 20077977934/20077971232 neok
        //Arguments.of(7990000L, 3994143L, 3994265L) 20077977934/20077971232 neok
        //Arguments.of(7995000L, 3996642L, 3996765L) 20077977934/20077971232 ok
        //Arguments.of(7993000L, 3995642L, 3995737L) 20077977934/20077971232 neok
        //Arguments.of(7994000L, 3996142L, 3996267L) 20077977934/20077971232 neok
        //Arguments.of(7994999L, 3996642L, 3996760L) 20077977934/20077971232 neok
        //следовательно, для 20077977934/20077971232 минимальный реплениш = 7995000 = 7,995 usdn
        return Stream.of(Arguments.of(50000000L, 19674241L, 19474561L), Arguments.of(100000000L, 50031789L, 50078744L), Arguments.of(10000000000L, 5003055952L, 5002989259L));
    }
    @ParameterizedTest(name = "firstCaller replenish {0} tokenA, {1} virtualSwapTokenPay, {2} virtualSwapTokenGet")
    @MethodSource("replenishOneTokenAProvider")
    void d_firstCallerReplenishOneTokenA(long tokenReceiveAmount, long virtualSwapTokenPay, long virtualSwapTokenGet) {
        System.out.println(firstExchanger.dataInt("amountTokenA"));
        System.out.println(firstExchanger.dataInt("amountTokenB"));
        long dAppTokensAmountA = firstExchanger.dataInt("amountTokenA");
        long dAppTokensAmountB = firstExchanger.dataInt("amountTokenB");
        long tokenShareSupply = firstExchanger.dataInt("share_token_supply");
        long callerTokenShareBalance = firstCaller.balance(shareTokenId);
//        long tokenReceiveAmount = replenishAmount * (long) Math.pow(10, aDecimal);
//        long virtualSwapTokenPay = virtualPayAmount * (long) Math.pow(10, aDecimal);
//        long virtualSwapTokenGet = virtualGetAmount * (long) Math.pow(10, aDecimal);
        long amountVirtualReplanishTokenA = tokenReceiveAmount - virtualSwapTokenPay;
        long amountVirtualReplanishTokenB = virtualSwapTokenGet;
        long contractBalanceAfterVirtualSwapTokenA = dAppTokensAmountA + virtualSwapTokenPay;
        long contractBalanceAfterVirtualSwapTokenB = dAppTokensAmountB - virtualSwapTokenGet;
        System.out.println("contractBalanceAfterVirtualSwapTokenA " + contractBalanceAfterVirtualSwapTokenA);
        System.out.println("contractBalanceAfterVirtualSwapTokenB " + contractBalanceAfterVirtualSwapTokenB);
        //fraction(amountVirtualReplanishTokenA,scaleValue8,contractBalanceAfterVirtualSwapTokenA)
        //if true then throw(toString(fraction(contractBalanceAfterVirtualSwapTokenA,scaleValue8*scaleValue8,contractBalanceAfterVirtualSwapTokenB)) + " " toString(fraction(amountVirtualReplanishTokenA,scaleValue8,amountVirtualReplanishTokenB))) else

        System.out.println("this");
        System.out.println(BigDecimal.valueOf(contractBalanceAfterVirtualSwapTokenA).divide(BigDecimal.valueOf(contractBalanceAfterVirtualSwapTokenB), 30, RoundingMode.HALF_DOWN));
        System.out.println(BigDecimal.valueOf(amountVirtualReplanishTokenA).divide(BigDecimal.valueOf(amountVirtualReplanishTokenB), 30, RoundingMode.HALF_DOWN));
        double ratioShareTokensInA = BigDecimal.valueOf(amountVirtualReplanishTokenA).multiply(BigDecimal.valueOf(scaleValue8)).divide(BigDecimal.valueOf(contractBalanceAfterVirtualSwapTokenA), 8, RoundingMode.HALF_DOWN).longValue();
        System.out.println("ratioShareTokensInA" + ratioShareTokensInA);
        //fraction(amountVirtualReplanishTokenB,scaleValue8,contractBalanceAfterVirtualSwapTokenB)
        double ratioShareTokensInB = BigDecimal.valueOf(amountVirtualReplanishTokenB).multiply(BigDecimal.valueOf(scaleValue8)).divide(BigDecimal.valueOf(contractBalanceAfterVirtualSwapTokenB), 8, RoundingMode.HALF_DOWN).longValue();
        System.out.println("ratioShareTokensInB" + ratioShareTokensInB);

        long shareTokenToPayAmount;
        if (ratioShareTokensInA <= ratioShareTokensInB) {
            //fraction(ratioShareTokensInA,tokenShareSupply,scaleValue8)
            shareTokenToPayAmount = BigDecimal.valueOf(ratioShareTokensInA).multiply(BigDecimal.valueOf(tokenShareSupply)).divide(BigDecimal.valueOf(scaleValue8), 8, RoundingMode.HALF_DOWN).longValue();
        } else {
            //fraction(ratioShareTokensInB,tokenShareSupply,scaleValue8)
            shareTokenToPayAmount = BigDecimal.valueOf(ratioShareTokensInB).multiply(BigDecimal.valueOf(tokenShareSupply)).divide(BigDecimal.valueOf(scaleValue8), 8, RoundingMode.HALF_DOWN).longValue();

        }

        System.out.println("shareTokenToPayAmount: " + shareTokenToPayAmount);
        long invariantCalcualated = invariantCalc(dAppTokensAmountA + tokenReceiveAmount, dAppTokensAmountB).longValue();

        String invokeId = firstCaller.invokes(i -> i.dApp(firstExchanger).function("replanishmentWithOneToken", arg(virtualSwapTokenPay), arg(virtualSwapTokenGet)).payment(tokenReceiveAmount, tokenA).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);
        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(dAppTokensAmountA + tokenReceiveAmount),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(dAppTokensAmountB),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("invariant")).isEqualTo(invariantCalcualated),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(firstExchanger.dataStr("share_token_id")).isEqualTo(shareTokenId),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(tokenShareSupply + shareTokenToPayAmount),
                () -> assertThat(firstCaller.balance(shareTokenId)).isEqualTo(callerTokenShareBalance + shareTokenToPayAmount)

        );
    }

    static Stream<Arguments> replenishOneTokenBProvider() {
        //Arguments.of(19200000L, 9599851L, 9598306L) 200k
        //Arguments.of(19199999L, 9599851L, 9598304L) 200k neok
        //
        //Arguments.of(8000000L, 3999140L, 3999265L) 20077977934/20077971232 ok
        //Arguments.of(7999999L, 3999140L, 3999264L) 20077977934/20077971232 ok
        //Arguments.of(6000000L, 2999416L, 2999687L) 20077977934/20077971232 neok
        //Arguments.of(7000000L, 3499290L, 3499492L) 20077977934/20077971232 neok
        //Arguments.of(7750000L, 3874180L, 3874325L) 20077977934/20077971232 neok
        //Arguments.of(7900000L, 3949156L, 3949288L) 20077977934/20077971232 neok
        //Arguments.of(7950000L, 3974148L, 3974276L) 20077977934/20077971232 neok
        //Arguments.of(7975000L, 3986644L, 3986768L) 20077977934/20077971232 neok
        //Arguments.of(7985000L, 3991643L, 3991767L) 20077977934/20077971232 neok
        //Arguments.of(7990000L, 3994143L, 3994265L) 20077977934/20077971232 neok
        //Arguments.of(7995000L, 3996642L, 3996765L) 20077977934/20077971232 ok
        //Arguments.of(7993000L, 3995642L, 3995737L) 20077977934/20077971232 neok
        //Arguments.of(7994000L, 3996142L, 3996267L) 20077977934/20077971232 neok
        //Arguments.of(7994999L, 3996642L, 3996760L) 20077977934/20077971232 neok
        //следовательно, для 20077977934/20077971232 минимальный реплениш = 7995000 = 7,995 usdn
        return Stream.of(Arguments.of(50000000L, 19674241L, 19474561L), Arguments.of(100000000L, 50031789L, 50078744L), Arguments.of(10000000000L, 5003055952L, 5002989259L));
    }
    @ParameterizedTest(name = "firstCaller replenish {0} tokenB, {1} virtualSwapTokenPay, {2} virtualSwapTokenGet")
    @MethodSource("replenishOneTokenBProvider")
    void e_firstCallerReplenishOneTokenB(long tokenReceiveAmount, long virtualSwapTokenPay, long virtualSwapTokenGet) {
        System.out.println(firstExchanger.dataInt("amountTokenA"));
        System.out.println(firstExchanger.dataInt("amountTokenB"));
        long dAppTokensAmountA = firstExchanger.dataInt("amountTokenA");
        long dAppTokensAmountB = firstExchanger.dataInt("amountTokenB");
        long tokenShareSupply = firstExchanger.dataInt("share_token_supply");
        long callerTokenShareBalance = firstCaller.balance(shareTokenId);
//        long tokenReceiveAmount = replenishAmount * (long) Math.pow(10, aDecimal);
//        long virtualSwapTokenPay = virtualPayAmount * (long) Math.pow(10, aDecimal);
//        long virtualSwapTokenGet = virtualGetAmount * (long) Math.pow(10, aDecimal);
        long amountVirtualReplanishTokenA = tokenReceiveAmount - virtualSwapTokenPay;
        long amountVirtualReplanishTokenB = virtualSwapTokenGet;
        long contractBalanceAfterVirtualSwapTokenA = dAppTokensAmountA + virtualSwapTokenPay;
        long contractBalanceAfterVirtualSwapTokenB = dAppTokensAmountB - virtualSwapTokenGet;
        System.out.println("contractBalanceAfterVirtualSwapTokenA " + contractBalanceAfterVirtualSwapTokenA);
        System.out.println("contractBalanceAfterVirtualSwapTokenB " + contractBalanceAfterVirtualSwapTokenB);
        //fraction(amountVirtualReplanishTokenA,scaleValue8,contractBalanceAfterVirtualSwapTokenA)
        //if true then throw(toString(fraction(contractBalanceAfterVirtualSwapTokenA,scaleValue8*scaleValue8,contractBalanceAfterVirtualSwapTokenB)) + " " toString(fraction(amountVirtualReplanishTokenA,scaleValue8,amountVirtualReplanishTokenB))) else

        System.out.println("this");
        System.out.println(BigDecimal.valueOf(contractBalanceAfterVirtualSwapTokenA).divide(BigDecimal.valueOf(contractBalanceAfterVirtualSwapTokenB), 30, RoundingMode.HALF_DOWN));
        System.out.println(BigDecimal.valueOf(amountVirtualReplanishTokenA).divide(BigDecimal.valueOf(amountVirtualReplanishTokenB), 30, RoundingMode.HALF_DOWN));
        double ratioShareTokensInA = BigDecimal.valueOf(amountVirtualReplanishTokenA).multiply(BigDecimal.valueOf(scaleValue8)).divide(BigDecimal.valueOf(contractBalanceAfterVirtualSwapTokenA), 8, RoundingMode.HALF_DOWN).longValue();
        System.out.println("ratioShareTokensInA" + ratioShareTokensInA);
        //fraction(amountVirtualReplanishTokenB,scaleValue8,contractBalanceAfterVirtualSwapTokenB)
        double ratioShareTokensInB = BigDecimal.valueOf(amountVirtualReplanishTokenB).multiply(BigDecimal.valueOf(scaleValue8)).divide(BigDecimal.valueOf(contractBalanceAfterVirtualSwapTokenB), 8, RoundingMode.HALF_DOWN).longValue();
        System.out.println("ratioShareTokensInB" + ratioShareTokensInB);

        long shareTokenToPayAmount;
        if (ratioShareTokensInA <= ratioShareTokensInB) {
            //fraction(ratioShareTokensInA,tokenShareSupply,scaleValue8)
            shareTokenToPayAmount = BigDecimal.valueOf(ratioShareTokensInA).multiply(BigDecimal.valueOf(tokenShareSupply)).divide(BigDecimal.valueOf(scaleValue8), 8, RoundingMode.HALF_DOWN).longValue();
        } else {
            //fraction(ratioShareTokensInB,tokenShareSupply,scaleValue8)
            shareTokenToPayAmount = BigDecimal.valueOf(ratioShareTokensInB).multiply(BigDecimal.valueOf(tokenShareSupply)).divide(BigDecimal.valueOf(scaleValue8), 8, RoundingMode.HALF_DOWN).longValue();

        }

        System.out.println("shareTokenToPayAmount: " + shareTokenToPayAmount);
        long invariantCalcualated = invariantCalc(dAppTokensAmountA + tokenReceiveAmount, dAppTokensAmountB).longValue();

        String invokeId = firstCaller.invokes(i -> i.dApp(firstExchanger).function("replanishmentWithOneToken", arg(virtualSwapTokenPay), arg(virtualSwapTokenGet)).payment(tokenReceiveAmount, tokenA).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);
        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(dAppTokensAmountA + tokenReceiveAmount),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(dAppTokensAmountB),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("invariant")).isEqualTo(invariantCalcualated),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(firstExchanger.dataStr("share_token_id")).isEqualTo(shareTokenId),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(tokenShareSupply + shareTokenToPayAmount),
                () -> assertThat(firstCaller.balance(shareTokenId)).isEqualTo(callerTokenShareBalance + shareTokenToPayAmount)

        );
    }

    @Test
    void e_secondCallerReplenishAB() {
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
        long dAppTokensAmountA = firstExchanger.dataInt("amountTokenA");
        long dAppTokensAmountB = firstExchanger.dataInt("amountTokenB");
        long shareTokenSupplyBefore = firstExchanger.dataInt("share_token_supply");
        long tokenReceiveAmountA = dAppTokensAmountA;
        long tokenReceiveAmountB = dAppTokensAmountB;
//        long shareTokenAmountBefore = secondCaller.balance(shareTokenId);

        String invokeId = secondCaller.invokes(i -> i.dApp(firstExchanger).function("replenishmentWithTwoToken", arg(-1000000000000L)).payment(tokenReceiveAmountA, tokenA).payment(tokenReceiveAmountB, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        long shareTokenToPay = (BigInteger.valueOf(dAppTokensAmountA).multiply(BigInteger.valueOf(shareTokenSupplyBefore)).divide(BigInteger.valueOf(dAppTokensAmountA))).longValue();


        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(dAppTokensAmountA + tokenReceiveAmountA),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(dAppTokensAmountB + tokenReceiveAmountB),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("invariant")).isEqualTo(invariantCalc(dAppTokensAmountA + tokenReceiveAmountA, dAppTokensAmountB + tokenReceiveAmountB).longValue()),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(firstExchanger.dataStr("share_token_id")).isEqualTo(shareTokenId),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSupplyBefore + shareTokenToPay),
                () -> assertThat(secondCaller.balance(shareTokenId)).isEqualTo(shareTokenToPay)

        );
    }

    @Test
    void f_secondCallerWithdrawAB() {
        long dAppTokensAmountA = firstExchanger.balance(tokenA);
        long dAppTokensAmountB = firstExchanger.balance(tokenB);
        long secondCallerAmountA = secondCaller.balance(tokenA);
        long secondCallerAmountB = secondCaller.balance(tokenB);
        long shareTokenSupply = firstExchanger.dataInt("share_token_supply");
        long secondCallerShareBalance = secondCaller.balance(shareTokenId);
        long tokensToPayA =
                BigDecimal.valueOf(secondCallerShareBalance)
                        .multiply(BigDecimal.valueOf(dAppTokensAmountA))
                        .divide(BigDecimal.valueOf(shareTokenSupply), 8, RoundingMode.HALF_DOWN).longValue();

        long tokensToPayB =
                BigDecimal.valueOf(secondCallerShareBalance)
                        .multiply(BigDecimal.valueOf(dAppTokensAmountB))
                        .divide(BigDecimal.valueOf(shareTokenSupply), 8, RoundingMode.HALF_DOWN).longValue();

        String invokeId = secondCaller.invokes(i -> i.dApp(firstExchanger).function("withdraw").payment(secondCallerShareBalance, shareTokenId).fee(1_00500000L)).getId().getBase58String();
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
                () -> assertThat(firstExchanger.dataInt("invariant")).isEqualTo(invariantCalc(dAppTokensAmountA - tokensToPayA, dAppTokensAmountB - tokensToPayB).longValue()),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(firstExchanger.dataStr("share_token_id")).isEqualTo(shareTokenId),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSupply - secondCallerShareBalance),
                () -> assertThat(secondCaller.balance(tokenA)).isEqualTo(secondCallerAmountA + tokensToPayA),
                () -> assertThat(secondCaller.balance(tokenB)).isEqualTo(secondCallerAmountB + tokensToPayB)

        );
    }


    //func skeweness (x: Int,y:Int) = {(fraction(scaleValue8,x,y)+fraction(scaleValue8,y,x))/2}
//    private double skeweness(long x, long y) {
//        // ( (scaleValue8 * x / y) + (scaleValue8 * y / x) )
//        return ((BigInteger.valueOf(scaleValue8)
//                .multiply(BigInteger.valueOf(x))
//                .divide(BigInteger.valueOf(y)))
//                    .add(BigInteger.valueOf(scaleValue8)
//                            .multiply(BigInteger.valueOf(y))
//                            .divide(BigInteger.valueOf(x))))
//                .doubleValue() / 2;
//    }

    //    private double skeweness(long x, long y) {
//        return (BigInteger.valueOf(x)
//                .divide(BigInteger.valueOf(y)))
//                .add(BigInteger.valueOf(y)
//                        .divide(BigInteger.valueOf(x)))
//                .doubleValue() / 2;
//    }
//    private double skeweness2(long x, long y) {
//        return (BigDecimal.valueOf(x)
//                .divide(BigDecimal.valueOf(y), 8, RoundingMode.HALF_DOWN))
//                .add(BigDecimal.valueOf(y)
//                        .divide(BigDecimal.valueOf(x),8, RoundingMode.HALF_DOWN))
//                .divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_DOWN).doubleValue();
//    }

    private double skeweness(long x, long y) {
        return (BigDecimal.valueOf(x)
                .divide(BigDecimal.valueOf(y), 12, RoundingMode.DOWN))
                .add(BigDecimal.valueOf(y)
                        .divide(BigDecimal.valueOf(x), 12, RoundingMode.DOWN))
                .divide(BigDecimal.valueOf(2), 12, RoundingMode.DOWN).setScale(8, RoundingMode.DOWN).doubleValue();
    }

    private double skeweness2(long x, long y) {
        return (BigDecimal.valueOf(x)
                .divide(BigDecimal.valueOf(y), 12, RoundingMode.HALF_DOWN))
                .add(BigDecimal.valueOf(y)
                        .divide(BigDecimal.valueOf(x), 12, RoundingMode.HALF_DOWN))
                .divide(BigDecimal.valueOf(2), 12, RoundingMode.HALF_DOWN).setScale(8, RoundingMode.DOWN).doubleValue();
    }

    private BigDecimal invariantCalc(long x, long y) {
        double sk = skeweness(x, y);

        BigDecimal firstTerm =
                (BigDecimal.valueOf(x).add(BigDecimal.valueOf(y)))
                        .divide(BigDecimal.valueOf(Math.pow(sk, alpha)).setScale(8, RoundingMode.UP), 8, RoundingMode.UP).setScale(0, RoundingMode.HALF_UP);

//        System.out.println("skeweness: " + sk);
//        System.out.println("sk**alpha: " + (BigDecimalMath.pow(BigDecimal.valueOf(sk).divide(BigDecimal.valueOf(scaleValue8), 20, RoundingMode.UP),BigDecimal.valueOf(alpha * scaleValue8), new MathContext(100))));
//        BigDecimal firstTerm2 =
//                ((BigDecimal.valueOf(x).add(BigDecimal.valueOf(y))).multiply(BigDecimal.valueOf(scaleValue8)))
//                        .divide(BigDecimal.valueOf(Math.pow(sk / scaleValue8, alpha * scaleValue8)), 100, RoundingMode.UP).setScale(8, RoundingMode.HALF_UP);
//        System.out.println("firstTerm: " + firstTerm2);
//        System.out.println("skeweness: " + sk);


        BigDecimal nestedFraction = (BigDecimal.valueOf(x).multiply(BigDecimal.valueOf(y))).divide(BigDecimal.valueOf(scaleValue8));
        BigDecimal firstPow = BigDecimalMath.sqrt(nestedFraction, new MathContext(20)).setScale(4, RoundingMode.DOWN).movePointRight(4);
        System.out.println("second_sum_first_part: " + firstPow);
        BigDecimal secondPow = BigDecimal.valueOf(Math.pow(sk - betta, alpha)).setScale(8, RoundingMode.DOWN).movePointRight(8);
        System.out.println("second_sum_second_part: " + secondPow);
        BigDecimal fraction = firstPow.multiply(secondPow).divide(BigDecimal.valueOf(scaleValue8)).setScale(0, RoundingMode.DOWN);
        //
        BigDecimal mPfraction = (firstPow.multiply(secondPow).divide(BigDecimal.valueOf(scaleValue8))).setScale(1, RoundingMode.DOWN);
        System.out.println("second_sum: " + BigDecimal.valueOf(2).multiply(fraction));
        System.out.println("second_fraction: " + firstPow.multiply(secondPow).divide(BigDecimal.valueOf(scaleValue8)));

//        System.out.println("invariantPrecised: " + firstTerm2.add(BigDecimal.valueOf(2).multiply(mPfraction)));
//        System.out.println("invariant: " + firstTerm2.add(BigDecimal.valueOf(2).multiply(fraction)).longValue());
        //7348480386695.733125 * 2 =
        //14696960773391,5 second sum
        //19999982685404
        //19999922510330
        //34696943458795.4

        //
        return firstTerm.add(BigDecimal.valueOf(2).multiply(fraction)).setScale(0,RoundingMode.DOWN);
//        long nestedFraction = (BigDecimal.valueOf(x).multiply(BigDecimal.valueOf(y))).divide(BigDecimal.valueOf(scaleValue8)).longValue();
//        System.out.println(nestedFraction);
//        BigDecimal firstPow = BigDecimal.valueOf(Math.pow(nestedFraction, 0.5)).setScale(4, RoundingMode.DOWN).movePointRight(4);
//        BigDecimal secondPow = BigDecimal.valueOf(Math.pow(sk - betta, alpha)).setScale(8, RoundingMode.DOWN).movePointRight(8);
//        long fraction = firstPow.multiply(secondPow).divide(BigDecimal.valueOf(scaleValue8)).longValue();
//
//        return firstTerm.add(BigDecimal.valueOf(2 * fraction)).longValue();
    }

    private long amountToSendEstimated(long x_balance, long y_balance, long x_balance_new) {
        long actual_invariant = invariantCalc(x_balance, y_balance).longValue();
        long y_left = 1;
        long y_right = 100 * y_balance;
        for (int i = 0; i < 50; i++) {
//            System.out.println(i);
            long mean = (y_left + y_right) / 2;
            long invariant_delta_in_left = actual_invariant - invariantCalc(x_balance_new, y_left).longValue();
//            System.out.println();
//            System.out.println("invariant_delta_in_left: " + invariant_delta_in_left);
//            System.out.println();
            long invariant_delta_in_right = actual_invariant - invariantCalc(x_balance_new, y_right).longValue();
//            System.out.println();
//            System.out.println("invariant_delta_in_right: " + invariant_delta_in_right);
//            System.out.println();
            long invariant_delta_in_mean = actual_invariant - invariantCalc(x_balance_new, mean).longValue();
//            System.out.println();
//            System.out.println("invariant_delta_in_mean: " + invariant_delta_in_mean);
//            System.out.println();

//            System.out.println("x_balance: " + x_balance);
//            System.out.println("y_balance: " + y_balance);
//            System.out.println("x_balance_new: " + x_balance_new);

            if (BigInteger.valueOf(invariant_delta_in_mean).multiply(BigInteger.valueOf(invariant_delta_in_left)).signum() != 1) {
                y_left = y_left;
                y_right = mean;
            } else if (BigInteger.valueOf(invariant_delta_in_mean).multiply(BigInteger.valueOf(invariant_delta_in_right)).signum() != 1) {
                y_left = mean;
                y_right = y_right;
            } else {
//                System.out.println("1y_balance: " + y_balance + " 1y_right: " + y_right);
                return y_balance - y_right - 2;
            }
        }
//        System.out.println("1y_balance: " + y_balance + " 1y_right: " + y_right);
        return y_balance - y_right - 2;
    }

    private long calculateHowManySendTokenA(long amountToSendEstimated, long minTokenRecieveAmount, long amountTokenA, long amountTokenB, long tokenReceiveAmount, long invariant) {
        int slippageValue = scaleValue8 - scaleValue8 * 1 / 10000000; // 0.000001% of slippage
        long deltaBetweenMaxAndMinSendValue = amountToSendEstimated - minTokenRecieveAmount;
        long amountToSendStep1 = amountToSendEstimated - 1 * deltaBetweenMaxAndMinSendValue / 5;
        long amountToSendStep2 = amountToSendEstimated - 2 * deltaBetweenMaxAndMinSendValue / 5;
        long amountToSendStep3 = amountToSendEstimated - 3 * deltaBetweenMaxAndMinSendValue / 5;
        long amountToSendStep4 = amountToSendEstimated - 4 * deltaBetweenMaxAndMinSendValue / 5;
        long amountToSendStep5 = amountToSendEstimated - 5 * deltaBetweenMaxAndMinSendValue / 5;

        long invariantEstimatedRatio = BigDecimal.valueOf(invariant).multiply(BigDecimal.valueOf(scaleValue8)).divide(invariantCalc(amountTokenA - amountToSendEstimated, amountTokenB + tokenReceiveAmount), 8, RoundingMode.HALF_DOWN).longValue();
        if (invariantEstimatedRatio > slippageValue && invariantEstimatedRatio < scaleValue8) {
            return amountToSendEstimated;
        } else {
            if (invariantCalc(amountTokenA - amountToSendStep1, amountTokenB + tokenReceiveAmount).longValue() - invariant > 0) {
                System.out.println(1);
                return amountToSendStep1 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            } else if (invariantCalc(amountTokenA - amountToSendStep2, amountTokenB + tokenReceiveAmount).longValue() - invariant > 0) {
                System.out.println(2);
                return amountToSendStep2 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            } else if (invariantCalc(amountTokenA - amountToSendStep3, amountTokenB + tokenReceiveAmount).longValue() - invariant > 0) {
                System.out.println(3);
                return amountToSendStep3 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            } else if (invariantCalc(amountTokenA - amountToSendStep4, amountTokenB + tokenReceiveAmount).longValue() - invariant > 0) {
                System.out.println(4);
                return amountToSendStep4 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            } else if (invariantCalc(amountTokenA - amountToSendStep5, amountTokenB + tokenReceiveAmount).longValue() - invariant > 0) {
                System.out.println(5);
                return amountToSendStep5 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            } else {
                System.out.println(6);
                return amountToSendStep5 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            }
        }
    }

    private long calculateHowManySendTokenB(long amountToSendEstimated, long minTokenRecieveAmount, long amountTokenA, long amountTokenB, long tokenReceiveAmount, long invariant) {
        int slippageValue = scaleValue8 - scaleValue8 * 1 / 10000000; // 0.000001% of slippage
        long deltaBetweenMaxAndMinSendValue = amountToSendEstimated - minTokenRecieveAmount;
        long amountToSendStep1 = amountToSendEstimated - 1 * deltaBetweenMaxAndMinSendValue / 5;
        long amountToSendStep2 = amountToSendEstimated - 2 * deltaBetweenMaxAndMinSendValue / 5;
        long amountToSendStep3 = amountToSendEstimated - 3 * deltaBetweenMaxAndMinSendValue / 5;
        long amountToSendStep4 = amountToSendEstimated - 4 * deltaBetweenMaxAndMinSendValue / 5;
        long amountToSendStep5 = amountToSendEstimated - 5 * deltaBetweenMaxAndMinSendValue / 5;

        long invariantEstimatedRatio = BigDecimal.valueOf(invariant).multiply(BigDecimal.valueOf(scaleValue8)).divide(invariantCalc(amountTokenA + tokenReceiveAmount, amountTokenB - amountToSendEstimated), 8, RoundingMode.HALF_DOWN).longValue();
        if (invariantEstimatedRatio > slippageValue && invariantEstimatedRatio < scaleValue8) {
            return amountToSendEstimated;
        } else {
            if (invariantCalc(amountTokenA - amountToSendStep1, amountTokenB + tokenReceiveAmount).longValue() - invariant > 0) {
                System.out.println(1);
                return amountToSendStep1 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            } else if (invariantCalc(amountTokenA - amountToSendStep2, amountTokenB + tokenReceiveAmount).longValue() - invariant > 0) {
                System.out.println(2);
                return amountToSendStep2 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            } else if (invariantCalc(amountTokenA - amountToSendStep3, amountTokenB + tokenReceiveAmount).longValue() - invariant > 0) {
                System.out.println(3);
                return amountToSendStep3 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            } else if (invariantCalc(amountTokenA - amountToSendStep4, amountTokenB + tokenReceiveAmount).longValue() - invariant > 0) {
                System.out.println(4);
                return amountToSendStep4 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            } else if (invariantCalc(amountTokenA - amountToSendStep5, amountTokenB + tokenReceiveAmount).longValue() - invariant > 0) {
                System.out.println(5);
                return amountToSendStep5 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            } else {
                System.out.println(6);
                return amountToSendStep5 * (comissionScaleDelimiter - comission) / comissionScaleDelimiter;
            }
        }
    }
    //def find_how_many_you_getV2(x_balance,y_balance,x_balance_new,alpha,betta):
    //  actual_invarian = invV2(x_balance,y_balance,alpha,betta)
    //  y_left = 1
    //  y_right = 100*y_balance
    //  for _ in range(50):
    //    mean = (y_left + y_right)/2
    //    print(mean)
    //    invariant_delta_in_left = actual_invarian - invV2(x_balance_new,y_left,alpha,betta)
    //    invariant_delta_in_right = actual_invarian - invV2(x_balance_new,y_right,alpha,betta)
    //    invariant_delta_in_mean = actual_invarian - invV2(x_balance_new,mean,alpha,betta)
    //    if invariant_delta_in_mean*invariant_delta_in_left < 0:
    //      y_left = y_left
    //      y_right = mean
    //    elif invariant_delta_in_mean*invariant_delta_in_right <0:
    //      y_left = mean
    //      y_right = y_right
    //    else:
    //      return y_balance -  y_right# это не ошибка. специально вычитаем бОльшее значение(y_right>mean>y_left)
    //  return y_balance -  y_right #

//    private long calculateHowManySendTokenA(long invariant, long amountToSendEstimated, long minTokenReceiveAmount, long amountTokenA, long amountTokenB, long tokenReceiveAmount) {
//        int slippageValue = scaleValue8 - scaleValue8 * 1 / 10000;
//        long invariantEstimatedRatio = BigInteger
//
//        return 1;
//    }
}
