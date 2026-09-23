package com.socialaiassistant.keyboard.ime

/**
 * Stage-3 expanded offline Bangla lexicon.
 *
 * The base Stage-1 vocabulary remains intact and this file adds a broader everyday
 * vocabulary used for fast local candidate lookup. The lexicon is intentionally
 * Android/network free so it can be tested on the JVM and replaced by a larger
 * packaged dictionary later without changing the IME contract.
 */
object ProductionBanglaLexicon {
    private val extraWords: Map<String, String> = mapOf(
        // Core conversation
        "abar" to "আবার", "age" to "আগে", "pore" to "পরে", "ekhane" to "এখানে",
        "okhane" to "ওখানে", "sekane" to "সেখানে", "sathe" to "সাথে", "sange" to "সঙ্গে",
        "jonno" to "জন্য", "jonyo" to "জন্য", "theke" to "থেকে", "porjonto" to "পর্যন্ত",
        "moddhe" to "মধ্যে", "baire" to "বাইরে", "vitore" to "ভিতরে", "upore" to "উপরে",
        "niche" to "নিচে", "samne" to "সামনে", "pichone" to "পিছনে", "pashe" to "পাশে",
        "kache" to "কাছে", "dure" to "দূরে", "sob" to "সব", "shob" to "সব",
        "sobai" to "সবাই", "shobai" to "সবাই", "kichu" to "কিছু", "kisu" to "কিছু",
        "keu" to "কেউ", "kew" to "কেউ", "onekta" to "অনেকটা", "ektai" to "একটাই",
        "sudhu" to "শুধু", "shudhu" to "শুধু", "prochur" to "প্রচুর", "kom" to "কম",
        "beshi" to "বেশি", "beshi" to "বেশি", "motamuti" to "মোটামুটি", "asole" to "আসলে",
        "ashole" to "আসলে", "mone" to "মনে", "monehoy" to "মনে হয়", "monehoye" to "মনে হয়",
        "bujhi" to "বুঝি", "bujhlam" to "বুঝলাম", "bujhte" to "বুঝতে", "bujhtechi" to "বুঝতেছি",
        "bujhtechi" to "বুঝতেছি", "bujhchi" to "বুঝছি", "bujhini" to "বুঝিনি", "janbo" to "জানবো",
        "jante" to "জানতে", "jantechai" to "জানতে চাই", "chai" to "চাই", "chay" to "চায়",
        "chao" to "চাও", "chan" to "চান", "chacchi" to "চাচ্ছি", "chachi" to "চাচ্ছি",
        "lagbe" to "লাগবে", "lage" to "লাগে", "lagche" to "লাগছে", "laglo" to "লাগলো",
        "pari" to "পারি", "parbo" to "পারবো", "paro" to "পারো", "paren" to "পারেন",
        "parina" to "পারি না", "parbona" to "পারবো না", "dorkar" to "দরকার", "dorkarhobe" to "দরকার হবে",
        "lagbena" to "লাগবে না", "somossa" to "সমস্যা", "shomossha" to "সমস্যা", "somadhan" to "সমাধান",
        "shomadhan" to "সমাধান", "prosno" to "প্রশ্ন", "proshno" to "প্রশ্ন", "uttor" to "উত্তর",
        "kotha" to "কথা", "kothaboli" to "কথা বলি", "golpo" to "গল্প", "moja" to "মজা",
        "mojaa" to "মজা", "hashi" to "হাসি", "hasi" to "হাসি", "haso" to "হাসো",
        "hasho" to "হাসো", "rag" to "রাগ", "ragkoro" to "রাগ করো", "mon" to "মন",
        "iccha" to "ইচ্ছা", "ichcha" to "ইচ্ছা", "pochondo" to "পছন্দ", "posondo" to "পছন্দ",
        "birokto" to "বিরক্ত", "chinta" to "চিন্তা", "vabna" to "ভাবনা", "bhavna" to "ভাবনা",
        "vabi" to "ভাবি", "bhabi" to "ভাবি", "vabchi" to "ভাবছি", "bhavchi" to "ভাবছি",
        "moneporo" to "মনে পড়ো", "monepore" to "মনে পড়ে", "miss" to "মিস",

        // Greetings / courtesy
        "assalamualaikum" to "আসসালামু আলাইকুম", "walaikumassalam" to "ওয়ালাইকুম আসসালাম",
        "salam" to "সালাম", "shagotom" to "স্বাগতম", "swagatom" to "স্বাগতম",
        "shubhosokal" to "শুভ সকাল", "subhosokal" to "শুভ সকাল", "shubhoratri" to "শুভ রাত্রি",
        "subhoratri" to "শুভ রাত্রি", "shubhechha" to "শুভেচ্ছা", "subhechha" to "শুভেচ্ছা",
        "ovinondon" to "অভিনন্দন", "abhinondon" to "অভিনন্দন", "welcome" to "ওয়েলকাম",
        "thanks" to "থ্যাংকস", "thankyou" to "থ্যাংক ইউ", "maaf" to "মাফ", "maf" to "মাফ",
        "khoma" to "ক্ষমা", "doya" to "দয়া", "doyakore" to "দয়া করে", "pleasekorun" to "প্লিজ করুন",
        "thakben" to "থাকবেন", "thako" to "থাকো", "thaki" to "থাকি", "thakbo" to "থাকবো",
        "valothako" to "ভালো থাকো", "valothakben" to "ভালো থাকবেন",

        // People / family
        "ma" to "মা", "maa" to "মা", "baba" to "বাবা", "abba" to "আব্বা", "ammu" to "আম্মু",
        "bon" to "বোন", "bhaiya" to "ভাইয়া", "vaiya" to "ভাইয়া", "apa" to "আপা",
        "chacha" to "চাচা", "chachi" to "চাচি", "mama" to "মামা", "mami" to "মামি",
        "khala" to "খালা", "khalu" to "খালু", "dada" to "দাদা", "dadi" to "দাদি",
        "nana" to "নানা", "nani" to "নানি", "chele" to "ছেলে", "meye" to "মেয়ে",
        "baccha" to "বাচ্চা", "shishu" to "শিশু", "poribar" to "পরিবার", "family" to "ফ্যামিলি",
        "shami" to "স্বামী", "stree" to "স্ত্রী", "bou" to "বউ", "bor" to "বর",
        "premik" to "প্রেমিক", "premika" to "প্রেমিকা", "manush" to "মানুষ", "lok" to "লোক",
        "customer" to "কাস্টমার", "client" to "ক্লায়েন্ট", "boss" to "বস", "colleague" to "কলিগ",

        // Common verbs
        "khabo" to "খাবো", "khacchi" to "খাচ্ছি", "khai" to "খাই", "khao" to "খাও",
        "khan" to "খান", "khete" to "খেতে", "kheye" to "খেয়ে", "ghumabo" to "ঘুমাবো",
        "ghumacchi" to "ঘুমাচ্ছি", "ghumai" to "ঘুমাই", "ghumao" to "ঘুমাও", "ghum" to "ঘুম",
        "uthbo" to "উঠবো", "utchi" to "উঠছি", "utho" to "উঠো", "uthen" to "উঠেন",
        "bosbo" to "বসবো", "boschi" to "বসছি", "boso" to "বসো", "bosen" to "বসেন",
        "jabo" to "যাবো", "jawa" to "যাওয়া", "jete" to "যেতে", "jao" to "যাও", "jan" to "জান",
        "ashchi" to "আসছি", "aste" to "আস্তে", "astechi" to "আসতেছি", "asben" to "আসবেন",
        "ashben" to "আসবেন", "asun" to "আসুন", "ashun" to "আসুন", "gelam" to "গেলাম",
        "gelo" to "গেলো", "jabe" to "যাবে", "jaben" to "যাবেন", "jabe na" to "যাবে না",
        "dekha" to "দেখা", "dekhte" to "দেখতে", "dekhe" to "দেখে", "dekhchi" to "দেখছি",
        "dekhsi" to "দেখছি", "dekhlam" to "দেখলাম", "shunbo" to "শুনবো", "sunbo" to "শুনবো",
        "shunchi" to "শুনছি", "sunchi" to "শুনছি", "shuni" to "শুনি", "suni" to "শুনি",
        "shuno" to "শুনো", "suno" to "শুনো", "bolte" to "বলতে", "boli" to "বলি",
        "bolle" to "বললে", "bolben" to "বলবেন", "likhbo" to "লিখবো", "likhchi" to "লিখছি",
        "likhi" to "লিখি", "likho" to "লিখো", "likhun" to "লিখুন", "pora" to "পড়া",
        "porbo" to "পড়বো", "porchi" to "পড়ছি", "pori" to "পড়ি", "poro" to "পড়ো",
        "porun" to "পড়ুন", "sikhbo" to "শিখবো", "shikhbo" to "শিখবো", "sikhchi" to "শিখছি",
        "shikhchi" to "শিখছি", "shikhi" to "শিখি", "khelbo" to "খেলবো", "khelchi" to "খেলছি",
        "kheli" to "খেলি", "khelo" to "খেলো", "kinbo" to "কিনবো", "kinchi" to "কিনছি",
        "kini" to "কিনি", "kino" to "কিনো", "bechbo" to "বেচবো", "bechi" to "বেচি",
        "dibena" to "দিবে না", "dibe" to "দিবে", "diben" to "দিবেন", "niye" to "নিয়ে",
        "nite" to "নিতে", "niche" to "নিচে", "nitechai" to "নিতে চাই", "pathabo" to "পাঠাবো",
        "pathacchi" to "পাঠাচ্ছি", "pathan" to "পাঠান", "pathate" to "পাঠাতে", "pabo" to "পাবো",
        "pacchi" to "পাচ্ছি", "pai" to "পাই", "paben" to "পাবেন", "paini" to "পাইনি",
        "rakhbo" to "রাখবো", "rakhi" to "রাখি", "rakho" to "রাখো", "rakhben" to "রাখবেন",
        "khulbo" to "খুলবো", "khulchi" to "খুলছি", "khulo" to "খুলো", "khulun" to "খুলুন",
        "bondho" to "বন্ধ", "bondhokoro" to "বন্ধ করো", "start" to "স্টার্ট", "shuru" to "শুরু",
        "shurukoro" to "শুরু করো", "ses" to "শেষ", "shesh" to "শেষ", "sheshkoro" to "শেষ করো",

        // Time / calendar
        "ajke" to "আজকে", "kalke" to "কালকে", "poroshu" to "পরশু", "gotokal" to "গতকাল",
        "agamikal" to "আগামীকাল", "ekhoni" to "এখনই", "porei" to "পরেই", "taratari" to "তাড়াতাড়ি",
        "deri" to "দেরি", "minute" to "মিনিট", "minit" to "মিনিট", "ghonta" to "ঘণ্টা",
        "soptaho" to "সপ্তাহ", "shoptaho" to "সপ্তাহ", "mas" to "মাস", "bochor" to "বছর",
        "bosor" to "বছর", "robibar" to "রবিবার", "somobar" to "সোমবার", "mongolbar" to "মঙ্গলবার",
        "budhbar" to "বুধবার", "brihospotibar" to "বৃহস্পতিবার", "shukrobar" to "শুক্রবার",
        "sukrobar" to "শুক্রবার", "shonibar" to "শনিবার", "sonibar" to "শনিবার", "january" to "জানুয়ারি",
        "february" to "ফেব্রুয়ারি", "march" to "মার্চ", "april" to "এপ্রিল", "may" to "মে",
        "june" to "জুন", "july" to "জুলাই", "august" to "আগস্ট", "september" to "সেপ্টেম্বর",
        "october" to "অক্টোবর", "november" to "নভেম্বর", "december" to "ডিসেম্বর",

        // Home / food / travel
        "ghor" to "ঘর", "room" to "রুম", "dorja" to "দরজা", "janala" to "জানালা",
        "ranna" to "রান্না", "khabar" to "খাবার", "pani" to "পানি", "cha" to "চা", "coffee" to "কফি",
        "vat" to "ভাত", "bhat" to "ভাত", "mach" to "মাছ", "mangsho" to "মাংস", "dim" to "ডিম",
        "dudh" to "দুধ", "ruti" to "রুটি", "sobji" to "সবজি", "shobji" to "সবজি", "fol" to "ফল",
        "bazar" to "বাজার", "dokaan" to "দোকান", "dokan" to "দোকান", "hotel" to "হোটেল",
        "restaurant" to "রেস্টুরেন্ট", "road" to "রোড", "rasta" to "রাস্তা", "bus" to "বাস",
        "train" to "ট্রেন", "plane" to "প্লেন", "flight" to "ফ্লাইট", "car" to "কার",
        "gari" to "গাড়ি", "rickshaw" to "রিকশা", "bike" to "বাইক", "ticket" to "টিকিট",
        "station" to "স্টেশন", "airport" to "এয়ারপোর্ট", "jatra" to "যাত্রা", "vromon" to "ভ্রমণ",
        "ghurte" to "ঘুরতে", "ghurbo" to "ঘুরবো", "location" to "লোকেশন", "thikana" to "ঠিকানা",

        // Work / study / technology
        "chakri" to "চাকরি", "business" to "বিজনেস", "bebsha" to "ব্যবসা", "kajer" to "কাজের",
        "meeting" to "মিটিং", "project" to "প্রজেক্ট", "report" to "রিপোর্ট", "file" to "ফাইল",
        "document" to "ডকুমেন্ট", "email" to "ইমেইল", "mail" to "মেইল", "call" to "কল",
        "class" to "ক্লাস", "porikkha" to "পরীক্ষা", "exam" to "এক্সাম", "result" to "রেজাল্ট",
        "teacher" to "টিচার", "student" to "স্টুডেন্ট", "boi" to "বই", "khata" to "খাতা",
        "pen" to "পেন", "computer" to "কম্পিউটার", "laptop" to "ল্যাপটপ", "keyboard" to "কিবোর্ড",
        "app" to "অ্যাপ", "software" to "সফটওয়্যার", "website" to "ওয়েবসাইট", "link" to "লিংক",
        "password" to "পাসওয়ার্ড", "otp" to "ওটিপি", "account" to "অ্যাকাউন্ট", "login" to "লগইন",
        "logout" to "লগআউট", "download" to "ডাউনলোড", "upload" to "আপলোড", "update" to "আপডেট",
        "install" to "ইনস্টল", "setting" to "সেটিং", "settings" to "সেটিংস", "option" to "অপশন",
        "feature" to "ফিচার", "ai" to "এআই", "smart" to "স্মার্ট", "online" to "অনলাইন",
        "offline" to "অফলাইন", "network" to "নেটওয়ার্ক", "data" to "ডাটা", "wifi" to "ওয়াইফাই",
        "charge" to "চার্জ", "battery" to "ব্যাটারি", "camera" to "ক্যামেরা", "image" to "ইমেজ",
        "caption" to "ক্যাপশন", "comment" to "কমেন্ট", "post" to "পোস্ট", "reel" to "রিল",
        "shorts" to "শর্টস", "page" to "পেজ", "profile" to "প্রোফাইল", "group" to "গ্রুপ",

        // Money / commerce
        "dam" to "দাম", "price" to "প্রাইস", "khoroch" to "খরচ", "khorocha" to "খরচ",
        "payment" to "পেমেন্ট", "bikash" to "বিকাশ", "bkash" to "বিকাশ", "nagad" to "নগদ",
        "bank" to "ব্যাংক", "balance" to "ব্যালেন্স", "offer" to "অফার", "discount" to "ডিসকাউন্ট",
        "order" to "অর্ডার", "delivery" to "ডেলিভারি", "product" to "প্রোডাক্ট", "service" to "সার্ভিস",
        "free" to "ফ্রি", "premium" to "প্রিমিয়াম", "subscription" to "সাবস্ক্রিপশন", "monthly" to "মাসিক",
        "yearly" to "বার্ষিক", "bill" to "বিল", "invoice" to "ইনভয়েস", "cash" to "ক্যাশ",

        // Places / Bangladesh
        "bangladeshi" to "বাংলাদেশি", "tangail" to "টাঙ্গাইল", "sirajganj" to "সিরাজগঞ্জ",
        "gazipur" to "গাজীপুর", "narayanganj" to "নারায়ণগঞ্জ", "comilla" to "কুমিল্লা", "cumilla" to "কুমিল্লা",
        "bogura" to "বগুড়া", "pabna" to "পাবনা", "kushtia" to "কুষ্টিয়া", "jessore" to "যশোর",
        "jashore" to "যশোর", "coxsbazar" to "কক্সবাজার", "noakhali" to "নোয়াখালী",
        "faridpur" to "ফরিদপুর", "district" to "জেলা", "jela" to "জেলা", "upazila" to "উপজেলা",
        "union" to "ইউনিয়ন", "gram" to "গ্রাম", "shohor" to "শহর", "city" to "সিটি",

        // Adjectives / states
        "valo" to "ভালো", "kharap" to "খারাপ", "darun" to "দারুণ", "osadharon" to "অসাধারণ",
        "oshadharon" to "অসাধারণ", "chomotkar" to "চমৎকার", "smart" to "স্মার্ট", "sohoj" to "সহজ",
        "kothin" to "কঠিন", "notun" to "নতুন", "puraton" to "পুরাতন", "gorom" to "গরম",
        "thanda" to "ঠান্ডা", "boro" to "বড়", "choto" to "ছোট", "lomba" to "লম্বা",
        "khato" to "খাটো", "taratari" to "তাড়াতাড়ি", "dhire" to "ধীরে", "shotti" to "সত্যি",
        "sotti" to "সত্যি", "mithya" to "মিথ্যা", "thik" to "ঠিক", "vul" to "ভুল",
        "bhul" to "ভুল", "ready" to "রেডি", "busy" to "বিজি", "freeachi" to "ফ্রি আছি",
        "available" to "অ্যাভেইলেবল", "important" to "ইম্পর্ট্যান্ট", "joruri" to "জরুরি",
        "safe" to "সেফ", "secure" to "সিকিউর", "private" to "প্রাইভেট", "public" to "পাবলিক",

        // Common particles / grammar helpers
        "to" to "তো", "je" to "যে", "j" to "যে", "ei" to "এই", "ek" to "এক",
        "dui" to "দুই", "tin" to "তিন", "char" to "চার", "pach" to "পাঁচ", "choy" to "ছয়",
        "sat" to "সাত", "aat" to "আট", "noy" to "নয়", "dosh" to "দশ", "praye" to "প্রায়",
        "hoyto" to "হয়তো", "tai" to "তাই", "tai na" to "তাই না", "tobe" to "তবে",
        "jodio" to "যদিও", "nahole" to "না হলে", "naki" to "নাকি", "mane" to "মানে",
        "orthat" to "অর্থাৎ", "karon" to "কারণ", "jemon" to "যেমন", "temon" to "তেমন",
        "ebar" to "এবার", "proti" to "প্রতি", "nijer" to "নিজের", "nijeke" to "নিজেকে",
        "nijera" to "নিজেরা", "amaderke" to "আমাদেরকে", "tomader" to "তোমাদের", "oder" to "ওদের"
    )

