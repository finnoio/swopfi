import com.wavesplatform.wavesj.Base58;
import com.wavesplatform.wavesj.DataEntry;
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
    private int aDecimal = 6;// 4ok  3ok  8ok 2shareTokenSuply diff300 1longOverflow
    private int bDecimal = 8;// 5ok  3ok  8ok 3shareTokenSuply diff300 1longOverflow
    private int wavesDecimal = 8;
    private int comission = 0;
    private int comissionScaleDelimiter = 10000;
    private int scaleValue8 = 100000000;
    private int scaleValue8Digits = 8;
    private long scaleValue12 = 1000000000000L;
    private int ratioThresholdMax = 100000000;
    private int ratioThresholdMin = 99999000;
    private double alpha = 0.5;
    private double betta = 0.46;


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

        long fundAmountA = 10000000000000L;
        long fundAmountB = 1000000000000L;
        //                10000000000000
        //10000000000000
        //10000000000000
        int digitsInShareToken = (aDecimal + bDecimal) / 2;//316227.7660168379 * 100000.0 / 100.0
        double a = new BigDecimal(Math.pow(fundAmountA / Math.pow(10, aDecimal), 0.5)).setScale(aDecimal, RoundingMode.HALF_DOWN).doubleValue();//.setScale(aDecimal, RoundingMode.HALF_DOWN)
        double b = new BigDecimal(Math.pow(fundAmountB / Math.pow(10, bDecimal), 0.5)).setScale(bDecimal, RoundingMode.HALF_DOWN).doubleValue();//.setScale(bDecimal, RoundingMode.HALF_DOWN)


        //1000000.0 * 1000000.0 / 10.0
        //10000000 *  10000000 / 10
        node().waitForTransaction(tokenA);
        node().waitForTransaction(tokenB);
        System.out.println("invariantCalc: " + invariantCalc(fundAmountA,fundAmountB));
        String invokeId = firstCaller.invokes(i -> i.dApp(firstExchanger).function("fund").payment(fundAmountA, tokenA).payment(fundAmountB, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);
        node().waitNBlocks(1);


        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(fundAmountA),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(fundAmountB),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataInt("invariant")).isEqualTo(invariantCalc(fundAmountA,fundAmountB).longValue()),
                () -> assertThat(firstExchanger.dataInt("exchange_count")).isEqualTo(0),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo("2.0.0"),
                () -> assertThat(firstExchanger.dataStr("share_token_id")).isNotNull(),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(
                        //pow(tokenReceiveAmountA,digitTokenA,5,1,4,HALFDOWN)*pow(tokenReceiveAmountB,digitTokenB,5,1,4,HALFDOWN)
                        (long) (((new BigDecimal(Math.pow(fundAmountA / Math.pow(10, aDecimal), 0.5)).setScale(aDecimal, RoundingMode.HALF_DOWN).movePointRight(aDecimal).doubleValue() *
                                new BigDecimal(Math.pow(fundAmountB / Math.pow(10, bDecimal), 0.5)).setScale(bDecimal, RoundingMode.HALF_DOWN).movePointRight(bDecimal).doubleValue()) / Math.pow(10, digitsInShareToken)))
                )

        );
    }

    private static Stream<Arguments> amountAndCountProvider() {
        return Stream.of(Arguments.of(100L, 1), Arguments.of(10000L, 2), Arguments.of(1000L, 3));
    }

    @Disabled
    @ParameterizedTest(name = "firstCaller exchanges {0} tokens")
    @MethodSource("amountAndCountProvider")
    void b_canExchangeAB(long exchTokenAmount, int exchangeCount) {

        long tokenReceiveAmount = exchTokenAmount * (long) Math.pow(10, aDecimal);
        System.out.println(tokenReceiveAmount);
        long amountTokenA = firstExchanger.dataInt("amountTokenA");
        long amountTokenB = firstExchanger.dataInt("amountTokenB");
        long shareTokenSuplyBefore = firstExchanger.dataInt("share_token_supply");
        long tokenSendAmount = (
                BigInteger.valueOf(tokenReceiveAmount)
                        .multiply(BigInteger.valueOf(amountTokenB))
                        .divide(BigInteger.valueOf(tokenReceiveAmount + amountTokenA))
                        .longValue() * (comissionScaleDelimiter - comission)) / comissionScaleDelimiter;

        String invokeId = firstCaller.invokes(i -> i.dApp(firstExchanger).function("exchanger", arg(tokenSendAmount)).payment(tokenReceiveAmount, tokenA).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        for (DataEntry data : firstExchanger.data()) System.out.printf("%s: %s%n", data.getKey(), data.getValue());

        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(amountTokenA + tokenReceiveAmount),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(amountTokenB - tokenSendAmount),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataInt("exchange_count")).isEqualTo(exchangeCount),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(comission),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(comissionScaleDelimiter),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(firstExchanger.dataBin("share_token_id")).isNotNull(),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSuplyBefore)

        );

    }

    @Disabled
    @Test
    void f_firstCallerReplenishAB() {
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
        String shareTokenId = Base58.encode(firstExchanger.dataBin("share_token_id"));
//        long shareTokenAmountBefore = secondCaller.balance(shareTokenId);

        String invokeId = secondCaller.invokes(i -> i.dApp(firstExchanger).function("replenishment").payment(amountTokenABefore, tokenA).payment(amountTokenBBefore, tokenB).fee(1_00500000L)).getId().getBase58String();
        node().waitForTransaction(invokeId);

        long shareTokenToPay = (BigInteger.valueOf(amountTokenABefore).multiply(BigInteger.valueOf(shareTokenSupplyBefore)).divide(BigInteger.valueOf(amountTokenABefore))).longValue();


        assertAll("data and balances",
                () -> assertThat(firstExchanger.dataInt("amountTokenA")).isEqualTo(amountTokenABefore + amountTokenABefore),
                () -> assertThat(firstExchanger.dataInt("amountTokenB")).isEqualTo(amountTokenBBefore + amountTokenBBefore),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenA")).isEqualTo(tokenA),
                () -> assertThat(firstExchanger.dataStr("assetIdTokenB")).isEqualTo(tokenB),
                () -> assertThat(firstExchanger.dataInt("exchange_count")).isEqualTo(3),
                () -> assertThat(firstExchanger.dataBool("status")).isEqualTo(true),
                () -> assertThat(firstExchanger.dataInt("comission")).isEqualTo(200),
                () -> assertThat(firstExchanger.dataInt("comissionScaleDelimiter")).isEqualTo(10000),
                () -> assertThat(firstExchanger.dataStr("version")).isEqualTo("0.0.2"),
                () -> assertThat(firstExchanger.dataBin("share_token_id")).isNotNull(),
                () -> assertThat(firstExchanger.dataInt("share_token_supply")).isEqualTo(shareTokenSupplyBefore + shareTokenToPay),
                () -> assertThat(secondCaller.balance(shareTokenId)).isEqualTo(shareTokenToPay)

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
    private double skeweness(long x, long y) {
        return (BigDecimal.valueOf(x)
                .divide(BigDecimal.valueOf(y)))
                .add(BigDecimal.valueOf(y)
                        .divide(BigDecimal.valueOf(x)))
                .divide(BigDecimal.valueOf(2)).doubleValue();
    }

    //fraction(x+y,pow(sk,scaleValue8Digits,-alpha,alphaDigits,8,HALFDOWN),scaleValue8)+
    //    2*fraction(fraction(pow(x,0,5,1,scaleValue8Digits,HALFDOWN),pow(y,0,5,1,scaleValue8Digits,HALFDOWN),scaleValue8*scaleValue8),pow(sk - betta*scaleValue8/100,0,alpha,alphaDigits,scaleValue8Digits,HALFDOWN),scaleValue12)
    private BigDecimal invariantCalc(long x, long y) {
        double sk = skeweness(x, y);
        System.out.println("sk: " + sk);
        System.out.println("sk ** alpha" + BigDecimal.valueOf(Math.pow(sk,alpha)).setScale(8,RoundingMode.UP));

        BigDecimal firstTerm =
                (BigDecimal.valueOf(x).add(BigDecimal.valueOf(y)))
                        .divide(BigDecimal.valueOf(Math.pow(sk,alpha)).setScale(8,RoundingMode.UP), 8, RoundingMode.UP).setScale(8, RoundingMode.UP);
        System.out.println("first term: " + firstTerm);

        //2*fraction(pow(fraction(x,y,scaleValue8),0,5,1,scaleValue8Digits/2,DOWN),pow(sk - beta,scaleValue8Digits,alpha,alphaDigits,scaleValue8Digits,DOWN),scaleValue8)
        BigDecimal secondTerm =
                BigDecimal.valueOf(2)
                        .multiply(BigDecimal.valueOf(Math.pow(x, 0.5)).setScale(8,RoundingMode.DOWN))
                        .multiply(BigDecimal.valueOf(Math.pow(y, 0.5)).setScale(8,RoundingMode.DOWN))
                        .multiply(BigDecimal.valueOf(Math.pow(sk - betta, alpha)).setScale(8,RoundingMode.DOWN));

        long nestedFraction = BigDecimal.valueOf(x).multiply(BigDecimal.valueOf(y)).divide(BigDecimal.valueOf(scaleValue8)).longValue();
        BigDecimal firstPow = BigDecimal.valueOf(Math.pow(nestedFraction,0.5)).setScale(4,RoundingMode.DOWN);
        BigDecimal secondPow = BigDecimal.valueOf(Math.pow(sk - betta, alpha)).setScale(8,RoundingMode.DOWN);

        BigDecimal fraction = firstPow.multiply(secondPow).divide(BigDecimal.valueOf(scaleValue8));

        BigDecimal secondTerm2 = BigDecimal.valueOf(2).multiply(fraction);




//        BigDecimal sqrtxy = BigDecimalMath.sqrt((BigDecimal.valueOf(x).multiply(BigDecimal.valueOf(y))).setScale(8),new MathContext(20)).setScale(4,RoundingMode.DOWN);

        System.out.println("sk - betta ** alpha" + BigDecimal.valueOf(Math.pow(sk - betta, alpha)).setScale(8,RoundingMode.DOWN));

        System.out.println("x ** 0.5: " + BigDecimal.valueOf(Math.pow(x, 0.5)).setScale(8,RoundingMode.HALF_DOWN));
        System.out.println("y ** 0.5: " + BigDecimal.valueOf(Math.pow(y, 0.5)).setScale(8,RoundingMode.HALF_DOWN));
        System.out.println("(x*y)**0.5: " + BigDecimal.valueOf(Math.pow(x, 0.5)).setScale(8,RoundingMode.HALF_DOWN).multiply(BigDecimal.valueOf(Math.pow(y, 0.5)).setScale(8,RoundingMode.HALF_DOWN)));
        System.out.println("second term: " + secondTerm2);
        System.out.println("inv: " + firstTerm.add(secondTerm));
        return firstTerm.add(secondTerm);


        //x ** 0.5: 3162277.66016838
        //x ** 0.5: 316227766016838
        //y ** 0.5: 1000000.00000000
        //y ** 0.5: 100000000000000"


//        BigDecimal firstPow = BigDecimal.valueOf(Math.pow(sk / scaleValue8, -alpha * Math.pow(10,alphaDigits))).setScale(8,RoundingMode.HALF_DOWN);
//        BigDecimal firstFraction = BigDecimal.valueOf(x + y).multiply(firstPow).divide(BigDecimal.valueOf(scaleValue8), 0, RoundingMode.HALF_DOWN);
//        BigDecimal secondPow = BigDecimal.valueOf(Math.pow(sk / scaleValue8, -alpha * Math.pow(10,alphaDigits))).setScale(8,RoundingMode.HALF_DOWN);
//        BigDecimal thirdPow = BigDecimal.valueOf(Math.pow(sk / scaleValue8, -alpha * Math.pow(10,alphaDigits))).setScale(8,RoundingMode.HALF_DOWN);
//        BigDecimal fourthPow = BigDecimal.valueOf(Math.pow(sk / scaleValue8, -alpha * Math.pow(10,alphaDigits))).setScale(8,RoundingMode.HALF_DOWN);
//        BigDecimal secondFraction = firstPow.multiply(secondPow).divide(BigDecimal.valueOf(scaleValue8 * scaleValue8),0,RoundingMode.HALF_DOWN);
//
//
//
//
//        BigDecimal a = BigDecimal.valueOf(10000L).multiply(BigDecimal.valueOf(x).add(BigDecimal.valueOf(y)));
//        BigDecimal firstPow = BigDecimal.valueOf(Math.pow((double)skeweness(x,y) / scaleValue8, 0.15)).setScale(4,RoundingMode.HALF_DOWN);
//        BigDecimal firstFraction = a.divide(BigDecimal.valueOf(scaleValue8).multiply(firstPow), 0, RoundingMode.HALF_DOWN).movePointLeft(4).setScale(0,RoundingMode.DOWN);
//        BigDecimal secondPow = BigDecimal.valueOf(Math.pow((double)skeweness(x,y) / scaleValue8, 0.15)).setScale(6,RoundingMode.HALF_DOWN).movePointRight(6);
//        BigDecimal secondFraction = BigDecimal.valueOf(x).multiply(BigDecimal.valueOf(y).divide(BigDecimal.valueOf(scaleValue8),6, RoundingMode.HALF_DOWN));
//        BigDecimal thirdFraction = secondFraction.multiply(secondPow).divide(BigDecimal.valueOf(1000000), 6, RoundingMode.HALF_DOWN);
//        System.out.println("a: " + a);
//        System.out.println("firstPow: " + firstPow);
//        System.out.println("firstFraction: " + firstFraction);
//        System.out.println("secondPow: " + secondPow);
//        System.out.println("secondFraction: " + secondFraction);
//        System.out.println("thirdFraction: " + thirdFraction);
//        return firstFraction.add(thirdFraction);
    }

//    private long calculateHowManySendTokenA(long invariant, long amountToSendEstimated, long minTokenReceiveAmount, long amountTokenA, long amountTokenB, long tokenReceiveAmount) {
//        int slippageValue = scaleValue8 - scaleValue8 * 1 / 10000;
//        long invariantEstimatedRatio = BigInteger
//
//        return 1;
//    }


    //a: 110000000000000000
    //   110000000000000000
    //
    //firstPow: 1.2750
    //          12750
    //
    //firstFraction: 86274.5098
    //               86274
    //
    //secondPow: 1.274952
    //           1274952
    //
    //secondFraction: 100000000000000000.000000
    //                100000000000000000
    //
    //thirdFraction: 127495200000.000000
    //               127495200000000000
    //
    //invariantCalc: 127495286275000000
    //               127495200000086274
    //
    // a: 110000000000000000firstPow: 12750firstFraction: 86274secondPow: 1274952secondFraction: 100000000000000000thirdFraction: 127495200000000000invar: 127495200000086274
}
