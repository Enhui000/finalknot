# Knot — Android Client  
## Checkpoint Progress Report

## 1. Weekly Roles

| Week | Coordinator | Observer |
|------|-------------|-----------|
| **Week 1 (Last week)** | Shenwei Zhang | Xiangyi Peng |
| **Week 2 (This week)** | Liye Fu | Enhui Zhao |

---

## 2. Work Summary — Previous Checkpoint

### Implemented Features

- **Map and Comment UI** — *Xiangyi Peng*  
  - Basic map interface  
  - Comment display & creation prototype  

- **Chat and Settings UI** — *Liye Fu*  
  - Chat page basic structure  
  - Settings page components  

- **Backend Core Implementation** — *Shenwei Zhang*  
  - User/session backend setup  
  - Initial friend & profile backend endpoints  
  - Supported basic data models for the app

- **Login & Signup UI** — *Enhui Zhao*  
  - Completed initial login and register pages  
  - Connected navigation flow  

### Deliverables
- Completed PPT summarizing UI flow, backend architecture, and feature overview.

---

## 3. Work Summary — Current Checkpoint

### Implemented Features
- **Chat System Enhancements** — *Liye Fu*  
  - Updated chat interface  
  - Prepared structure for backend messaging integration  

- **Login/Register Feature Improvements** — *Xiangyi Peng*  
  - Improved login/register UI UX  
  - Added loading states and future error handling placeholders  

- **User Profile + Map Feature (Backend)** — *Shenwei Zhang*  
  - User profile API implementation  
  - Map backend logic and data handling  
  - Provided updated models for UI integration

- **Friend System UI** — *Enhui Zhao*  
  - Friend request list (incoming & outgoing)  
  - Friend list display  
  - Implemented FriendUiState & related data models  


### Codebase Maintenance
- Merged branch `Xiangyi_c2` → `main`  
- Merged branch `Shenwei_user_profile` → `main`  
  - All conflicts resolved by **keeping the version from `Shenwei_user_profile`**  
- Added and updated README on `main`

---

## 4. Next Steps
- Integrate frontend with backend APIs (Retrofit)  
- Add real-time messaging and friend updates using WebSocket  
- Implement avatar picker & full profile edit screen  
- Integrate map feature into main navigation flow  
- Add ViewModel unit testing  
