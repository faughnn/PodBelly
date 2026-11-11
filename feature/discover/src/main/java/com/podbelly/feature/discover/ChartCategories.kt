package com.podbelly.feature.discover

/**
 * Apple Podcasts' category tree: every top-level category with its sub-categories,
 * each mapping a chip label to an iTunes genre id (any of which can be charted).
 * Taken from Apple's genre service (itunes.apple.com/WebObjects/MZStoreServices.woa/ws/genres?id=26),
 * unchanged since Apple's 2019 category overhaul.
 */
internal val APPLE_PODCAST_CATEGORIES: List<ChartCategory> = listOf(
    ChartCategory(
        "Comedy", 1303,
        listOf(
            ChartCategory("Comedy Interviews", 1496),
            ChartCategory("Improv", 1495),
            ChartCategory("Stand-Up", 1497),
        ),
    ),
    ChartCategory(
        "News", 1489,
        listOf(
            ChartCategory("Business News", 1490),
            ChartCategory("Daily News", 1526),
            ChartCategory("Entertainment News", 1531),
            ChartCategory("News Commentary", 1530),
            ChartCategory("Politics", 1527),
            ChartCategory("Sports News", 1529),
            ChartCategory("Tech News", 1528),
        ),
    ),
    ChartCategory("True Crime", 1488),
    ChartCategory("Technology", 1318),
    ChartCategory(
        "Society & Culture", 1324,
        listOf(
            ChartCategory("Documentary", 1543),
            ChartCategory("Personal Journals", 1302),
            ChartCategory("Philosophy", 1443),
            ChartCategory("Places & Travel", 1320),
            ChartCategory("Relationships", 1544),
        ),
    ),
    ChartCategory(
        "Sport", 1545,
        listOf(
            ChartCategory("Baseball", 1549),
            ChartCategory("Basketball", 1548),
            ChartCategory("Cricket", 1554),
            ChartCategory("Fantasy Sports", 1560),
            ChartCategory("Football", 1547),
            ChartCategory("Golf", 1553),
            ChartCategory("Hockey", 1550),
            ChartCategory("Rugby", 1552),
            ChartCategory("Running", 1551),
            ChartCategory("Soccer", 1546),
            ChartCategory("Swimming", 1558),
            ChartCategory("Tennis", 1556),
            ChartCategory("Volleyball", 1557),
            ChartCategory("Wilderness", 1559),
            ChartCategory("Wrestling", 1555),
        ),
    ),
    ChartCategory(
        "Business", 1321,
        listOf(
            ChartCategory("Careers", 1410),
            ChartCategory("Entrepreneurship", 1493),
            ChartCategory("Investing", 1412),
            ChartCategory("Management", 1491),
            ChartCategory("Marketing", 1492),
            ChartCategory("Non-Profit", 1494),
        ),
    ),
    ChartCategory("History", 1487),
    ChartCategory(
        "Science", 1533,
        listOf(
            ChartCategory("Astronomy", 1538),
            ChartCategory("Chemistry", 1539),
            ChartCategory("Earth Sciences", 1540),
            ChartCategory("Life Sciences", 1541),
            ChartCategory("Mathematics", 1536),
            ChartCategory("Natural Sciences", 1534),
            ChartCategory("Nature", 1537),
            ChartCategory("Physics", 1542),
            ChartCategory("Social Sciences", 1535),
        ),
    ),
    ChartCategory(
        "Health & Fitness", 1512,
        listOf(
            ChartCategory("Alternative Health", 1513),
            ChartCategory("Fitness", 1514),
            ChartCategory("Medicine", 1518),
            ChartCategory("Mental Health", 1517),
            ChartCategory("Nutrition", 1515),
            ChartCategory("Sexuality", 1516),
        ),
    ),
    ChartCategory(
        "Music", 1310,
        listOf(
            ChartCategory("Music Commentary", 1523),
            ChartCategory("Music History", 1524),
            ChartCategory("Music Interviews", 1525),
        ),
    ),
    ChartCategory(
        "TV & Film", 1309,
        listOf(
            ChartCategory("After Shows", 1562),
            ChartCategory("Film History", 1564),
            ChartCategory("Film Interviews", 1565),
            ChartCategory("Film Reviews", 1563),
            ChartCategory("TV Reviews", 1561),
        ),
    ),
    ChartCategory(
        "Education", 1304,
        listOf(
            ChartCategory("Courses", 1501),
            ChartCategory("How To", 1499),
            ChartCategory("Language Learning", 1498),
            ChartCategory("Self-Improvement", 1500),
        ),
    ),
    ChartCategory(
        "Arts", 1301,
        listOf(
            ChartCategory("Books", 1482),
            ChartCategory("Design", 1402),
            ChartCategory("Fashion & Beauty", 1459),
            ChartCategory("Food", 1306),
            ChartCategory("Performing Arts", 1405),
            ChartCategory("Visual Arts", 1406),
        ),
    ),
    ChartCategory(
        "Fiction", 1483,
        listOf(
            ChartCategory("Comedy Fiction", 1486),
            ChartCategory("Drama", 1484),
            ChartCategory("Science Fiction", 1485),
        ),
    ),
    ChartCategory(
        "Kids & Family", 1305,
        listOf(
            ChartCategory("Education for Kids", 1519),
            ChartCategory("Parenting", 1521),
            ChartCategory("Pets & Animals", 1522),
            ChartCategory("Stories for Kids", 1520),
        ),
    ),
    ChartCategory(
        "Leisure", 1502,
        listOf(
            ChartCategory("Animation & Manga", 1510),
            ChartCategory("Automotive", 1503),
            ChartCategory("Aviation", 1504),
            ChartCategory("Crafts", 1506),
            ChartCategory("Games", 1507),
            ChartCategory("Hobbies", 1505),
            ChartCategory("Home & Garden", 1508),
            ChartCategory("Video Games", 1509),
        ),
    ),
    ChartCategory(
        "Religion & Spirituality", 1314,
        listOf(
            ChartCategory("Buddhism", 1438),
            ChartCategory("Christianity", 1439),
            ChartCategory("Hinduism", 1463),
            ChartCategory("Islam", 1440),
            ChartCategory("Judaism", 1441),
            ChartCategory("Religion", 1532),
            ChartCategory("Spirituality", 1444),
        ),
    ),
    ChartCategory("Government", 1511),
)
