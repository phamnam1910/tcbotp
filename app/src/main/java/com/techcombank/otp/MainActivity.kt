package com.techcombank.otp

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.services.docs.v1.Docs
import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity

import com.google.api.client.json.JsonFactory

import com.google.api.client.extensions.android.http.AndroidHttp

import com.google.api.client.http.HttpTransport

import com.google.api.client.util.ExponentialBackOff

import com.google.api.services.drive.DriveScopes

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory

import com.google.api.services.drive.Drive
import java.util.*
import com.google.api.client.http.FileContent
import com.google.api.client.util.IOUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = "Endless Service"

        findViewById<Button>(R.id.btnStartService).let {
            it.setOnClickListener {
                log("START THE FOREGROUND SERVICE ON DEMAND")
                actionOnService(Actions.START)
            }
        }

        findViewById<Button>(R.id.btnStopService).let {
            it.setOnClickListener {
                log("STOP THE FOREGROUND SERVICE ON DEMAND")
                actionOnService(Actions.STOP)
            }
        }

        findViewById<Button>(R.id.btnUpdateDoc).let {
            it.setOnClickListener {

                val outputDir: File = this.cacheDir // context being the Activity pointer

                val outputFile = File.createTempFile("prefix", ".extension", outputDir)
                val `in` =
                    DocsQuickstart::class.java.getResourceAsStream("/testDrive.txt")
                        ?: throw FileNotFoundException("Resource not found: " +"/testDrive.txt")


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.copy(`in`, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                };
                createService(this)?.let { it1 -> uploadFile(outputFile, it1) }
            }
        }
    }



    private fun createService(context: Context): Drive? {
        val mCredential = GoogleAccountCredential.usingOAuth2(
            context.applicationContext,
            listOf(DriveScopes.DRIVE)
        ).setBackOff(ExponentialBackOff())
        mCredential.selectedAccountName = "phamnam1910@gmail.com"
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
        return Drive.Builder(
            transport, jsonFactory, mCredential
        )
            .setApplicationName("TechcombankOtp")
            .build()
    }

    fun uploadFile(fileContent: File?, mService: Drive) {
        try {
            val file = com.google.api.services.drive.model.File()
            file.name = "driveTest"
//            val parents: MutableList<String> = ArrayList(1)
////            parents.add("parent_folder_id") // Here you need to get the parent folder id
//            file.parents = parents
            val mediaContent = FileContent("text/plain", fileContent)
            mService.files().create(file, mediaContent).setFields("id").execute()
            log("File uploaded")
        } catch (e: IOException) {
            log("Error uploading file: $e")
            e.printStackTrace()
        }
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, EndlessService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                log("Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            log("Starting the service in < 26 Mode")
            startService(it)
        }
    }
}
