package com.example.model

object SiteCatalog {
    val sites: List<QuickSite> = listOf(
        // Search (10)
        QuickSite("Google", "https://www.google.com", SiteCategory.SEARCH, "Global", "GO", "World's largest search engine"),
        QuickSite("Bing", "https://www.bing.com", SiteCategory.SEARCH, "Global", "BI", "Microsoft search & rewards"),
        QuickSite("DuckDuckGo", "https://duckduckgo.com", SiteCategory.SEARCH, "Global", "DD", "Privacy-focused web search"),
        QuickSite("Brave Search", "https://search.brave.com", SiteCategory.SEARCH, "Global", "BR", "Independent private search"),
        QuickSite("Yahoo", "https://www.yahoo.com", SiteCategory.SEARCH, "Global", "YH", "Search, headlines & portals"),
        QuickSite("Yandex", "https://yandex.com", SiteCategory.SEARCH, "Eurasia", "YX", "Fast web & image search"),
        QuickSite("Baidu", "https://www.baidu.com", SiteCategory.SEARCH, "China", "BD", "Leading Chinese search portal"),
        QuickSite("Startpage", "https://www.startpage.com", SiteCategory.SEARCH, "EU", "SP", "Private Google-quality results"),
        QuickSite("Qwant", "https://www.qwant.com", SiteCategory.SEARCH, "France", "QW", "European privacy search"),
        QuickSite("Ecosia", "https://www.ecosia.org", SiteCategory.SEARCH, "Global", "EC", "Search that plants trees"),

        // Google & Chrome Tools (10)
        QuickSite("Gmail", "https://mail.google.com", SiteCategory.GOOGLE_CHROME, "Global", "GM", "Google email inbox"),
        QuickSite("Google Maps", "https://maps.google.com", SiteCategory.GOOGLE_CHROME, "Global", "MP", "Navigation & places"),
        QuickSite("Google Translate", "https://translate.google.com", SiteCategory.GOOGLE_CHROME, "Global", "TR", "130+ language translation"),
        QuickSite("Google Drive", "https://drive.google.com", SiteCategory.GOOGLE_CHROME, "Global", "DR", "Cloud files & backups"),
        QuickSite("Google Docs", "https://docs.google.com", SiteCategory.GOOGLE_CHROME, "Global", "DC", "Documents & collaboration"),
        QuickSite("Google Scholar", "https://scholar.google.com", SiteCategory.GOOGLE_CHROME, "Global", "SC", "Academic papers & citations"),
        QuickSite("Google News", "https://news.google.com", SiteCategory.GOOGLE_CHROME, "Global", "GN", "Personalized world headlines"),
        QuickSite("Google Photos", "https://photos.google.com", SiteCategory.GOOGLE_CHROME, "Global", "PH", "Cloud photo library"),
        QuickSite("Chrome Web Store", "https://chromewebstore.google.com", SiteCategory.GOOGLE_CHROME, "Global", "CW", "Extensions & web apps"),
        QuickSite("Google Earth", "https://earth.google.com/web", SiteCategory.GOOGLE_CHROME, "Global", "EA", "3D satellite globe"),

        // Social (10)
        QuickSite("Facebook", "https://m.facebook.com", SiteCategory.SOCIAL, "Global", "FB", "Connect with friends & groups"),
        QuickSite("Instagram", "https://www.instagram.com", SiteCategory.SOCIAL, "Global", "IG", "Photos, reels & stories"),
        QuickSite("X (Twitter)", "https://x.com", SiteCategory.SOCIAL, "Global", "X", "Real-time global conversations"),
        QuickSite("Reddit", "https://www.reddit.com", SiteCategory.SOCIAL, "Global", "RD", "Communities & discussions"),
        QuickSite("LinkedIn", "https://www.linkedin.com", SiteCategory.SOCIAL, "Global", "IN", "Professional network & jobs"),
        QuickSite("Pinterest", "https://www.pinterest.com", SiteCategory.SOCIAL, "Global", "PN", "Visual ideas & boards"),
        QuickSite("Discord", "https://discord.com/app", SiteCategory.SOCIAL, "Global", "DS", "Voice & community servers"),
        QuickSite("Mastodon", "https://mastodon.social", SiteCategory.SOCIAL, "Global", "MS", "Open decentralized social"),
        QuickSite("Bluesky", "https://bsky.app", SiteCategory.SOCIAL, "Global", "BS", "Open social microblogging"),
        QuickSite("Telegram Web", "https://web.telegram.org", SiteCategory.SOCIAL, "Global", "TG", "Cloud messaging on web"),

        // Video (9)
        QuickSite("YouTube", "https://m.youtube.com", SiteCategory.VIDEO, "Global", "YT", "Videos, music & live streams"),
        QuickSite("TikTok", "https://www.tiktok.com", SiteCategory.VIDEO, "Global", "TT", "Short-form video feed"),
        QuickSite("Twitch", "https://m.twitch.tv", SiteCategory.VIDEO, "Global", "TW", "Live gaming & streams"),
        QuickSite("Vimeo", "https://vimeo.com", SiteCategory.VIDEO, "Global", "VM", "High-quality creative video"),
        QuickSite("Dailymotion", "https://www.dailymotion.com", SiteCategory.VIDEO, "France", "DM", "International video platform"),
        QuickSite("Netflix", "https://www.netflix.com", SiteCategory.VIDEO, "Global", "NF", "Movies & series"),
        QuickSite("Bilibili", "https://www.bilibili.com", SiteCategory.VIDEO, "China", "BL", "Anime, tech & creators"),
        QuickSite("Rumble", "https://rumble.com", SiteCategory.VIDEO, "Global", "RM", "Creator video hosting"),
        QuickSite("TED", "https://www.ted.com", SiteCategory.VIDEO, "Global", "TD", "Ideas worth spreading"),

        // News (10)
        QuickSite("BBC News", "https://www.bbc.com/news", SiteCategory.NEWS, "UK", "BB", "Global breaking news"),
        QuickSite("Reuters", "https://www.reuters.com", SiteCategory.NEWS, "Global", "RT", "International wire journalism"),
        QuickSite("Al Jazeera", "https://www.aljazeera.com", SiteCategory.NEWS, "Qatar", "AJ", "World news & live coverage"),
        QuickSite("CNN", "https://edition.cnn.com", SiteCategory.NEWS, "USA", "CN", "Breaking news & analysis"),
        QuickSite("The Guardian", "https://www.theguardian.com", SiteCategory.NEWS, "UK", "TG", "Independent world news"),
        QuickSite("AP News", "https://apnews.com", SiteCategory.NEWS, "USA", "AP", "Fact-based news agency"),
        QuickSite("DW News", "https://www.dw.com", SiteCategory.NEWS, "Germany", "DW", "European & global reporting"),
        QuickSite("France 24", "https://www.france24.com/en/", SiteCategory.NEWS, "France", "F24", "International 24/7 news"),
        QuickSite("NHK World", "https://www3.nhk.or.jp/nhkworld/", SiteCategory.NEWS, "Japan", "NH", "Asia & Japan news"),
        QuickSite("Prothom Alo", "https://www.prothomalo.com", SiteCategory.NEWS, "Bangladesh", "PA", "Leading Bangla news portal"),

        // Shopping (9)
        QuickSite("Amazon", "https://www.amazon.com", SiteCategory.SHOPPING, "Global", "AZ", "Global marketplace"),
        QuickSite("eBay", "https://www.ebay.com", SiteCategory.SHOPPING, "Global", "EB", "Auctions & global deals"),
        QuickSite("AliExpress", "https://www.aliexpress.com", SiteCategory.SHOPPING, "Global", "AE", "Direct factory shopping"),
        QuickSite("Etsy", "https://www.etsy.com", SiteCategory.SHOPPING, "Global", "ET", "Handmade & vintage goods"),
        QuickSite("Walmart", "https://www.walmart.com", SiteCategory.SHOPPING, "USA", "WM", "Everyday retail & essentials"),
        QuickSite("Daraz", "https://www.daraz.com.bd", SiteCategory.SHOPPING, "South Asia", "DZ", "Regional online shopping"),
        QuickSite("Flipkart", "https://www.flipkart.com", SiteCategory.SHOPPING, "India", "FK", "Electronics & fashion"),
        QuickSite("Rakuten", "https://www.rakuten.com", SiteCategory.SHOPPING, "Japan", "RK", "Cashback & marketplace"),
        QuickSite("IKEA", "https://www.ikea.com", SiteCategory.SHOPPING, "Sweden", "IK", "Home & furniture catalog"),

        // Learning (9)
        QuickSite("Wikipedia", "https://www.wikipedia.org", SiteCategory.LEARNING, "Global", "WK", "Free multilingual encyclopedia"),
        QuickSite("Khan Academy", "https://www.khanacademy.org", SiteCategory.LEARNING, "Global", "KA", "Free math & science courses"),
        QuickSite("Coursera", "https://www.coursera.org", SiteCategory.LEARNING, "Global", "CR", "University degrees & courses"),
        QuickSite("edX", "https://www.edx.org", SiteCategory.LEARNING, "Global", "EX", "MIT & Harvard open courses"),
        QuickSite("Duolingo", "https://www.duolingo.com", SiteCategory.LEARNING, "Global", "DL", "Learn 40+ languages free"),
        QuickSite("MDN Web Docs", "https://developer.mozilla.org", SiteCategory.LEARNING, "Global", "MD", "Web standards documentation"),
        QuickSite("Stack Overflow", "https://stackoverflow.com", SiteCategory.LEARNING, "Global", "SO", "Developer Q&A community"),
        QuickSite("GitHub", "https://github.com", SiteCategory.LEARNING, "Global", "GH", "Open-source code & repos"),
        QuickSite("MIT OpenCourse", "https://ocw.mit.edu", SiteCategory.LEARNING, "USA", "MT", "Free university lecture notes"),

        // AI (8)
        QuickSite("Gemini", "https://gemini.google.com", SiteCategory.AI, "Global", "GM", "Google multimodal AI"),
        QuickSite("ChatGPT", "https://chatgpt.com", SiteCategory.AI, "Global", "CG", "Conversational AI assistant"),
        QuickSite("Claude", "https://claude.ai", SiteCategory.AI, "Global", "CL", "Reasoning & writing AI"),
        QuickSite("Perplexity", "https://www.perplexity.ai", SiteCategory.AI, "Global", "PX", "AI answer & citation engine"),
        QuickSite("Hugging Face", "https://huggingface.co", SiteCategory.AI, "Global", "HF", "Open models & datasets"),
        QuickSite("Poe", "https://poe.com", SiteCategory.AI, "Global", "PO", "Multi-model AI platform"),
        QuickSite("DeepL", "https://www.deepl.com", SiteCategory.AI, "Germany", "DP", "Neural precision translator"),
        QuickSite("NotebookLM", "https://notebooklm.google.com", SiteCategory.AI, "Global", "NL", "AI research notebook"),

        // Mail (7)
        QuickSite("Outlook", "https://outlook.live.com", SiteCategory.MAIL, "Global", "OL", "Microsoft email & calendar"),
        QuickSite("Proton Mail", "https://mail.proton.me", SiteCategory.MAIL, "Swiss", "PM", "End-to-end encrypted mail"),
        QuickSite("Yahoo Mail", "https://mail.yahoo.com", SiteCategory.MAIL, "Global", "YM", "Classic webmail portal"),
        QuickSite("Zoho Mail", "https://mail.zoho.com", SiteCategory.MAIL, "Global", "ZM", "Clean ad-free webmail"),
        QuickSite("Tuta Mail", "https://app.tuta.com", SiteCategory.MAIL, "Germany", "TU", "Encrypted German email"),
        QuickSite("GMX", "https://www.gmx.com", SiteCategory.MAIL, "EU", "GX", "Free webmail & cloud"),
        QuickSite("Fastmail", "https://www.fastmail.com", SiteCategory.MAIL, "Australia", "FM", "Fast private email"),

        // Travel (8)
        QuickSite("Booking.com", "https://www.booking.com", SiteCategory.TRAVEL, "Global", "BK", "Hotels & stays worldwide"),
        QuickSite("Airbnb", "https://www.airbnb.com", SiteCategory.TRAVEL, "Global", "AB", "Homes & local experiences"),
        QuickSite("Skyscanner", "https://www.skyscanner.net", SiteCategory.TRAVEL, "UK", "SK", "Compare global flights"),
        QuickSite("Tripadvisor", "https://www.tripadvisor.com", SiteCategory.TRAVEL, "Global", "TA", "Reviews & attractions"),
        QuickSite("Expedia", "https://www.expedia.com", SiteCategory.TRAVEL, "Global", "EP", "Flights, cars & hotels"),
        QuickSite("Google Flights", "https://www.google.com/travel/flights", SiteCategory.TRAVEL, "Global", "GF", "Fast fare finder"),
        QuickSite("Rome2Rio", "https://www.rome2rio.com", SiteCategory.TRAVEL, "Global", "R2", "Transit routes worldwide"),
        QuickSite("Agoda", "https://www.agoda.com", SiteCategory.TRAVEL, "Asia", "AG", "Asia & global hotel deals"),

        // Games (8)
        QuickSite("Chess.com", "https://www.chess.com", SiteCategory.GAMES, "Global", "CH", "Play chess online"),
        QuickSite("Lichess", "https://lichess.org", SiteCategory.GAMES, "Global", "LC", "Free open-source chess"),
        QuickSite("Poki", "https://poki.com", SiteCategory.GAMES, "Global", "PK", "Instant browser mini-games"),
        QuickSite("CrazyGames", "https://www.crazygames.com", SiteCategory.GAMES, "Global", "CZ", "Free HTML5 web games"),
        QuickSite("Itch.io", "https://itch.io", SiteCategory.GAMES, "Global", "IT", "Indie games marketplace"),
        QuickSite("Steam Store", "https://store.steampowered.com", SiteCategory.GAMES, "Global", "ST", "PC gaming store & news"),
        QuickSite("IGN", "https://www.ign.com", SiteCategory.GAMES, "Global", "IG", "Game reviews & guides"),
        QuickSite("Coolmath Games", "https://www.coolmathgames.com", SiteCategory.GAMES, "USA", "CM", "Logic & puzzle games"),

        // Finance (8)
        QuickSite("Yahoo Finance", "https://finance.yahoo.com", SiteCategory.FINANCE, "Global", "YF", "Live stocks & indices"),
        QuickSite("Bloomberg", "https://www.bloomberg.com", SiteCategory.FINANCE, "Global", "BL", "Global markets & business"),
        QuickSite("TradingView", "https://www.tradingview.com", SiteCategory.FINANCE, "Global", "TV", "Interactive market charts"),
        QuickSite("CoinGecko", "https://www.coingecko.com", SiteCategory.FINANCE, "Global", "CG", "Crypto prices & analytics"),
        QuickSite("Investing.com", "https://www.investing.com", SiteCategory.FINANCE, "Global", "IV", "Forex, stocks & news"),
        QuickSite("XE Currency", "https://www.xe.com", SiteCategory.FINANCE, "Global", "XE", "Live exchange rates"),
        QuickSite("CNBC", "https://www.cnbc.com", SiteCategory.FINANCE, "USA", "CB", "Market news & live data"),
        QuickSite("Financial Times", "https://www.ft.com", SiteCategory.FINANCE, "UK", "FT", "World economic reporting"),

        // Sports (8)
        QuickSite("ESPN", "https://www.espn.com", SiteCategory.SPORTS, "Global", "ES", "Scores, highlights & news"),
        QuickSite("ESPNcricinfo", "https://www.espncricinfo.com", SiteCategory.SPORTS, "Global", "EC", "Live cricket scores & stats"),
        QuickSite("BBC Sport", "https://www.bbc.com/sport", SiteCategory.SPORTS, "UK", "BS", "Football, F1 & athletics"),
        QuickSite("FIFA", "https://www.fifa.com", SiteCategory.SPORTS, "Global", "FF", "World football & rankings"),
        QuickSite("Flashscore", "https://www.flashscore.com", SiteCategory.SPORTS, "Global", "FS", "Real-time multi-sport scores"),
        QuickSite("Goal.com", "https://www.goal.com", SiteCategory.SPORTS, "Global", "GL", "Football transfers & tables"),
        QuickSite("NBA", "https://www.nba.com", SiteCategory.SPORTS, "USA", "NB", "Basketball live scores"),
        QuickSite("Formula 1", "https://www.formula1.com", SiteCategory.SPORTS, "Global", "F1", "Grand Prix schedules & timing")
    )
}
