package com.example.appmobile

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.appmobile.databinding.ActivityEditBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.himanshoe.kalendar.common.data.KalendarEvent
import com.squareup.picasso.Picasso
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.HashMap
import kotlin.math.roundToInt

class EditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBinding
    lateinit var session : SessionManager
    private var listEvents = ArrayList<KalendarEvent>()
    data class Reservas(val nreserva : Int, val datareserva : String, val horainicio : String, val horafim : String, val estadoreserva : Int, val sala : String, val nsala : Int, val lugares : Int, val alocacao : Int, val descricao : String)
    private var listReservas = ArrayList<Reservas>()
    data class Salas(val nsala: Int, val nestado: Int, val sala: String, val lugares: Int, val estadosala: Int, val descricao: String, val alocacao: Int, val intervalolimpeza: Int)
    private var listSalas = ArrayList<Salas>()

    lateinit var dataFinal : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

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

        /* Botao Notificacoes */
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

        /* Voltar */
        val voltarBtn = findViewById<TextView>(R.id.goback)
        voltarBtn.setOnClickListener {
            finish()
        }

        //Obter texto do Extra
        val bundle = intent.extras
        val sala = bundle?.getString("sala")
        val data = bundle?.getString("data")
        val inicio = bundle?.getString("inicio")
        val fim = bundle?.getString("fim")
        val imagem = bundle?.getString("imagem")

        //Obter Dia e Mes formatados
        val delim = "-"
        val dataSeparada = data?.split(delim)

        //Atualizar campos
        binding.mudarSala.text = sala
        binding.txtDia.text = dataSeparada!![0]
        binding.txtMes.text = getMonth(dataSeparada[1].toInt())
        binding.txtComeco.text = inicio
        binding.txtFim.text = fim
        dataFinal = data
        Picasso.get().load(appSettings.URL_imagem_sala + imagem)
            .resize(600, 200) // resizes the image to these dimensions (in pixel)
            .centerCrop()
            .placeholder(R.drawable.sala_exemplo)
            .error(R.drawable.sala_exemplo)
            .into(binding.imagemSala);

        /* Obter Reservas */
        getReserva(object: VolleyCallBack {
            val progessBar = findViewById<LinearProgressIndicator>(R.id.progessBarEditar)
            override fun onSuccess() {
                val idReserva = listReservas[0].nreserva
                val lugares = listReservas[0].lugares
                val alocacao = listReservas[0].alocacao
                val inicio = listReservas[0].horainicio
                val fim = listReservas[0].horafim
                val lugaresFinal = Math.floor( (lugares.toInt() * alocacao / 100).toDouble() ).roundToInt()
                binding.lugares.text = "$lugaresFinal Lugares"
                binding.desc.text = listReservas[0].descricao
                binding.mudarSala.isEnabled = false

                atualizarSala(idReserva.toString())

                /* Sistema Data */
                binding.dataEdit.setOnClickListener {

                    obterDatas(listReservas[0].nreserva.toString(), listReservas[0].nsala.toString(), listReservas[0].datareserva, inicio, fim)

                }

            }

            override fun onFailure(error: String) {
                progessBar.visibility = View.GONE
            }

        })

        binding.horasEdit.setOnClickListener {
            val bundle = intent.extras
            val nsala = bundle?.getString("nsala")
            val nreserva = bundle?.getString("nreserva")
            val i = Intent(applicationContext, EditActivityCalendar::class.java)
            i.putExtra("nsala", nsala)
            i.putExtra("data", dataFinal)
            i.putExtra("nreserva", nreserva)
            startActivity(i)
        }

    }
    /* Converter Int mes para String */
    fun getMonth(mes : Int) : String{
        return when(mes){
            1 -> "Janeiro"
            2 -> "Fevreiro"
            3 -> "Março"
            4 -> "Abril"
            5 -> "Maio"
            6 -> "Junho"
            7 -> "Julho"
            8 -> "Agosto"
            9 -> "Setembro"
            10 -> "Outubro"
            11 -> "Novembro"
            12 -> "Dezembro"
            else -> "Erro"
        }
    }

    fun atualizarSala(nreserva: String){
        val progessBar = findViewById<LinearProgressIndicator>(R.id.progessBarEditar)
        obterSalas(object: VolleyCallBack {
            override fun onSuccess() {
                binding.mudarSala.isEnabled = true
                progessBar.visibility = View.GONE
                val popupMenu = PopupMenu(applicationContext, binding.mudarSala)

                //Inserir Salas com Disponbilidade
                for(item in listSalas){
                    popupMenu.menu.add(item.sala)
                }

                popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->

                    //Mostrar Dialog
                    val dialog = AlertDialog.Builder(this@EditActivity)
                    val dialogView = layoutInflater.inflate(R.layout.dialog_pergunta_edit, null)
                    val btnVoltar = dialogView.findViewById<Button>(R.id.dialogVoltar)
                    val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
                    val txtChange = dialogView.findViewById<TextView>(R.id.txtChange)
                    val s = "A reunião será feita na <b>" + menuItem.title + "</b>"
                    txtChange.text = Html.fromHtml(s)
                    dialog.setView(dialogView)
                    dialog.setCancelable(true)
                    val customDialog = dialog.create()
                    customDialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                    customDialog.show()
                    btnVoltar.setOnClickListener {
                        customDialog.dismiss()
                    }
                    btnContinuar.setOnClickListener {
                        customDialog.dismiss()
                        var id = 0
                        for(item in listSalas)
                            if(item.sala != menuItem.title)
                                id++
                            else break

                        binding.mudarSala.text = listSalas[id].sala
                        val lugares = listSalas[id].lugares
                        val alocacao = listSalas[id].alocacao
                        val lugaresFinal = Math.floor( (lugares.toInt() * alocacao / 100).toDouble() ).roundToInt()
                        binding.lugares.text = "$lugaresFinal Lugares"
                        binding.desc.text = listSalas[id].descricao
                        editSala(nreserva, listSalas[id].nsala.toString())
                    }
                    false
                }
                binding.mudarSala.setOnClickListener {
                    popupMenu.show()
                }
            }
            override fun onFailure(error: String) {
                progessBar.visibility = View.GONE
            }

        }, session.getEstabelecimento().toString(), listReservas[0].datareserva, listReservas[0].horainicio, listReservas[0].horafim)
    }

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure(error: String)
    }
    /* Obter datas a bloquear no calendario e mostrar calendario */
    fun obterDatas(nreserva: String, nsala : String, dataold : String, inicio : String, fim : String){
        val url = appSettings.URLdatasEdit
        val params = HashMap<String, String>()
        params["nsala"] = nsala
        params["inicio"] = inicio
        params["fim"] = fim
        listEvents.clear()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {
            if(response.getBoolean("status")){
                val jsonArray = response.getJSONArray("" + "data")
                for (i in 0 until jsonArray.length()) {
                    val distr = jsonArray.getJSONObject(i)
                    val datareserva = distr.getString("datareserva")
                    listEvents.add(KalendarEvent(LocalDate.parse(datareserva), "indisponível", ""))
                }
                var dialog = FireyCalendar()
                dialog.setEvents(listEvents)
                dialog.setValues(dataold)
                dialog.show(supportFragmentManager, "TAG")
                dialog.setOnClickListener(object : FireyCalendar.MyCallback{
                    override fun onListClick(data: String) {
                        if(dataold != data)
                            editData(nreserva, nsala, data, inicio, fim)
                        else{
                            alertaDanger(this@EditActivity, "Atenção", getString(R.string.erro_data_indisponivel))
                        }
                    }

                })
            }else{
                Log.i("Edit", "Retornou falso");
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> Log.i("Edit", error.printStackTrace().toString()); })
        requestQueue.add(request)
    }
    /* Enviar um pedido ao servidor para mudar a data da reserva */
    fun editData(nreserva : String, nsala : String, data : String, inicio : String, fim : String){
        val requestQueue = Volley.newRequestQueue(this)
        val strRequest: StringRequest = object : StringRequest(
            Method.PUT, appSettings.URLeditReservaData + nreserva,
            Response.Listener { response ->
                if(response.toBoolean()){
                    alertaSuccess(this@EditActivity, "Sucesso", getString(R.string.edit_sucesso))
                    val delim = "-"
                    val dataSeparada = data?.split(delim)
                    //Atualizar campos
                    binding.txtDia.text = dataSeparada!![2]
                    binding.txtMes.text = getMonth(dataSeparada[1].toInt())
                    dataFinal = data

                    atualizarSala(nreserva)

                }else{
                    alertaDanger(this@EditActivity, "Atenção", getString(R.string.edit_reserva_erro))
                }
            },
            Response.ErrorListener { error ->
                Log.i("Edit", error.toString())
                alertaDanger(this@EditActivity, "Atenção", getString(R.string.erro_geral))
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["nsala"] = nsala
                params["inicio"] = inicio
                params["data"] = data
                params["fim"] = fim
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }
        }
        requestQueue.add(strRequest)
    }
    /* Obter informações da reserva a editar */
    private fun getReserva(callback: VolleyCallBack?) {
        val progessBar = findViewById<LinearProgressIndicator>(R.id.progessBarEditar)
        progessBar.visibility = View.VISIBLE
        val bundle = intent.extras
        val nreserva = bundle?.getString("nreserva")
        Log.i("Edit", nreserva.toString())
        val url = appSettings.URLgetbypk + nreserva.toString()
        listReservas.clear()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {
                    val salaarray = response.getJSONObject("sala")
                    //val nreserva : Int, val datareserva : String, val horainicio : String, val horafim : String, val estadoreserva : Int, val sala : String
                    val nreserva = response.getInt("nreserva")
                    val nsala = response.getInt("nsala")
                    val datareserva = response.getString("datareserva")
                    val horainicio = response.getString("horainicio")
                    val horafim = response.getString("horafim")
                    val estadoreserva = response.getInt("estado")
                    var formatted = ""
                    val current = LocalDate.parse(datareserva)
                    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                    formatted = current.format(formatter).toString()
                    val sala = salaarray.getString("nomesala")
                    val lugares = salaarray.getInt("lugares")
                    val alocacao = salaarray.getInt("alocacao")
                    val descricao = salaarray.getString("descricao")
                    listReservas.add(Reservas(nreserva, formatted, horainicio, horafim, estadoreserva, sala, nsala, lugares, alocacao, descricao))
                    callback?.onSuccess()

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        },{ error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }
    /* Obter as salas que podem ser alteradas para a data e hora selecionadas
       Esta vai procurar as salas que na data x e intervalo y esta com disponibilidade para ser alterada.
    */
    fun obterSalas(callback: VolleyCallBack?, estabelecimento : String, data : String, inicio : String, fim : String){
        val url = appSettings.URL_salas_disponiveis
        listSalas.clear()
        val params = HashMap<String, String>()
        params["nestabelecimento"] = estabelecimento
        params["data"] = dataFinal
        params["inicio"] = inicio
        params["fim"] = fim
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {
            if(response.getBoolean("status")){
                val jsonArray = response.getJSONArray("" + "data")
                for (i in 0 until jsonArray.length()) {
                    val distr = jsonArray.getJSONObject(i)

                    val nsala = distr.getInt("nsala")
                    val nestado = distr.getInt("nestado")
                    val sala = distr.getString("sala")
                    val lugares = distr.getInt("lugares")
                    val estadosala = distr.getInt("estadosala")
                    val descricao = distr.getString("descricao")
                    val alocacao = distr.getInt("alocacao")
                    val intervalolimpeza = distr.getInt("intervalolimpeza")
                    listSalas.add(Salas(nsala,
                        nestado,
                        sala,
                        lugares,
                        estadosala,
                        descricao,
                        alocacao,
                        intervalolimpeza))
                    callback?.onSuccess();
                }
            }else{
                callback?.onFailure("Retornou falso");
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }
    /* Pedido ao servidor para mudar de sala */
    fun editSala(nreserva : String, nsala : String){
        val requestQueue = Volley.newRequestQueue(this)
        val strRequest: StringRequest = object : StringRequest(
            Method.PUT, appSettings.URLeditReservaSala + nreserva,
            Response.Listener { response ->
                if(response.toBoolean()){
                    alertaSuccess(this@EditActivity, "Sucesso", getString(R.string.edit_sucesso))
                }else{
                    alertaDanger(this@EditActivity, "Atenção", getString(R.string.edit_reserva_erro))
                }
            },
            Response.ErrorListener { error ->
                Log.i("Edit", error.toString())
                alertaDanger(this@EditActivity, "Atenção", getString(R.string.erro_geral))
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["nsala"] = nsala
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }
        }
        requestQueue.add(strRequest)
    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val menuItem = bottomNavigationView.menu.getItem(3)
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
        }
    }

}