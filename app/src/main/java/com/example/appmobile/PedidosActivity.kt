package com.example.appmobile

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.appmobile.databinding.ActivityPedidosBinding
import com.example.appmobile.models.Pedidos
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.imageview.ShapeableImageView
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.picasso.Picasso
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.HashMap

class PedidosActivity : AppCompatActivity() {
    lateinit var session : SessionManager
    private lateinit var binding: ActivityPedidosBinding

    private var listPedidos = ArrayList<Pedidos>()
    var tipoPedido = "aguarda"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPedidosBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        AndroidThreeTen.init(this)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        /* Botoes do Menu */
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.page_1 -> {
                    val OutrosActivity = Intent(applicationContext, outrosActivity::class.java)
                    startActivity(OutrosActivity)
                }
                R.id.page_2 -> {
                    val PedidosActivity = Intent(applicationContext, PedidosActivity::class.java)
                    startActivity(PedidosActivity)
                }
                R.id.page_3 -> {
                    session.LogoutUser()
                }
            }
            return@setOnItemSelectedListener true
        }

        binding.chipGroupChoice.setOnCheckedChangeListener { group, checkedId ->
            val chip: Chip? = group.findViewById(checkedId)
            chip?.let {chipView ->
                when(chip.text){
                    "Aguarda Serviço" -> tipoPedido = "aguarda"
                    "Em atraso" -> tipoPedido = "atraso"
                    "Finalizados" -> tipoPedido = "concluidos"
                }
                getTipoPedido()
            } ?: kotlin.run {
            }
        }

        val swipe : SwipeRefreshLayout = findViewById(R.id.swipeRefresh)

        swipe.setOnRefreshListener {
            getTipoPedido()
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

    }

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure(error: String)
    }

    /* Obter lista de reservas backend e carregar para a recyclerview */
    private fun getTipoPedido(){
        binding.progressPedidos.visibility = View.VISIBLE
        val swipe : SwipeRefreshLayout = findViewById(R.id.swipeRefresh)
        getPedidos(object: VolleyCallBack {
            override fun onSuccess() {
                binding.progressPedidos.visibility = View.GONE
                if(listPedidos.size != 0){
                    binding.nadaEncontrado.visibility = View.GONE
                    binding.recyclerPedidos.visibility = View.VISIBLE

                    val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(applicationContext)
                    binding.recyclerPedidos.layoutManager = layoutManager
                    val mainListAdapter = RecyclerOutros(listPedidos)
                    binding.recyclerPedidos.adapter = mainListAdapter

                    /* Callback para utilizar evento onclick fora do adaptador */
                    mainListAdapter.setOnClickListener(object :
                        RecyclerOutros.MyCallback {

                        override fun onListClick(npedido : String, tipo : String, descricao : String) {

                            if(tipo == "manutencao"){
                                val dialog = AlertDialog.Builder(this@PedidosActivity, R.style.CustomAlertDialog)
                                val dialogView = layoutInflater.inflate(R.layout.dialog_limpeza, null)
                                val dialogTerminar = dialogView.findViewById<Button>(R.id.dialogTerminar)
                                val dialogLimpa = dialogView.findViewById<Button>(R.id.dialogLimpa)
                                val dialogLimpaDesinfectada = dialogView.findViewById<Button>(R.id.dialogLimpaDesinfectada)
                                val descricaoPedido = dialogView.findViewById<TextView>(R.id.descricaoPedido)
                                if(descricao != ""){
                                    descricaoPedido.text = descricao
                                    descricaoPedido.visibility = View.VISIBLE
                                }else descricaoPedido.visibility = View.GONE
                                dialogLimpaDesinfectada.visibility = View.GONE
                                dialogLimpa.text = "Concluído"
                                dialog.setView(dialogView)
                                dialog.setCancelable(true)
                                val customDialog = dialog.create()
                                customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                customDialog.show()
                                dialogLimpa.setOnClickListener {
                                    terminarPedido(npedido, session.getUser().toString(), "concluido")
                                    customDialog.dismiss()
                                }
                                dialogTerminar.setOnClickListener {
                                    customDialog.dismiss()
                                }
                            }else{
                                val dialog = AlertDialog.Builder(this@PedidosActivity, R.style.CustomAlertDialog)
                                val dialogView = layoutInflater.inflate(R.layout.dialog_limpeza, null)
                                val dialogTerminar = dialogView.findViewById<Button>(R.id.dialogTerminar)
                                val dialogLimpa = dialogView.findViewById<Button>(R.id.dialogLimpa)
                                val dialogLimpaDesinfectada = dialogView.findViewById<Button>(R.id.dialogLimpaDesinfectada)
                                val descricaoPedido = dialogView.findViewById<TextView>(R.id.descricaoPedido)
                                if(descricao != ""){
                                    descricaoPedido.text = descricao
                                    descricaoPedido.visibility = View.VISIBLE
                                }else descricaoPedido.visibility = View.GONE
                                dialog.setView(dialogView)
                                dialog.setCancelable(true)
                                val customDialog = dialog.create()
                                customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                customDialog.show()
                                dialogLimpa.setOnClickListener {
                                    terminarPedido(npedido, session.getUser().toString(), "limpo")
                                    customDialog.dismiss()
                                }
                                dialogLimpaDesinfectada.setOnClickListener {
                                    terminarPedido(npedido, session.getUser().toString(), "desinfetado")
                                    customDialog.dismiss()
                                }
                                dialogTerminar.setOnClickListener {
                                    customDialog.dismiss()
                                }
                            }

                        }

                    })

                }else{
                    binding.recyclerPedidos.visibility = View.GONE
                    binding.nadaEncontrado.visibility = View.VISIBLE
                }
                swipe.isRefreshing = false
            }
            override fun onFailure(error: String) {
                Log.i("Pedidos", error)
                binding.progressPedidos.visibility = View.GONE
                binding.recyclerPedidos.visibility = View.GONE
                binding.nadaEncontrado.visibility = View.VISIBLE
                swipe.isRefreshing = false
            }
        })
    }


    /* Criação de uma recyclerview customizada */
    internal class RecyclerOutros(private val Pedidos : ArrayList<Pedidos>) : RecyclerView.Adapter<RecyclerOutros.MyViewHolder>() {

        internal interface MyCallback {
            fun onListClick(npedido : String, tipo : String, descricao : String)
        }

        var mItemClickListener: MyCallback? = null
        fun setOnClickListener(click: MyCallback) {
            mItemClickListener = click
        }

        internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var titlePedido: TextView = view.findViewById(R.id.titlePedido)
            var horario: TextView = view.findViewById(R.id.horario)
            var estado: TextView = view.findViewById(R.id.estado)
            var tipoServico: ShapeableImageView = view.findViewById(R.id.tipoServico)
            var cardColor: LinearLayout = view.findViewById(R.id.cardColor)
            val cardbtn: MaterialCardView = view.findViewById(R.id.card)
            val arrowRight: ImageView = view.findViewById(R.id.arrowRight)

        }
        @NonNull
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pedidos, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

            holder.titlePedido.text = "Pedido na " + Pedidos[position].sala
            holder.horario.text = Pedidos[position].horainicio + " - " + Pedidos[position].horafim

            var estadoTxt = "Aguarda Serviço"

            when(Pedidos[position].tipo){
                "limpeza" -> {
                    holder.tipoServico.setImageResource(R.drawable.dust)
                    estadoTxt = "Aguarda Limpeza"
                }
                "manutencao" -> {
                    holder.tipoServico.setImageResource(R.drawable.wrench)
                    estadoTxt = "Aguarda Manutenção"
                }
                "diversos" -> {
                    holder.tipoServico.setImageResource(R.drawable.menu)
                }
                else ->  holder.tipoServico.setImageResource(R.drawable.menu)
            }

            when(Pedidos[position].terminado){
                "aguarda" -> {
                    holder.estado.text = estadoTxt
                    holder.estado.compoundDrawables[0]?.setTint(Color.parseColor("#2563EB"))
                    holder.cardColor.setBackgroundResource(R.drawable.border_left)
                    holder.arrowRight.visibility = View.VISIBLE
                }
                "atraso" -> {
                    holder.estado.text = "Em atraso"
                    holder.estado.compoundDrawables[0]?.setTint(Color.parseColor("#d92929"))
                    holder.cardColor.setBackgroundResource(R.drawable.border_left_red)
                    holder.arrowRight.visibility = View.VISIBLE
                }
                "cancelado" -> {
                    holder.estado.text = "Cancelada"
                    holder.estado.compoundDrawables[0]?.setTint(Color.parseColor("#b5b5b5"))
                    holder.cardColor.setBackgroundResource(R.drawable.border_left_grey)
                }
                "concluido" -> {
                    holder.estado.text = "Concluído"
                    holder.estado.compoundDrawables[0]?.setTint(Color.parseColor("#4CAF50"))
                    holder.cardColor.setBackgroundResource(R.drawable.border_left_green)
                }
                "limpo" -> {
                    holder.estado.text = "Limpa"
                    holder.estado.compoundDrawables[0]?.setTint(Color.parseColor("#4CAF50"))
                    holder.cardColor.setBackgroundResource(R.drawable.border_left_green)
                }
                "desinfetado" -> {
                    holder.estado.text = "Limpa e Desinfetada"
                    holder.estado.compoundDrawables[0]?.setTint(Color.parseColor("#4CAF50"))
                    holder.cardColor.setBackgroundResource(R.drawable.border_left_green)
                }else -> {
                    holder.estado.text = "Indefinido"
                    holder.estado.compoundDrawables[0]?.setTint(Color.parseColor("#2563EB"))
                    holder.cardColor.setBackgroundResource(R.drawable.border_left)
                }
            }

            if(Pedidos[position].terminado == "aguarda" || Pedidos[position].terminado == "atraso"){
                holder.cardbtn.setOnClickListener(View.OnClickListener { v ->
                    mItemClickListener?.onListClick(Pedidos[position].npedido.toString(), Pedidos[position].tipo, Pedidos[position].descricao)
                })
            }

        }
        override fun getItemCount(): Int {
            return Pedidos.size
        }

    }

    /* Obter lista de reserva de hoje pela backend */
    private fun getPedidos(callback: VolleyCallBack?) {
        val url = appSettings.URL_list_tipos + session.getEstabelecimento()

        listPedidos.clear()

        val params = HashMap<String, String>()
        params["tipo"] = tipoPedido

        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {
            val jsonArray = response.getJSONArray("data")
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)

                var nutilizador = 0
                var utilizador = ""

                if(distr.has("utilizador") &&  !distr.isNull("utilizador")){
                    val users = distr.getJSONObject("utilizador")
                    nutilizador = distr.getInt("nutilizador")
                    utilizador = users.getString("utilizador")
                }

                val salas = distr.getJSONObject("sala")
                val npedido = distr.getInt("npedido")
                val nsala = distr.getInt("nsala")
                val sala = salas.getString("sala")
                val tipo = distr.getString("tipo")
                val descricao = distr.getString("descricao")
                val data = distr.getString("data")
                val horainicio = distr.getString("horainicio")
                val horafim = distr.getString("horafim")
                val terminado = distr.getString("terminado")

                listPedidos.add(Pedidos(npedido, nsala, sala, nutilizador, utilizador, tipo, descricao, data, horainicio, horafim, terminado))

            }
            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    /* Função para atualizar o estado de uma sala apos ter sido limpa */
    private fun finalizar(callback: VolleyCallBack?, npedido : String, nutilizador : String, terminado : String) {
        val url = appSettings.URL_finalizar_pedido
        val params = HashMap<String, String>()
        params["npedido"] = npedido
        params["nutilizador"] = nutilizador
        params["terminado"] = terminado

        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {
            if(response.getBoolean("success")){
                callback?.onSuccess();
            }else callback?.onFailure(response.getString("message"));
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    private fun terminarPedido(npedido : String, nutilizador : String, terminado : String){
        finalizar(object: VolleyCallBack {

            override fun onSuccess() {
                getTipoPedido()
                alertaSuccess(this@PedidosActivity, "Sucesso", getString(R.string.sucesso_pedido_terminado))
            }

            override fun onFailure(error: String) {
                alertaDanger(this@PedidosActivity, "Atenção", getString(R.string.erro_pedido_terminado))
            }

        }, npedido, nutilizador, terminado)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return false
    }

    override fun onResume() {
        super.onResume()
        getTipoPedido()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val menuItem = bottomNavigationView.menu.getItem(1)
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
        }
    }
}