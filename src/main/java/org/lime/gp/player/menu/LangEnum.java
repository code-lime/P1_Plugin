package org.lime.gp.player.menu;

public enum LangEnum {
    CHAT_TELL_ALL("lang.chat.tell_all"),
    CHAT("lang.chat"),

    SMS("lang.sms"),
    SMS_DISPLAY("lang.sms.display"),
    SMS_CALL("lang.sms.call"),

    RADIO("lang.radio"),

    DO("lang.do"),
    ME("lang.me"),
    TRY("lang.try"),
    TRYDO("lang.trydo"),

    TIME("lang.time"),
    STATS("lang.stats"),

    DIE_TIMER("lang.die_timer"),
    POLICE_HANDCUFFS("lang.police.handcuffs"),

    SEARCH("lang.search"),
    SEARCH_ITEM("lang.search.item"),

    NEWS("lang.news"),
    NEWS_TEST("lang.news.test");

    private final String key;

    LangEnum(String key) { this.key = key; }
    public String key() { return this.key; }
}
