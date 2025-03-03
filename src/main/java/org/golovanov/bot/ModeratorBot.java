package org.golovanov.bot;

import lombok.extern.log4j.Log4j;
import org.golovanov.filter.Censor;
import org.glassfish.grizzly.utils.Pair;
import org.golovanov.model.MessageDb;
import org.golovanov.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;

@Component
@Log4j
public class ModeratorBot extends TelegramLongPollingBot {

    private static final MessageRepository repository = new MessageRepository();
    private static final int badMessagesLimit = 10;

    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = getMessageFromUpdate(update);
        if (message != null) {
            // если сообщение от спаммера, то мы его сразу удаляем
            if (!validateMessageSender(message)) {
                // если сообщение матерное, проверяем, не пора ли блокировать
                if (validateMessageContent(message)) {
                    checkForBan(message);
                }
            }
        }
    }


    private boolean validateMessageSender(Message message) {
        boolean isSenderBad = Censor.validateMessageSender(message);
        if (isSenderBad) {
            try {
                DeleteMessage deleteMessage = new DeleteMessage(String.valueOf(message.getChatId()), message.getMessageId());
                execute(deleteMessage);
            } catch (TelegramApiException e) {
                log.error("Something went wrong with Telegram execution", e);
            }
        }
        return isSenderBad;
    }

    private boolean validateMessageContent(Message message) {
        Pair<Boolean, String> pair = Censor.validateMessage(message);

        if (pair.getFirst() && pair.getSecond() != null) {
            prepareMessageForDbAndSave(message, pair);

            DeleteMessage deleteMessage = new DeleteMessage(String.valueOf(message.getChatId()), message.getMessageId());

            SendMessage response = new SendMessage();
            response.setChatId(message.getChatId());
            response.setText("Сообщение удалено\nПричина: мат, обсценная лексика.\nУчитесь выражать свои мысли культурно.\nТут должна быть ссылка на правила");
            response.setReplyToMessageId(message.getMessageId());
            try {
                execute(response);
                execute(deleteMessage);
            } catch (TelegramApiException e) {
                log.error("Something went wrong with Telegram execution", e);
            }
            return true;
        }
        return false;
    }

    private void checkForBan(Message message) {
       int count = repository.getBadMessageCountById(message.getFrom().getId());
        log.debug("User with id " + message.getFrom().getId() + " broke the rules " + count + " times :(");
       if (count >= badMessagesLimit) {
           log.debug("Ban-hammer should be implemented!");
           // TODO: 23.12.2022 prepare something to ban user
           ChatPermissions permissions = new ChatPermissions();
           permissions.setCanSendMessages(false);
           try {
               // RestrictChatMember с указанными выше доступами позволяет пользователю читать посты и комментарии, но не позволяет писать.
               this.execute(RestrictChatMember.builder()
                       .chatId(message.getChatId())
                       .userId(message.getFrom().getId())
                       .permissions(permissions)
                       .untilDate(60) // в секундах? автоматический разбан не происходит...
                       .build());
           } catch (TelegramApiException e) {
               log.error("Something went wrong with ban-hammer :(", e);
           }


       }
    }

    private void prepareMessageForDbAndSave(Message message, Pair<Boolean, String> pair) {
        MessageDb messageDb = new MessageDb();
        messageDb.setTgUserId(message.getFrom().getId());
        messageDb.setTgUserName(message.getFrom().getUserName());
        messageDb.setIsBot(message.getFrom().getIsBot());
        messageDb.setDate(LocalDate.now());
        messageDb.setText(pair.getSecond());

        repository.saveMessage(messageDb);
    }

    private Message getMessageFromUpdate(Update update) {
        if (update.hasMessage()) {
            return update.getMessage();
        }

        if (update.hasEditedMessage()) {
            return update.getEditedMessage();
        }
        log.error("There is not message or edited message");

        return null;
    }
}
