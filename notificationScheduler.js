Java.perform(function() {
    console.log("[*] Hooking NotificationScheduler.getTime()...");

    var NotificationScheduler = Java.use("com.example.bdwisher.NotificationScheduler");

    // Check if the method exists before hooking
    if (NotificationScheduler.getTime) {
        NotificationScheduler.getTime.implementation = function() {
            console.log("[*] Hooked getTime()!");
            var fakeTime = 235 * 60 * 1000;  // Example fake timestamp
            console.log("[*] Returning modified time: " + fakeTime);
            return fakeTime;
        };
    } else {
        console.log("[!] getTime() method not found. Check if it's static or has an obfuscated name.");
    }
});
