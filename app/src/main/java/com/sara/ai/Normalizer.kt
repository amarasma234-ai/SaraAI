package com.sara.ai

object Normalizer {
    private val map = listOf(
        "क्विक सेटिंग्स" to "quick settings", "क्विक सेटिंग" to "quick settings",
        "स्क्रॉल" to "scroll", "स्वाइप" to "swipe", "नीचे" to "neeche", "ऊपर" to "upar",
        "बाएं" to "left", "दाएं" to "right", "क्लिक" to "click", "दबाओ" to "dabao",
        "वापस" to "wapas", "पीछे" to "peeche", "होम" to "home", "नोटिफिकेशन" to "notification",
        "सर्च" to "search", "पहला" to "pehla", "रिजल्ट" to "result", "वीडियो" to "video",
        "टाइप" to "type", "हाँ" to "haan", "हां" to "haan", "नहीं" to "nahi", "रुको" to "ruko",
        "भेज दो" to "bhej do", "भेजो" to "bhejo", "रिसेंट" to "recent",
        "प्ले स्टोर" to "play store", "प्लेस्टोर" to "play store",
        "व्हाट्सएप" to "whatsapp", "व्हाट्सऐप" to "whatsapp", "वाट्सएप" to "whatsapp",
        "वॉट्सऐप" to "whatsapp", "व्हाट्सप" to "whatsapp", "व्हाट्स एप" to "whatsapp",
        "यूट्यूब" to "youtube", "यू ट्यूब" to "youtube", "यूटूब" to "youtube",
        "क्रोम" to "chrome", "इंस्टाग्राम" to "instagram", "फेसबुक" to "facebook",
        "सेटिंग्स" to "settings", "सेटिंग" to "settings", "गूगल" to "google",
        "खोल दो" to "kholo", "खोलो" to "kholo", "खोलिए" to "kholo", "खोल" to "kholo",
        "चालू" to "chalu", "बंद" to "band", "चलाओ" to "chalao", "चला दो" to "chalao",
        "ऑन" to " on", "ऑफ" to " off",
        "टॉर्च" to "torch", "टार्च" to "torch", "फ्लैशलाइट" to "torch", "फ्लैश लाइट" to "torch",
        "वॉल्यूम" to "volume", "वोल्यूम" to "volume", "आवाज़" to "awaaz", "आवाज" to "awaaz",
        "बढ़ाओ" to "badha", "बढ़ा" to "badha", "बढाओ" to "badha", "बढा" to "badha",
        "कम करो" to "kam", "कम" to "kam", "घटाओ" to "kam",
        "ब्राइटनेस" to "brightness", "ब्राईटनेस" to "brightness",
        "बैटरी" to "battery", "स्टोरेज" to "storage",
        "वाईफाई" to "wifi", "वाई फाई" to "wifi", "वाईफ़ाई" to "wifi",
        "नेटवर्क" to "network", "इंटरनेट" to "internet",
        "टेम्परेचर" to "temperature", "तापमान" to "temperature",
        "डिवाइस" to "device", "पर्सेंट" to "", "प्रतिशत" to "",
        "सारा" to "", "हेलो" to ""
    )
    private val digits = "०१२३४५६७८९"

    fun apply(s: String): String {
        var t = s
        for ((k, v) in map) t = t.replace(k, " $v ")
        for (i in digits.indices) t = t.replace(digits[i], ('0' + i))
        t = " $t ".replace(" को ", " ko ").replace(" पे ", " pe ").replace(" पर ", " pe ")
            .replace(" में ", " me ").replace(" और ", " aur ").replace(" फिर ", " phir ")
        return t.replace(Regex("\\s+"), " ").trim()
    }
}
