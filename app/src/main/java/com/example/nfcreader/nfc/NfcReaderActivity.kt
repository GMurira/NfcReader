package com.example.nfcreader.nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.tech.NfcF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NfcReaderActivity : ComponentActivity() {
    private lateinit var nfcAdapter: NfcAdapter

    private val _messages = MutableLiveData<List<NdefMessage>>()
    val messages: LiveData<List<NdefMessage>> = _messages

    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("Failed to add MIME type.", e)
            }
        }
        intentFiltersArray = arrayOf(ndef)

        // Example: Filter for NfcF technology
        techListsArray = arrayOf(arrayOf<String>(NfcF::class.java.name))

        // Set content for the activity
        setContent {
            NfcMessageList(messages = _messages.value ?: emptyList())
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->
                val messages: List<NdefMessage> = rawMessages.filterIsInstance<NdefMessage>()
                _messages.postValue(messages)
            }
        }
    }
}

@Composable
fun NfcMessageList(messages: List<NdefMessage>) {
    LazyColumn {
        items(messages) { message ->
            NDEFMessageItem(message)
        }
    }
}

@Composable
fun NDEFMessageItem(message: NdefMessage) {
    Text(text = "NDEF Message: ${String(message.records[0].payload)}")
}
