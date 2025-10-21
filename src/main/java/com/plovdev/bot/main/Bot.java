package com.plovdev.bot.main;

import com.plovdev.bot.bots.Button;
import com.plovdev.bot.bots.ExpiryListener;
import com.plovdev.bot.bots.LanguageManager;
import com.plovdev.bot.bots.Utils;
import com.plovdev.bot.modules.beerjes.BitGetTradeService;
import com.plovdev.bot.modules.beerjes.TradeService;
import com.plovdev.bot.modules.databases.BlanksDB;
import com.plovdev.bot.modules.databases.ReferralDB;
import com.plovdev.bot.modules.databases.UserDB;
import com.plovdev.bot.modules.databases.UserEntity;
import com.plovdev.bot.modules.messages.AdminComands;
import com.plovdev.bot.modules.messages.Messager;
import com.plovdev.bot.modules.messages.Pending;
import com.plovdev.bot.modules.models.OrderResult;
import com.plovdev.bot.modules.models.SymbolInfo;
import com.plovdev.bot.modules.parsers.Signal;
import com.plovdev.bot.modules.parsers.TelegramSignalParser;
import com.plovdev.bot.modules.parsers.TradingViewSignalParser;
import com.plovdev.bot.modules.parsers.notifies.SignalListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

public class Bot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(Bot.class);
    private final Preferences prefs = Preferences.userRoot().node("TradingBot");

    @Override
    public String getBotToken() {
        return Utils.getBotToken();
    }

    @Override
    public String getBotUsername() {
        return "TradingBot";
    }

    private final Messager messager = new Messager(this);
    private final AdminComands comands = new AdminComands(this);
    private final LanguageManager manager = new LanguageManager();
    private final UserDB userDB = new UserDB();
    private final ReferralDB referralDB = new ReferralDB();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    private final BlanksDB blanksDB = new BlanksDB();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();


    public Bot() {
        messager.registerComands(); // добавляем команды для бота.
        comands.registerComands();

        SignalListener.addSignalListener(((signal) -> {
            try {
                // 1 раз загружаем данные, и у всех пользователей открываем по ним.
                // Так, доступ к публичному endpoint не забанят, а private - там лимиты для каждого UID отдельно.
                CompletableFuture<SymbolInfo> getBI = CompletableFuture.supplyAsync(() -> TestUtils.bitgetService.getSymbolInfo(TestUtils.bitgetTestUser, signal.getSymbol()));
                CompletableFuture<BigDecimal> getBEP = CompletableFuture.supplyAsync(() -> TestUtils.bitgetService.getEntryPrice(signal.getSymbol()));

                CompletableFuture<SymbolInfo> getUI = CompletableFuture.supplyAsync(() -> TestUtils.bitunixService.getSymbolInfo(TestUtils.bitunixUser, signal.getSymbol()));
                CompletableFuture<BigDecimal> getUEP = CompletableFuture.supplyAsync(() -> TestUtils.bitunixService.getEntryPrice(signal.getSymbol()));

                CompletableFuture.allOf(getBI, getUI, getBEP, getUEP).join();

                SymbolInfo bInfo = getBI.get(); // BitGet
                SymbolInfo uInfo = getUI.get(); // BitUnix
                BigDecimal bep = getBEP.get(); // BitGet
                BigDecimal uep = getUEP.get(); // BitUnix

                log.info("First data getted: binfo: {}, uinfo: {}, bep: {}, uep: {}", bInfo, uInfo, bep, uep);

                List<UserEntity> activeUsers = userDB.getAll().stream().filter(u -> "ACTIVE".equals(u.getStatus())).toList();
                log.info("🚀 Executing signal for {} active users", activeUsers.size());

                List<CompletableFuture<OrderResult>> futures = activeUsers.stream().map(user -> processUser(executor, user, signal, bInfo, uInfo, bep, uep)).toList();
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                        .thenAccept(v -> log.info("✅ Signal execution completed for all users"))
                        .exceptionally(e -> {
                            log.error("❌ Signal execution failed: {}", e.getMessage());
                            return null;
                        });
            } catch (Exception e) {
                log.error("ER: ", e);
            }
        }));

        ExpiryListener.addListener((id, lang) -> {
            try {
                execute(new SendMessage(id, manager.getText(lang, "expireOut")));
            } catch (Exception e) {
                log.error("Failed to send expiry notifications to user {}:", id, e);
            }
        });
    }

    private CompletableFuture<OrderResult> processUser(ExecutorService executor, UserEntity repository, Signal signal, SymbolInfo bsi, SymbolInfo usi, BigDecimal bep, BigDecimal uep) {
        return CompletableFuture.supplyAsync(() -> {
            OrderResult result = OrderResult.no();
            try {
                TradeService service = repository.getUserBeerj();
                int all = service.getPositions(repository).size();
                String positions = repository.getPositions();
                if (positions.equals("all")) {
                    if (service instanceof BitGetTradeService) {
                        result = service.openOrder(signal, repository, bsi, bep);
                    } else {
                        result = service.openOrder(signal, repository, usi, uep);
                    }
                    System.out.println(result);
                    if (result.succes() && !repository.getReferral().equals("none")) {
                        System.out.println("managing");
                        manageReferral(repository);
                    }
                } else {
                    log.info("Open order for user");
                    if ((Integer.parseInt(repository.getPositions()) - all) > 0) {
                        if (service instanceof BitGetTradeService) {
                            log.info("Open bitget order");
                            result = service.openOrder(signal, repository, bsi, bep);
                        } else {
                            result = service.openOrder(signal, repository, usi, uep);
                        }
                        System.out.println(result);
                        if (result.succes() && !repository.getReferral().equals("none")) {
                            manageReferral(repository);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error process user signal: ", e);
            }
            return result;
        }, executor);
    }

    private void manageReferral(UserEntity repository) {
        int posOpened = repository.getPosOpened() + 1;
        repository.setPosOpened(posOpened);
        userDB.update("posOpened", String.valueOf(posOpened), repository.getTgId());
        if (posOpened >= 50) {
            repository.setActiveRef(true);
            userDB.update("isActiveRef", "true", repository.getTgId());

            UserEntity repo = (UserEntity) userDB.get(repository.getReferral());
            int actives = repo.getActiveRefCount() + 1;
            repo.setActiveRefCount(actives);
            userDB.update("activeRefCount", String.valueOf(actives), repo.getTgId());

            if (actives >= 10) {
                SendMessage message = new SendMessage(repository.getReferral(), manager.getText(repo.getLanguage(), "referrDone"));
                blanksDB.add(repo.getTgId(), repo.getUID(), repo.getTgName(), "waiting", "ref", repo.getBeerj());
                Button button = new Button(manager.getText(repo.getLanguage(), "pendReferr"), "REFERRAL_PENDING:" + repository.getTgId());
                button.setActionListener(((update, message1, from, chatId, text, repository1) -> {
                    EditMessageText edit = new EditMessageText(manager.getText(((UserEntity) repository1).getLanguage(), "refPending"));
                    edit.setMessageId(message1.getMessageId());
                    execute(edit);

                    userDB.getAll().stream()
                            .filter(e -> e.getRole().equals("admin"))
                            .forEach(repos -> {
                                String header = "<b>Новая заявка на реферал!</b>\n\n";
                                String tgId = "ID в телеграм: <b>" + repo.getTgId() + "</b>\n";
                                String name = "Имя: " + repo.getTgName() + "\n";
                                String uid = "UID на beerже: <b>" + repo.getUID() + "</b>";
                                Pending pending = new Pending(repos.getTgId(), header + tgId + name + uid, repo.getTgId(), this, "none", "referrAccept", "referrReject");
                                try {
                                    execute(pending);
                                } catch (TelegramApiException e) {
                                    log.error("Ошибка отправки заявки: {}", e.getMessage());
                                }
                            });

                }));
                message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(button))));
                try {
                    execute(message);
                    userDB.update("activeRefCount", "0", repo.getTgId());
                    userDB.update("inited", "0", repo.getTgId());

                    userDB.update("isActiveRef", "false", repository.getTgId());
                    userDB.update("posOpened", "0", repository.getTgId());
                } catch (TelegramApiException e) {
                    log.error("Telegram ref error: ", e);
                }
            }
        }
        try {
            referralDB.upadateByKey("positions", "0", repository.getReferral());
            referralDB.upadateByKey("invited", "0", repository.getReferral());
        } catch (Exception e) {
            log.error("Произошла ошибка выпонения реферала: {}", e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            messager.notifyBotComands(update); // уведомляем команд
        } else if (update.hasChannelPost() && update.getChannelPost().hasText()) {
            Message message = update.getChannelPost();
            String chatId = message.getChatId().toString();
            String text = message.getText();


            if (chatId.equals(prefs.get("chanel-id", "-1002729649638"))) {
                if (TelegramSignalParser.validate(text)) SignalListener.notifySignals(TelegramSignalParser.parse(text));
                else System.err.println("false");
            }
            if (chatId.equals(prefs.get("tv-chanel-id", "-1002729649638"))) {
                if (TradingViewSignalParser.validate(text)) {
                    SignalListener.notifySignals(TradingViewSignalParser.parse(text));
                }
            }
        } else if (update.hasCallbackQuery()) {
            messager.notifyBotButtons(update); // уведомляем калбеки.
        }
    }
}
