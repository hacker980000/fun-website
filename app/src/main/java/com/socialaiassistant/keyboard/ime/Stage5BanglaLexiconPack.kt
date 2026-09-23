package com.socialaiassistant.keyboard.ime

/**
 * Stage-5 curated high-frequency expansion for Bangladesh-first daily typing.
 *
 * This pack is intentionally hand-curated rather than copied from a copyleft
 * dictionary. Entries focus on common conversation, work, study, commerce,
 * technology, creator and everyday Bangladesh vocabulary. It remains Android-free.
 */
object Stage5BanglaLexiconPack {
    val words: Map<String, String> = mapOf(
        // Connectors / grammar / conversational particles
        "othoba" to "অথবা", "ebong" to "এবং", "kintu" to "কিন্তু", "borong" to "বরং",
        "jodi" to "যদি", "jodiou" to "যদিও", "tahole" to "তাহলে", "taholeo" to "তাহলেও",
        "jehetu" to "যেহেতু", "sehetu" to "সেহেতু", "jotokkhon" to "যতক্ষণ", "totokkhon" to "ততক্ষণ",
        "jotokkhone" to "যতক্ষণে", "jokhon" to "যখন", "tokhon" to "তখন", "jekhane" to "যেখানে",
        "sekhane" to "সেখানে", "jedike" to "যেদিকে", "sedike" to "সেদিকে", "jei" to "যেই",
        "sei" to "সেই", "jeta" to "যেটা", "seta" to "সেটা", "jegulo" to "যেগুলো", "segulo" to "সেগুলো",
        "jar" to "যার", "tar" to "তার", "jader" to "যাদের", "tader" to "তাদের",
        "jekono" to "যেকোনো", "kono" to "কোনো", "prottek" to "প্রত্যেক", "prottekta" to "প্রত্যেকটা",
        "prothom" to "প্রথম", "ditiyo" to "দ্বিতীয়", "tritiyo" to "তৃতীয়", "sheshporjonto" to "শেষ পর্যন্ত",
        "ekebare" to "একেবারে", "motew" to "মোটেও", "praye" to "প্রায়", "prayi" to "প্রায়ই",
        "obosshoi" to "অবশ্যই", "oboshshoi" to "অবশ্যই", "hoyni" to "হয়নি", "hoyechilo" to "হয়েছিল",
        "hoyeche" to "হয়েছে", "hocchilo" to "হচ্ছিল", "hoyechhe" to "হয়েছে", "acheki" to "আছে কি",
        "nei" to "নেই", "chilo" to "ছিল", "chilen" to "ছিলেন", "chilam" to "ছিলাম", "chilona" to "ছিল না",
        "thakle" to "থাকলে", "thakleo" to "থাকলেও", "thakena" to "থাকে না", "thakbe" to "থাকবে",

        // Common actions / verb forms
        "kori" to "করি", "koro" to "করো", "korun" to "করুন", "kore" to "করে", "korle" to "করলে",
        "korben" to "করবেন", "korbe" to "করবে", "kortechi" to "করতেছি", "kortesi" to "করতেছি",
        "korlam" to "করলাম", "korechi" to "করেছি", "korechen" to "করেছেন", "koreni" to "করেনি",
        "dibo" to "দিবো", "dib" to "দিব", "dao" to "দাও", "den" to "দেন", "din" to "দিন",
        "diye" to "দিয়ে", "dite" to "দিতে", "dilam" to "দিলাম", "diyechi" to "দিয়েছি", "dibenna" to "দিবেন না",
        "nibo" to "নিবো", "nio" to "নিও", "nin" to "নিন", "nilam" to "নিলাম", "niyesi" to "নিয়েছি",
        "nebo" to "নেবো", "nen" to "নেন", "nile" to "নিলে", "newa" to "নেওয়া",
        "bolbo" to "বলবো", "bolo" to "বলো", "bolun" to "বলুন", "bolchi" to "বলছি", "bolsi" to "বলছি",
        "bollam" to "বললাম", "bolechi" to "বলেছি", "bolen" to "বলেন", "bole" to "বলে",
        "janai" to "জানাই", "janao" to "জানাও", "janan" to "জানান", "janabo" to "জানাবো", "janaben" to "জানাবেন",
        "janiye" to "জানিয়ে", "janale" to "জানালে", "janano" to "জানানো", "jene" to "জেনে", "jenesi" to "জেনেছি",
        "dekhi" to "দেখি", "dekho" to "দেখো", "dekhun" to "দেখুন", "dekhben" to "দেখবেন", "dekheni" to "দেখিনি",
        "dekhabo" to "দেখাবো", "dekhate" to "দেখাতে", "dekhale" to "দেখালে", "dekhan" to "দেখান",
        "shikhi" to "শিখি", "shikho" to "শিখো", "shikhun" to "শিখুন", "shikhte" to "শিখতে", "shikhechi" to "শিখেছি",
        "baniye" to "বানিয়ে", "banabo" to "বানাবো", "banacchi" to "বানাচ্ছি", "banan" to "বানান", "banate" to "বানাতে",
        "likhe" to "লিখে", "likhechi" to "লিখেছি", "likhben" to "লিখবেন", "likhte" to "লিখতে", "lekha" to "লেখা",
        "porte" to "পড়তে", "porechi" to "পড়েছি", "porben" to "পড়বেন", "porashona" to "পড়াশোনা",
        "bujhtepari" to "বুঝতে পারি", "bujhteparchi" to "বুঝতে পারছি", "bujhbe" to "বুঝবে", "bujhben" to "বুঝবেন",
        "bujhao" to "বুঝাও", "bujhan" to "বুঝান", "bojha" to "বোঝা", "bujhe" to "বুঝে",
        "chesta" to "চেষ্টা", "cheshta" to "চেষ্টা", "chestakorchi" to "চেষ্টা করছি", "trykorbo" to "ট্রাই করবো",
        "pouchabo" to "পৌঁছাবো", "pouchaisi" to "পৌঁছেছি", "pouchate" to "পৌঁছাতে", "pouche" to "পৌঁছে",
        "berhobo" to "বের হবো", "berhocchi" to "বের হচ্ছি", "beriye" to "বেরিয়ে", "berho" to "বের হও",
        "firbo" to "ফিরবো", "firchi" to "ফিরছি", "fire" to "ফিরে", "ferot" to "ফেরত",
        "pathiye" to "পাঠিয়ে", "pathiyechi" to "পাঠিয়েছি", "pathaben" to "পাঠাবেন", "pathano" to "পাঠানো",
        "paisi" to "পেয়েছি", "peyechi" to "পেয়েছি", "pelen" to "পেলেন", "pele" to "পেলে",
        "harie" to "হারিয়ে", "hariye" to "হারিয়ে", "hariyechi" to "হারিয়েছি", "khujchi" to "খুঁজছি", "khuje" to "খুঁজে",
        "khujen" to "খুঁজেন", "khujbo" to "খুঁজবো", "monekorchi" to "মনে করছি", "moneholo" to "মনে হলো",

        // Feelings / qualities
        "sundor" to "সুন্দর", "darun" to "দারুণ", "chomotkar" to "চমৎকার", "osadharon" to "অসাধারণ",
        "durdanto" to "দুর্দান্ত", "perfect" to "পারফেক্ট", "smart" to "স্মার্ট", "simple" to "সিম্পল",
        "sohoj" to "সহজ", "kothin" to "কঠিন", "jotil" to "জটিল", "joruri" to "জরুরি",
        "important" to "ইম্পর্ট্যান্ট", "proyojonio" to "প্রয়োজনীয়", "notun" to "নতুন", "purono" to "পুরোনো",
        "boro" to "বড়", "choto" to "ছোট", "lomba" to "লম্বা", "khato" to "খাটো",
        "druto" to "দ্রুত", "slow" to "স্লো", "shanto" to "শান্ত", "byasto" to "ব্যস্ত",
        "busy" to "বিজি", "free" to "ফ্রি", "khushi" to "খুশি", "dukhi" to "দুঃখী",
        "monkharap" to "মন খারাপ", "klanto" to "ক্লান্ত", "oshustho" to "অসুস্থ", "sustho" to "সুস্থ",
        "nirapod" to "নিরাপদ", "jhokim" to "ঝুঁকি", "shothik" to "সঠিক", "vul" to "ভুল",
        "thikthak" to "ঠিকঠাক", "clear" to "ক্লিয়ার", "porishkar" to "পরিষ্কার", "ready" to "রেডি",
        "complete" to "কমপ্লিট", "final" to "ফাইনাল", "confirm" to "কনফার্ম", "cancel" to "ক্যানসেল",

        // Communication / social
        "message" to "মেসেজ", "reply" to "রিপ্লাই", "response" to "রেসপন্স", "comment" to "কমেন্ট",
        "post" to "পোস্ট", "caption" to "ক্যাপশন", "status" to "স্ট্যাটাস", "story" to "স্টোরি",
        "reel" to "রিল", "reels" to "রিলস", "shorts" to "শর্টস", "video" to "ভিডিও",
        "photo" to "ফটো", "chobi" to "ছবি", "image" to "ইমেজ", "audio" to "অডিও",
        "voice" to "ভয়েস", "record" to "রেকর্ড", "live" to "লাইভ", "share" to "শেয়ার",
        "like" to "লাইক", "follow" to "ফলো", "follower" to "ফলোয়ার", "subscribe" to "সাবস্ক্রাইব",
        "subscriber" to "সাবস্ক্রাইবার", "channel" to "চ্যানেল", "page" to "পেজ", "profile" to "প্রোফাইল",
        "inbox" to "ইনবক্স", "notification" to "নোটিফিকেশন", "group" to "গ্রুপ", "community" to "কমিউনিটি",
        "facebook" to "ফেসবুক", "youtube" to "ইউটিউব", "instagram" to "ইনস্টাগ্রাম", "whatsapp" to "হোয়াটসঅ্যাপ",
        "messenger" to "মেসেঞ্জার", "telegram" to "টেলিগ্রাম", "tiktok" to "টিকটক",

        // Technology / keyboard / AI
        "android" to "অ্যান্ড্রয়েড", "mobile" to "মোবাইল", "phone" to "ফোন", "smartphone" to "স্মার্টফোন",
        "device" to "ডিভাইস", "screen" to "স্ক্রিন", "display" to "ডিসপ্লে", "touch" to "টাচ",
        "button" to "বাটন", "menu" to "মেনু", "toolbar" to "টুলবার", "panel" to "প্যানেল",
        "theme" to "থিম", "color" to "কালার", "font" to "ফন্ট", "layout" to "লেআউট",
        "emoji" to "ইমোজি", "clipboard" to "ক্লিপবোর্ড", "suggestion" to "সাজেশন", "autocorrect" to "অটোকরেক্ট",
        "prediction" to "প্রেডিকশন", "typing" to "টাইপিং", "phonetic" to "ফোনেটিক", "banglish" to "বাংলিশ",
        "english" to "ইংলিশ", "language" to "ল্যাঙ্গুয়েজ", "translate" to "ট্রান্সলেট", "translation" to "ট্রান্সলেশন",
        "grammar" to "গ্রামার", "rewrite" to "রিরাইট", "context" to "কনটেক্সট", "privacy" to "প্রাইভেসি",
        "security" to "সিকিউরিটি", "permission" to "পারমিশন", "access" to "অ্যাক্সেস", "accessibility" to "অ্যাক্সেসিবিলিটি",
        "server" to "সার্ভার", "backend" to "ব্যাকএন্ড", "database" to "ডাটাবেস", "cloud" to "ক্লাউড",
        "api" to "এপিআই", "code" to "কোড", "coding" to "কোডিং", "developer" to "ডেভেলপার",
        "version" to "ভার্সন", "build" to "বিল্ড", "release" to "রিলিজ", "bug" to "বাগ",
        "issue" to "ইস্যু", "fix" to "ফিক্স", "test" to "টেস্ট", "testing" to "টেস্টিং",
        "data" to "ডাটা", "backup" to "ব্যাকআপ", "restore" to "রিস্টোর", "sync" to "সিঙ্ক",
        "network" to "নেটওয়ার্ক", "internet" to "ইন্টারনেট", "wifi" to "ওয়াইফাই", "online" to "অনলাইন",
        "offline" to "অফলাইন", "speed" to "স্পিড", "performance" to "পারফরম্যান্স", "battery" to "ব্যাটারি",
        "memory" to "মেমোরি", "storage" to "স্টোরেজ", "ram" to "র‍্যাম", "processor" to "প্রসেসর",

        // Work / business / commerce
        "office" to "অফিস", "company" to "কোম্পানি", "team" to "টিম", "staff" to "স্টাফ",
        "admin" to "অ্যাডমিন", "manager" to "ম্যানেজার", "chairman" to "চেয়ারম্যান", "director" to "ডিরেক্টর",
        "owner" to "ওনার", "employee" to "এমপ্লয়ি", "job" to "জব", "career" to "ক্যারিয়ার",
        "salary" to "স্যালারি", "beton" to "বেতন", "bonus" to "বোনাস", "target" to "টার্গেট",
        "task" to "টাস্ক", "deadline" to "ডেডলাইন", "schedule" to "শিডিউল", "plan" to "প্ল্যান",
        "planning" to "প্ল্যানিং", "strategy" to "স্ট্র্যাটেজি", "marketing" to "মার্কেটিং", "sales" to "সেলস",
        "customer" to "কাস্টমার", "buyer" to "বায়ার", "seller" to "সেলার", "client" to "ক্লায়েন্ট",
        "lead" to "লিড", "followup" to "ফলোআপ", "deal" to "ডিল", "contract" to "কন্ট্রাক্ট",
        "invoice" to "ইনভয়েস", "bill" to "বিল", "price" to "প্রাইস", "dam" to "দাম",
        "discount" to "ডিসকাউন্ট", "offer" to "অফার", "package" to "প্যাকেজ", "subscription" to "সাবস্ক্রিপশন",
        "payment" to "পেমেন্ট", "cash" to "ক্যাশ", "bkash" to "বিকাশ", "nagad" to "নগদ",
        "bank" to "ব্যাংক", "balance" to "ব্যালেন্স", "transaction" to "ট্রানজ্যাকশন", "refund" to "রিফান্ড",
        "order" to "অর্ডার", "delivery" to "ডেলিভারি", "courier" to "কুরিয়ার", "product" to "প্রোডাক্ট",
        "service" to "সার্ভিস", "quality" to "কোয়ালিটি", "brand" to "ব্র্যান্ড", "stock" to "স্টক",
        "available" to "অ্যাভেইলেবল", "unavailable" to "আনঅ্যাভেইলেবল", "booking" to "বুকিং", "confirmkorun" to "কনফার্ম করুন",

        // Education / knowledge
        "school" to "স্কুল", "college" to "কলেজ", "university" to "ইউনিভার্সিটি", "campus" to "ক্যাম্পাস",
        "department" to "ডিপার্টমেন্ট", "subject" to "সাবজেক্ট", "course" to "কোর্স", "semester" to "সেমিস্টার",
        "classroom" to "ক্লাসরুম", "lecture" to "লেকচার", "lesson" to "লেসন", "chapter" to "চ্যাপ্টার",
        "question" to "কোয়েশ্চেন", "answer" to "আনসার", "assignment" to "অ্যাসাইনমেন্ট", "homework" to "হোমওয়ার্ক",
        "exam" to "এক্সাম", "porikkha" to "পরীক্ষা", "result" to "রেজাল্ট", "mark" to "মার্ক",
        "grade" to "গ্রেড", "certificate" to "সার্টিফিকেট", "admission" to "অ্যাডমিশন", "registration" to "রেজিস্ট্রেশন",
        "research" to "রিসার্চ", "thesis" to "থিসিস", "science" to "সায়েন্স", "technology" to "টেকনোলজি",
        "engineering" to "ইঞ্জিনিয়ারিং", "engineer" to "ইঞ্জিনিয়ার", "math" to "ম্যাথ", "statistics" to "স্ট্যাটিসটিক্স",
        "physics" to "ফিজিক্স", "chemistry" to "কেমিস্ট্রি", "biology" to "বায়োলজি", "computerscience" to "কম্পিউটার সায়েন্স",
        "teacher" to "টিচার", "sir" to "স্যার", "madam" to "ম্যাডাম", "student" to "স্টুডেন্ট",

        // Home / everyday life
        "basha" to "বাসা", "bari" to "বাড়ি", "flat" to "ফ্ল্যাট", "building" to "বিল্ডিং",
        "floor" to "ফ্লোর", "lift" to "লিফট", "gate" to "গেট", "garage" to "গ্যারেজ",
        "bedroom" to "বেডরুম", "bathroom" to "বাথরুম", "kitchen" to "কিচেন", "balcony" to "বারান্দা",
        "table" to "টেবিল", "chair" to "চেয়ার", "bed" to "বেড", "fan" to "ফ্যান",
        "light" to "লাইট", "current" to "কারেন্ট", "electricity" to "বিদ্যুৎ", "solar" to "সোলার",
        "ac" to "এসি", "fridge" to "ফ্রিজ", "tv" to "টিভি", "camera" to "ক্যামেরা",
        "jama" to "জামা", "kapor" to "কাপড়", "juta" to "জুতা", "bag" to "ব্যাগ",
        "ghori" to "ঘড়ি", "chabi" to "চাবি", "taka" to "টাকা", "poisha" to "পয়সা",
        "khudha" to "ক্ষুধা", "pipasaa" to "পিপাসা", "nasta" to "নাস্তা", "lunch" to "লাঞ্চ",
        "dinner" to "ডিনার", "biryani" to "বিরিয়ানি", "polao" to "পোলাও", "chicken" to "চিকেন",
        "beef" to "বিফ", "dal" to "ডাল", "alu" to "আলু", "peyaj" to "পেঁয়াজ",
        "lobon" to "লবণ", "chini" to "চিনি", "tel" to "তেল", "moshla" to "মসলা",

        // Travel / places / Bangladesh
        "dhaka" to "ঢাকা", "chattogram" to "চট্টগ্রাম", "chittagong" to "চট্টগ্রাম", "rajshahi" to "রাজশাহী",
        "khulna" to "খুলনা", "barishal" to "বরিশাল", "sylhet" to "সিলেট", "rangpur" to "রংপুর",
        "mymensingh" to "ময়মনসিংহ", "tangail" to "টাঙ্গাইল", "sirajganj" to "সিরাজগঞ্জ", "gazipur" to "গাজীপুর",
        "narayanganj" to "নারায়ণগঞ্জ", "cumilla" to "কুমিল্লা", "comilla" to "কুমিল্লা", "bogura" to "বগুড়া",
        "jessore" to "যশোর", "jashore" to "যশোর", "coxsbazar" to "কক্সবাজার", "bangladesh" to "বাংলাদেশ",
        "desh" to "দেশ", "jela" to "জেলা", "upojela" to "উপজেলা", "union" to "ইউনিয়ন",
        "gram" to "গ্রাম", "shohor" to "শহর", "city" to "সিটি", "area" to "এরিয়া",
        "airport" to "এয়ারপোর্ট", "station" to "স্টেশন", "terminal" to "টার্মিনাল", "platform" to "প্ল্যাটফর্ম",
        "busstand" to "বাসস্ট্যান্ড", "vara" to "ভাড়া", "ticket" to "টিকিট", "seat" to "সিট",
        "hotel" to "হোটেল", "restaurant" to "রেস্টুরেন্ট", "tour" to "ট্যুর", "travel" to "ট্রাভেল",
        "trip" to "ট্রিপ", "passport" to "পাসপোর্ট", "visa" to "ভিসা", "abroad" to "বিদেশ",

        // Numbers / quantities
        "shunno" to "শূন্য", "zero" to "জিরো", "ek" to "এক", "dui" to "দুই", "tin" to "তিন",
        "char" to "চার", "pach" to "পাঁচ", "choy" to "ছয়", "sat" to "সাত", "aat" to "আট",
        "noy" to "নয়", "dosh" to "দশ", "egaro" to "এগারো", "baro" to "বারো", "tero" to "তেরো",
        "chouddo" to "চৌদ্দ", "ponero" to "পনেরো", "sholo" to "ষোলো", "shotero" to "সতেরো",
        "atharo" to "আঠারো", "unish" to "উনিশ", "bish" to "বিশ", "trish" to "ত্রিশ",
        "chollish" to "চল্লিশ", "ponchash" to "পঞ্চাশ", "shat" to "ষাট", "sottor" to "সত্তর",
        "ashi" to "আশি", "nobboi" to "নব্বই", "sho" to "শত", "hajar" to "হাজার",
        "lakh" to "লাখ", "koti" to "কোটি", "half" to "হাফ", "adha" to "আধা",

        // Useful Bangladesh conversation shortcuts / forms
        "alhamdulillah" to "আলহামদুলিল্লাহ", "inshallah" to "ইনশাআল্লাহ", "inshaallah" to "ইনশাআল্লাহ",
        "mashallah" to "মাশাআল্লাহ", "mashaallah" to "মাশাআল্লাহ", "allah" to "আল্লাহ",
        "vai" to "ভাই", "bhai" to "ভাই", "apu" to "আপু", "bro" to "ব্রো",
        "dost" to "দোস্ত", "bondhu" to "বন্ধু", "jan" to "জান", "priyo" to "প্রিয়",
        "valoachi" to "ভালো আছি", "kemonacho" to "কেমন আছো", "kemonachen" to "কেমন আছেন",
        "kothayacho" to "কোথায় আছো", "kothayachen" to "কোথায় আছেন", "kikorcho" to "কি করছো",
        "kikorchen" to "কি করছেন", "thikache" to "ঠিক আছে", "somossanei" to "সমস্যা নেই",
        "chintanei" to "চিন্তা নেই", "dekhi" to "দেখি", "janio" to "জানিও", "janaben" to "জানাবেন",
        "ektu" to "একটু", "please" to "প্লিজ", "sorry" to "সরি", "sori" to "সরি",
        "okay" to "ওকে", "ok" to "ওকে", "bye" to "বাই", "goodnight" to "গুড নাইট",
        "goodmorning" to "গুড মর্নিং", "congrats" to "কংগ্র্যাটস", "congratulations" to "কংগ্র্যাচুলেশনস"
    )

    val frequencyBonus: Map<String, Int> = buildMap {
        val veryHigh = listOf(
            "kintu", "jodi", "tahole", "jokhon", "tokhon", "jeta", "seta", "tar", "sei",
            "kori", "koro", "korun", "kore", "korben", "dao", "din", "dibo", "bolbo", "bolo",
            "janabo", "janaben", "dekhi", "dekho", "sundor", "sohoj", "notun", "vul", "shothik",
            "message", "reply", "video", "photo", "share", "mobile", "phone", "typing", "banglish",
            "office", "company", "team", "payment", "order", "service", "basha", "bari", "taka",
            "dhaka", "bangladesh", "ektu", "please", "sorry", "ok", "vai", "apu"
        )
        veryHigh.forEach { put(it, 110) }
        words.keys.forEach { key -> if (key !in this) put(key, 72) }
    }
}
