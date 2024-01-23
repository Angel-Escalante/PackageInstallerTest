package com.example.test

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider.getUriForFile
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.example.test.ui.theme.TestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Button(onClick = ::installApk) {
                        Text(text = "Download")
                    }
                }
            }
        }
    }

    private fun installApk() {
        val installer = packageManager.packageInstaller
        val uri = getUriForFile(this, "com.example.test", File(filesDir, "test.apk"))

        lifecycleScope.launch(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use {apkStream ->
                val length =
                    DocumentFile.fromSingleUri(application, uri)?.length() ?: -1
                val params =
                    PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                val sessionId = installer.createSession(params)
                val session = installer.openSession(sessionId)

                session.openWrite("test-install", 0, length).use { sessionStream ->
                    apkStream.copyTo(sessionStream)
                    session.fsync(sessionStream)
                }

                val intent = Intent(application, CustomBroadcastReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    application,
                    4444,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                session.commit(pi.intentSender)
                session.close()
            }
        }
    }// installApk
}
