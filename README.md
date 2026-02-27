# 🔥 Real-Time Gas Cylinder Monitoring System

## 📌 Overview

The **Real-Time Gas Cylinder Monitoring System** is an IoT-based solution designed to detect LPG gas leakage and monitor cylinder gas levels in real-time. The system integrates hardware sensors with a cloud database and Android applications to provide live monitoring and alert notifications.

This project aims to improve household safety by detecting gas leakage early and enabling timely refill booking.

---

## 🎯 Objectives

- Detect LPG gas leakage using a gas sensor
- Monitor gas concentration levels in real-time
- Trigger alerts when gas concentration exceeds safe limits
- Enable remote monitoring via Android application
- Provide refill booking functionality through a separate app
- Send SMS notifications for low gas levels and leakage detection

---

## 🛠️ Technologies Used

### Hardware
- ESP32
- MQ-2 Gas Sensor
- Buzzer

### Software & Tools
- Android (Java)
- Firebase Realtime Database
- SQLite
- VS Code
- Git

---

## ⚙️ System Architecture

1. The **MQ-2 gas sensor** detects gas concentration levels.
2. The **ESP32** processes sensor data.
3. If gas concentration exceeds **200 PPM**, a buzzer alert is triggered.
4. Sensor data is transmitted in real-time to **Firebase**.
5. The Android app fetches data from Firebase and displays live gas levels.
6. SMS notifications are sent during leakage detection or low gas conditions.
7. A separate Android application allows users to manage refill requests using SQLite.

---

## 📱 Android Applications

### 1️⃣ Gas Monitoring App
- Displays real-time gas levels
- Shows alert notifications for leakage detection
- Connected to Firebase for live data updates

### 2️⃣ Gas Booking App
- Manages refill booking requests
- Stores booking data using SQLite
- Provides a structured interface for managing cylinder refill status

---

## 🚨 Key Features

- Real-time gas monitoring
- Leakage detection above 200 PPM
- Buzzer alert system
- Firebase cloud integration
- SMS notification system
- Separate booking management application

---

## 🧠 Learning Outcomes

- Practical implementation of IoT systems
- Integration of hardware with cloud platforms
- Android application development using Java
- Working with Firebase Realtime Database
- Database handling using SQLite
- Real-time data transmission and alert mechanisms

---

## 📌 Future Improvements

- Add graphical visualization of gas usage trends
- Implement threshold customization in app
- Add user authentication for multi-user access
- Improve sensor calibration to reduce false positives