    private val extraFrequency: Map<String, Int> = mapOf(
        "abar" to 95, "age" to 100, "pore" to 105, "ekhane" to 100, "okhane" to 88,
        "sathe" to 100, "jonno" to 110, "theke" to 115, "sob" to 115, "sobai" to 95,
        "kichu" to 105, "keu" to 90, "sudhu" to 92, "beshi" to 100, "kom" to 95,
        "asole" to 100, "mone" to 108, "bujhi" to 95, "bujhlam" to 95, "chai" to 115,
        "lagbe" to 110, "lage" to 105, "pari" to 105, "parbo" to 100, "dorkar" to 105,
        "somossa" to 105, "somadhan" to 90, "kotha" to 110, "moja" to 95, "mon" to 100,
        "iccha" to 95, "pochondo" to 90, "chinta" to 95, "ma" to 105, "baba" to 105,
        "bari" to 108, "khabar" to 105, "pani" to 105, "khabo" to 100, "khacchi" to 95,
        "ghum" to 95, "ashchi" to 105, "asben" to 95, "jabe" to 100, "dekha" to 100,
        "dekhte" to 95, "bolte" to 100, "likhbo" to 90, "ajke" to 108, "kalke" to 100,
        "taratari" to 95, "deri" to 95, "ghonta" to 90, "soptaho" to 88, "mas" to 95,
        "bochor" to 95, "chakri" to 90, "business" to 85, "meeting" to 90, "project" to 90,
        "call" to 100, "class" to 90, "boi" to 90, "computer" to 85, "keyboard" to 95,
        "app" to 100, "email" to 90, "account" to 95, "update" to 90, "feature" to 90,
        "ai" to 100, "dam" to 100, "payment" to 90, "order" to 90, "delivery" to 88,
        "product" to 88, "service" to 92, "free" to 90, "premium" to 80, "to" to 115,
        "je" to 118, "ei" to 110, "ek" to 112, "tai" to 105, "karon" to 95, "mane" to 100
    )

