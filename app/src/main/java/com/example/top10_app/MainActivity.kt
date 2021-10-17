package com.example.top10_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class FeedEntry{
    var name:String=""
    override fun toString(): String {
        return """name = $name""".trimIndent()
    }
}

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var rvMain: RecyclerView
    private lateinit var btnGet: Button
    private lateinit var itemsList : ArrayList<String>

    val feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=10/xml"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGet=findViewById(R.id.btnGet)
        btnGet.setOnClickListener {
            requestApi(feedURL)

            rvMain=findViewById(R.id.rvMain)
            rvMain.layoutManager=LinearLayoutManager(this)
            rvMain.setHasFixedSize(true)

        }
    }



    private fun downlodXml(urlPath: String?):String {
    val xmlResult=StringBuilder()
        try {
            val url=URL(urlPath)
            val connection : HttpURLConnection=url.openConnection() as HttpURLConnection
            val response = connection.responseCode
            Log.d(TAG, "downloadXML: The response code was $response")

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val inputBuffer = CharArray(500)
            var charsRead = 0

            while (charsRead >= 0) {
                charsRead = reader.read(inputBuffer)

                if (charsRead > 0) {
                    xmlResult.append(String(inputBuffer, 0, charsRead))
                }
            }
            reader.close()

            Log.d(TAG, "Received ${xmlResult.length} bytes")
            return xmlResult.toString()

        }catch (e: MalformedURLException) {
            Log.e(TAG, "downloadXML: Invalid URL ${e.message}")
        } catch (e: IOException) {
            Log.e(TAG, "downloadXML: IO Exception reading data: ${e.message}")
        } catch (e: SecurityException) {
            e.printStackTrace()
            Log.e(TAG, "downloadXML: Security exception.  Needs permissions? ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error: ${e.message}")
        }
        return ""
    }
    private fun requestApi(url: String) {

        var listItems = ArrayList<FeedEntry>()

        CoroutineScope(Dispatchers.IO).launch {

            val rssFeed = async { downlodXml(url) }  .await()
            if(rssFeed.isEmpty()){
                Log.e(TAG, "requestApi fun: Error downloading")
            }else{
                val parseApplications = async {
                    FeedParser()
                } .await()
                parseApplications.parse(rssFeed)
                listItems=parseApplications.getParsedList()

                withContext(Dispatchers.Main){
                    rvMain.adapter=RVAdapter(listItems)
                }
            }
        }

    }
}