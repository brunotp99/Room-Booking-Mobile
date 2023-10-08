package com.example.appmobile

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.appmobile.databinding.ActivityPesquisaSalasBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.picasso.Picasso
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class pesquisaSalas : AppCompatActivity() {

    private lateinit var binding: ActivityPesquisaSalasBinding
    lateinit var session : SessionManager

    data class Salas(val nsala: Int, val nestado: Int, val estado : String, val sala: String, val lugares: Int, val estadosala: Int, val descricao: String, val alocacao: Int, val intervalolimpeza: Int, val imagem: String)
    private var listSalas = ArrayList<Salas>()
    private var listSalasDatetime = ArrayList<Salas>()

    lateinit var setDate : String
    lateinit var setStart : String
    lateinit var setEnd : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPesquisaSalasBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        AndroidThreeTen.init(this);

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        /* Para remover um sombrado atraz do menu */
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.background = null
        /* Desativar o icon escondido que alinha o menu */
        bottomNavigationView.menu.getItem(2).isEnabled = false
        bottomNavigationView.selectedItemId = R.id.page_2

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

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setOnMenuItemClickListener {menuItem ->
            when (menuItem.itemId) {
                R.id.notify -> {
                    var dialog = NotificacoesFragment()
                    dialog.show(supportFragmentManager, "TAG")
                    true
                }
                else -> false
            }

        }

        /* Atualizar o estado da sala */
        getSalas(object: VolleyCallBack {
            override fun onSuccess() {
                val listView = findViewById<ListView>(R.id.pesquisa_listview)
                if(listSalas.size != 0){
                    listView.adapter = MyCustomAdapter(applicationContext, listSalas, true, 0, "")
                    listView.visibility = View.VISIBLE
                    emptyLayout(false)
                }else{
                    listView.visibility = View.GONE
                    emptyLayout(true)
                }
            }
            override fun onFailure(error: String) {
                Log.i("Salas", error)
                alertaDanger(this@pesquisaSalas, "Atenção", getString(R.string.erro_mostrar_salas))
            }
        })

        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val formatted = current.format(formatter)
        binding.setDate.text = formatted

        val formatterSQL = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedSQL = current.format(formatterSQL)
        setDate = formattedSQL
        setStart = "09:00"
        setEnd = "00:00"

        /* Tipo de Pesquisa */
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                if(tab?.position  == 0){
                    tabSala()
                }
                else if(tab?.position  == 1) {

                    populateList(setDate, setStart, setEnd)

                    binding.tabSala.visibility = View.GONE
                    binding.tabData.visibility = View.VISIBLE

                    val pt = Locale("pt", "PT")
                    Locale.setDefault(pt);

                    binding.setDate.setOnClickListener {
                        val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Selecione a data").setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build()
                        datePicker.show(supportFragmentManager, "DatePicker")

                        // Setting up the event for when ok is clicked
                        datePicker.addOnPositiveButtonClickListener {
                            // formatting date in dd-mm-yyyy format.
                            val dateFormatter = SimpleDateFormat("dd/MM/yyyy")
                            val date = dateFormatter.format(Date(it))
                            binding.setDate.text = date

                            val dateFormatterSQL = SimpleDateFormat("yyyy-MM-dd")
                            val dateSQL = dateFormatterSQL.format(Date(it))
                            setDate = dateSQL
                            populateList(setDate, setStart, setEnd)

                        }

                    }

                    binding.setInicio.setOnClickListener {
                        val picker =
                            MaterialTimePicker.Builder()
                                .setTimeFormat(TimeFormat.CLOCK_24H)
                                .setHour(12)
                                .setMinute(10)
                                .setTitleText("Inicio reserva")
                                .build()
                        picker.show(supportFragmentManager, "TimePicker")

                        // Setting up the event for when ok is clicked
                        picker.addOnPositiveButtonClickListener {
                            val f = DecimalFormat("00")
                            val hInicio = "${f.format(picker.hour)}:${f.format(picker.minute)}"
                            binding.setInicio.text = hInicio
                          //  binding.setInicio.text = String.format("%02d:%02d", picker.hour, picker.minute)
                            setStart = hInicio
                            populateList(setDate, setStart, setEnd)

                        }

                        // Setting up the event for when cancelled is clicked
                        picker.addOnNegativeButtonClickListener {
                            // Toast.makeText(this, "${datePicker.headerText} is cancelled", Toast.LENGTH_LONG).show()
                        }

                        // Setting up the event for when back button is pressed
                        picker.addOnCancelListener {
                            //Toast.makeText(this, "Date Picker Cancelled", Toast.LENGTH_LONG).show()
                        }
                    }

                    binding.setFim.setOnClickListener {
                        val picker =
                            MaterialTimePicker.Builder()
                                .setTimeFormat(TimeFormat.CLOCK_24H)
                                .setHour(12)
                                .setMinute(10)
                                .setTitleText("Inicio reserva")
                                .build()
                        picker.show(supportFragmentManager, "TimePicker")

                        // Setting up the event for when ok is clicked
                        picker.addOnPositiveButtonClickListener {
                            val f = DecimalFormat("00")
                            val hFim = "${f.format(picker.hour)}:${f.format(picker.minute)}"
                            binding.setFim.text = hFim
                            setEnd = hFim
                            populateList(setDate, setStart, setEnd)

                        }

                    }

                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Handle tab reselect
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Handle tab unselect
            }
        })

    }

    /* Criação de uma listview custom */
    internal class MyCustomAdapter(context: Context, val Salas : ArrayList<Salas>, val ShowStatus : Boolean, val Mode : Int, val Data : String): BaseAdapter() {
        private val mContext: Context
        init {
            mContext = context
        }
        override fun getCount(): Int {
            return Salas.size
        }
        override fun getItemId(posicao: Int): Long {
            return posicao.toLong()
        }
        override fun getItem(posicao: Int): Any {
            return "Item"
        }

        internal interface MyCallback {
            fun onListClick(nsala : String, intervalo : Long, lugares : Int, alocacao :Int)
        }

        var mItemClickListener: MyCallback? = null
        fun setOnClickListener(click: MyCallback) {
            mItemClickListener = click
        }

        override fun getView(posicao: Int, convertView: View?, viewGroup: ViewGroup?): View {

            val layoutInflater = LayoutInflater.from(mContext)
            val rowMain = layoutInflater.inflate(R.layout.card_pesquisa, viewGroup, false)

            val userImg = rowMain.findViewById<ImageView>(R.id.imagemSala)
            Picasso.get().load(appSettings.URL_imagem_sala + Salas[posicao].imagem)
                .resize(600, 200) // resizes the image to these dimensions (in pixel)
                .centerCrop()
                .placeholder(R.drawable.sala_exemplo)
                .error(R.drawable.sala_exemplo)
                .into(userImg);

            val nomeSala = rowMain.findViewById<TextView>(R.id.nomeSala)
            nomeSala.text = Salas[posicao].sala

            val lugaresSala = rowMain.findViewById<TextView>(R.id.lugaresSala)
            lugaresSala.text = Salas[posicao].lugares.toString() + " Lugares"

            val descricaoSala = rowMain.findViewById<TextView>(R.id.descricaoSala)
            descricaoSala.text = Salas[posicao].descricao

            val estadoSala = rowMain.findViewById<TextView>(R.id.estadoSala)
            val aposLimpeza = rowMain.findViewById<TextView>(R.id.aposLimpeza)
            if(ShowStatus){
                when(Salas[posicao].nestado){
                    1 -> {
                        estadoSala.text = Salas[posicao].estado
                        estadoSala.compoundDrawables[2]?.setTint(Color.parseColor("#28a745")) //Disponivel
                    }
                    2 -> {
                        estadoSala.text = "Dísponivel"
                        aposLimpeza.visibility = View.VISIBLE
                        aposLimpeza.text = Salas[posicao].estado
                        aposLimpeza.compoundDrawables[2]?.setTint(Color.parseColor("#007bff")) //Limpa
                        estadoSala.compoundDrawables[2]?.setTint(Color.parseColor("#28a745"))
                    }
                    3 -> {
                        estadoSala.text = "Dísponivel"
                        aposLimpeza.visibility = View.VISIBLE
                        aposLimpeza.text = Salas[posicao].estado
                        aposLimpeza.compoundDrawables[2]?.setTint(Color.parseColor("#007bff")) //Limpa e Desinfectada
                        estadoSala.compoundDrawables[2]?.setTint(Color.parseColor("#28a745"))
                    }
                    4 -> {
                        estadoSala.text = Salas[posicao].estado
                        estadoSala.compoundDrawables[2]?.setTint(Color.parseColor("#17a2b8")) //Aguarda Limpeza
                    }
                    5 -> {
                        estadoSala.text = Salas[posicao].estado
                        estadoSala.compoundDrawables[2]?.setTint(Color.parseColor("#ffc107")) //Ocupado
                    }
                    6 -> {
                        estadoSala.text = Salas[posicao].estado
                        estadoSala.compoundDrawables[2]?.setTint(Color.parseColor("#dc3545")) //Bloqueado
                    }
                    else -> estadoSala.compoundDrawables[2]?.setTint(Color.parseColor("#6c757d")) //Indefinido
                }
            }else estadoSala.visibility = View.GONE


            val cardbtn = rowMain.findViewById<MaterialCardView>(R.id.cardSala)

            if(Mode == 0){ //Pesquisa por Sala
                cardbtn.setOnClickListener {
                    val inte = Intent(mContext, pesquisaCalendar::class.java)
                    inte.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    inte.putExtra("sala", Salas[posicao].nsala.toString());
                    inte.putExtra("alocacao", Salas[posicao].alocacao.toString())
                    inte.putExtra("lugares", Salas[posicao].lugares.toString())
                    inte.putExtra("intervalo", Salas[posicao].intervalolimpeza.toString())
                    mContext.startActivity(inte);
                }
            }else if(Mode == 1){ //Pesquisa por Data/Inicio
                cardbtn.setOnClickListener {
                    val inte = Intent(mContext, pesquisaCalendarFilter::class.java)
                    inte.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    inte.putExtra("nsala", Salas[posicao].nsala.toString());
                    inte.putExtra("alocacao", Salas[posicao].alocacao.toString())
                    inte.putExtra("lugares", Salas[posicao].lugares.toString())
                    inte.putExtra("intervalo", Salas[posicao].intervalolimpeza.toString())
                    inte.putExtra("data", Data)
                    mContext.startActivity(inte);
                }
            }else if(Mode == 2){ //Pesquisa por DataInicio/Fim
                cardbtn.setOnClickListener {
                    mItemClickListener?.onListClick(Salas[posicao].nsala.toString(), Salas[posicao].intervalolimpeza.toLong(), Salas[posicao].lugares, Salas[posicao].alocacao)
                }
            }

            this.notifyDataSetChanged();
            return rowMain
        }
    }

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure(error: String)
    }

    /* Obter lista de salas */
    private fun getSalas(callback: VolleyCallBack?) {
        val url = appSettings.URLsalasEstabelecimento + session.getEstabelecimento()
        listSalas.clear()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, {
                response ->try {
            val jsonArray = response.getJSONArray("" + "data")
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)
                val estadoarray = distr.getJSONObject("estado")

                val nsala = distr.getInt("nsala")
                val nestado = distr.getInt("nestado")
                val estado = estadoarray.getString("estado")
                val sala = distr.getString("sala")
                val lugares = distr.getInt("lugares")
                val estadosala = distr.getInt("estadosala")
                val descricao = distr.getString("descricao")
                val alocacao = distr.getInt("alocacao")
                val intervalolimpeza = distr.getInt("intervalolimpeza")
                val imagem = distr.getString("imagem")
                listSalas.add(Salas(nsala, nestado, estado, sala, lugares, estadosala, descricao, alocacao, intervalolimpeza, imagem))

            }
            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    /* Obter lista de salas atraves da data e hora */
    private fun getSalasByDateTime(callback: VolleyCallBack?, data : String, inicio : String, fim : String) {
        val url = appSettings.URL_pesquisa_data_hora

        val params = HashMap<String, String>()
        params["nestabelecimento"] = session.getEstabelecimento().toString()
        params["datareserva"] = data
        params["horainicio"] = inicio
        params["horafim"] = fim

        listSalasDatetime.clear()

        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), {
                response ->try {

                    if(response.getBoolean("status")){
                        val jsonArray = response.getJSONArray("" + "data")
                        for (i in 0 until jsonArray.length()) {
                            val distr = jsonArray.getJSONObject(i)

                            val nsala = distr.getInt("nsala")
                            val nestado = distr.getInt("nestado")
                            val estado = distr.getInt("estadosala").toString()
                            val sala = distr.getString("sala")
                            val lugares = distr.getInt("lugares")
                            val estadosala = distr.getInt("estadosala")
                            val descricao = distr.getString("descricao")
                            val alocacao = distr.getInt("alocacao")
                            val intervalolimpeza = distr.getInt("intervalolimpeza")
                            val imagem = distr.getString("imagem")
                            listSalasDatetime.add(Salas(nsala, nestado, estado, sala, lugares, estadosala, descricao, alocacao, intervalolimpeza, imagem))
                    }
                }else callback?.onFailure("Retornou falso")
            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    /* Carregar elementos para a lista */
    private fun populateList(data : String, inicio: String, fim : String){

        val inicioLT = LocalTime.parse(inicio)
        val fimLT = LocalTime.parse(fim)
        if(inicioLT.isAfter(fimLT) && fim != "00:00"){
            alertaDanger(this@pesquisaSalas, "Atenção", getString(R.string.erro_intervalo_indisponivel))
            return
        }else if(inicio == "00:00"){
            alertaDanger(this@pesquisaSalas, "Atenção", getString(R.string.erro_selecionar_hora))
            return
        }

        getSalasByDateTime(object: VolleyCallBack {

            override fun onSuccess() {
                val listData = findViewById<ListView>(R.id.listData)

                if(listSalasDatetime.size != 0){
                    listData.visibility = View.VISIBLE
                    if(inicio != "00:00" && fim == "00:00"){ //Pesquisa por Data/Inicio
                        listData.adapter = MyCustomAdapter(applicationContext, listSalasDatetime, false, 1, data)

                    }else if(inicio != "00:00" && fim != "00:00"){ //Pesquisa por Data/Inicio/Fim
                        val ListAdapter = MyCustomAdapter(applicationContext, listSalasDatetime, false, 2, data)
                        listData.adapter = ListAdapter
                        ListAdapter.setOnClickListener(object : MyCustomAdapter.MyCallback {
                            override fun onListClick(nsala : String, intervalo : Long, lugares : Int, alocacao :Int) {
                                envio_dados(session.getUser().toString(), setStart, setEnd, setDate, nsala, intervalo, lugares, alocacao)
                            }

                        })
                    }
                }else{
                    listData.visibility = View.GONE
                }

            }
            override fun onFailure(error: String) {
                Log.i("Salas", error)
                alertaDanger(this@pesquisaSalas, "Atenção", getString(R.string.erro_mostrar_salas))
            }
        }, data, inicio, fim)
    }

    /* Envio de dados para a backend para validar uma reserva */
    fun envio_dados(nutilizador : String, inicio : String, fim : String, data : String, nsala : String, intervalo : Long, lugares : Int, alocacao :Int){
        val requestQueue = Volley.newRequestQueue(this)
        val strRequest: StringRequest = object : StringRequest(
            Method.POST, appSettings.URLvalidarReserva + session.getEstabelecimento(),
            Response.Listener { response ->
                if(response.toBoolean()){
                    val dialog = AlertDialog.Builder(this)
                    val dialogView = layoutInflater.inflate(R.layout.dialog_pergunta, null)
                    val btnVoltar = dialogView.findViewById<Button>(R.id.dialogVoltar)
                    val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
                    val txtCapacidade = dialogView.findViewById<TextView>(R.id.txtCapacidade)

                    val lugaresFinal = Math.floor( (lugares * alocacao / 100).toDouble() ).roundToInt()
                    if(alocacao < 100){
                        txtCapacidade.text = "A capacidade máxima da sala esta limitada a $lugaresFinal Lugares"
                    }else txtCapacidade.text = "A capacidade máxima é de $lugaresFinal Lugares"

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
                        fazerReserva(nutilizador, inicio, fim, data, nsala, intervalo)
                    }

                }else{
                    alertaDanger(this@pesquisaSalas, "Atenção", getString(R.string.erro_intervalo_indisponivel))
                }
            },
            Response.ErrorListener { error ->
                alertaDanger(this@pesquisaSalas, "Atenção", getString(R.string.erro_geral))
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                val horafim = LocalTime.parse(fim).plusMinutes(intervalo)
                params["nutilizador"] = nutilizador
                params["inicio"] = inicio;
                params["fim"] = horafim.toString()
                params["data"] = data
                params["nsala"] = nsala
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }
        }
        requestQueue.add(strRequest)
    }

    /* Envio de dados para a backend para registar uma reserva */
    fun fazerReserva(nutilizador : String, inicio : String, fim : String, data : String, nsala : String, intervalo : Long){
        val requestQueue = Volley.newRequestQueue(this)
        val strRequest: StringRequest = object : StringRequest(
            Method.POST, appSettings.URLaddReserva,
            Response.Listener { response ->
                if(response.toBoolean()){
                    alertaSuccess(this@pesquisaSalas, "Sucesso", getString(R.string.sucesso_add_reserva))
                    Handler(Looper.getMainLooper()).postDelayed({
                        val i = Intent(applicationContext, MinhasReservas::class.java)
                        startActivity(i)
                    }, 1500)
                }else{
                    alertaDanger(this@pesquisaSalas, "Atenção", getString(R.string.erro_add_reserva))
                }
            },
            Response.ErrorListener { error ->
                alertaDanger(this@pesquisaSalas, "Atenção", getString(R.string.erro_geral))
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                val horafim = LocalTime.parse(fim).plusMinutes(intervalo)
                params["nsala"] =  nsala
                params["nutilizador"] = nutilizador
                params["datareserva"] = data
                params["horainicio"] = inicio
                params["horafim"] = horafim.toString()
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }
        }
        requestQueue.add(strRequest)
    }

    fun tabSala(){
        binding.tabSala.visibility = View.VISIBLE
        binding.tabData.visibility = View.GONE
        listSalas.clear()
        getSalas(object: VolleyCallBack {
            override fun onSuccess() {
                val listView = findViewById<ListView>(R.id.pesquisa_listview)
                if(listSalas.size != 0){
                    listView.adapter = MyCustomAdapter(applicationContext, listSalas, true, 0, "")
                    listView.visibility = View.VISIBLE
                    // titleHoje.visibility = View.VISIBLE
                }else{
                    listView.visibility = View.GONE
                    //  titleHoje.visibility = View.GONE
                }
            }
            override fun onFailure(error: String) {
                Log.i("Salas", error)
                alertaDanger(this@pesquisaSalas, "Atenção", getString(R.string.erro_mostrar_salas))
            }
        })
    }

    fun emptyLayout(status : Boolean){
        if(status){
            binding.nenhumResultado.visibility = View.VISIBLE
        }else binding.nenhumResultado.visibility = View.GONE
    }

    /* Verificar se a backend esta online, caso contrario mostrar aviso de erro */
    fun callBackend(){
        val url = appSettings.callBackend
        val requestQueue = Volley.newRequestQueue(applicationContext)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error ->
            emptyLayout(true)
            val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            val dialogView = layoutInflater.inflate(R.layout.dialog_connection, null)
            val btnVoltar = dialogView.findViewById<Button>(R.id.dialogVoltar)
            dialog.setView(dialogView)
            dialog.setCancelable(true)
            val customDialog = dialog.create()
            customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            customDialog.show()
            btnVoltar.setOnClickListener {
                customDialog.dismiss()
            }
        })
        requestQueue.add(request)
    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val menuItem = bottomNavigationView.menu.getItem(1)
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
        }
        callBackend()
    }

}