    val words: Map<String, String> by lazy {
        buildMap {
            putAll(CommonBanglaPhoneticLexicon.words)
            putAll(extraWords)
            putAll(Stage5BanglaLexiconPack.words)
            putAll(Stage12AvroGoldenLexiconPack.words)
        }
    }

    val frequencyBonus: Map<String, Int> by lazy {
        buildMap {
            putAll(baseFrequency)
            extraFrequency.forEach { (key, value) ->
                put(key, maxOf(this[key] ?: 0, value))
            }
            Stage5BanglaLexiconPack.frequencyBonus.forEach { (key, value) ->
                put(key, maxOf(this[key] ?: 0, value))
            }
        }
    }

    private val baseFrequency: Map<String, Int> = mapOf(
        "ami" to 120, "amar" to 115, "amake" to 100, "amra" to 95,
        "tumi" to 120, "tomar" to 115, "tomake" to 100, "apni" to 120,
        "apnar" to 110, "ki" to 125, "kemon" to 115, "keno" to 105,
        "acho" to 115, "achi" to 110, "valo" to 110, "bhalo" to 108,
        "khub" to 105, "onek" to 100, "ekhon" to 105, "aj" to 105,
        "kal" to 100, "kaj" to 100, "hobe" to 110, "hoy" to 105,
        "korbo" to 100, "korchi" to 95, "jabo" to 95, "jani" to 95,
        "na" to 120, "hya" to 100, "thik" to 100, "accha" to 100,
        "dhonnobad" to 90, "bangla" to 90, "bangladesh" to 85,
        "vai" to 90, "bhai" to 90
    )
}

