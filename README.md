# 📰 Neural News - Automated Instagram News Poster

An automated backend service that fetches news from **MediaStack**, summarizes it using **Gemini**, retrieves relevant images from **Pexels**, overlays text on the images, and posts them to **Instagram**. The system is controlled via a **Telegram Bot** for easy management.

---

## 🚀 Features

✅ Fetches real-time news from **MediaStack API**  
✅ Summarizes news using **Google Gemini API**  
✅ Retrieves **free images** from **Pexels API**  
✅ Overlays news headlines on images using **Java Graphics2D (G2D)**  
✅ Uploads images to **AWS S3** for public access  
✅ Posts images to **Instagram** via **Meta Graph API**  
✅ Telegram Bot integration for bot status, manual posting, and logs  

---

## 🛠️ Tech Stack

- **Backend:** Java (Spring Boot)  
- **News API:** MediaStack  
- **Summarization:** Google Gemini API  
- **Image Source:** Pexels API  
- **Image Processing:** Java Graphics2D (G2D)  
- **Storage:** AWS S3  
- **Automation & Scheduling:** Spring Scheduler  
- **Bot Control:** Telegram Bot (python-telegram-bot)  
- **Deployment:** Docker (optional)  

---

## 📌 Installation & Setup

### 1️⃣ Clone the Repository
```bash
git clone https://github.com/your-username/neural-news.git
cd neural-news
```

### 2️⃣ Set Up Environment Variables
Create a `.env` file and add your credentials:
```ini
# AWS S3
AWS_ACCESS_KEY=your_aws_access_key
AWS_SECRET_KEY=your_aws_secret_key
AWS_REGION=your_region
AWS_S3_BUCKET=your_bucket_name

# MediaStack API
MEDIASTACK_API_KEY=your_mediastack_api_key

# Google Gemini API
GEMINI_API_KEY=your_gemini_api_key

# Pexels API
PEXELS_API_KEY=your_pexels_api_key

# Instagram API
INSTAGRAM_ACCESS_TOKEN=your_instagram_access_token
INSTAGRAM_BUSINESS_ID=your_instagram_business_id

# Telegram Bot
TELEGRAM_BOT_TOKEN=your_telegram_bot_token
TELEGRAM_CHAT_ID=your_telegram_chat_id
```

### 3️⃣ Install Dependencies
```bash
mvn clean install
```

### 4️⃣ Run the Application
```bash
mvn spring-boot:run
```

---

## 📜 API Endpoints

| Method | Endpoint | Description |
|--------|---------|-------------|
| `GET` | `/news/fetch` | Fetch latest news from MediaStack |
| `POST` | `/news/process` | Process and generate images |
| `POST` | `/news/post` | Post processed news to Instagram |
| `GET` | `/bot/status` | Check bot status |
| `POST` | `/bot/start` | Start bot |
| `POST` | `/bot/stop` | Stop bot |

---

## 🤖 Telegram Bot Commands

| Command | Description |
|---------|------------|
| `/status` | Check if the bot is running |
| `/start` | Start the bot manually |
| `/stop` | Stop the bot manually |
| `/post` | Post the latest processed news to Instagram |
| `/logs` | Retrieve recent logs |


---
