# DoubtBridge — Complete Setup Guide

## 🔥 Firebase Setup (Required before running)

### Step 1: Create Firebase Project
1. Go to https://console.firebase.google.com
2. Click **Add project** → name it "DoubtBridge"
3. Disable Google Analytics (optional) → **Create project**

### Step 2: Add Android App
1. In Firebase Console → **Project settings** (⚙️) → **Your apps** → **Add app** → Android
2. Package name: `com.example.doubtbridge`
3. App nickname: DoubtBridge
4. Click **Register app**
5. **Download `google-services.json`**
6. Place it in: `app/google-services.json`

### Step 3: Enable Authentication
1. Firebase Console → **Authentication** → **Get started**
2. Sign-in method → **Email/Password** → Enable → **Save**

### Step 4: Create Firestore Database
1. Firebase Console → **Firestore Database** → **Create database**
2. Choose **Start in test mode** → Select region → **Enable**

### Step 5: Set Firestore Security Rules (Recommended)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow read: if request.auth != null;
    }
    match /doubts/{doubtId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null;
    }
    match /notes/{noteId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    match /bookmarks/{bookmarkId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Step 6: Create Firestore Indexes
Go to **Firestore → Indexes → Composite** and add:

| Collection | Fields | Order |
|------------|--------|-------|
| doubts | timestamp | Descending |
| doubts | status + timestamp | Descending |
| bookmarks | userId + timestamp | Descending |
| notes | uploadedBy + timestamp | Descending |
| notes | subject (any) | |

---

## 📁 Project Structure

```
app/src/main/
├── java/com/example/doubtbridge/
│   ├── SplashActivity.java          ← Auto-login check
│   ├── LoginActivity.java           ← Firebase Auth login
│   ├── SignupActivity.java          ← Student/Teacher registration
│   ├── DashboardActivity.java       ← Student dashboard (tabs, search, filter)
│   ├── TeacherDashboardActivity.java ← Teacher panel (stats, tabs)
│   ├── AskDoubtActivity.java        ← Post doubt + auto-matching
│   ├── DoubtDetailActivity.java     ← View doubt + answer + bookmark
│   ├── TeacherAnswerActivity.java   ← Teacher answers + mark important
│   ├── BookmarkActivity.java        ← Student saved doubts
│   ├── UploadNotesActivity.java     ← Teacher uploads notes (keyword indexed)
│   ├── AnalyticsActivity.java       ← Teacher analytics dashboard
│   ├── adapters/
│   │   ├── DoubtAdapter.java        ← RecyclerView adapter (student + teacher mode)
│   │   └── NoteAdapter.java         ← Notes list adapter
│   ├── models/
│   │   ├── Doubt.java              ← Full Firestore model
│   │   └── Note.java               ← Notes model with keywords
│   └── utils/
│       ├── KeywordMatcher.java      ← Intelligent keyword matching engine
│       └── TimeUtils.java          ← Relative time helper
└── res/
    ├── layout/                      ← All XML layouts
    ├── values/colors.xml            ← Material3 color palette
    ├── values/strings.xml
    └── values/themes.xml
```

---

## 🗄️ Firestore Collections

```
/users/{uid}
  name, email, role ("student"|"teacher"), uid, createdAt

/doubts/{id}
  title, description, userName, subject, status ("pending"|"answered"),
  answer, answeredBy, isImportant, frequency, timestamp, answeredAt

/notes/{id}
  title, content, subject, uploadedBy, keywords[], timestamp

/bookmarks/{id}
  userId, doubtId, title, description, userName, subject, timestamp
```

---

## ✅ Features Implemented

### Student
- [x] Sign up / Login
- [x] Dashboard with tabs (All, Answered, Pending, ⭐ Important)
- [x] Search doubts by title/description
- [x] Filter by subject
- [x] Ask a doubt (text)
- [x] Instant auto-answer via keyword matching
- [x] View doubt detail + answer
- [x] Bookmark any doubt
- [x] Bookmarks page

### Teacher
- [x] Separate login flow → Teacher Dashboard
- [x] Stats card (Pending / Answered / Total)
- [x] Tabs (All / Pending / Answered)
- [x] Click any doubt → Answer it
- [x] Mark doubt as ⭐ Important
- [x] Upload notes (auto keyword-indexed)
- [x] Notes re-process pending doubts on upload
- [x] Analytics dashboard

### System
- [x] Keyword extraction (stop-word filtered)
- [x] Auto-matching with configurable threshold (30%)
- [x] Partial keyword matching
- [x] Re-match pending doubts when new notes uploaded
- [x] Role-based navigation
- [x] Auto-login if session exists

---

## 🆕 v2.0 — New Features Added

### ✅ Done in this update:
1. **Simplified Backend** — Removed Firebase Storage dependency to streamline the architecture. The system now focuses on high-precision text extraction and RAG-based AI answering stored directly in Firestore.
2. **AI-Powered RAG Answering** — Integrated Google Gemini 1.5 Flash. The system extracts text from uploaded PDF/PPTX/DOCX files and uses it as context to generate precise academic answers.
3. **Notifications** — When a teacher answers a doubt (manually or via AI re-match), students get a notification.
4. **Department & Class Categories** — Filterable categories for both doubts and notes.
5. **Profile Module** — Comprehensive profile management with stats.

### 📊 Firestore Indexes Required:
Add composite indexes in Firebase Console → Firestore → Indexes:
- `notifications` collection: `userId ASC, isRead ASC, timestamp DESC`
- `notifications` collection: `userId ASC, timestamp DESC`
- `doubts` collection: `status ASC, subject ASC`
- `bookmarks` collection: `userId ASC, timestamp DESC`
- `notes` collection: `uploadedBy ASC, timestamp DESC`
- `notes` collection: `subject ASC`

---
*Note: Firebase Storage is no longer used. Uploaded files are processed locally for text extraction and indexed in Firestore.*
