# Project-specific R8 and ProGuard keep rules

# Keep all domain models
-keep class com.example.project.models.** { *; }

# Keep database helper and repository classes
-keep class com.example.project.data.** { *; }

# Keep utility and business logic classes
-keep class com.example.project.utils.** { *; }

# Keep ViewModels and ViewHolders
-keep class com.example.project.viewmodels.** { *; }
-keepclassmembers class * extends androidx.recyclerview.widget.RecyclerView$ViewHolder {
    public <init>(...);
}

# Keep FileProvider for PDF invoice and prescription sharing
-keep class androidx.core.content.FileProvider { *; }

# Keep Material Design components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Keep SQLiteOpenHelper constructors
-keepclassmembers class * extends android.database.sqlite.SQLiteOpenHelper {
    public <init>(...);
}

# Keep WorkManager worker classes if invoked via reflection
-keep class * extends androidx.work.Worker {
    public <init>(...);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
}
-dontwarn androidx.work.**
