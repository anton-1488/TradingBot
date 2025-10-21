import com.plovdev.bot.modules.beerjes.Order;
import com.plovdev.bot.modules.beerjes.Position;
import com.plovdev.bot.modules.beerjes.bitget.ws.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.plovdev.bot.main.TestUtils.bitgetService;
import static com.plovdev.bot.main.TestUtils.bitgetUser;

public class Test {
    private static final Logger log = LoggerFactory.getLogger(Test.class);

    public static void main(String[] args) throws Exception {
        getPrice();
    }

    private static void getPrice() {
        while (true) {
            try {
                System.out.print("\r" + bitgetService.getEntryPrice("SCRTUSDT"));
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
    private static void cancelAll() {
        List<Position> positions = bitgetService.getPositions(bitgetUser);
        List<Order> orders = bitgetService.getOrders(bitgetUser);

        for (Position p : positions) {
            bitgetService.closePosition(bitgetUser, p);
        }
        for (Order o : orders) {
            bitgetService.closeOrder(bitgetUser, o);
        }
    }

    private static void bws() {
        PrivateWsClient client = new PrivateWsClient(bitgetUser);

        BitGetWSListener listener = new BitGetWSListenerAdapter() {
            @Override
            public void onLogined(WSResult result) {
                System.out.println("Я успешно залогинился");
                //null - default
                client.subscribe(null, Type.USDT_FUTURES, Channel.ORDER);
            }

            @Override
            public void onSubscride(SubscribeResult result) {
                System.out.println("Подписка на канал оформлена: " + result);
            }

            @Override
            public void onError(WSResult result) {
                System.out.println("Произошла ошибка: code: " + result.code() + ", msg: " + result.msg());
            }

            @Override
            public void onPlaceOrder(Order place) {
                System.out.println(place);
            }
        };

        client.connectToBitget(listener);
    }
}