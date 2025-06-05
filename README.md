# TrafficEye
TrafficEye is a mobile application for static image traffic sign recognition, but with an attempt to implement real-time sign recognition. The system consists of an Android app (built with Kotlin) and a backend server (built with FastAPI in Python) which hosts a YOLOv8-based detection model.

[//]: # (![LOGO]&#40;documentation/uploads/trafficeye_logo.png&#41;)

<p align="left">
  <img src="documentation/uploads/trafficeye_logo.png" width="300"/>
</p>

# Features

[//]: # (- Real-time traffic sign detection using device camera &#40;via TFLite model&#41;)
- Static image traffic sign detection via backend (FastAPI server)
- Bounding boxes and confidence levels displayed on camera preview

[//]: # (- Offline on-device detection and optional server detection)

# Technologies Used
<details>
<summary>🔍 Click for see more information about technologies</summary>

#### Mobile App:
- Kotlin
- Android Studio
- CameraX API
- TensorFlow Lite

#### Backend Server:
- Python 3.10+
- FastAPI
- Uvicorn
- OpenCV
- Ultralytics YOLOv8 (custom trained model)
</details>


# Installation
Clone repository or download it by .zip
```
# Select directory at the start, then clone repo
git clone https://github.com/Cavarano/Projekt-zespolowy-Visimind
```
### Android App

Open trafficeye2 folder in Android Studio.

The next step is to synchronize the project with Gradle so that all the necessary dependencies are installed

<p align="left">
  <img src="documentation/uploads/file1.png" width="450"/>
</p>
Before enabling the application itself, you need to start the FastAPI server.

Build and run the app on a physical device (CameraX requires a real device).

### Backend Server (Local Development)

Open traffic_eye_serwer folder in any python code redactor.

1. Create and activate virtual environment:
```
python -m venv .venv
.venv\Scripts\activate # source .venv/bin/activate on Linux, MacOS
```
2. Install dependencies:
```
pip install -r requirements.txt
```
3. Run the server:
```
cd traffic_signs_detection
uvicorn main:app --reload --host 0.0.0.0 --port 8000 
```

> [!NOTE] Ensure the Android device and server are on the same local network. In the app, use the local IP of your machine in API requests. 
><details>
><summary>🔍 How to ensure</summary>
>If you use a USB cable and distribute the Internet through the cable, you should:
>
>1. Enter the command in Powershell or another terminal
>```
>ipconfig
>```
>2. Find there an Ethernet adapter Ethernet: or similar to this adapter and save its IPv4 Address
><p align="left">
>  <img src="documentation/uploads/file2.png" width="500"/>
></p>
>
>3. And add this IP address to two files
>   1. app/res/xml/network_security_config.xml -> In the network-security-config block
>   2. app/kotlin+java/com.example.trafficeye2/MainActivity -> In the uploadImageToServer function
>   \
>   val request = Request.Builder()
>                   .url(“http://192.168.246.178:8000/detection/detect-signs/”)
>
>
>After all these steps, you will most likely be in the same local network as the Phone and the Device running the local server
></details>

# Research & ML
During the project, a lot of effort was spent on data pre-processing. 
We received the data in image format without labeling, so we had to determine 
which signs our model would recognize and which would not, and manually label 
all the photos. We used such technology as labelme. The company provided us 
with 5244 photos, but after labeling and discarding the photos with no road 
signs at all or hardly visible, we were left with 2600+- photos. Our team 
immediately realized that for such a large project, we still needed to expand 
the dataset, which we did. After two additional dataset expansions, 6729 photos
were provided for our model, of which 75% of the data was used for training,
15% for validation, and 10% for testing.  This was very important for 
the project, as it was the first time we worked with model training and 
wanted to do the best we could and at the same time understand how the 
training works.

## Achievements

|               Training Metrics                | Labels Histogram |
|:---------------------------------------------:|:----------------:|
| ![results](documentation/uploads/results.png) | ![results](documentation/uploads/labels.jpg) |

### Training and Validation Metrics
This multi-plot figure shows the training and validation progress over epochs:
- Box loss / Class loss / DFL loss: All loss functions decrease — indicating learning progress.
- Precision / Recall / mAP@50 / mAP@50-95: All metrics increase — suggesting the model generalizes well to validation data.

### Dataset Label Distribution and Placement
This figure provides a detailed analysis of the annotated training dataset:

- Top-left: Histogram of instances per class. Useful for detecting class imbalance.
- Top-right: Bounding box anchor visualization — shows how anchor boxes are sized and placed.
- Bottom-left: Object center heatmap (x/y coordinates). This highlights where in the image objects usually appear.
- Bottom-right: Width/height distribution — shows that most objects are relatively small.


|                       Correlogram                        | Confusion Matrix |
|:--------------------------------------------------------:|:----------------:|
| ![results](documentation/uploads/labels_correlogram.jpg) | ![results](documentation/uploads/confusion_matrix_normalized.png) |

### Class Co-occurrence (Correlogram)
This correlogram represents how frequently different traffic sign classes appear together in the same image.
- Each square represents a pair of classes.
- Brighter colors mean higher co-occurrence.

### Confusion Matrix (Normalized)

This matrix illustrates how often predictions match the actual classes:

- Diagonal cells represent correct predictions.
- Off-diagonal cells show confusion between classes.




# Traffic Eye Team

## GUI & App:

- [Maciej Kała](https://github.com/Cavarano)
- [Alina Upirowa](https://github.com/alinaupyrova)


## Backend

### Connecting the model to the camera in real time, Tester
- [Mateusz Kruszewsi](https://github.com/kru3zec)

### Database, FastAPI server
- [Eryk Żabiński]()

## ML, fixing errors, combining logic
- [Ruslan Zhukotynskyi](https://github.com/ruslanzhuk)