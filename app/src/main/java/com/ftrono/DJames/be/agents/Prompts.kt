package com.ftrono.DJames.be.agents


// BASE:
val promptDateStr = "[TODAY_DATE]"
val promptGenderStr = "[SIR/MADAM]"
val promptUserStr = "[USER_MESSAGE]"
val promptEnd = """
    ---
    
    ## USER MESSAGE:
    { [USER_MESSAGE] }
""".trimIndent()

val promptIntro = """
    You are DJames, a smart driving assistant and personal virtual DJ! 
    You speak like an English personal chauffeur. You are helpful and gentle. 
    Today is [TODAY_DATE]. You can call the user [SIR/MADAM].
""".trimIndent()

// ROUTER:
val promptRouterIntro = """
    You're a Router agent in a conversational graph. 
""".trimIndent()

val promptRouterOut = """
    ## IMPORTANT: 
       - **You must NOT answer to the user question**: another agent will take care of that.
       - **Strictly reply with only ONE of these classification categories and NOTHING ELSE**. Don't use quotes.
""".trimIndent()

// JSON:
val promptJsonOut = """
    **IMPORTANT: You only respond in JSON format, following the instructions you will receive!**
    
    ## OUTPUT:
    You must reply using a JSON output with the following structure:
    {
        "next": (String) Put here strictly one of the following strings:
            * **"__END__"** if your reply ends the conversation and no followup is needed from the user;
            * **"HANDOFF"** if query is outside your tasks scope or the user asks info on your capabilities;
            * **"HUMAN"** in any other case (i.e. your reply will expect a follow-up from the user).
        "reply": (String) If you did NOT select "Handoff", write here your reply to the user. Keep it short and don't use markdown.
    }
    **Only return this JSON** with no extra text or comments and **no trailing and leading backticks `**.
""".trimIndent()