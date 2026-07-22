# ProGuard rules
-keep class com.nfctools.app.** { *; }
-keep class android.nfc.** { *; }
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
