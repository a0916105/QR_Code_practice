# QR Code練習

## **設定和Layout**

1. Open **app/build.gradle** and add [Google ML Kit barcode](https://developers.google.com/ml-kit/vision/barcode-scanning/android) and [Jetpack CameraX](https://developer.android.com/jetpack/androidx/releases/camera) dependencies:

   ```groovy
   // ViewModel and LiveData (Navigation Drawer Activity預設就有)
   implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.4.0'
   implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0'
   
   // Barcode model dependencies
   implementation 'com.google.mlkit:barcode-scanning:17.0.0'
   
   // CameraX dependencies
   def camerax_version = "1.0.2"
   implementation "androidx.camera:camera-camera2:${camerax_version}"
   implementation "androidx.camera:camera-lifecycle:${camerax_version}"
   implementation 'androidx.camera:camera-view:1.0.0-alpha30'
   ```

   - Also we need to set java 1.8 as a targetCompatibility, add the following to the end of the Android block(新版Android Studio預設就是，不用修改):

     ```groovy
     android {
     ,,,,compileOptions {
         sourceCompatibility JavaVersion.VERSION_1_8
         targetCompatibility JavaVersion.VERSION_1_8
     }
     ,,,,
     }
     ```

2. Open your **AndroidManifest.xml** file to add required permissions and google play service meta-data:

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <manifest xmlns:android="http://schemas.android.com/apk/res/android"
       xmlns:tools="http://schemas.android.com/tools"
       package="Project自動建立的package">
   
       <uses-sdk tools:overrideLibrary="
         androidx.camera.camera2, androidx.camera.core,
         androidx.camera.view, androidx.camera.lifecycle" />
   
       <uses-feature android:name="android.hardware.camera" />
   
       <uses-permission android:name="android.permission.CAMERA"/>
       <uses-permission android:name="android.permission.SEND_SMS"/>
   
       <application
           android:allowBackup="true"
           android:icon="@mipmap/ic_launcher"
           android:label="@string/app_name"
           android:roundIcon="@mipmap/ic_launcher_round"
           android:supportsRtl="true"
           android:theme="@style/Theme.Project名稱">
           <activity
               android:name=".MainActivity"
               android:exported="true"
               android:label="@string/app_name"
               android:theme="@style/Theme.Project名稱.NoActionBar">
               <!-- Add google service in your application -->
               <meta-data
                   android:name="com.google.android.gms.version"
                   android:value="@integer/google_play_services_version"/>
   ,,,
   ```

2. Add PreviewView to the main activity layout (**activity_main.xml**).用來顯示CameraX預覽的相機畫面。



### 參考資料：

- [https://badgameshow.com/fly/android-qr-code掃描器-各種條碼都能掃/fly/android/](https://badgameshow.com/fly/android-qr-code%E6%8E%83%E6%8F%8F%E5%99%A8-%E5%90%84%E7%A8%AE%E6%A2%9D%E7%A2%BC%E9%83%BD%E8%83%BD%E6%8E%83/fly/android/)
- [https://xken831.pixnet.net/blog/post/532799914-[android]-qr-code-掃描器](https://xken831.pixnet.net/blog/post/532799914-%5Bandroid%5D-qr-code-%E6%8E%83%E6%8F%8F%E5%99%A8)
- [http://www.quickmark.com.tw/cht/qrcode-datamatrix-generator/default.asp](http://www.quickmark.com.tw/cht/qrcode-datamatrix-generator/default.asp)
- [https://proandroiddev.com/building-barcode-qr-code-scanner-for-android-using-google-ml-kit-and-camerax-220b2852589e](https://proandroiddev.com/building-barcode-qr-code-scanner-for-android-using-google-ml-kit-and-camerax-220b2852589e)

