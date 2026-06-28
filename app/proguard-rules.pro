# Add project specific ProGuard rules here.
-keep class com.lifelog.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.itextpdf.**