/**
 * Prefix and typo lookup index so the suggestion engine does not linearly scan the
 * full dictionary for every key press.
 */
class BanglaLexiconIndex(
    private val lexicon: Map<String, String> = ProductionBanglaLexicon.words,
    private val frequencyBonus: Map<String, Int> = ProductionBanglaLexicon.frequencyBonus
) {
    data class Entry(val roman: String, val bangla: String, val bonus: Int)

    private val exact: Map<String, Entry> = lexicon.mapValues { (roman, bangla) ->
        Entry(roman, bangla, frequencyBonus[roman] ?: 0)
    }

    private val prefixBuckets: Map<String, List<Entry>> = buildMap<String, MutableList<Entry>> {
        exact.values.forEach { entry ->
            val maxPrefix = minOf(MAX_PREFIX_INDEX, entry.roman.length)
            for (length in 1..maxPrefix) {
                getOrPut(entry.roman.substring(0, length)) { mutableListOf() }.add(entry)
            }
        }
    }.mapValues { (_, entries) ->
        entries.sortedWith(compareByDescending<Entry> { it.bonus }.thenBy { it.roman.length }.thenBy { it.roman })
    }

    private val typoBuckets: Map<String, List<Entry>> = exact.values.groupBy { entry ->
        val first = entry.roman.firstOrNull() ?: '_'
        "$first:${entry.roman.length}"
    }

    private val oneEditBuckets: Map<String, List<Entry>> = buildMap<String, MutableList<Entry>> {
        exact.values.forEach { entry ->
            oneEditSignatures(entry.roman).forEach { signature ->
                getOrPut(signature) { mutableListOf() }.add(entry)
            }
        }
    }.mapValues { (_, entries) ->
        entries.distinctBy { it.roman }
            .sortedWith(compareByDescending<Entry> { it.bonus }.thenBy { it.roman.length }.thenBy { it.roman })
    }

    private val banglaWords: Set<String> = exact.values.mapTo(hashSetOf()) { it.bangla }

    fun exact(roman: String): Entry? = exact[roman.lowercase()]

    fun prefix(roman: String, limit: Int): List<Entry> {
        if (roman.isBlank() || limit <= 0) return emptyList()
        val normalized = roman.lowercase()
        val key = normalized.take(MAX_PREFIX_INDEX)
        return prefixBuckets[key].orEmpty()
            .asSequence()
            .filter { it.roman.startsWith(normalized) && it.roman != normalized }
            .take(limit)
            .toList()
    }

    fun typoCandidates(roman: String, maxDistance: Int, scanLimit: Int): List<Entry> {
        if (roman.isBlank() || scanLimit <= 0) return emptyList()
        val normalized = roman.lowercase()
        val fast = oneEditSignatures(normalized).asSequence()
            .flatMap { signature -> oneEditBuckets[signature].orEmpty().asSequence() }
            .filter { it.roman != normalized }
            .distinctBy { it.roman }
            .toList()

        if (maxDistance <= 1 || fast.size >= scanLimit) {
            return fast.sortedWith(compareByDescending<Entry> { it.bonus }
                .thenBy { kotlin.math.abs(it.roman.length - normalized.length) }
                .thenBy { it.roman })
                .take(scanLimit)
        }

        val first = normalized.first()
        val fallback = ((normalized.length - maxDistance)..(normalized.length + maxDistance))
            .asSequence()
            .filter { it > 0 }
            .flatMap { length -> typoBuckets["$first:$length"].orEmpty().asSequence() }
        return (fast.asSequence() + fallback)
            .distinctBy { it.roman }
            .sortedWith(compareByDescending<Entry> { it.bonus }.thenBy { kotlin.math.abs(it.roman.length - normalized.length) }.thenBy { it.roman })
            .take(scanLimit)
            .toList()
    }

    fun containsBangla(text: String): Boolean = text in banglaWords

    fun size(): Int = exact.size

    private fun oneEditSignatures(value: String): Set<String> = buildSet {
        add(value)
        value.indices.forEach { index -> add(value.removeRange(index, index + 1)) }
    }

    companion object {
        private const val MAX_PREFIX_INDEX = 6
    }
}
