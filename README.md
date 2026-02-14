🅿️ bPark - Automated Parking Management System

bPark היא מערכת ניהול חניונים חכמה בפורמט Client-Server, שנועדה לייעל את תהליכי החניה עבור לקוחות (מזדמנים ומנויים) ועבור עובדי החניון. המערכת מטפלת בזמן אמת בהזמנות, ניהול שטחי חניה, הפקת דוחות וגביית תשלומים.
🚀 פיצ'רים מרכזיים (Key Features)

    ניהול לקוחות ומנויים: מערכת רישום וניהול מנויים (חודשיים/שנתיים) ולקוחות מזדמנים.

    הזמנת חניה מראש: ממשק ידידותי לבחירת חניון, תאריך ושעה, כולל אימות זמינות בזמן אמת.

    ניהול חניונים אוטומטי: אלגוריתם לניהול סטטוס חניות (פנוי/תפוס/שמור) ועדכון אוטומטי של תפוסת החניון.

    ארכיטקטורת שרת-לקוח: תקשורת מבוססת TCP/IP תוך שימוש בתשתית OCSF (Open Client Server Framework).

    ריבוי משימות (Multi-threading): השרת מסוגל לטפל בעשרות לקוחות במקביל ללא תקיעה של ממשק המשתמש (GUI).

    מחולל דוחות: הפקת דוחות ניהוליים (תפוסה, הכנסות, תלונות) עבור מנהלי חניונים.

🛠 Tech Stack

    Language: Java 17+

    UI Framework: JavaFX (עם Scene Builder)

    Networking: OCSF Framework (TCP/IP)

    Database: MySQL (עם JDBC Connector)

    Project Management: Maven

🏗 ארכיטקטורה ודפוסי עיצוב (Design Patterns)

המערכת נבנתה תחת עקרונות הנדסת תוכנה מתקדמים כדי להבטיח תחזוקתיות וגמישות:

    ECB Pattern (Entity-Control-Boundary): הפרדה מוחלטת בין שכבת התצוגה (Boundary), הלוגיקה העסקית (Control) ומבנה הנתונים (Entity).

    Singleton: לניהול חיבור יחיד למסד הנתונים ולשרת.

    Client-Server: הפרדה מלאה בין ה-Client (ממשק המשתמש) לבין ה-Server (עיבוד הנתונים והגישה ל-DB).

    Observer Pattern: לעדכון רכיבי GUI בעת קבלת הודעות חדשות מהשרת.

📂 מבנה הפרויקט (Project Structure)
Plaintext

bPark-system/
│
├── bPark-Server/              # לוגיקת צד שרת וניהול DB
│   ├── src/main/java/server/
│   │   ├── EchoServer.java    # ניהול תקשורת נכנסת
│   │   └── DBController.java  # שאילתות MySQL
│   └── resources/             # סכמות SQL
│
├── bPark-Client/              # ממשק משתמש וניהול לקוח
│   ├── src/main/java/client/
│   │   ├── ClientUI.java      # נקודת כניסה
│   │   └── ChatClient.java    # תקשורת מול השרת
│   ├── src/main/java/gui/     # JavaFX Controllers
│   └── src/main/resources/    # קבצי FXML (עיצוב הממשק)
│
└── Common/                    # ישויות משותפות (Entities)
    └── src/main/java/logic/   # מחלקות Order, Car, Parking, User

⚙️ התקנה והרצה

    מסד נתונים:

        ייבא את קובץ ה-SQL המצורף בתיקיית resources לתוך שרת ה-MySQL שלך.

        עדכן את פרטי הגישה (User/Password) ב-DBController.java.

    הרצת השרת:

        הפעל את ה-Jar של השרת או הרץ את EchoServer.java.

        הזן את הפורט המבוקש (למשל 5555).

    הרצת הלקוח:

        הפעל את ה-Jar של הלקוח.

        הזן את ה-IP של השרת (localhost להרצה מקומית) והפורט.

👤 Contact

Fuad Abbas

    Email: Foaad.Abbas@e.braude.ac.il

    LinkedIn: linkedin.com/in/fuad-abbas

    GitHub: github.com/FoaadAbbas

פרויקט זה פותח במסגרת הלימודים לתואר ראשון בהנדסת תוכנה במכללה האקדמית להנדסה בראודה.
