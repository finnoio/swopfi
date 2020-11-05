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
    private int aDecimal = 8;
    private int bDecimal = 6;
    private int commission = 3000;
    private int commisionGovernance = 1200;
    private int commissionScaleDelimiter = 1000000;
    private String version = "1.0.0";
    private HashMap<Account, String> shareTokenIds = new HashMap<>();
    private String dAppScript = fromFile("dApps/exchanger.ride")
            .replace("governanceAddress = Address(base58'3MSNMcqyweiM9cWpvf4Fn8GAWeuPstxj2hK')",
                    "governanceAddress = Address(base58'3MP9d7iovdAZtsPeRcq97skdsQH5MPEsfgm')");

    @BeforeAll
    void before() {
        async(
                () -> {
                    firstExchanger = new Account(1000_00000000L);
                    firstExchanger.setsScript(s -> s.script(dAppScript));
                },
                () -> {
                    secondExchanger = new Account(1000_00000000L);
                    secondExchanger.setsScript(s -> s.script(dAppScript));
                },
                () -> {
                    thirdExchanger = new Account(1000_00000000L);
                    thirdExchanger.setsScript(s -> s.script(dAppScript));
                },
                () -> {
                    firstCaller = new Account(1000_00000000L);
                    tokenA = firstCaller.issues(a -> a.quantity(20000000_00000000L).name("tokenA").decimals(aDecimal)).getId().toString();
                    tokenB = firstCaller.issues(a -> a.quantity(20000000_00000000L).name("tokenB").decimals(bDecimal)).getId().toString();
                },
                () -> {
                    secondCaller = new Account(1000_00000000L);
                }


        );
    }

    Stream<Arguments> fundProvider() {
        return Stream.of(
                Arguments.of(firstExchanger, 1000000, 100000),
                Arguments.of(secondExchanger, 100000, 100000),
                Arguments.of(thirdExchanger, 212345, 3456789));
    }

    @ParameterizedTest(name = "caller inits {index} exchanger with {1} tokenA and {2} tokenB")
    @MethodSource("fundProvider")
    void a_canFundAB(Account exchanger, int x, int y) {
        node().waitForTransaction(tokenA);
        node().waitForTransaction(tokenB);

        long fundAmountA = x * (long) Math.pow(10, aDecimal);
        long fundAmountB = y * (long) Math.pow(10, bDecimal);

        int digitsInShareToken = (aDecimal + bDecimal) / 2;

        String invokeId = firstCaller.invokes(i -> i.dApp(exchanger).function("init").payment(fundAmountA, tokenA).payment(fundAmountB, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);
        node().waitNBlocks(1);

        shareTokenIds.put(exchanger,exchanger.dataStr("share_asset_id"));

        long shareTokenSupply = (long) (((new BigDecimal(Math.pow(fundAmountA / Math.pow(10, aDecimal), 0.5)).setScale(aDecimal, RoundingMode.HALF_DOWN).movePointRight(aDecimal).doubleValue() *
                new BigDecimal(Math.pow(fundAmountB / Math.pow(10, bDecimal), 0.5)).setScale(bDecimal, RoundingMode.HALF_DOWN).movePointRight(bDecimal).doubleValue()) / Math.pow(10, digitsInShareToken)));

        assertAll("data and balances",
                () -> assertThat(exchanger.dataInt("A_asset_balance")).isEqualTo(fundAmountA),
                () -> assertThat(exchanger.dataInt("B_asset_balance")).isEqualTo(fundAmountB),
                () -> assertThat(exchanger.balance(tokenA)).isEqualTo(fundAmountA),
                () -> assertThat(exchanger.balance(tokenB)).isEqualTo(fundAmountB),
                () -> assertThat(exchanger.dataStr("A_asset_id")).isEqualTo(tokenA),
                () -> assertThat(exchanger.dataStr("B_asset_id")).isEqualTo(tokenB),
                () -> assertThat(exchanger.dataBool("active")).isEqualTo(true),
                () -> assertThat(exchanger.dataInt("commission")).isEqualTo(commission),
                () -> assertThat(exchanger.dataInt("commission_scale_delimiter")).isEqualTo(commissionScaleDelimiter),
                () -> assertThat(exchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(exchanger.dataStr("share_asset_id")).isNotNull(),
                () -> assertThat(exchanger.dataInt("share_asset_supply")).isEqualTo(shareTokenSupply),
                () -> assertThat(firstCaller.balance(shareTokenIds.get(exchanger))).isEqualTo(shareTokenSupply)

        );
    }

    Stream<Arguments> aExchangerProvider() {
        return Stream.of(
                Arguments.of(firstExchanger, 100), Arguments.of(firstExchanger, 10000), Arguments.of(firstExchanger, 1899),
                Arguments.of(secondExchanger, 100), Arguments.of(secondExchanger, 10000), Arguments.of(secondExchanger, 2856),
                Arguments.of(thirdExchanger, 100), Arguments.of(thirdExchanger, 10000), Arguments.of(thirdExchanger, 1000)
        );
    }

    @ParameterizedTest(name = "firstCaller exchanges {1} tokenA")
    @MethodSource("aExchangerProvider")
    void b_canExchangeA(Account exchanger, int exchTokenAmount) {

        long tokenReceiveAmount = exchTokenAmount * (long) Math.pow(10, aDecimal);
        long amountTokenA = exchanger.dataInt("A_asset_balance");
        long amountTokenB = exchanger.dataInt("B_asset_balance");
        long callerBalanceA = firstCaller.balance(tokenA);
        long callerBalanceB = firstCaller.balance(tokenB);
        long shareTokenSuplyBefore = exchanger.dataInt("share_asset_supply");
        BigInteger tokenSendAmountWithoutFee =
                BigInteger.valueOf(tokenReceiveAmount)
                        .multiply(BigInteger.valueOf(amountTokenB))
                        .divide(BigInteger.valueOf(tokenReceiveAmount + amountTokenA));


        long tokenSendAmountWithFee = tokenSendAmountWithoutFee.multiply(BigInteger.valueOf(commissionScaleDelimiter - commission)).divide(BigInteger.valueOf(commissionScaleDelimiter)).longValue();
        long tokenSendGovernance = tokenSendAmountWithoutFee.longValue() * commisionGovernance / commissionScaleDelimiter;

        String invokeId = firstCaller.invokes(i -> i.dApp(exchanger).function("exchange", arg(tokenSendAmountWithFee)).payment(tokenReceiveAmount, tokenA).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        for (DataEntry data : exchanger.data()) System.out.printf("%s: %s%n", data.getKey(), data.getValue());

        assertAll("data and balances",
                () -> assertThat(exchanger.dataInt("A_asset_balance")).isEqualTo(amountTokenA + tokenReceiveAmount),
                () -> assertThat(exchanger.dataInt("B_asset_balance")).isEqualTo(amountTokenB - tokenSendAmountWithFee - tokenSendGovernance),
                () -> assertThat(exchanger.balance(tokenA)).isEqualTo(amountTokenA + tokenReceiveAmount),
                () -> assertThat(exchanger.balance(tokenB)).isEqualTo(amountTokenB - tokenSendAmountWithFee - tokenSendGovernance),
                () -> assertThat(firstCaller.balance(tokenA)).isEqualTo(callerBalanceA - tokenReceiveAmount),
                () -> assertThat(firstCaller.balance(tokenB)).isEqualTo(callerBalanceB + tokenSendAmountWithFee),
                () -> assertThat(exchanger.dataStr("A_asset_id")).isEqualTo(tokenA),
                () -> assertThat(exchanger.dataStr("B_asset_id")).isEqualTo(tokenB),
                () -> assertThat(exchanger.dataBool("active")).isEqualTo(true),
                () -> assertThat(exchanger.dataInt("commission")).isEqualTo(commission),
                () -> assertThat(exchanger.dataInt("commission_scale_delimiter")).isEqualTo(commissionScaleDelimiter),
                () -> assertThat(exchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(exchanger.dataStr("share_asset_id")).isEqualTo(shareTokenIds.get(exchanger)),
                () -> assertThat(exchanger.dataInt("share_asset_supply")).isEqualTo(shareTokenSuplyBefore)

        );

    }

    Stream<Arguments> bExchangerProvider() {
        return Stream.of(
                Arguments.of(firstExchanger, 100), Arguments.of(firstExchanger, 10000), Arguments.of(firstExchanger, 1899),
                Arguments.of(secondExchanger, 100), Arguments.of(secondExchanger, 10000), Arguments.of(secondExchanger, 2856),
                Arguments.of(thirdExchanger, 100), Arguments.of(thirdExchanger, 10000), Arguments.of(thirdExchanger, 1000)
        );
    }

    @ParameterizedTest(name = "firstCaller exchanges {1} tokenB")
    @MethodSource("bExchangerProvider")
    void c_canExchangeB(Account exchanger, int exchTokenAmount) {

        long tokenReceiveAmount = exchTokenAmount * (long) Math.pow(10, bDecimal);
        long amountTokenA = exchanger.dataInt("A_asset_balance");
        long amountTokenB = exchanger.dataInt("B_asset_balance");
        long callerBalanceA = firstCaller.balance(tokenA);
        long callerBalanceB = firstCaller.balance(tokenB);
        long shareTokenSuplyBefore = exchanger.dataInt("share_asset_supply");
        BigInteger tokenSendAmountWithoutFee =
                BigInteger.valueOf(tokenReceiveAmount)
                        .multiply(BigInteger.valueOf(amountTokenA))
                        .divide(BigInteger.valueOf(tokenReceiveAmount + amountTokenB));

        long tokenSendAmountWithFee = tokenSendAmountWithoutFee.multiply(BigInteger.valueOf(commissionScaleDelimiter - commission)).divide(BigInteger.valueOf(commissionScaleDelimiter)).longValue();
        long tokenSendGovernance = tokenSendAmountWithoutFee.longValue() * commisionGovernance / commissionScaleDelimiter;

        String invokeId = firstCaller.invokes(i -> i.dApp(exchanger).function("exchange", arg(tokenSendAmountWithFee)).payment(tokenReceiveAmount, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        for (DataEntry data : exchanger.data()) System.out.printf("%s: %s%n", data.getKey(), data.getValue());

        assertAll("data and balances",
                () -> assertThat(exchanger.dataInt("A_asset_balance")).isEqualTo(amountTokenA - tokenSendAmountWithFee - tokenSendGovernance),//91832344013287
                () -> assertThat(exchanger.dataInt("B_asset_balance")).isEqualTo(amountTokenB + tokenReceiveAmount),
                () -> assertThat(exchanger.balance(tokenA)).isEqualTo(amountTokenA - tokenSendAmountWithFee - tokenSendGovernance),
                () -> assertThat(exchanger.balance(tokenB)).isEqualTo(amountTokenB + tokenReceiveAmount),
                () -> assertThat(firstCaller.balance(tokenA)).isEqualTo(callerBalanceA + tokenSendAmountWithFee),
                () -> assertThat(firstCaller.balance(tokenB)).isEqualTo(callerBalanceB - tokenReceiveAmount),
                () -> assertThat(exchanger.dataStr("A_asset_id")).isEqualTo(tokenA),
                () -> assertThat(exchanger.dataStr("B_asset_id")).isEqualTo(tokenB),
                () -> assertThat(exchanger.dataBool("active")).isEqualTo(true),
                () -> assertThat(exchanger.dataInt("commission")).isEqualTo(commission),
                () -> assertThat(exchanger.dataInt("commission_scale_delimiter")).isEqualTo(commissionScaleDelimiter),
                () -> assertThat(exchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(exchanger.dataStr("share_asset_id")).isEqualTo(shareTokenIds.get(exchanger)),
                () -> assertThat(exchanger.dataInt("share_asset_supply")).isEqualTo(shareTokenSuplyBefore)

        );
    }

    Stream<Arguments> replenishByTwiceProvider() {
        return Stream.of(
                Arguments.of(firstExchanger),
                Arguments.of(secondExchanger),
                Arguments.of(thirdExchanger));
    }

    @ParameterizedTest(name = "secondCaller replenish A/B by twice")
    @MethodSource("replenishByTwiceProvider")
    void c_secondCallerReplenishByTwice(Account exchanger) {
        long amountTokenABefore = exchanger.dataInt("A_asset_balance");
        long amountTokenBBefore = exchanger.dataInt("B_asset_balance");
        String transfer1 = firstCaller.transfers(t -> t
                .to(secondCaller)
                .amount(amountTokenABefore)
                .asset(tokenA)).getId().getBase58String();
        String transfer2 = firstCaller.transfers(t -> t
                .to(secondCaller)
                .amount(amountTokenBBefore)
                .asset(tokenB)).getId().getBase58String();
        node().waitForTransaction(transfer1);
        node().waitForTransaction(transfer2);
        long shareTokenSupplyBefore = exchanger.dataInt("share_asset_supply");
        String invokeId = secondCaller.invokes(i -> i.dApp(exchanger).function("replenishWithTwoTokens", arg(0)).payment(amountTokenABefore, tokenA).payment(amountTokenBBefore, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        long shareTokenToPay = (BigInteger.valueOf(amountTokenABefore).multiply(BigInteger.valueOf(shareTokenSupplyBefore)).divide(BigInteger.valueOf(amountTokenABefore))).longValue();

        assertAll("data and balances",
                () -> assertThat(exchanger.dataInt("A_asset_balance")).isEqualTo(amountTokenABefore + amountTokenABefore),
                () -> assertThat(exchanger.dataInt("B_asset_balance")).isEqualTo(amountTokenBBefore + amountTokenBBefore),
                () -> assertThat(exchanger.dataStr("A_asset_id")).isEqualTo(tokenA),
                () -> assertThat(exchanger.dataStr("B_asset_id")).isEqualTo(tokenB),
                () -> assertThat(exchanger.dataBool("active")).isEqualTo(true),
                () -> assertThat(exchanger.dataInt("commission")).isEqualTo(commission),
                () -> assertThat(exchanger.dataInt("commission_scale_delimiter")).isEqualTo(commissionScaleDelimiter),
                () -> assertThat(exchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(exchanger.dataStr("share_asset_id")).isEqualTo(shareTokenIds.get(exchanger)),
                () -> assertThat(exchanger.dataInt("share_asset_supply")).isEqualTo(shareTokenSupplyBefore + shareTokenToPay),
                () -> assertThat(secondCaller.balance(shareTokenIds.get(exchanger))).isEqualTo(shareTokenToPay)

        );
    }

    Stream<Arguments> withdrawByTwiceProvider() {
        return Stream.of(
                Arguments.of(firstExchanger),
                Arguments.of(secondExchanger),
                Arguments.of(thirdExchanger));
    }

    @ParameterizedTest(name = "secondCaller withdraw A/B by twice")
    @MethodSource("withdrawByTwiceProvider")
    void d_secondCallerWithdrawAB(Account exchanger) {
        long dAppTokensAmountA = exchanger.balance(tokenA);
        long dAppTokensAmountB = exchanger.balance(tokenB);
        long secondCallerAmountA = secondCaller.balance(tokenA);
        long secondCallerAmountB = secondCaller.balance(tokenB);
        long shareTokenSupply = exchanger.dataInt("share_asset_supply");
        long secondCallerShareBalance = secondCaller.balance(shareTokenIds.get(exchanger));
        long tokensToPayA =
                BigDecimal.valueOf(secondCallerShareBalance)
                        .multiply(BigDecimal.valueOf(dAppTokensAmountA))
                        .divide(BigDecimal.valueOf(shareTokenSupply), 8, RoundingMode.HALF_DOWN).longValue();

        long tokensToPayB =
                BigDecimal.valueOf(secondCallerShareBalance)
                        .multiply(BigDecimal.valueOf(dAppTokensAmountB))
                        .divide(BigDecimal.valueOf(shareTokenSupply), 8, RoundingMode.HALF_DOWN).longValue();

        String invokeId = secondCaller.invokes(i -> i.dApp(exchanger).function("withdraw").payment(secondCallerShareBalance, shareTokenIds.get(exchanger)).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        assertAll("data and balances",
                () -> assertThat(exchanger.dataInt("A_asset_balance")).isEqualTo(dAppTokensAmountA - tokensToPayA),
                () -> assertThat(exchanger.dataInt("B_asset_balance")).isEqualTo(dAppTokensAmountB - tokensToPayB),
                () -> assertThat(exchanger.dataStr("A_asset_id")).isEqualTo(tokenA),
                () -> assertThat(exchanger.dataStr("B_asset_id")).isEqualTo(tokenB),
                () -> assertThat(exchanger.dataBool("active")).isEqualTo(true),
                () -> assertThat(exchanger.dataInt("commission")).isEqualTo(commission),
                () -> assertThat(exchanger.dataInt("commission_scale_delimiter")).isEqualTo(commissionScaleDelimiter),
                () -> assertThat(exchanger.dataStr("version")).isEqualTo(version),
                () -> assertThat(exchanger.dataStr("share_asset_id")).isEqualTo(shareTokenIds.get(exchanger)),
                () -> assertThat(exchanger.dataInt("share_asset_supply")).isEqualTo(shareTokenSupply - secondCallerShareBalance),
                () -> assertThat(secondCaller.balance(shareTokenIds.get(exchanger))).isEqualTo(0),
                () -> assertThat(secondCaller.balance(tokenA)).isEqualTo(secondCallerAmountA + tokensToPayA),
                () -> assertThat(secondCaller.balance(tokenB)).isEqualTo(secondCallerAmountB + tokensToPayB)

        );
    }

    @Disabled
    @Test
    void f_canReplenishAB() {
        long amountTokenABefore = firstExchanger.dataInt("A_asset_balance");
        long amountTokenBBefore = firstExchanger.dataInt("B_asset_balance");
        long bReplenishAmount = 10000000000L;
        long shareTokenSupplyBefore = firstExchanger.dataInt("share_asset_supply");
        String shareTokenId = Base58.encode(firstExchanger.dataBin("share_asset_id"));
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
                () -> assertThat(firstExchanger.dataInt("A_asset_balance")).isEqualTo(amountTokenABefore + replenishAmounts.get("aReplenishAmount")),
                () -> assertThat(firstExchanger.dataInt("B_asset_balance")).isEqualTo(amountTokenBBefore + bReplenishAmount),
                () -> assertThat(firstExchanger.dataStr("A_asset_id")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("B_asset_id")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataBool("active")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("commission")).isEqualTo(2000),
                () -> assertThat(firstExchanger.dataInt("commission_scale_delimiter")).isEqualTo(10000),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(firstExchanger.dataBin("share_asset_id")).isNotNull(),
                () -> assertThat(firstExchanger.dataInt("share_asset_supply")).isEqualTo(shareTokenSupplyBefore + shareTokenToPay),
                () -> assertThat(firstCaller.balance(shareTokenId)).isEqualTo(shareTokenAmountBefore + shareTokenToPay)

        );
    }

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
