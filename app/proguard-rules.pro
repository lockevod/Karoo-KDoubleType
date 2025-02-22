# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Mantener la extensión Karoo
# Mantener la extensión Karoo
-keep class io.hammerhead.karooext.** { *; }


# Mantener las clases de tu aplicación
-keep class com.enderthor.kCustomField.** { *; }
-keep class com.enderthor.kCustomField.datatype.** { *; }
-keep class com.enderthor.kCustomField.extensions.** { *; }
-keep class com.enderthor.kCustomField.screens.** { *; }
-keep class com.enderthor.kCustomField.theme.** { *; }

# Reglas para Timber
-dontwarn org.jetbrains.annotations.**
-keep class timber.log.** { *; }

# Reglas generales para Android
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Mantener constructores necesarios para JSON/Serialización si los usas
-keepclassmembers class * {
    public <init>();
}

# Mantener enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Si usas Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Mantener interfaces de callbacks y listeners
-keepclassmembers class * {
    void onCommand(**);
    void onStateChange(**);
    void onConnectionStateChange(**);
}

# Reglas específicas para servicios en segundo plano
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver

# Si usas Composables
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.** *;
}

# Glance
-keep class androidx.glance.** { *; }
-keep class androidx.glance.appwidget.** { *; }
-keep class androidx.glance.state.** { *; }
-keep class androidx.glance.wear.** { *; }
-keep class androidx.glance.action.** { *; }
-keep class androidx.glance.layout.** { *; }
-keep class androidx.glance.text.** { *; }
-keep class androidx.glance.unit.** { *; }
-keep class androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class androidx.glance.appwidget.GlanceAppWidget { *; }


# Mantener los composables utilizados en Glance
-keep @androidx.compose.runtime.Composable class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Mantener las clases que implementan GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidget {
    <init>(...);
}

# Mantener las clases que implementan GlanceAppWidgetReceiver
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver {
    <init>(...);
}

# Mantener las clases específicas de tu aplicación que usas en Glance
-keep class com.enderthor.kCustomField.** {
    <init>(...);
}

# Mantener las clases de datos utilizadas en Glance
-keep class com.enderthor.kCustomField.model.** {
    <init>(...);
}

# Mantener los métodos de los composables
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Mantener los constructores de las clases utilizadas en los composables
-keepclassmembers class * {
    <init>(...);
}