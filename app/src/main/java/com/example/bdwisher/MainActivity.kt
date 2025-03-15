package com.example.bdwisher

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.Serializable
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var birthdayAdapter: BirthdayAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: FloatingActionButton

    private val REQUEST_PERMISSIONS = 1001

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()

        dbHelper = DatabaseHelper(this)
        recyclerView = findViewById(R.id.recyclerView)
        addButton = findViewById(R.id.addButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        loadBirthdays()

        addButton.setOnClickListener {
            startActivity(Intent(this, AddBirthdayActivity::class.java))
        }



        attachSwipeToDelete()
    }

    override fun onResume() {
        super.onResume()
        // Refresh birthdays list when returning to this activity
        loadBirthdays()
    }

    private fun loadBirthdays() {
        val birthdays = dbHelper.getAllBirthdays().toMutableList()
        birthdayAdapter = BirthdayAdapter(birthdays) { birthday ->
            val intent = Intent(this, EditBirthdayActivity::class.java)
            intent.putExtra("BIRTHDAY_ID", birthday.id)
            startActivity(intent)
        }
        recyclerView.adapter = birthdayAdapter

        // Schedule all birthday reminders
        scheduleBirthdayReminders()

        // Start service for each birthday
        for (birthday in birthdays) {
            Log.d("Info PASSED", "${birthday.date} ${birthday.id} ${birthday.name}")
        }
    }

    private fun scheduleBirthdayReminders() {
        val birthdays = dbHelper.getAllBirthdays()
        for (birthday in birthdays) {
            NotificationScheduler.scheduleBirthdayNotification(this, birthday)
        }
    }

    private fun attachSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val swipeCount = mutableMapOf<Int, Int>()

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val paint = Paint().apply { color = Color.RED }
                    val icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)
                    val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2

                    if (dX < 0) { // Swiping left
                        c.drawRect(
                            itemView.right.toFloat() + dX, itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                        )

                        // Position icon correctly
                        val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                        icon.setBounds(iconLeft, iconTop, itemView.right - iconMargin, iconTop + icon.intrinsicHeight)
                        icon.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }


            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                swipeCount[position] = swipeCount.getOrDefault(position, 0) + 1

                if (swipeCount[position] == 2) {
                    // Show confirmation dialog before deleting
                    showDeleteConfirmation(position)
                    swipeCount[position] = 0 // Reset count after confirmation
                } else {
                    // Reset item back if not swiped twice
                    birthdayAdapter.notifyItemChanged(position)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun showDeleteConfirmation(position: Int) {
        val birthday = birthdayAdapter.getItem(position)

        AlertDialog.Builder(this)
            .setTitle("Delete Birthday?")
            .setMessage("Are you sure you want to delete ${birthday.name}'s birthday?")
            .setPositiveButton("Confirm") { _, _ ->
                dbHelper.deleteBirthday(this, birthday.id)
                loadBirthdays() // Refresh list
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                birthdayAdapter.notifyItemChanged(position) // Reset item position
            }
            .setCancelable(false)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.POST_NOTIFICATIONS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            checkSpecialPermissions()
        }

//        disableBatteryOptimization(this)

//        enableBackgroundActivity(this)

    }

    private fun checkSpecialPermissions() {
//        if (!Settings.canDrawOverlays(this)) {
//            showEnableOverlayPermissionDialog()
//        }
//        if (!isAccessibilityServiceEnabled(this, WhatsAppAccessibilityService::class.java)) {
//            showEnableAccessibilityDialog()
//        }
        if (!isNotificationListenerEnabled()) {
            showEnableNotificationDialog()
        }

    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val componentName = ComponentName(context, service)
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(componentName.flattenToString()) == true
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val contentResolver = applicationContext.contentResolver
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return listeners?.contains(packageName) == true
    }

    private fun showEnableOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Overlay Permission")
            .setMessage("This app requires overlay permission to function properly.")
            .setPositiveButton("Grant Permission") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEnableAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility Service")
            .setMessage("Enable Accessibility Service to allow auto WhatsApp messaging. This is required for sending birthday wishes automatically.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showEnableNotificationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Notification Access")
            .setMessage("This app requires Notification Access to work properly.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            checkSpecialPermissions()
        }
    }

    fun enableBackgroundActivity(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    fun disableBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:" + context.packageName)
            context.startActivity(intent)
        }
    }

    fun scheduleWhatsAppWorker(context: Context, birthday: Birthday) {
        val workManager = WorkManager.getInstance(context)
        val inputData = Data.Builder()
            .putString("name", birthday.name)
            .putString("whatsapp", birthday.whatsapp)
            .putInt("id", birthday.id)
            .putString("wish", birthday.wish)
            .putString("date", birthday.date)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<WhatsAppWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)  // Adjust delay as needed
            .setInputData(inputData)
            .build()

        workManager.enqueue(workRequest)
    }

    
}
