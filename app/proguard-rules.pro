# Wallora release shrinking rules (R8).

# Readable crash stack traces in Play Console (upload the mapping file with each release).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson reads/writes these classes by field name via reflection:
#  - network.*: API responses (Wallhaven, Pixabay, Picsum, Supabase)
#  - model.*: Wallpaper/Category; Liked wallpapers are stored as JSON in SharedPreferences,
#    so renaming fields would make existing users lose their Liked list after an update.
-keep class com.skd.wallpaper.network.** { *; }
-keep class com.skd.wallpaper.model.** { *; }

# Gson generic type information (FavoritesStore uses a TypeToken for List<Wallpaper>).
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Retrofit with suspend functions under R8 full mode (default since AGP 8).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
