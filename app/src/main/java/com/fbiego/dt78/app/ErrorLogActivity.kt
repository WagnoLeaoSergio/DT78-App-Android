/*
 *
 * MIT License
 *
 * Copyright (c) 2021 Felix Biego
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.fbiego.dt78.app

import android.content.Intent
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fbiego.dt78.R
import com.fbiego.dt78.SleepActivity
import com.fbiego.dt78.data.*
import kotlinx.android.synthetic.main.activity_error_log.*
import kotlinx.android.synthetic.main.activity_sleep.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class ErrorLogActivity : AppCompatActivity() {


    private var errorList = ArrayList<ErrorData>()
    private var errorToday = ArrayList<ErrorData>()
    private var current = 0
    private var maxDay = 0
    companion object {
        lateinit var errorRecycler: RecyclerView

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(myTheme(pref.getInt(SettingsActivity.PREF_ACCENT, 0)))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error_log)

        Thread.setDefaultUncaughtExceptionHandler(CrashLogger(this))

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)


        errorRecycler = findViewById<View>(R.id.recyclerViewError) as RecyclerView
        errorRecycler.layoutManager =
            LinearLayoutManager(this)

        errorRecycler.isNestedScrollingEnabled = false

        buttonErrorNext.setOnClickListener {
            current--
            if (current <= 0){
                buttonErrorNext.isEnabled = false
            }
            if (current < maxDay-1){
                buttonErrorPrev.isEnabled = true
            }
            plotData(maxDay - (1 + current))
        }

        buttonErrorPrev.setOnClickListener {
            current++
            if (current >= maxDay-1) {
                buttonErrorPrev.isEnabled = false
            }
            if (current > 0){
                buttonErrorNext.isEnabled = true
            }
            plotData(maxDay - (1 + current))
        }

    }

    override fun onResume() {
        super.onResume()

        loadData()

    }

    private fun plotData(day: Int){
        val dbHandler = MyDBHandler(this, null, null, 1)

        errorToday.clear()
        errorToday = dbHandler.getErrorDay(errorList[day].year, errorList[day].month, errorList[day].day)
        errorToday.reverse()

        val sleepAdapter = ErrorAdapter(errorToday, this)
        errorRecycler.apply {
            layoutManager =
                LinearLayoutManager(this@ErrorLogActivity)
            adapter = sleepAdapter
        }

        textErrorDate.text = today(errorList[day])

        service_start.text = errorToday.count { it.error == SERVICE_STARTED }.toString()
        service_stop.text = errorToday.count { it.error == SERVICE_STOPPED }.toString()
        watch_connect.text = errorToday.count { it.error == WATCH_CONNECTED }.toString()
        disconnect.text = errorToday.count { it.error == WATCH_DISCONNECT }.toString()
        watch_reconnect.text = errorToday.count { it.error == WATCH_RECONNECT }.toString()
        link_loss.text = errorToday.count { it.error == LINK_LOSS }.toString()
        bt_off.text = errorToday.count { it.error == BLUETOOTH_OFF }.toString()
        watch_error.text = errorToday.count { it.error == WATCH_ERROR || it.error == WATCH_ERROR_133 }.toString()
    }

    private fun loadData(){
        val dbHandler = MyDBHandler(this, null, null, 1)
        errorList.clear()
        errorList = dbHandler.getDaysWithErrors()
        maxDay = errorList.size
        if (errorList.isNotEmpty()){

            plotData(maxDay - (1 + 0))
        } else {
            textErrorDate.text = this.getString(R.string.no_data)
        }

        buttonErrorNext.isEnabled = false
        buttonErrorPrev.isEnabled = maxDay > 1
    }

    private fun clearData(){
        val dbHandler = MyDBHandler(this, null, null, 1)
        dbHandler.clearAllErrors()
        errorToday.clear()
        errorList.clear()
        buttonErrorNext.isEnabled = false
        buttonErrorPrev.isEnabled = false
        val sleepAdapter = ErrorAdapter(errorToday, this)
        errorRecycler.apply {
            layoutManager =
                LinearLayoutManager(this@ErrorLogActivity)
            adapter = sleepAdapter
        }
        textErrorDate.text = this.getString(R.string.no_data)
        service_start.text = errorToday.count { it.error == SERVICE_STARTED }.toString()
        service_stop.text = errorToday.count { it.error == SERVICE_STOPPED }.toString()
        watch_connect.text = errorToday.count { it.error == WATCH_CONNECTED }.toString()
        disconnect.text = errorToday.count { it.error == WATCH_DISCONNECT }.toString()
        watch_reconnect.text = errorToday.count { it.error == WATCH_RECONNECT }.toString()
        link_loss.text = errorToday.count { it.error == LINK_LOSS }.toString()
        bt_off.text = errorToday.count { it.error == BLUETOOTH_OFF }.toString()
        watch_error.text = errorToday.count { it.error == WATCH_ERROR || it.error == WATCH_ERROR_133 }.toString()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.error_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                val cachePath = this.cacheDir
                val file = File(cachePath, "error.txt")
                val data = File(cachePath, "data.txt")

                if (data.exists()){
                    data.delete()
                }
                if (file.exists()){

                    val alert = AlertDialog.Builder(this)
                    alert.setTitle(getString(R.string.clear_data))
                    alert.setMessage(R.string.clear_data_info)
                    alert.setPositiveButton(R.string.yes) { _, _ ->
                        clearData()
                        file.delete()
                        Toast.makeText(this, R.string.log_deleted, Toast.LENGTH_SHORT).show()
                    }
                    alert.setNegativeButton(R.string.cancel) { _, _ ->

                    }
                    alert.show()

                } else {
                    Toast.makeText(this, R.string.nothing, Toast.LENGTH_SHORT).show()
                }

                true
            }
            R.id.share -> {
                val cachePath = this.cacheDir
                val file = File(cachePath, "data.txt")
                val error = File(cachePath, "error.txt")

                var text = ""
                text += if (file.exists()) "data.txt -> data from watch\n" else "No data logs\n"
                text += if (error.exists()) "error.txt -> error logs" else "No error logs"

                val builder = AlertDialog.Builder(this)
                builder.setTitle("Share logs")
                builder.setMessage(text)
                if (error.exists()) {
                    builder.setPositiveButton("Error") { _, _ ->
                        val contentUri =
                            FileProvider.getUriForFile(this, "com.fbiego.dt78.fileprovider", error)
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "*/*"
                        intent.putExtra(Intent.EXTRA_STREAM, contentUri)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.putExtra(Intent.EXTRA_TEXT, "Share error log")
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Send")
                        startActivity(Intent.createChooser(intent, "Share"))
                    }
                }
                if (file.exists()) {
                    builder.setNeutralButton("Data") { _, _ ->
                        val contentUri =
                            FileProvider.getUriForFile(this, "com.fbiego.dt78.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "*/*"
                        intent.putExtra(Intent.EXTRA_STREAM, contentUri)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.putExtra(Intent.EXTRA_TEXT, "Share data log")
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Send")
                        startActivity(Intent.createChooser(intent, "Share"))
                    }
                }
                builder.setNegativeButton(R.string.cancel, null)
                builder.show()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun today(stepsData: ErrorData): String{
        val cal = Calendar.getInstance(Locale.getDefault())
        return if (cal.get(Calendar.DAY_OF_MONTH) == stepsData.day && cal.get(Calendar.MONTH)+1 == stepsData.month && cal.get(
                Calendar.YEAR) == stepsData.year+2000){
            this.getString(R.string.today)
        } else {
            String.format("%02d-%02d-20%02d", stepsData.day, stepsData.month, stepsData.year)
        }
    }
}