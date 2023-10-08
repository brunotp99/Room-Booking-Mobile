package com.example.appmobile

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.appmobile.databinding.ActivityCriarReservaBinding
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.threetenabp.AndroidThreeTen
import nl.joery.timerangepicker.TimeRangePicker
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

class criarReserva : AppCompatActivity() {

    private lateinit var binding: ActivityCriarReservaBinding
    lateinit var session : SessionManager

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCriarReservaBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        AndroidThreeTen.init(this)
        val pt = Locale("pt", "PT")
        Locale.setDefault(pt);

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        /* Para remover um sombrado atraz do menu */
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.background = null
        /* Desativar o icon escondido que alinha o menu */
        bottomNavigationView.menu.getItem(2).isEnabled = false

        /* Executar botao QRCODE */
        val btnQrcode = findViewById<FloatingActionButton>(R.id.fab)
        btnQrcode.setOnClickListener {
            var i = Intent(applicationContext, QRcodeActivity::class.java)
            startActivity(i)
        }

        /* Botoes do Menu */
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.page_1 -> {
                    val iHome = Intent(applicationContext, Home::class.java)
                    startActivity(iHome)
                }
                R.id.page_2 -> {
                    val iPesquisa = Intent(applicationContext, pesquisaSalas::class.java)
                    startActivity(iPesquisa)
                }
                R.id.page_3 -> {
                    val iMinhas = Intent(applicationContext, MinhasReservas::class.java)
                    startActivity(iMinhas)
                }
                R.id.page_4 -> {
                    val Opcoes = Intent(applicationContext, Opcoes::class.java)
                    startActivity(Opcoes)
                }
            }
            return@setOnItemSelectedListener true
        }

        /* Obter dados para gerar o relogio */
        val bundle = intent.extras
        val inicio = bundle!!.getString("inicio")
        val dataReserva = bundle.getString("data")
        Log.i("Teste", dataReserva!!)
        val delim2 = "-"
        val dataYMD = dataReserva?.split(delim2)
        binding.txtReserva.text = "Reserva para " + dataYMD!![2] + "/" + dataYMD!![1] + "/" + dataYMD!![0]

        val delim = ":"
        val inicioHM = inicio!!.split(delim)
        var inicioMinutos = (inicioHM[0].toInt() * 60) + inicioHM[1].toInt()
        var fimMinutos = ((inicioHM[0].toInt()+1) * 60) + 30

        if(dataReserva == LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString()){

            if(LocalTime.of(inicioHM[0].toInt(), inicioHM[1].toInt()).isBefore(LocalTime.now())){
                val current = LocalDateTime.now()
                val formatterHM = DateTimeFormatter.ofPattern("HH:mm")
                val formatted = current.format(formatterHM)
                val atualHM = formatted.split(delim)
                Log.i("Criar", atualHM[0] + ' ' + atualHM[1])
                inicioMinutos = (atualHM[0].toInt() * 60) + atualHM[1].toInt()
            }

            if(LocalTime.of(inicioHM[0].toInt(), 0).isBefore(LocalTime.parse(appSettings.Fecho))){
                if(LocalTime.of(inicioHM[0].toInt()+1, 30).isAfter(LocalTime.parse(appSettings.Fecho))){
                    val bundle = intent.extras
                    val intervalo = bundle?.getString("intervalo")
                    if (intervalo != null) {
                        fimMinutos = ((20 * 60) - intervalo.toInt())
                    }
                }
            }

        }

        /* Gerar o time picker/relogio */
        val picker = binding.picker
        picker.thumbSize = 32.px
        picker.sliderWidth = 32.px
        picker.sliderColor = Color.rgb(233, 233, 233)
        picker.thumbColor = Color.TRANSPARENT
        picker.thumbIconColor = Color.WHITE
        picker.sliderRangeGradientStart = Color.parseColor("#2d63ed")
        picker.sliderRangeGradientMiddle = null
        picker.sliderRangeGradientEnd = Color.parseColor("#0099ff")
        picker.thumbSizeActiveGrow = 1.0f
        picker.clockFace = TimeRangePicker.ClockFace.SAMSUNG
        picker.clockVisible = false
        picker.hourFormat = TimeRangePicker.HourFormat.FORMAT_12
        picker.startTimeMinutes  = inicioMinutos
        picker.endTimeMinutes = fimMinutos

        updateTimes()
        updateDuration()

        picker.setOnTimeChangeListener(object : TimeRangePicker.OnTimeChangeListener {
            override fun onStartTimeChange(startTime: TimeRangePicker.Time) {
                updateTimes()
            }

            override fun onEndTimeChange(endTime: TimeRangePicker.Time) {
                updateTimes()
            }

            override fun onDurationChange(duration: TimeRangePicker.TimeDuration) {
                updateDuration()
            }
        })

        picker.setOnDragChangeListener(object : TimeRangePicker.OnDragChangeListener {

            override fun onDragStart(thumb: TimeRangePicker.Thumb): Boolean {

                if(thumb != TimeRangePicker.Thumb.BOTH) {
                    animate(thumb, true)
                }
                return true
            }

            override fun onDragStop(thumb: TimeRangePicker.Thumb) {

                val delim = ":"
                val startHM = picker.startTime.toString().split(delim)
                val endHM = picker.endTime.toString().split(delim)

                if(LocalTime.of(startHM[0].toInt(), startHM[1].toInt()).isBefore(LocalTime.of(9, 0))){
                    val bundle = intent.extras
                    val inicio = bundle!!.getString("inicio")
                    val delim = ":"
                    val inicioHM = picker.startTime.toString().split(delim)
                    picker.startTimeMinutes = 9 * 60
                    updateTimes()
                }

                if(thumb != TimeRangePicker.Thumb.BOTH) {
                    animate(thumb, false)
                }

                Log.d(
                    "TimeRangePicker",
                    "Start time: " + picker.startTime
                )
                Log.d(
                    "TimeRangePicker",
                    "End time: " + picker.endTime
                )
                Log.d(
                    "TimeRangePicker",
                    "Total duration: " + picker.duration
                )
            }
        })
        /* Evento Criar Reserva */
        binding.criarReserva.setOnClickListener {
            validarSobreposicao()
        }

        /* Voltar */
        val voltarBtn = findViewById<TextView>(R.id.goback)
        voltarBtn.setOnClickListener {
            finish()
        }

    }

    /* Atualizar as horas que aparecem no centro do picker */
    private fun updateTimes() {
        val end_time = findViewById<TextView>(R.id.end_time)
        val start_time = findViewById<TextView>(R.id.start_time)
        end_time.text = (binding.picker.endTime.toString())
        start_time.text = binding.picker.startTime.toString()
    }

    /* Atualizar mensagem de duração */
    @SuppressLint("SetTextI18n")
    private fun updateDuration() {
        val bundle = intent.extras
        val intervalo = bundle?.getString("intervalo")
        if(intervalo?.toInt() != 0)
            binding.duration.text = "Duração: " + binding.picker.duration + " + " + intervalo + " para limpezas"
        else binding.duration.text = "Duração: " + binding.picker.duration
    }

    private fun animate(thumb: TimeRangePicker.Thumb, active: Boolean) {
        val activeView = if(thumb == TimeRangePicker.Thumb.START) binding.bedtimeLayout else binding.wakeLayout
        val inactiveView = if(thumb == TimeRangePicker.Thumb.START) binding.wakeLayout else binding.bedtimeLayout
        val direction = if(thumb == TimeRangePicker.Thumb.START) 1 else -1

        activeView
            .animate()
            .translationY(if(active) (activeView.measuredHeight / 2f)*direction else 0f)
            .setDuration(300)
            .start()
        inactiveView
            .animate()
            .alpha(if(active) 0f else 1f)
            .setDuration(300)
            .start()
    }

    private val Int.px: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure(error: String)
    }

    /* Enviar informações da reserva e verificar disponibilidade */
    fun validarSobreposicao(){
        val url = appSettings.URL_validar_sobreposicao
        val params = HashMap<String, String>()
        val bundle = intent.extras
        val dataReserva = bundle?.getString("data")
        val nsala = bundle?.getString("sala")
        val intervalo = bundle?.getString("intervalo")
        val horafim = LocalTime.parse(binding.picker.endTime.toString()).plusMinutes(intervalo!!.toLong())
        params["horainicio"] = binding.picker.startTime.toString();
        params["horafim"] = horafim.toString()
        params["datareserva"] = dataReserva!!;
        params["nsala"] = nsala!!;
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {
            if(response.getBoolean("success")){
                val dialog = AlertDialog.Builder(this)
                val dialogView = layoutInflater.inflate(R.layout.dialog_pergunta, null)
                val btnVoltar = dialogView.findViewById<Button>(R.id.dialogVoltar)
                val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
                val txtCapacidade = dialogView.findViewById<TextView>(R.id.txtCapacidade)
                val bundle = intent.extras
                if(bundle != null){
                    val alocacao = bundle.getString("alocacao")
                    val lugares = bundle.getString("lugares")
                    val alocacaoInt = alocacao?.toInt()
                    if (alocacaoInt != null) {
                        val lugaresFinal = Math.floor( (lugares!!.toInt() * alocacaoInt / 100).toDouble() ).roundToInt()
                        if(alocacaoInt < 100){
                            txtCapacidade.text = "A capacidade máxima da sala esta limitada a $lugaresFinal Lugares"
                        }else txtCapacidade.text = "A capacidade máxima é de $lugaresFinal Lugares"
                    }

                }
                dialog.setView(dialogView)
                dialog.setCancelable(true)
                val customDialog = dialog.create()
                customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                customDialog.show()
                btnVoltar.setOnClickListener {
                    customDialog.dismiss()
                }
                btnContinuar.setOnClickListener {
                    customDialog.dismiss()
                    fazerReserva()
                }

            }else{
                alertaDanger(this@criarReserva, "Atenção", "Por favor, escolha um intervalo disponível!")
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace().toString() })
        requestQueue.add(request)
    }

    /* Se a intervalo escolhido tem disponibilidade, enviar os dados da reserva e registar */
    fun fazerReserva(){
        val requestQueue = Volley.newRequestQueue(this)
        val strRequest: StringRequest = object : StringRequest(
            Method.POST, appSettings.URLaddReserva,
            Response.Listener { response ->
                if(response.toBoolean()){
                    alertaSuccess(this@criarReserva, "Sucesso", getString(R.string.sucesso_add_reserva))
                    Handler(Looper.getMainLooper()).postDelayed({
                            val i = Intent(applicationContext, MinhasReservas::class.java)
                    startActivity(i) }, 1500)
                }else{
                    alertaDanger(this@criarReserva, "Atenção", getString(R.string.erro_add_reserva))
                }
            },
            Response.ErrorListener { error ->
                alertaDanger(this@criarReserva, "Atenção", getString(R.string.erro_geral))
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                val bundle = intent.extras
                val dataReserva = bundle?.getString("data")
                val nsala = bundle?.getString("sala")
                val intervalo = bundle?.getString("intervalo")
                val horafim = LocalTime.parse(binding.picker.endTime.toString()).plusMinutes(intervalo!!.toLong())
                Log.i("Criar", binding.picker.startTime.toString())
                Log.i("Criar", binding.picker.endTime.toString())
                Log.i("Criar", horafim.toString())
                params["nsala"] =  nsala!!
                params["nutilizador"] = session.getUser().toString()
                params["datareserva"] = dataReserva!!
                params["horainicio"] = binding.picker.startTime.toString()
                params["horafim"] = horafim.toString()
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }
        }
        requestQueue.add(strRequest)
    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val menuItem = bottomNavigationView.menu.getItem(1)
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
        }
    }
}