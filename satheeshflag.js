Java.perform(function() {
    console.log("[*] Attempting to start UnusedActivity...");

    try {
        // Obtain the current activity using the ActivityManager
        var ActivityManager = Java.use("android.app.ActivityManager");
        var currentApplication = Java.use("android.app.ActivityThread").currentApplication();
        var context = currentApplication.getApplicationContext();

        // Creating an Intent to start the UnusedActivity
        var Intent = Java.use("android.content.Intent");
        var intent = Intent.$new();
        intent.setClassName("com.example.bdwisher", "com.example.bdwisher.UnusedActivity");
        intent.setFlags(0x10000000);  // FLAG_ACTIVITY_NEW_TASK

        // Start the exported activity
        context.startActivity(intent);
        console.log("[+] UnusedActivity triggered successfully!");
    } catch (e) {
        console.error("[-] Error triggering activity: " + e);
    }
});
