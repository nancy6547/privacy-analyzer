# 🛡️ Privacy Analyzer — Powered by ML

An advanced Android application that uses **Machine Learning** to scan and categorize installed apps based on their privacy risk level. Developed with a focus on system security and transparency, this tool helps users identify potentially intrusive applications.

---

## 🚀 Key Features

*   **ML-Powered Analysis**: Uses a custom TensorFlow Lite model to evaluate app risk levels.
*   **Foreground Monitoring**: Runs as a persistent background service to keep your device protected.
*   **Interactive Dashboard**: A modern Material 3 UI built with Jetpack Compose.
*   **Risk Categorization**: Automatically groups apps into **High**, **Medium**, and **Safe** categories.
*   **Detailed Reasons**: Explains *why* an app was flagged (e.g., access to Camera, Location, or Microphone).
*   **Real-time Updates**: Dashboard updates instantly as the background engine completes its scan.

---

## 🧠 The ML Model

The core of this project is a **Deep Learning Classifier** trained to detect privacy threats based on application metadata and permission requests.

### Model Architecture
*   **Type**: Sequential Neural Network (Keras/TensorFlow)
*   **Input**: 8 Features (Numerical & Categorical)
*   **Output**: 3 Classes (0: Safe, 1: Medium Risk, 2: High Risk)
*   **Deployment**: Optimized for mobile using **TensorFlow Lite (TFLite)**.

### Input Features
The model evaluates apps based on:
1.  **Total Permission Count**: How many permissions does the app request?
2.  **Location Access**: Does it track your movement?
3.  **Camera Access**: Can it take photos/videos?
4.  **Microphone Access**: Can it record audio?
5.  **Contacts Access**: Can it read your phonebook?
6.  **SMS Access**: Can it read your private messages?
7.  **Background Activity**: Does it run without your knowledge?
8.  **Storage Access**: Can it read/write your files?

---

## 🛠️ Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Machine Learning**: TensorFlow Lite
*   **Architecture**: Android Services, Broadcast Receivers, and Modern State Management.
*   **Training Script**: Python (Scikit-learn, Pandas, TensorFlow).

---

## 📸 Dashboard Preview

| **Summary View** | **Detailed Risk List** |
|:---:|:---:|
| Visual cards showing total counts | List of app names and their access reasons |

---

## 📥 Installation

1.  Clone this repository.
2.  Open in **Android Studio (Ladybug or later)**.
3.  Ensure `privacy_model.tflite` is present in the `app/src/main/assets/` folder.
4.  Sync Gradle and Run on a physical device or emulator.

---

## 🎓 Author
Developed by **Nancy**. Built with a focus on enhancing mobile privacy and security through automated machine learning analysis.

---
*Disclaimer: This tool provides risk assessments based on requested permissions. It does not monitor actual network traffic or data exfiltration.*
<img width="503" height="922" alt="Screenshot 2026-06-28 155701" src="https://github.com/user-attachments/assets/348b4704-6ff3-4755-8655-48894db9f43e" />
<img width="425" height="867" alt="Screenshot 2026-06-28 155650" src="https://github.com/user-attachments/assets/4586e0a8-6e8e-44db-9c2d-de134e1ad199" />
<img width="529" height="875" alt="Screenshot 2026-06-28 155631" src="https://github.com/user-attachments/assets/c322f4f7-32c1-4a48-a61d-abf2c12c31b3" />
<img width="568" height="924" alt="Screenshot 2026-06-28 155518" src="https://github.com/user-attachments/assets/43a011f7-e7fe-4e38-aa97-57e63a3dcb3c" />
