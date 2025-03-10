# 📰 Neural News - Automated Instagram News Poster

An automated backend service that fetches news from **MediaStack**, summarizes it using **Gemini**, retrieves relevant images from **Pexels**, overlays text on the images, and posts them to **Instagram**. The system is controlled via a **Telegram Bot** for easy management.

---

## 🚀 Features

✅ Fetches real-time news from **MediaStack API**  
✅ Summarizes news using **Google Gemini API**  
✅ Retrieves **free images** from **Pexels API**  
✅ Overlays news headlines on images using **Java Graphics2D (G2D)**  
✅ Uploads images to **AWS S3** for public access  
✅ Posts images to **Instagram** via **Instagram4j**  
✅ Telegram Bot integration for bot status, manual posting, and logs  

---

## 🛠️ Tech Stack

- **Backend:** Java (Spring Boot)  
- **Database:** PostgreSQL
- **News API:** MediaStack  
- **Summarization:** Google Gemini API  
- **Image Source:** Pexels API  
- **Image Processing:** Java Graphics2D (G2D)  
- **Storage:** AWS S3  
- **Automation & Scheduling:** Spring Scheduler  
- **Bot Control:** Telegram Bot API  
- **Deployment:** Docker  

---

## 📌 Installation & Setup

### 1️⃣ Clone the Repository
```bash
git clone https://github.com/your-username/news-bot.git
cd news-bot
```

### 2️⃣ Set Up Environment Variables
Create a `.env` file and add your credentials:
```ini
# Database
DB_URL=jdbc:postgresql://localhost:5432/tweetbot
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# AWS Configuration
AWS_ACCESS_KEY=your_aws_access_key
AWS_SECRET_KEY=your_aws_secret_key
AWS_REGION=eu-north-1
AWS_S3_BUCKET=your_bucket_name

# API Keys
PEXELS_API_KEY=your_pexels_api_key
INSTAGRAM_USERID=your_instagram_userid
INSTAGRAM_ACCESS_TOKEN=your_instagram_access_token
MEDIASTACK_API_KEY=your_mediastack_api_key
GEMINI_API_KEY=your_gemini_api_key
HUGGINGFACE_API_KEY=your_huggingface_api_key
TELEGRAM_BOT_USERNAME=your_bot_username
TELEGRAM_BOT_TOKEN=your_bot_token

# Optional Configuration
APP_SCHEDULER_FETCH_NEWS_RATE=300000
APP_SCHEDULER_POST_RATE=600000

# Instagram Configuration
INSTAGRAM_USERNAME=your_instagram_username
INSTAGRAM_PASSWORD=your_instagram_password
```

### 3️⃣ Database Setup
```bash
# Start PostgreSQL using Docker
docker-compose up -d
```

### 4️⃣ Install Dependencies
```bash
./mvnw clean install
```

### 5️⃣ Run the Application
```bash
./mvnw spring-boot:run
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

## 🔧 Configuration

- News fetch interval: 5 minutes (300000ms)
- Post interval: 10 minutes (600000ms)
- Supported image formats: JPEG, PNG
- Database: PostgreSQL 
- Java version: 17

---
