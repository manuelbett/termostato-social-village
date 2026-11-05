# Add project specific ProGuard rules here.

# Mantieni le classi JavaScript interface (WebViewApiClient)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Mantieni i modelli dati (usati da JSONObject)
-keep class it.termostato.app.data.** { *; }

# Corretto JSON
-keepattributes *Annotation*
-keepattributes Signature

# Non rimuovere i nomi delle classi in stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
