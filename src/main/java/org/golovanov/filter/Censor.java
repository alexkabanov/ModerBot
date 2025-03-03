package org.golovanov.filter;

import lombok.extern.log4j.Log4j;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.grizzly.utils.Pair;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

@Log4j
public class Censor {

    // TODO: think about emoji
    //                                          средний палец
    private static final String [] badEmojis = {"\uD83D\uDD95", "(_!_)", "(__!__)", "(_!__)", "(__!_)", "(!)", "(_?_)", "(_$_)", "(_x_)"};

    private static final Pattern megaPattern = Pattern.compile("(?iu)\\b(([уyu]+|[нзnz3]+[аa]+|([хhиiтtрrоo]+|[нnеe]+)?[вvb]+[зz3]?[ыyьъi']+|[сsc]+[ьъy'`]|([иiu]|[рr]+[аa]+)[зz3сsc]ъ*|([оo][тtбb]|[пp][оo0][дd])[ьъ]?|(.\\B)+?[оoаaеeиiu]+)?-?([еёe]+[бb]+(?!о[рй])|[иiu]+[бb]+[аaеe]+[тtmцc]+).*?|([нn]+[иiuеeаa]+|([дd9пpn]+|[вv]+[еe]+[рrp]+[тtm]+)[оo0]+|[рrp][аa]+[зz3сs]+|з?а|[сs]([мm][еe])?|[оo0]([тtm]|[дd][нn][оo0])?|[аa][пpn]ч|[пpn]+[еe]+[рrp]+[еe]+|[пpn]+[рrp]+[иiu]+|[вv]+)?-?[хxh]+[уyu]+([яйiyиiuеёeюu]|[лl]+[иiu]+(?!о|ган)).*?|([сs]+[тtm]+[рrp]+[аa]+[хhx]+[оo0]+|[вv][зz3ыyui]|[.]*жды|(н|сук)а)?-?[бb]+[лl]+(я+(?!(х|ш[кн]|мб)[ауеыио]).*?|[еeэe][дd9тtm]ь?)|([рrp]+[аa]+[сscзz3]+|[зz3нnh]+[аa]+|[сscоo0]|[вv]+[ыuy]?|[пpn]+([еe]+[рrp]+[еe]+|[рrp]+[оo0иiuеe]|[оo0]+[дd9]+)|[иiu][зz3сsc]ъ?|[аaоo0][тtm]|[дd9]+[оo0]+)?[пpn]+[иiuеёe]+[зz3]+[дd9]+.*?|([зz3]+[аa]+)?[пpn]+[иiuеe]+[дd9][аaоo0еeё]*[рrp]+([нn]+[уy]+.*?|[оo0аa]+[мm]+|([аa]+[сsc]+)?([иiu]+([лl]+[иiu]+)?[нnщкkтtmлl]ь?)?|([оo0]+(ч[еeиiu])?|[аa]+[сsc])?[кk]+([оo0]+[йiy]+)|[юu]+[гgr]+)[аaуyеeыu]?|[мm]+[аa]+[нnh]+[дd9]+([аaуyеeыuиi]([лl]+([иiu]+[сscзz3щ]+)?[аaуyuеeиiы]+)?|[оo0]+[йiu]+|[аaоo0][вvb]+[оo0]+шь?([еe]?[кk]+[аaуyеe])?|[юu]+[кk]+([оo0]+[вvb]+|[аaуyиiu])?)|[мm]+[уyu]+[дd9]+([яаaиiuоo0].*?|[еe]?[нn]+([ьюиiuя]|[еe]+[йiu]))|[мm]+[лl]+[яya]+([тtmдd9]ь)?|[лl]+[яya]+[тtm]+ь|([нnзz3]+[аa]+|[пpn]+[оo0]+)[хxh]+|[мm]+[аaоo0]+[лl]+[аaоo0]+ф+ь+([яиiuю]+|[еёeоo0]+[йiu]+)|([еe]+[лl]+[дd9]+[аa]*.*))\\b");
    // для удобства                                                   "(?iu)\\b(  (у   |[нз]     а   |  (хитро      |не     )?  в     з  ?[ыьъ]    | с    [ьъ]   | (и   | ра )  [зс]    ъ?| (о  [тб]  | п    о   д ) [ьъ]?|(.\\B)+?[  оаеи] )   ?-?([её]   б   (?!о[рй])|  и   [б]  [ае]   [тц])    .*?|(н    [иеа]     |([дп]     | в    е     р    т    ) о    | р    а   [зс]    |з?а| с   (м   е  )?| о    (т   | д   н   о)   ?| а   п   ч| п     е    р     е   | п     р      и   | в )  ?-?[х]+  [у]+  ([яйиеёю]      | л    и    (?!о|ган)).*?|                                    (в  [зы]     |[.]*жды|(н|сук)а)?-?[б]+ [л]+ (я (?!(х|ш[кн]|мб)[ауеыио]).*?|[еэ]  [дт]    ь?)|( р     а   [сз]     |[зн]      а   |[со]    | в    ы?   | п    ( е    р     е   | р    [оие]     | о     д)    | и   [зс]    ъ?|[ао]    т   | д     о)?     п    [иеё]+    з     д    .*?|( з     а )?   п    [ие]     д   [аое]?      р    ( н    у   .*?|[оа]     м   |( а    с)?    ( и    ( л     и)?   [нщктл]     ь?)?|( о    (ч[еи])?   | а    с)?    к    (о     й)    | ю    г)    [ауеы]?    | м    а    н     д    ([ауеыи]     (л    ( и    [сзщ])?     [ауеиы])?      | о     й    |[ао]    в     о    шь?( е?   к   [ауе])?   | ю    к   (  о     в   |[ауи])?)    | м     у     д   ([яаио]     .*?|е?   н    ([ьюия]  | е    й  )) |       мля     ([тд]ь)?     |     лять         |([нз]      а  |      по    )  х   | м   [ао]     л   [ао]     фь ([яию]   |[еёо]     й)    |(  елд[а]?.*))            \\b");

