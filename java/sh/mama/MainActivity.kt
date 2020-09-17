package sh.mama

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.bachors.prefixinput.EditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {
    private lateinit var studentID: EditText

    fun EditText.onSubmit(func: () -> Unit) {
        setOnEditorActionListener{ _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(windowToken, 0)
                func()
            }

            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        studentID = findViewById(R.id.studentID)
        studentID.setPrefix("s")
        studentID.onSubmit{
            register(studentId = studentID.text.toString())
            Toast.makeText(this, "Starter registrering", Toast.LENGTH_SHORT).show()
        }
    }


    private fun register(studentId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val url = "https://docs.google.com/spreadsheets/d/10UT4S48U9TMZTW3--dWaxZM34fF8Vfhw1Q-gP-A8XKI/export?format=csv&id=10UT4S48U9TMZTW3--dWaxZM34fF8Vfhw1Q-gP-A8XKI&gid=730350562"
            val csv = URL(url).readText().split("\n")
            val kode = csv[0].split(",")[1]
            val regex = """s\d{6}""".toRegex()
            var response = ""
            if (regex.matches(studentId)){
                for (line in csv){
                    val student = line.split(",")
                    if (student.size > 2) {
                        if (regex.matches(student[2])){
                            if (student[2]==studentId){
                                response = postData(studentId, kode)
                                launch(Dispatchers.Main){
                                    Toast.makeText(this@MainActivity,validateRespose(response), Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                        }
                    }
                }
                launch(Dispatchers.Main){
                    Toast.makeText(this@MainActivity,"Student nummer findes ikke på listen...", Toast.LENGTH_SHORT).show()
                }
            } else {
                launch(Dispatchers.Main){
                    Toast.makeText(this@MainActivity,"Du har ikke skrevet et student nummer...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validateRespose(response: String): String {
        return when {
            "Du er allerede registreret" in response -> {
                "Du er allerede registeret"
            }
            "Der skete en fejl" in response -> {
                "Der skete en fejl, det er ikke min skyld. #CheckYourself"
            }
            "Forkert registreringskode" in response -> {
                "Jeg får åbenbart en forkert registreringskode, Dette er min egen skyld..."
            }
            "Registreringssystemet er lukket" in response -> {
                "Registreringssystemet er lukket!"
            }
            else -> "Du er vist nok registeret nu!"
        }
    }

    private fun postData(studentId: String, kode: String): String {
        val url = "https://script.google.com/macros/s/AKfycbwBDFeKurY7fDBQ9booRQGo_QyIHl_gEnR0UYKNCb_xGCtPftHu/callback?nocache_id=5"
        val req = URL(url)
        val con = req.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.connectTimeout = 300000
        con.doOutput = true
        val data = ("request=%5B%22submit%22%2C%22%5B%7B%5C%22sno%5C%22%3A%5C%22$studentId%5C%22%2C%5C%22regcode%5C%22%3A%5C%22$kode%5C%22%7D%5D%22%2Cnull%2C%5B0%5D%2Cnull%2Cnull%2Ctrue%2C0%5D").toByteArray()
        con.setRequestProperty("User-Agent", "Your-Mom")
        con.setRequestProperty("X-Same-Domain", "1")
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
        con.setRequestProperty("Referer", "https://script.google.com/macros/s/AKfycbwBDFeKurY7fDBQ9booRQGo_QyIHl_gEnR0UYKNCb_xGCtPftHu/exec?regCodeCell=B1&dateRange=H3:3&studentRange=c4:c&sp=https://docs.google.com/spreadsheets/d/10UT4S48U9TMZTW3--dWaxZM34fF8Vfhw1Q-gP-A8XKI/edit")

        val request = DataOutputStream(con.outputStream)
        request.write(data)
        request.flush()
        con.inputStream.bufferedReader().use {
            val response = StringBuffer()
            var inputLine = it.readLine()
            while (inputLine != null) {
                response.append(inputLine)
                inputLine = it.readLine()
            }
            return response.toString()
        }
    }

}

