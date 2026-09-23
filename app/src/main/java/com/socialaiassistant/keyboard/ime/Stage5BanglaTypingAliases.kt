package com.socialaiassistant.keyboard.ime

/** Conservative explicit shorthand aliases allowed to autocorrect on a word boundary. */
object Stage5BanglaTypingAliases {
    val aliasToCanonical: Map<String, String> = mapOf(
        "plz" to "please", "pls" to "please", "sry" to "sorry", "thnx" to "thanks", "tnx" to "thanks",
        "msg" to "message", "rply" to "reply", "cmnt" to "comment", "vdo" to "video", "vid" to "video",
        "phto" to "photo", "phn" to "phone", "mbl" to "mobile", "kbd" to "keyboard", "kybrd" to "keyboard",
        "typng" to "typing", "suggstn" to "suggestion", "predctn" to "prediction", "trnslte" to "translate",
        "grmmr" to "grammar", "cntxt" to "context", "prvcy" to "privacy", "scrty" to "security",
        "prmssn" to "permission", "acess" to "access", "accss" to "access", "bcknd" to "backend",
        "srvr" to "server", "db" to "database", "ntwrk" to "network", "intrnet" to "internet",
        "oflin" to "offline", "onlin" to "online", "prfrmnc" to "performance", "bttry" to "battery",
        "strg" to "storage", "dvlpr" to "developer", "vrsn" to "version", "rls" to "release",
        "tstng" to "testing", "bkp" to "backup", "rstor" to "restore",

        "ofc" to "office", "cmpny" to "company", "mngr" to "manager", "empl" to "employee",
        "mtng" to "meeting", "prjct" to "project", "rprt" to "report", "dln" to "deadline",
        "schdl" to "schedule", "mrktng" to "marketing", "sls" to "sales", "cstmr" to "customer",
        "clnt" to "client", "fllwp" to "followup", "pymnt" to "payment", "ordr" to "order",
        "dlvry" to "delivery", "prdct" to "product", "srvc" to "service", "dscnt" to "discount",
        "subscrptn" to "subscription",

        "krun" to "korun", "krben" to "korben", "krle" to "korle", "kre" to "kore",
        "dben" to "diben", "dibo" to "dibo", "dite" to "dite", "blun" to "bolun",
        "blben" to "bolben", "blle" to "bolle", "jnan" to "janan", "jnben" to "janaben",
        "dkn" to "dekhan", "dkhun" to "dekhun", "dkhben" to "dekhben", "lkhbo" to "likhbo",
        "lkhchi" to "likhchi", "lkhun" to "likhun", "ptabo" to "pathabo", "pthan" to "pathan",
        "pouchbo" to "pouchabo", "frbo" to "firbo", "brhbo" to "berhobo",

        "vlo" to "valo", "bhalo" to "bhalo", "sundr" to "sundor", "drun" to "darun",
        "sohj" to "sohoj", "kthin" to "kothin", "jruri" to "joruri", "ntun" to "notun",
        "prono" to "purono", "sothik" to "shothik", "poriskar" to "porishkar",
        "rdy" to "ready", "cnfrm" to "confirm", "cncl" to "cancel",

        "thikase" to "thikache", "thikace" to "thikache", "thikachee" to "thikache",
        "kmnaso" to "kemonacho", "kmnacho" to "kemonacho", "kmnachen" to "kemonachen",
        "kthyacho" to "kothayacho", "kthyachen" to "kothayachen", "kikrcho" to "kikorcho",
        "kikrchen" to "kikorchen", "smossanei" to "somossanei", "somossanai" to "somossanei",
        "chntanei" to "chintanei", "alhamdulilah" to "alhamdulillah", "inshAllah" to "inshallah",
        "inshalla" to "inshallah", "mashalla" to "mashallah", "assalamu alaikum" to "assalamualaikum"
    ).mapKeys { it.key.lowercase().replace(" ", "") }
}