    /**
     * заготовка для отсечения спамеров, пока всегда false
     */
    public static boolean validateMessageSender(Message message) {
        User sender = message.getFrom();
        String senderName = sender.getUserName();
        String senderFirstName = sender.getFirstName();
        String senderLastName = sender.getLastName();
        Boolean isBot = sender.getIsBot();
        log.debug("User name: " + senderName);
        log.debug("User firstname: " + senderFirstName);
        log.debug("User lastname: " + senderLastName);
        log.debug("is bot: " + isBot);
        log.debug("text: " + message.getText());
        log.debug("======");
        // заготовка
        if (senderFirstName.equals("очень плохое слово")) {
            return true;
        }
        return false;
    }

    public static Pair<Boolean, String> validateMessage(Message message) {
        Pair<Boolean, String> pair = filterEmoji(message);
        if (pair.getFirst()) {
            return pair;
        } else {
            return filterText(message.getText());
        }
    }

    public static Pair<Boolean, String> filterText(String text) {
        if (text == null || text.isEmpty() || text.isBlank()) {
            return new Pair<>(false, null);
        }
        String clearText = text.toLowerCase().replaceAll("[-_+=!@#$%^&*()]{0,}", "");
        Matcher matcher = megaPattern.matcher(clearText);
        Boolean result = matcher.find();
        return new Pair<>(result, (result) ? text: null);
    }

    public static Pair<Boolean, String> filterEmoji(Message message) {
        boolean result = false;
        String text = null;
        if (message.hasSticker()) {
            result = "\uD83D\uDD95".equals(message.getSticker().getEmoji());
            text = (result) ? message.getSticker().getEmoji() : null;
        } else if (message.hasText() && message.getText() != null) {
            String str = message.getText(); // забыл зачем делал replaceAll(), но из-за этого был баг.
            for (String emoji : badEmojis) {
                if (str.contains(emoji)) {
                    result = true;
                    text = str;
                    break;
                }
            }
        }
        return new Pair<>(result, text);
    }
}
